# ChatGPT ↔ Claude Code bridge

This bridge lets ChatGPT use GitHub as a control plane while Claude Code performs implementation work locally.

It is intentionally conservative:

- `master` is never modified by the bridge.
- Each GitHub issue gets its own `claude/issue-N` branch and isolated git worktree.
- Claude may edit files and run local builds/tests.
- Claude does **not** commit, push, merge, create PRs, or modify issues itself.
- The bridge commits/pushes completed work to the issue branch only.
- Nothing is automatically merged.
- Claude session IDs are retained locally so follow-up requirements continue the same task context.

## Prerequisites

On Windows, make sure these commands work in PowerShell:

```powershell
git --version
gh --version
claude --version
```

Authenticate GitHub CLI if necessary:

```powershell
gh auth login
```

Claude Code must already be authenticated locally.

## Start the bridge

From the local `pvp-hud` checkout:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\claude-bridge.ps1
```

Optional polling interval:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\claude-bridge.ps1 -PollSeconds 5
```

Leave this terminal open while you want ChatGPT and Claude to exchange work.

## Task protocol

The bridge watches **open GitHub issues whose title contains `[CLAUDE]`**.

Example:

```text
[CLAUDE] Redesign effect presentation around semantic slots
```

The issue body is the primary requirement document.

Comments are treated as follow-up instructions and clarification. This is what lets ChatGPT gather a new requirement from the user later and steer the same Claude session without starting over.

### Claude statuses

Claude must return one of:

- `completed` — work is complete; the bridge commits and pushes the issue branch.
- `needs_input` — product/UX/behavior decision is required. No commit is made.
- `blocked` — a technical dependency or missing signal prevents reliable implementation.
- `failed` — implementation attempt failed.

For `needs_input`, ChatGPT can read the issue response, ask the user only the unresolved product question, and add the answer as a new issue comment. The bridge then resumes the stored Claude session.

## Local state

Bridge state is stored outside the repository under:

```text
%LOCALAPPDATA%\PvpHudClaudeBridge
```

This contains:

- Claude session IDs
- per-issue worktrees
- last processed issue/comment metadata

It is intentionally not committed to the repository.

## Branch model

For GitHub issue `#42`:

```text
master
  └── claude/issue-42
```

The bridge creates an isolated worktree for the branch. This allows more than one Claude task to exist without sharing an edited working directory.

A successful issue response includes the branch and commit SHA for review.

## Recommended ChatGPT workflow

1. User describes behavior or design intent conversationally.
2. ChatGPT inspects current PvP HUD code and asks only necessary product questions.
3. ChatGPT creates or updates one `[CLAUDE]` issue containing acceptance criteria.
4. Local bridge sends it to Claude Code.
5. Claude edits/tests and reports either completion or questions.
6. ChatGPT reviews the resulting commit/diff and technical claims.
7. If Claude needs clarification, ChatGPT asks the user and posts the answer to the issue.
8. Only after review should the task branch be merged.

## Why issues instead of files in the repo?

Issues provide a durable append-only discussion thread without requiring requirement-only commits to `master`. They also let ChatGPT read/write requirements directly through its GitHub connection while Claude Code remains local to the machine.

## Safety / trust model

The bridge currently grants Claude these tool categories:

```text
Read, Edit, Bash
```

Claude is explicitly instructed not to perform GitHub/Git publishing operations. Git operations are performed by the bridge after Claude returns `completed`.

If you want an even stricter mode later, split planning and implementation into separate issue labels/modes and restrict Claude's allowed tools during planning.

## UI-development recommendation

For the status-effects redesign, do not hand Claude a request like "fit all timers." Treat the UI as a projection of semantic combat state:

- CONTROL / critical danger
- DAMAGE / harmful periodic state
- SUSTAIN / protection and buffs
- READINESS / cooldowns
- PERIODIC / next-proc events

Effects should be able to own a semantic slot with primary and secondary information rather than producing one independent row per timer. For example, Prayer Regeneration can show total duration and next proc in one visual unit, and Freeze can transition the same slot into refreeze immunity.

The local debug-state system should be used to create dense synthetic effect combinations so layouts can be reviewed without reproducing every effect in-game.
