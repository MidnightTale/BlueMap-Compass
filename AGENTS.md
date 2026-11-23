# AGENTS.md — Awepeeps BlueMap Compass

Scope: this file applies to **every file in this repository**. Human overview: [README.md](README.md).
Repeatable local build/review: [.agents/skills/awepeeps-bluemap-compass-build-review/SKILL.md](.agents/skills/awepeeps-bluemap-compass-build-review/SKILL.md).

This is a maintained fork of MidnightTale/BlueMap-Compass. Keep the upstream
attribution. The upstream project publishes **no license** (see the
[License note](README.md#license) in the README), so this fork cannot grant one;
do not add a `LICENSE` file here.

## Purpose and layout

- Paper 26.3 / Java 25 plugin: sneak-right-click a compass to create, track, or remove BlueMap waypoints.
- Java sources: `src/main/java/fun/mntale/blueMapCompass/`; unit tests: `src/test/java/fun/mntale/blueMapCompass/`.
- Runtime resources: `src/main/resources/` — `paper-plugin.yml`, `config.yml`, `lodestone-waypoint.svg`.
- Reproducible build helper: `automation/build-bluemap-compass.sh`. Release flow: [RELEASING.md](RELEASING.md).
- Security policy: [SECURITY.md](SECURITY.md). Contribution guide: [CONTRIBUTING.md](CONTRIBUTING.md).

## Build and test

- Requires JDK 25; `just install` installs Temurin 25 and the pinned tools via mise.
- `just check` — format, workflow lint, one clean test+build.
- `just reproducible` — two clean builds, `cmp`, and the SHA-256; runs on `main` and on
  release pull requests (a PR that changes `CHANGELOG.md`).
- Reproducible containerized build: `bash automation/build-bluemap-compass.sh`.
- Output jar: `build/libs/awepeeps-bluemap-compass-1.4.1-awepeeps.12.jar`.
- Open changes as a pull request against `main`.

## Native defaults

- Prefer the Paper API and the BlueMap API already used in the code over new wrappers or dependencies.
- Convert imported marker coordinates with flow-math's native `Vector3d#toInt()` (exact double floor).
  Do not use an `(int)` cast (truncates toward zero) or `Vector3d#floor()` (narrows through `float`).

## Privacy and boundaries

- Keep the plugin name, permission node, config keys, and upstream attribution stable.
- This is a **public** repository: never add host paths, usernames, IPs, seeds, world coordinates,
  server or instance IDs, logs, config dumps, backups, or secrets.
- Exact deploy pins (source commit + jar SHA-256) live in the private minecraft-deployment
  repository, never here.

## Git hooks

Hooks are installed once per checkout with `just hooks` and run through Lefthook, loading the
shared jobs from `awepeeps/.github` at the `hooks-v2` tag. Agents never bypass them (C7.11):
no `LEFTHOOK=0`, no `git commit --no-verify`, and no `lefthook-local.yml`.
