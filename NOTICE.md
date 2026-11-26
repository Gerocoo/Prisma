# Notice of changes

Prisma is a modified version of [Spotube](https://github.com/team-spotube/spotube) by Kingkor
Roy Tirtho and the Spotube contributors, distributed under the GNU Affero General Public License
v3.0 (or any later version). This notice is provided to satisfy AGPLv3 section 5(a), which
requires that a modified work carry a prominent statement that it has been changed, and the date
of the change.

**This is a modified version of Spotube.** Changes have been made on an ongoing basis since the
fork was created; the first changes under the name "Prisma" were made in 2026. The full history
of what was changed, file by file, is in this repository's git history and in
[CHANGELOG.md](CHANGELOG.md); at a high level, the changes made so far include:

- A redesign of the user interface around Material 3 Expressive (navigation, player, lyrics,
  artist page, settings, queue and more), replacing Spotube's original interface.
- Project-specific branding (name, icon, application id) distinct from Spotube.
- An in-app update mechanism that checks this repository's own releases instead of Spotube's.
- Various feature changes and removals relative to upstream Spotube (for example, the in-app
  track-download feature has been removed; see [README.md](README.md) for what Prisma currently
  does).
- Bug fixes and behavior changes to the playback and plugin system inherited from Spotube.

Every build's release notes (see this repository's [Releases](../../releases) page) and the
commit history document the specific, dated changes as they are made, in addition to this notice.

This file does not replace the per-file "you changed this" practice some contributors may also
follow in individual source files; it is the project-level notice for the fork as a whole.
