#!/usr/bin/env python3
"""Crewline for Cursor: committed in a module as .cursor/crewline.py (from mosip-labs crewline/templates/module-cursor/).

Cursor has no plugin marketplace, so this file does the plugin's job: it keeps a cached copy of Crewline
(mosip/mosip-labs, folder crewline/) under ~/.crewline/, refreshes it at most once a day, and runs it.

  python .cursor/crewline.py hook session|shell|write|read     Cursor hooks (.cursor/hooks.json): stdin JSON -> stdout JSON
  python .cursor/crewline.py rule                         the current Crewline rule for this repository (the agent reads it)
  python .cursor/crewline.py where                        path of the cached Crewline core
  python .cursor/crewline.py <command> [args]             any Crewline validator, e.g. `manifest <m> --to DESIGN`

Off for one session: CREWLINE=off. Off for this clone, or a different source/ref for testing: .cursor/crewline.local.json
  {"enabled": false}   {"source": "https://github.com/<fork>/mosip-labs.git", "ref": "<branch>"}
(this file adds .cursor/crewline.local.json and .crewline-work/ to .git/info/exclude). Standard library only.
"""
from __future__ import annotations

import hashlib
import json
import os
import subprocess
import sys
import time
from pathlib import Path

SOURCE = "https://github.com/mosip/mosip-labs.git"
REF = "develop"
REFRESH_SECONDS = 24 * 3600
HERE = Path(__file__).resolve().parent              # <module>/.cursor
REPO = HERE.parent
UNAVAILABLE = ("Crewline could not be fetched ({why}); this session runs without it. Retry: python .cursor/crewline.py where")


def local() -> dict:
    try:
        return json.loads((HERE / "crewline.local.json").read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def disabled(cfg: dict) -> bool:
    return str(os.environ.get("CREWLINE", "")).lower() in ("off", "0", "false", "disabled") or cfg.get("enabled") is False


def _git(*args: str, cwd: Path | None = None, timeout: int = 60) -> subprocess.CompletedProcess:
    return subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True, timeout=timeout)


def exclude_local_files() -> None:
    try:
        common = _git("rev-parse", "--path-format=absolute", "--git-common-dir", cwd=REPO, timeout=10).stdout.strip()
        if not common:                                  # not a git repository: nothing to exclude from
            return
        ex = Path(common) / "info" / "exclude"
        have = ex.read_text(encoding="utf-8").splitlines() if ex.is_file() else []
        add = [p for p in (".cursor/crewline.local.json", ".crewline-work/") if p not in have]
        if add:
            ex.parent.mkdir(parents=True, exist_ok=True)
            with ex.open("a", encoding="utf-8") as f:
                f.write("".join(p + "\n" for p in add))
    except (OSError, subprocess.SubprocessError):
        pass


def core(cfg: dict) -> tuple[Path | None, str]:
    """The cached Crewline core (<cache>/repo/crewline), fetched or refreshed when due. Never raises."""
    if os.environ.get("CREWLINE_CORE"):                                    # development: a working copy of crewline/
        p = Path(os.environ["CREWLINE_CORE"])
        return (p, "") if (p / "validators" / "crewline_cli.py").is_file() else (None, f"CREWLINE_CORE={p} has no Crewline")
    source = cfg.get("source") or os.environ.get("CREWLINE_SOURCE") or SOURCE
    ref = cfg.get("ref") or os.environ.get("CREWLINE_REF") or REF
    home = Path(os.environ.get("CREWLINE_HOME") or Path.home() / ".crewline")
    key = hashlib.sha1(f"{source}@{ref}".encode("utf-8")).hexdigest()[:12]      # short: Windows path limit
    repo = home / "cache" / key
    target = repo / "crewline"
    stamp = repo / ".crewline-fetched"
    every = int(os.environ.get("CREWLINE_REFRESH_SECONDS", REFRESH_SECONDS))
    have = (target / "validators" / "crewline_cli.py").is_file()
    due = not have or not stamp.is_file() or time.time() - stamp.stat().st_mtime >= every
    if not due:
        return target, ""
    lock = home / "cache" / (key + ".lock")
    try:
        home.joinpath("cache").mkdir(parents=True, exist_ok=True)
        fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)          # one fetch at a time; others use what exists
    except FileExistsError:
        if time.time() - lock.stat().st_mtime > 300:
            lock.unlink(missing_ok=True)
        return (target, "") if have else (None, "another Crewline fetch is running")
    except OSError as e:
        return (target, "") if have else (None, str(e))
    try:
        os.close(fd)
        if not (repo / ".git").is_dir():
            r = _git("clone", "--quiet", "--depth", "1", "--filter=blob:none", "--sparse", "--branch", ref, source, str(repo))
            if r.returncode != 0:
                return (None, r.stderr.strip().splitlines()[-1] if r.stderr.strip() else "git clone failed")
            _git("sparse-checkout", "set", "crewline", ".claude-plugin", cwd=repo)
        else:
            r = _git("fetch", "--quiet", "--depth", "1", "origin", ref, cwd=repo)
            if r.returncode == 0:
                _git("checkout", "--quiet", "--force", "--detach", "FETCH_HEAD", cwd=repo)
            elif not have:
                return None, r.stderr.strip() or "git fetch failed"
        stamp.touch()
        ok = (target / "validators" / "crewline_cli.py").is_file()
        return (target, "") if ok else (None, f"{source}@{ref} has no crewline/ folder")
    except (OSError, subprocess.SubprocessError) as e:
        return (target, "") if have else (None, str(e))
    finally:
        lock.unlink(missing_ok=True)


def run_core(path: Path, args: list[str], stdin: str | None = None) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, "-X", "utf8", str(path / "validators" / "crewline_cli.py"), *args],
                          input=stdin, capture_output=True, text=True, encoding="utf-8", cwd=str(REPO))


def hook(mode: str) -> int:
    raw = sys.stdin.read()
    cfg = local()
    allow = {"permission": "allow"}
    if disabled(cfg):
        print(json.dumps({} if mode == "session" else allow))
        return 0
    exclude_local_files()
    path, why = core(cfg)
    if path is None:                    # offline or no git: never block the user's work because Crewline is missing
        print(json.dumps({"additional_context": UNAVAILABLE.format(why=why)} if mode == "session" else allow))
        return 0
    r = run_core(path, ["hook", "--cursor", "--plugin", mode], raw)
    if r.returncode == 0 and not r.stdout.strip():      # Crewline has nothing to say here (repository without .crewline/)
        print(json.dumps({} if mode == "session" else allow))
        return 0
    if r.returncode != 0:
        msg = f"Crewline hook failed ({(r.stderr.strip().splitlines() or ['no output'])[-1]}); fix it (e.g. pip install pyyaml)"
        if mode == "session":
            print(json.dumps({"additional_context": msg}))
        else:                           # fail closed, but let the user decide instead of blocking every command
            print(json.dumps({"permission": "ask", "user_message": msg, "agent_message": msg}))
        return 0
    sys.stdout.write(r.stdout)
    return 0


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 0
    if argv[0] == "hook":
        return hook(argv[1] if len(argv) > 1 else "session")
    cfg = local()
    path, why = core(cfg)
    if path is None:
        print(UNAVAILABLE.format(why=why), file=sys.stderr)
        return 2
    if argv[0] == "where":
        print(path)
        return 0
    if argv[0] == "rule":
        argv = ["cursor-rule"]
    r = run_core(path, argv)
    sys.stdout.write(r.stdout)
    sys.stderr.write(r.stderr)
    return r.returncode


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
