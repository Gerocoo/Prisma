<div align="center">

# Tubify

**Un client musicale open source con un'interfaccia pulita, scura e familiare.**

Tema scuro, accento ambra, font Figtree: un'app pensata per scorrere le playlist e ascoltare
senza distrazioni. Metadati, playlist e sorgenti audio arrivano dai plugin della community.

</div>

---

## Cos'è

Tubify è un fork di [Spotube](https://github.com/team-spotube/spotube), un client musicale
multipiattaforma basato su plugin. Il motore è quello di Spotube, la grafica è stata ridisegnata da
capo:

- 🌑 **Tema scuro di default** con superfici neutre e un solo colore d'accento (ambra, oppure
  viola o turchese dalle impostazioni).
- 🔤 **Font Figtree** su tutta l'app, con titoli marcati e gerarchia chiara.
- 🏠 **Home** con saluto in base all'ora, filtri a pillola e righe di copertine.
- 📀 **Playlist e album** con copertina grande, pulsante play rotondo e lista brani senza cornici.
- ▶️ **Mini player** a card sopra la barra di navigazione e **player a tutto schermo** con copertina
  piena e controlli grandi.

Restano tutte le funzioni di Spotube: plugin per qualsiasi servizio, download con metadati, testi
sincronizzati, nessuna telemetria, riproduzione gestita in locale.

## Scaricare l'app

Le build di prova per Android si trovano nella scheda **Actions** del repository: aprite l'ultima
esecuzione riuscita di **Build APK** e scaricate l'artefatto `tubify-android-apk`.

> Sono build di debug, non firmate per la distribuzione. Per installarle va consentita
> l'installazione da fonti sconosciute.

## Compilare dal sorgente

Tubify è un'app Compose Multiplatform (Kotlin). Servono **JDK 21**, **Rust** (`rustup` + `cargo`) e,
per Android, l'**Android SDK**.

```bash
# Android (APK di debug)
./gradlew :composeApp:assembleStableDebug

# Desktop
./gradlew :composeApp:run
```

Per i dettagli (build native desktop, GraalVM, dipendenze) valgono le istruzioni del progetto
originale: [Spotube — Building from source](https://github.com/team-spotube/spotube#readme).

## Crediti e licenza

Tubify si basa sul lavoro di [Kingkor Roy Tirtho e dei contributori di Spotube](https://github.com/team-spotube/spotube).
Come il progetto originale, è distribuito con licenza **AGPL-3.0-or-later**: vedi [LICENSE](LICENSE).

Il font Figtree è distribuito con [SIL Open Font License](assets/licenses/Figtree-OFL.txt).

Tubify è un progetto indipendente e non è affiliato, approvato o sponsorizzato da Spotify, YouTube
o dai rispettivi proprietari. Tutti i marchi citati appartengono ai legittimi titolari.
