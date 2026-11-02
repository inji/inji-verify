# inji-verify: Crewline context

[Crewline](https://github.com/mosip/mosip-labs/tree/develop/crewline) is a gated, evidence-first AI engineering workflow. Its
agents read this folder before they change this repository. The repository's own [`AGENTS.md`](../AGENTS.md) wins wherever it
is more specific.

| Folder | Role | Files |
|---|---|---|
| `development/` | developer | `local-context.md` (build, unit/component tests, coverage, CI, DB scripts, default change scope), `discovery.json` (machine facts) |
| `qa/` | SDET | `module-context.md` (API TestRig suite, ID capture), `known-issues.md`, `profile.yaml` (validator rules) |

Other roles (`product/`, `ui-ux/`, `devops/`, `security/`) are added when the module has something to say.

## Using it

Cloning this repository and opening it in Claude Code offers the Crewline plugin (`.claude/settings.json`). In Cursor,
`.cursor/` does the same job: `.cursor/rules/crewline.mdc` is the rule, and `.cursor/hooks.json` runs `.cursor/crewline.py`, which
keeps a cached copy of Crewline under `~/.crewline/`. Then ask for the work as usual. Agents follow the workflow, and your
approval is needed before every commit, push, PR or bug.

Prerequisites for the Cursor hooks: a `python` command (Python 3.8+), PyYAML and git. Without `python` the hooks cannot start, and
without PyYAML Cursor asks for confirmation on every read, write and shell command.

What the hooks do: they download Crewline from `mosip/mosip-labs` branch `develop`, refresh it at most once a day, and run it on
your machine. Once active, an agent cannot commit or push without a Crewline task. This applies to agent actions only; your own
terminal commands and git are not affected.

| Want | Do |
|---|---|
| Off for you in this clone (Claude Code) | `.claude/settings.local.json`: `{"enabledPlugins": {"crewline@mosip-labs": false}}` |
| Off for you in this clone (Cursor) | `.cursor/crewline.local.json`: `{"enabled": false}`. The always-on Cursor rule still tells the agent to run Crewline |
| Off for one session | start the tool with `CREWLINE=off` |
| Work files | `.crewline-work/` in your clone (task records, worktrees) |

Add `.crewline-work/` and your local settings files above to `.git/info/exclude`; never commit them.

## Freshness

Verified at `a4601a1ac7` (`develop`, 2026-11-02) with `crewline discover` and a re-run of the API test baseline. The backend and
verify-ui commands were run at `110c0529` (2026-10-29), as `development/local-context.md` states. Rows marked *Inferred* are still to
be confirmed by a run. The baseline and ID-capture values in `development/discovery.json` come from the unreviewed draft profile;
`qa/` holds the reviewed results. Refresh with the Crewline `module-onboarding` task when the build, suite or test layout changes.
