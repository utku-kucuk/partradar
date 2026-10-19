# Contributing

## Workflow
- `main` is protected and always deployable. Every change goes through a pull request.
- Branch from `main` using a short-lived branch: `feat/<scope>-<topic>`, `fix/...`, `chore/...`, `docs/...`, `ci/...`, `refactor/...`.
- PRs are **squash-merged**. The PR title becomes the commit message, so it must follow
  [Conventional Commits](https://www.conventionalcommits.org), e.g. `feat(inquiry): route part requests to nearby shops`.
- Link the issue being solved (`Closes #12`). CI must be green before merging.

## Commit types
`feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`.
Releases, version tags and the changelog are generated automatically by release-please from these types.

## Architecture decisions
Significant decisions are recorded as ADRs in [`docs/adr`](docs/adr).
