---
name: full-review
description: >
  Run a complete quality review on the current working-tree changes by
  discovering and executing every available *-audit and *-review skill in
  parallel against the changed source files, then always running the native
  simplify skill on top to catch reuse and quality issues. Use this skill
  whenever the user asks to review code, audit a diff, check quality, run
  a full review, validate changes before pushing, or asks "is this ready to
  commit?" — including variations like "revisa el código", "audita los
  cambios", "haz un review", "review my changes", "review the diff",
  "audit this", "checkea la calidad", "haz un full-review", "revisión
  completa", "antes de commitear revisa", "is this clean?", "find issues
  in my code", "encuentra problemas en este código". This skill operates on
  the current working tree (staged + unstaged + untracked source files) and
  is independent of git workflows — invoke it at any point during
  development, not only before a commit or push.
---

# Full Code Review

Run an end-to-end quality review on the changes in the current working tree, combining language-specific audit/review skills with a native simplify pass.

## Why this matters

Catching quality, security, and simplification issues *before* they ship is dramatically cheaper than catching them after — in code review, in QA, or in production. This skill batches every available reviewer into a single parallel pass over the diff so the cost of "just check it" stays low enough to run often, not only at the very end of a feature.

The skill is intentionally **decoupled from git workflows**: a review mid-feature is just as valuable as one right before a commit. Don't wait until you're about to push to ask "is this OK?".

## Flow

### Step 1: Detect changes in the working tree

Run these commands in parallel to identify every modified file:

```bash
git status --porcelain
git diff --name-only
git diff --name-only --staged
git ls-files --others --exclude-standard
```

Build the list of changed source files by combining staged, unstaged, and untracked output. Deduplicate.

If the working tree is clean, tell the user "No changes to review" and stop.

### Step 2: Decide whether there is anything to review

**Skip the rest of this skill** when the changeset contains only:
- Documentation files (`.md`, `.txt`, `.rst`)
- Config or data files (`.json`, `.yml`, `.yaml`, `.toml`, `.ini`, `.env.example`)
- CI/build descriptors with no executable code

In that case, tell the user: "No source code changes detected — nothing to review." and stop.

If the diff contains source code in any language, continue.

### Step 3: Discover audit and review skills

Search for skills whose directory name matches `*-audit` or `*-review`. Each candidate must contain a `SKILL.md`.

1. **Local first** — search the project's `.claude/skills/` directory
2. **Global fallback** — only if no matches were found locally, search `~/.claude/skills/`

The local-first priority matters: a project-specific reviewer is closer to the team's actual conventions and should win over a generic one. If both locations have matches, the local set wins entirely — global is a fallback, not an additive set.

```bash
# Local search
ls -d .claude/skills/*-audit .claude/skills/*-review 2>/dev/null | while read d; do [ -f "$d/SKILL.md" ] && echo "$d"; done

# Global search (only if no local results)
ls -d ~/.claude/skills/*-audit ~/.claude/skills/*-review 2>/dev/null | while read d; do [ -f "$d/SKILL.md" ] && echo "$d"; done
```

If neither search returns matches, that's fine — skip to Step 5 (simplify still runs on its own).

### Step 4: Run audit and review skills in parallel

For every discovered skill, launch a subagent in parallel via the Agent tool. Running them concurrently keeps wall-clock time low even when several reviewers are configured.

Each subagent should:
- Read its skill's `SKILL.md` to understand what to check
- Apply those checks **only to the changed source files**, not the whole codebase — the diff is what we are reviewing
- Return findings categorized by severity: `Critical`, `Must Fix`, `Low`, `Informational`, `Nice to Have`

Wait for every subagent to finish before continuing. If one fails or returns nothing useful, log the failure and proceed with the others — a single broken reviewer should not block the rest of the review.

### Step 5: Run simplify (always)

Invoke the native `simplify` skill via the Skill tool (`skill: "simplify"`). It reviews the changed code for reuse, quality, and efficiency, and may rewrite files directly.

If simplify modifies files:
- List the files it touched
- Warn the user that the working tree changed; if they had staged changes, the staged snapshot no longer matches the working tree and they may want to re-stage

If simplify finds nothing to fix, continue silently.

### Step 6: Report consolidated findings

Aggregate everything into one report grouped by severity:

```
## Full Review Results

### Critical / Must Fix
- <file:line> — <skill> — <issue>
...

### Low / Informational
- <file:line> — <skill> — <issue>
...

### Simplify
- <file> — <what was rewritten or what was suggested>

### Skills run
- audit/review: <list of skill names>
- simplify: yes
```

Rules for the report:
- If **Critical / Must Fix** is non-empty, surface it prominently and ask the user how to proceed (fix now, ignore, address selectively). Don't paper over blocking findings.
- If only Low/Informational findings exist, show them as a brief summary — they are useful context but not blocking.
- If no findings at all, say so explicitly: "No issues found." A clean run is information too; don't hide it.

## Edge cases

- **No working tree changes**: see Step 1 — stop and tell the user.
- **Only docs/config changed**: see Step 2 — skip and inform.
- **No `*-audit` / `*-review` skill found anywhere**: run simplify only and mention which locations were searched, so the user can add a reviewer for the language if they want one.
- **Subagent fails**: report the failure inline but don't abort the run — other reviewers and simplify still complete.
- **Simplify modifies staged files**: warn explicitly. Their staged snapshot no longer matches the working tree.
- **Not in a git repo**: this skill assumes one. If `git` commands fail, tell the user and stop.

## What this skill does NOT do

- It does **not** commit, stage, or push anything. It is read-only with respect to git state (other than what simplify rewrites).
- It does **not** run tests or build the project. Use a separate test/build step for that.
- It does **not** decide whether to commit. The user decides, based on the report.
