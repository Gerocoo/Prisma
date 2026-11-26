# Privacy Policy

Prisma is a free, open-source music client, built on top of [Spotube](https://github.com/team-spotube/spotube).
This policy explains what Prisma itself does and does not do with your information. Prisma has no
servers of its own, no user accounts, and no analytics or advertising SDKs.

## Information collection and use

Prisma does not collect, transmit or store your personal information on any server it operates,
because it does not operate any server. Everything Prisma itself keeps — app settings, your local
listening history (used only to build the on-device "year in review"), cached images and lyrics,
and local track files you add — stays on your device, in the app's own storage, and is not sent
anywhere by Prisma.

## Plugins and third-party services

Prisma's catalog, playback and account features come entirely from **plugins** you choose to
install (for example, a Spotify-metadata plugin or a YouTube-audio plugin). When you use a plugin:

- Any account you sign in with (for example, a Spotify account) is handled by that plugin; your
  session/credentials for it are stored only on your device, and are sent only to that service's
  own servers, never to Prisma's.
- The plugin talks directly to that third-party service (e.g. Spotify, YouTube) to fetch metadata,
  lyrics, artwork or audio. That service's own privacy policy governs what happens to requests
  Prisma sends it; Prisma does not see or retain that traffic.
- Some plugins may use services' internal, undocumented interfaces rather than official public
  APIs; using them is subject to that service's own terms, which is between you and that service.

## App updates

On launch, Prisma checks this GitHub repository's own release list to see if a newer build exists.
This request goes to GitHub, not to Prisma's authors directly, and carries no personal information
beyond what any HTTP request to GitHub ordinarily includes (e.g. your IP address, visible to
GitHub as the host, not to Prisma).

## Casting

If you use Google Cast / Chromecast from Prisma, device discovery and casting go through Google
Play services on your device, under Google's own privacy policy:
[https://policies.google.com/privacy](https://policies.google.com/privacy). Prisma does not add
any tracking of its own on top of this.

## Children's privacy

Prisma is not directed at children under 13, and does not knowingly collect personal information
from anyone, of any age — see above: it does not collect personal information at all.

## Changes to this policy

This policy may be updated as the app changes. Changes are published on this page, in this
repository; the current version is dated below.

Last updated: 2026-11-26

## Contact

Questions about this policy can be sent to **gerocooema@gmail.com**, or opened as an issue on
this repository.
