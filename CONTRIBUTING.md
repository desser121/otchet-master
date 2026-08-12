# Contributing

Thanks for your interest in Otchet-Master! Please follow these guidelines.

## Getting started

1. Fork the repository.
2. Create a branch from `develop`:

   - `feature/<short-name>` for new features
   - `fix/<short-name>` for bug fixes

3. Make your changes.
4. Run the available checks (unit tests, lint).
5. Open a pull request into `develop`.

## Commit messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) style:

- `feat: add customer management`
- `fix: crash on empty photo list`
- `docs: update architecture overview`
- `refactor: extract report generator`

Keep commits small and logically complete.

## Code style

- Match the existing style of the file you are editing.
- Do not add comments unless they explain non-obvious intent.
- No secrets, PII, or real user data in commits — ever.

## Pull requests

- Describe what you changed and why.
- Reference related issue/ADR if any.
- Keep the PR focused on a single concern.

## Documentation

Important decisions are recorded as ADRs in `docs/decisions/`. If your change affects architecture, data model, API, or security, update the relevant docs and add an ADR when needed.
