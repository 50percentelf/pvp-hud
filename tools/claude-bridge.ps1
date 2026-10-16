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

# PowerShell 5.1-compatible state loading. ConvertFrom-Json -AsHashtable is PowerShell 7+ only.
$state = @{}
if (Test-Path $statePath) {
    try {
        $parsedState = Get-Content $statePath -Raw | ConvertFrom-Json
        if ($parsedState) {
            foreach ($prop in $parsedState.PSObject.Properties) {
                $state[$prop.Name] = $prop.Value
            }
        }
    }
    catch {
        Write-Warning "Could not read existing bridge state; starting fresh. $($_.Exception.Message)"
        $state = @{}
    }
}

function Save-State {
    $state | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 $statePath
}

function Write-Dashboard {
    $issuesMap = @{}
    foreach ($k in $state.Keys) {
        $e = $state[$k]
        $issuesMap[$k] = @{
            number        = [int]$k
            title         = if ($e.title)          { [string]$e.title }          else { "" }
            url           = if ($e.url)             { [string]$e.url }            else { "" }
            status        = if ($e.last_status)     { [string]$e.last_status }    else { "unknown" }
            summary       = if ($e.summary)         { [string]$e.summary }        else { "" }
            branch        = if ($e.branch)          { [string]$e.branch }         else { "" }
            commitSha     = if ($e.commit_sha)      { [string]$e.commit_sha }     else { $null }
            questions     = if ($e.questions)       { @($e.questions) }           else { @() }
            blockers      = if ($e.blockers)        { @($e.blockers) }            else { @() }
            lastProcessed = if ($e.last_processed)  { [string]$e.last_processed } else { "" }
        }
    }
    $dashboard = @{ lastUpdated = (Get-Date -Format "o"); issues = $issuesMap }
    $dashboard | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 (Join-Path $stateRoot "dashboard.json")
}

$schema = @'
{"type":"object","properties":{"status":{"type":"string","enum":["completed","needs_input","blocked","failed"]},"summary":{"type":"string"},"questions":{"type":"array","items":{"type":"string"}},"changed_files":{"type":"array","items":{"type":"string"}},"tests":{"type":"array","items":{"type":"string"}},"blockers":{"type":"array","items":{"type":"string"}},"review_notes":{"type":"array","items":{"type":"string"}}},"required":["status","summary","questions","changed_files","tests","blockers","review_notes"]}
'@

function Get-IssueSnapshot([int]$Number) {
    $json = gh issue view $Number --repo $Repo --json number,title,body,comments,state,updatedAt,url
    if ($LASTEXITCODE -ne 0) { throw "gh issue view failed for issue #$Number" }
    return $json | ConvertFrom-Json
}

function Get-OpenClaudeIssues {
    # Avoid GitHub search parsing of square brackets. List open issues and filter titles locally.
    $json = gh issue list --repo $Repo --state open --limit 100 --json number,title,updatedAt,url
    if ($LASTEXITCODE -ne 0) { throw "gh issue list failed" }
    if (-not $json) { return @() }
    $issues = @($json | ConvertFrom-Json)
    return @($issues | Where-Object {
        $_.title -and $_.title.StartsWith("[CLAUDE]", [System.StringComparison]::OrdinalIgnoreCase)
    })
}

function Ensure-Worktree([int]$Number) {
    $branch = "claude/issue-$Number"
    $path = Join-Path $worktreeRoot "issue-$Number"

    git -C $repoRoot fetch origin $BaseBranch --quiet
    if ($LASTEXITCODE -ne 0) { throw "git fetch origin $BaseBranch failed" }

    if (-not (Test-Path $path)) {
        $remoteExists = $false
        git -C $repoRoot ls-remote --exit-code --heads origin $branch *> $null
        if ($LASTEXITCODE -eq 0) { $remoteExists = $true }

        if ($remoteExists) {
            git -C $repoRoot fetch origin $branch --quiet
            git -C $repoRoot worktree add $path $branch *> $null
            if ($LASTEXITCODE -ne 0) {
                git -C $repoRoot worktree add -b $branch $path "origin/$branch"
            }
        }
        else {
            git -C $repoRoot worktree add -b $branch $path "origin/$BaseBranch"
        }

        if ($LASTEXITCODE -ne 0) { throw "Failed to create worktree for issue #$Number" }
    }

    return @{ Branch = $branch; Path = $path }
}

