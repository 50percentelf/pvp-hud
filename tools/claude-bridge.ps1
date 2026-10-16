param(
    [string]$Repo = "50percentelf/pvp-hud",
    [int]$PollSeconds = 10,
    [string]$BaseBranch = "master"
)

$ErrorActionPreference = "Stop"

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found in PATH."
    }
}

Require-Command git
Require-Command gh
Require-Command claude

$repoRoot = (git rev-parse --show-toplevel).Trim()
if (-not $repoRoot) { throw "Run this script from inside the pvp-hud repository." }

$stateRoot = Join-Path $env:LOCALAPPDATA "PvpHudClaudeBridge"
$worktreeRoot = Join-Path $stateRoot "worktrees"
$statePath = Join-Path $stateRoot "state.json"
New-Item -ItemType Directory -Force -Path $stateRoot, $worktreeRoot | Out-Null

if (Test-Path $statePath) {
    $state = Get-Content $statePath -Raw | ConvertFrom-Json -AsHashtable
} else {
    $state = @{}
}

function Save-State {
    $state | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 $statePath
}

$schema = @'
{"type":"object","properties":{"status":{"type":"string","enum":["completed","needs_input","blocked","failed"]},"summary":{"type":"string"},"questions":{"type":"array","items":{"type":"string"}},"changed_files":{"type":"array","items":{"type":"string"}},"tests":{"type":"array","items":{"type":"string"}},"blockers":{"type":"array","items":{"type":"string"}},"review_notes":{"type":"array","items":{"type":"string"}}},"required":["status","summary","questions","changed_files","tests","blockers","review_notes"]}
'@

function Get-IssueSnapshot([int]$Number) {
    $json = gh issue view $Number --repo $Repo --json number,title,body,comments,state,updatedAt,url
    return $json | ConvertFrom-Json
}

function Get-OpenClaudeIssues {
    $json = gh issue list --repo $Repo --state open --search 'in:title "[CLAUDE]"' --limit 100 --json number,title,updatedAt,url
    if (-not $json) { return @() }
    return @($json | ConvertFrom-Json)
}

function Ensure-Worktree([int]$Number) {
    $branch = "claude/issue-$Number"
    $path = Join-Path $worktreeRoot "issue-$Number"

    git -C $repoRoot fetch origin $BaseBranch --quiet

    if (-not (Test-Path $path)) {
        $remoteExists = $false
        git -C $repoRoot ls-remote --exit-code --heads origin $branch *> $null
        if ($LASTEXITCODE -eq 0) { $remoteExists = $true }

        if ($remoteExists) {
            git -C $repoRoot fetch origin $branch --quiet
            git -C $repoRoot worktree add $path $branch
            if ($LASTEXITCODE -ne 0) {
                git -C $repoRoot worktree add -b $branch $path "origin/$branch"
            }
        } else {
            git -C $repoRoot worktree add -b $branch $path "origin/$BaseBranch"
        }
    }

    return @{ Branch = $branch; Path = $path }
}

function Build-Prompt($issue) {
    $commentText = @()
    foreach ($c in @($issue.comments)) {
        $commentText += "--- COMMENT by $($c.author.login) at $($c.createdAt) ---`n$($c.body)"
    }

    return @"
You are the implementation worker for PvP HUD. Work only inside the current repository/worktree.

This task is controlled through GitHub issue #$($issue.number).

TITLE:
$($issue.title)

REQUIREMENTS:
$($issue.body)

FOLLOW-UP DISCUSSION:
$($commentText -join "`n`n")

RULES:
- Inspect the current code before editing.
- Preserve existing working behavior unless the task explicitly changes it.
- Prefer authoritative RuneLite/client state over heuristics.
- Do not guess product decisions. If a requirement is materially ambiguous, return status=needs_input and ask concise questions.
- Do not commit, push, merge, create PRs, or modify GitHub issues. The bridge handles Git operations and communication.
- You may edit files and run tests/builds locally.
- Keep Vengeance disabled unless this issue explicitly changes that decision.
- If claiming a mechanic is not detectable, explain what RuneLite/client signals you inspected and why none are reliable.
- Finish with the requested structured status object.
"@
}

