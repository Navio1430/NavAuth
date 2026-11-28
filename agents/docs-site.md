# Docs site

The user documentation is a VitePress site in `docs/`, published to GitHub Pages under
`/NavAuth/docs` by `deploy-docs.yml`. It is written for server admins and plugin developers;
agent-facing knowledge lives in this wiki instead.

## Where it lives

| Path | What |
| --- | --- |
| `docs/docs/.vitepress/config.mts` | site config; the sidebar is built from the files by `vitepress-sidebar` (titles from the first heading, `generated/` excluded) |
| `docs/docs/index.md`, `navauth-release.md`, `setup.md`, `requirements.md`, `offer.md` | landing, release status, setup, requirements, comparison with other plugins |
| `docs/docs/general/` | commands overview, 2FA, user lookup, database |
| `docs/docs/configuration/` | config topics (multification) |
| `docs/docs/migration/` | migration from other plugins, database type migration |
| `docs/docs/developers/` | API usage |
| `docs/docs/integrations/` | AjQueue |
| `docs/docs/generated/` | **generated, do not edit by hand**: `velocity-commands.md`, `events-table.md`, `migrated-plugin-types.md` |
| `docs/docs/public/` | images (`lookup/profile.webp`, logos) |
| `navauth-docs-gen/` | generators for `generated/` (`CommandsGenerator` reads `@Command`, `@Execute`, `@Permission`, `@Description`; class and method permissions are both listed because both are required) |
| `docs/package.json` | VitePress and pnpm versions (`pnpm docs:dev`, `pnpm docs:build`) |

## Rules

- User-facing behaviour, commands, permissions and config keys are documented in `docs/docs/` in the
  same change as the code.
- Regenerate `docs/docs/generated/` (`./gradlew :navauth-docs-gen:generate`) after changing
  commands, their descriptions or permissions, events or migration origins.
- A new page appears in the sidebar by itself; its first heading is its title.
- Keep `README.md` (repository root) feature lists in step with what the plugin does.
- No security exploit details in user docs ([security.md](security.md)).
- English.

## Related

[commands-and-permissions.md](commands-and-permissions.md) · [api-and-events.md](api-and-events.md) ·
[build-and-release.md](build-and-release.md)