function Build-Prompt($issue) {
    $commentText = @()
    foreach ($c in @($issue.comments)) {
        # Do not echo the bridge's own machine-generated comments back as requirements.
        if ($c.body -and $c.body.Contains("<!-- CLAUDE_BRIDGE -->")) { continue }
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
        $claudeArgs = @(
            "-p", $prompt,
            "--output-format", "stream-json",
            "--json-schema", $schema,
            "--permission-mode", "bypassPermissions",
            "--allowedTools", "Bash,Read,Edit,Write,Glob,Grep",
            "--disallowedTools", "Bash(git push *)", "Bash(git commit *)", "Bash(gh *)"
        )

        if ($entry -and $entry.session_id) {
            $claudeArgs = @("--resume", [string]$entry.session_id) + $claudeArgs
        }

        # Stream Claude output line-by-line so Electron can display live progress.
        $claudeExe = (Get-Command claude -ErrorAction Stop).Source
        $psi = [System.Diagnostics.ProcessStartInfo]::new($claudeExe)
        $psi.Arguments          = ($claudeArgs | ForEach-Object { if ($_ -match '\s') { "`"$_`"" } else { $_ } }) -join " "
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError  = $true
        $psi.UseShellExecute        = $false
        $psi.WorkingDirectory       = $worktree.Path

        $proc = [System.Diagnostics.Process]::Start($psi)

        $lines = [System.Collections.Generic.List[string]]::new()
        while (-not $proc.StandardOutput.EndOfStream) {
            $line = $proc.StandardOutput.ReadLine()
            $lines.Add($line)
            Write-Host $line  # streamed to Electron log panel
        }
        $proc.WaitForExit()

        # Drain stderr to log as well
        $stderr = $proc.StandardError.ReadToEnd()
        if ($stderr) { Write-Warning $stderr }

        if ($proc.ExitCode -ne 0) { throw "Claude exited with code $($proc.ExitCode)" }
        if ($lines.Count -eq 0)   { throw "Claude returned no output" }

        # stream-json: find the result event line
        $resultLine = $null
        for ($i = $lines.Count - 1; $i -ge 0; $i--) {
            try {
                $candidate = $lines[$i] | ConvertFrom-Json
                if ($candidate.type -eq "result") { $resultLine = $candidate; break }
            } catch {}
        }
        if (-not $resultLine)                { throw "Claude stream contained no result event" }
        if (-not $resultLine.structured_output) { throw "Claude result missing structured_output" }

        return @{
            SessionId = $resultLine.session_id
            Result    = $resultLine.structured_output
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
        if ($LASTEXITCODE -ne 0) { throw "git add failed" }

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
    if ($LASTEXITCODE -ne 0) { throw "Failed to post bridge comment on issue #$Number" }
}

$gitVersion = (git --version) -join " "
$ghVersion = ((gh --version | Select-Object -First 1) -join " ")
$claudeVersion = ((claude --version | Select-Object -First 1) -join " ")
Write-Host "PvP HUD Claude bridge running. Repo=$Repo Poll=${PollSeconds}s" -ForegroundColor Green
Write-Host "Repo root: $repoRoot" -ForegroundColor DarkGray
Write-Host "$gitVersion | $ghVersion | Claude $claudeVersion" -ForegroundColor DarkGray
Write-Host "Watching open issues whose title starts with [CLAUDE]. Ctrl+C to stop." -ForegroundColor DarkGray

while ($true) {
    try {
        $claudeIssues = Get-OpenClaudeIssues
        foreach ($summary in $claudeIssues) {
            $key = [string]$summary.number
            $issue = Get-IssueSnapshot $summary.number
            $commentCount = @($issue.comments).Count

            $entry = $null
            if ($state.ContainsKey($key)) { $entry = $state[$key] }

            if ($entry -and [int]$entry.comment_count -eq $commentCount -and [string]$entry.issue_updated_at -eq [string]$issue.updatedAt) {
                continue
            }

            Write-Host "Processing issue #$($issue.number): $($issue.title)" -ForegroundColor Cyan
            $worktree = Ensure-Worktree $issue.number
            Write-Host "  worktree: $($worktree.Path)" -ForegroundColor DarkGray
            Write-Host "  branch:   $($worktree.Branch)" -ForegroundColor DarkGray

            $run = Invoke-ClaudeTask $issue $worktree $entry
            $result = $run.Result

            $commitSha = $null
            if ($result.status -eq "completed") {
                $commitSha = Commit-And-Push $issue $worktree
            }

            $lines = @(
                "<!-- CLAUDE_BRIDGE -->",
                "**Bridge status:** ``$($result.status)``",
                "",
                $result.summary
            )

            if (@($result.questions).Count -gt 0) {
                $lines += ""
                $lines += "**Questions for product owner / reviewer:**"
                foreach ($q in @($result.questions)) { $lines += "- $q" }
            }

            if (@($result.changed_files).Count -gt 0) {
                $lines += ""
                $lines += "**Changed files:**"
                foreach ($f in @($result.changed_files)) { $lines += "- $f" }
            }

            if (@($result.tests).Count -gt 0) {
                $lines += ""
                $lines += "**Tests:**"
                foreach ($t in @($result.tests)) { $lines += "- $t" }
            }

            if (@($result.blockers).Count -gt 0) {
                $lines += ""
                $lines += "**Blockers:**"
                foreach ($b in @($result.blockers)) { $lines += "- $b" }
            }

            if (@($result.review_notes).Count -gt 0) {
                $lines += ""
                $lines += "**Review notes:**"
                foreach ($n in @($result.review_notes)) { $lines += "- $n" }
            }

            if ($commitSha) {
                $lines += ""
                $lines += "**Task branch:** ``$($worktree.Branch)``"
                $lines += "**Commit:** ``$commitSha``"
                $lines += ""
                $lines += "No merge was performed. Review before merging."
            }
            elseif ($result.status -eq "completed") {
                $lines += ""
                $lines += "No files changed; no commit or remote task branch was created."
            }

            Post-BridgeComment $issue.number ($lines -join "`n")

            # Re-read after our own comment so the bridge does not trigger itself.
            $fresh = Get-IssueSnapshot $issue.number
            $state[$key] = @{
                session_id       = $run.SessionId
                comment_count    = @($fresh.comments).Count
                issue_updated_at = $fresh.updatedAt
                branch           = $worktree.Branch
                last_status      = $result.status
                title            = $issue.title
                url              = $issue.url
                summary          = $result.summary
                questions        = @($result.questions)
                blockers         = @($result.blockers)
                commit_sha       = if ($commitSha) { $commitSha } else { $null }
                last_processed   = (Get-Date -Format "o")
            }
            Save-State
            Write-Dashboard

            Write-Host "Finished issue #$($issue.number): $($result.status)" -ForegroundColor Green
        }
    }
    catch {
        Write-Warning ("Bridge loop error: " + $_.Exception.Message)
    }

    Start-Sleep -Seconds $PollSeconds
}