function Invoke-ClaudeTask($issue, $worktree, $entry) {
    $prompt = Build-Prompt $issue
    Push-Location $worktree.Path
    try {
        if ($entry -and $entry.session_id) {
            $raw = & claude --resume $entry.session_id -p $prompt --output-format json --json-schema $schema --allowedTools "Read,Edit,Bash"
        } else {
            $raw = & claude -p $prompt --output-format json --json-schema $schema --allowedTools "Read,Edit,Bash"
        }
        if ($LASTEXITCODE -ne 0) { throw "Claude exited with code $LASTEXITCODE" }
        $outer = $raw | ConvertFrom-Json
        return @{
            SessionId = $outer.session_id
            Result = $outer.structured_output
        }
    }
    finally {
        Pop-Location
    }
}

function Commit-And-Push($issue, $worktree) {
    Push-Location $worktree.Path
    try {
        $changes = git status --porcelain
        if (-not $changes) { return $null }

        git add -A
        git commit -m "Claude task #$($issue.number): $($issue.title)"
        if ($LASTEXITCODE -ne 0) { throw "git commit failed" }

        $sha = (git rev-parse HEAD).Trim()
        git push -u origin $worktree.Branch
        if ($LASTEXITCODE -ne 0) { throw "git push failed" }
        return $sha
    }
    finally {
        Pop-Location
    }
}

function Post-BridgeComment([int]$Number, [string]$Body) {
    gh issue comment $Number --repo $Repo --body $Body | Out-Null
}

Write-Host "PvP HUD Claude bridge running. Repo=$Repo Poll=${PollSeconds}s" -ForegroundColor Green
Write-Host "Watching open issues whose title contains [CLAUDE]. Ctrl+C to stop." -ForegroundColor DarkGray

while ($true) {
    try {
        foreach ($summary in Get-OpenClaudeIssues) {
            $key = [string]$summary.number
            $issue = Get-IssueSnapshot $summary.number
            $commentCount = @($issue.comments).Count

            $entry = $null
            if ($state.ContainsKey($key)) { $entry = $state[$key] }

            if ($entry -and [int]$entry.comment_count -eq $commentCount -and $entry.issue_updated_at -eq $issue.updatedAt) {
                continue
            }

            Write-Host "Processing issue #$($issue.number): $($issue.title)" -ForegroundColor Cyan
            $worktree = Ensure-Worktree $issue.number
            $run = Invoke-ClaudeTask $issue $worktree $entry
            $result = $run.Result

            $commitSha = $null
            if ($result.status -eq "completed") {
                $commitSha = Commit-And-Push $issue $worktree
            }

            $lines = @(
                "<!-- CLAUDE_BRIDGE -->",
                "**Bridge status:** `$($result.status)`",
                "",
                $result.summary
            )

            if ($result.questions.Count -gt 0) {
                $lines += ""
                $lines += "**Questions for product owner / reviewer:**"
                foreach ($q in $result.questions) { $lines += "- $q" }
            }

            if ($result.tests.Count -gt 0) {
                $lines += ""
                $lines += "**Tests:**"
                foreach ($t in $result.tests) { $lines += "- $t" }
            }

            if ($result.blockers.Count -gt 0) {
                $lines += ""
                $lines += "**Blockers:**"
                foreach ($b in $result.blockers) { $lines += "- $b" }
            }

            if ($commitSha) {
                $lines += ""
                $lines += "**Task branch:** `$($worktree.Branch)`"
                $lines += "**Commit:** `$commitSha`"
                $lines += ""
                $lines += "No merge was performed. Review before merging."
            }

            Post-BridgeComment $issue.number ($lines -join "`n")

            # Re-read after our own comment so the bridge does not trigger itself.
            $fresh = Get-IssueSnapshot $issue.number
            $state[$key] = @{
                session_id = $run.SessionId
                comment_count = @($fresh.comments).Count
                issue_updated_at = $fresh.updatedAt
                branch = $worktree.Branch
                last_status = $result.status
            }
            Save-State
        }
    }
    catch {
        Write-Warning $_
    }

    Start-Sleep -Seconds $PollSeconds
}
