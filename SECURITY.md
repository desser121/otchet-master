# Security Policy

## Reporting a vulnerability

If you discover a security vulnerability in this project, **do not open a public issue**. Please report it privately.

Contact the maintainers via GitHub Security Advisories:
https://github.com/your-org/otchet-master/security/advisories/new

Please include:

- description of the vulnerability;
- steps to reproduce;
- affected versions;
- suggested fix (if any).

We will acknowledge receipt within 5 business days and work on a fix.

## Security commitments

- No API keys, passwords, tokens, or production credentials are committed to the repository. All secrets live in environment variables (`.env`, secret managers).
- API keys used on the client side are restricted (anon keys with Row Level Security).
- Photos and personal data are never sent to the AI provider — only the text description is used.
- Access to user data is protected by Supabase Row Level Security (RLS).
- Real user data, real client photos, and real documents must never be placed in the repository.

## Supported versions

Only the latest `main` release is supported.
