# xBotenyy

Discord- und Twitch-Bot als eigenständige Maven-Module (`common`, `discordbot`, `twitchbot`). Über das zusätzliche
Modul `launcher` können beide Bots auch gemeinsam in **einem** Prozess/Server gestartet werden. Beide Bots können
optional über eine eingebaute Bridge miteinander sprechen (Moderations-Sync, Rollen-Sync, Account-Verknüpfung).

## Inhaltsverzeichnis

- [Voraussetzungen](#voraussetzungen)
- [Setup](#setup)
- [Discord-Bot](#discord-bot)
    - [Command-Referenz](#discord-command-referenz)
    - [AutoMod](#discord-automod)
    - [Event-Logging (`/serverlog`)](#event-logging-serverlog)
    - [Interne Systeme](#interne-systeme)
- [Twitch-Bot](#twitch-bot)
    - [Command-Referenz](#twitch-command-referenz)
    - [Zitat- & Umfrage-System](#zitat--und-umfrage-system)
    - [Twitch-Autorisierung](#twitch-autorisierung-generierung-der-tokens)
    - [AutoMod-Mapping](#automod-mapping)
    - [Persistenz](#persistenz)
- [Discord ↔ Twitch Integration](#discord--twitch-integration)
    - [Discord-Logging (Twitch → Discord)](#discord-logging-twitch--discord)
    - [Erweiterte Moderation](#erweiterte-moderation)
    - [Accounts verknüpfen](#accounts-verknüpfen)
    - [Twitch-Rollen synchronisieren](#twitch-rollen-mit-discord-synchronisieren)
    - [Die Bridge](#die-bridge-discord--twitch)
- [Kombinierter Start (`launcher`)](#kombinierter-start-launcher)
- [Automatische Builds und Releases](#automatische-builds-und-releases)

---

## Voraussetzungen

- JDK 21
- Maven 3.9+
- Discord-Bot-Token (für `discordbot`)
- Twitch Client-ID + Client-Secret (für `twitchbot` & Twitch-Integration im `discordbot`)

### API-Portale konfigurieren

* **Discord Developer Portal:** `Server Members Intent` und `Message Content Intent` zwingend aktivieren.
* **Twitch Developer Console:** App registrieren und `OAuth Redirect URL` auf `http://localhost:8080` (oder die URL
  deines OAuth-Handlers) setzen.

## Setup

1. Repository klonen.
2. `.env` Datei im **Hauptverzeichnis (Projekt-Root)** erstellen. *Hinweis: Niemals unter `src/main/resources` ablegen,
   da die Werte sonst in die JAR kompiliert werden!*

   ```env
   # Discord Konfiguration
   BOT_TOKEN=
   GROQ_API_KEY=

   # Twitch App Credentials
   TWITCH_CLIENT_ID=
   TWITCH_CLIENT_SECRET=

   # Twitch Bot-Account Tokens
   TWITCH_BOT_ACCESS_TOKEN=
   TWITCH_BOT_REFRESH_TOKEN=

   # Twitch Broadcaster-Account Tokens (für EventSub, Mod-Aktionen & Rollen-Abgleich)
   TWITCH_BROADCASTER_ACCESS_TOKEN=
   TWITCH_BROADCASTER_REFRESH_TOKEN=
   ```

3. `discordbot.properties` / `twitchbot.properties` konfigurieren (werden beim ersten Start mit Standardwerten
   automatisch im jeweiligen Modulordner angelegt).

---

## Discord-Bot

Systeme: Support-Tickets, Report-System, Reaction-Roles, Gewinnspiele, Willkommensnachrichten, Event-Logging,
YouTube-/Twitch-Ankündigungen, KI-gestütztes AutoMod, erweiterte manuelle Moderation mit Straf-Rollen und
Discord-↔-Twitch-Verknüpfung.

### Discord Command-Referenz

#### Server & Community

| Command | Subcommands | Funktion |
|---|---|---|
| `/ticket` | `panel`, `settings`, `list`, `claim`, `unclaim`, `close`, `priority`, `add`, `remove` | Support-Ticket-System mit Kategorien, Prioritäten, Claiming, Auto-Close und Transcript |
| `/report` | `send`, `settings` | Report-System - Mitglieder melden Vorfälle über einen geführten Dialog, Team konfiguriert Kanal/Rolle |
| `/reports` | – | Zeigt die eigenen eingereichten Reports und deren Status |
| `/reactionrole` | `add`, `remove`, `list` | Reaction Roles per Emoji oder Button auf einer beliebigen Nachricht |
| `/giveaway` | `start`, `end`, `reroll`, `cancel`, `list` | Gewinnspiel-System mit mehreren Gewinnern |
| `/welcome` | `settings`, `add`, `edit`, `remove`, `list`, `test`, `placeholders` | Willkommensnachrichten mit mehreren Varianten und Platzhaltern |
| `/socials` | `add`, `edit`, `message`, `remove`, `list`, `status`, `test`, `placeholders` | YouTube- & Twitch-Live-Ankündigungen |
| `/message` | `create`, `edit`, `placeholders` | Freie Nachrichten mit Platzhaltern (z. B. als Basis für Reaction Roles) |

#### Moderation

| Command | Subcommands | Funktion |
|---|---|---|
| `/mod` | `warn`, `timeout`, `untimeout`, `kick`, `ban`, `unban`, `cases` | Manuelle Moderation mit Fall-Historie (siehe [Erweiterte Moderation](#erweiterte-moderation)) |
| `/modroles` | `warn-role`, `mute-role`, `ban-role`, `add-moderator`, `remove-moderator`, `add-admin`, `remove-admin`, `sync-role`, `status` | Konfiguriert Straf-Rollen, wer `/mod` nutzen darf, und den Twitch-Rollen-Sync |
| `/link` | `twitch`, `verify`, `status`, `unlink`, `panel` | Discord-↔-Twitch-Account-Verknüpfung (freiwillig, siehe [Accounts verknüpfen](#accounts-verknüpfen)) |
| `/automod` | `status`, `test` | AutoMod-Diagnose (Konfiguration ausschließlich über `discordbot.properties`) |
| `/serverlog` | `setup`, `channel`, `toggle`, `event-channel`, `status` | Event-Logging konfigurieren (siehe [Event-Logging](#event-logging-serverlog)) |

#### Utility

| Command | Funktion |
|---|---|
| `/info` | Bot-Informationen |
| `/ping` | Bot-Latenz |

*Berechtigungen: `/mod` steht Mitgliedern mit `ADMINISTRATOR`/`Mitglieder moderieren` sowie allen über
`/modroles add-moderator` freigegebenen Rollen offen. `/modroles`, `/serverlog`, `/automod` und die
Team-Subcommands von `/ticket`/`/report` erfordern `ADMINISTRATOR`/`Server verwalten` (bzw. bei `/modroles` auch
über `add-admin` freigegebene Rollen).*

### Discord AutoMod

Wortfilter, Invite-/Link-Erkennung, Mention-Spam, Caps-Filter, Spam-/Duplikat-Erkennung, KI-Moderation (Groq,
`gpt-oss-safeguard-20b`), Strike-Eskalation (Warn → Timeout → Kick → Ban). Konfiguration ausschließlich über
`automod.*` in `discordbot.properties`. Diagnose und Testen einzelner Texte über `/automod status` / `/automod test`.

### Event-Logging (`/serverlog`)

Frei konfigurierbar pro Event-Typ: Standard-Kanal, individueller Kanal je Typ, sowie einzeln an-/abschaltbar.
`/serverlog setup` richtet alles mit einem Standardkanal ein, `/serverlog status` zeigt die aktuelle Konfiguration.

| Event-Typ | Bedeutung |
|---|---|
| Mitglied beigetreten / verlassen | Server-Beitritte und -Austritte |
| Server geboostet / Boost entfernt | Nitro-Boost-Aktivität |
| Mitglied gebannt / Bann aufgehoben | Discord meldet dies nativ, unabhängig von der Quelle (AutoMod, `/mod`, manuell) |
| Mitglied Timeout / Timeout aufgehoben | Ebenfalls nativ erfasst, unabhängig von der Quelle |
| Mitglied gekickt | Nur über `/mod kick`, da Discord Kicks nicht nativ von normalen Austritten unterscheidet |
| Mitglied verwarnt | Nur über `/mod warn`, da Warnungen kein natives Discord-Konzept sind |
| Nickname geändert | Änderungen am Servernamen eines Mitglieds |
| Rollen aktualisiert | Rollenänderungen an einem Mitglied |
| Voice-Aktivität | Beitritt/Verlassen von Sprachkanälen |
| Channel erstellt / gelöscht | Serverstruktur-Änderungen |
| Nachricht gelöscht | Gelöschte Nachrichten inkl. Inhalt (bis zu `eventlog.message-delete.content-max-length` Zeichen) |
| Command-Nutzung | Jede Slash-Command-Ausführung inkl. Status (ausgeführt/keine Berechtigung/Cooldown/Fehler) |

### Interne Systeme

- SQLite-Database mit automatischer Schema-Migration beim Start
- Automatische Backups (Rotation über `backup.*`)
- Audit-Log für administrative Aktionen
- Retry-Handling für Discord-API-Aufrufe
- Heartbeat-Logging mit Metriken pro Feature
- Automatische Bereinigung veralteter Datensätze

---

## Twitch-Bot

Eigenständiges Modul. Verbindet sich per moderner Helix-Chat-API + EventSub-Websockets. Systeme: Custom-Commands,
Zitate, Umfragen, wiederkehrende Ansagen, Watchtime-Tracking, KI-gestütztes AutoMod, erweiterte manuelle
Moderation, sowie optionale Discord-Integration (Logging, Moderations-Sync, Rollen-Sync).

### Twitch Command-Referenz

Prefix konfigurierbar (`twitch.chat.command.prefix`, Standard `!`):

| Befehl | Berechtigung | Funktion |
|---|---|---|
| `!ping` | alle | Erreichbarkeits-Check |
| `!uptime` | alle | Laufzeit des Bots |
| `!strikes` | alle | Eigene AutoMod-Strikes |
| `!watchtime [nutzer]` | alle | Watchtime des Nutzers |
| `!followage [nutzer]` | alle | Seit wann jemand folgt |
| `!link` | alle | Startet die Verknüpfung mit dem eigenen Discord-Account |
| `!verify <code>` | alle | Bestätigt einen auf Discord mit `/link twitch` erzeugten Code |
| `!automod` / `!mod` | Moderator | AutoMod-Status abfragen |
| `!command add/remove/list/cooldown` | Moderator | Custom-Commands verwalten (inkl. Cooldown je Command) |
| `!commands` / `!help` | alle | Listet Befehle auf |
| `!broadcast add/remove/list` | Moderator | Wiederkehrende Chat-Ansagen |
| `!eventlog` | Moderator | Event-Log-Einstellungen |
| `!warn <nutzer> [grund]` | Moderator | Verwarnt einen Nutzer manuell |
| `!timeout <nutzer> <sek> [grund]` | Moderator | Timeoutet einen Nutzer |
| `!ban <nutzer> [grund]` | Moderator | Bannt einen Nutzer dauerhaft |
| `!unban <nutzer>` | Moderator | Hebt Bann/Timeout auf |
| `!purge` / `!clear` | Moderator | Leert den kompletten Chat |
| `!permit <nutzer> [sek]` | Moderator | Vorübergehende AutoMod-Ausnahme für einen Nutzer |
| `!title [neuer titel]` | Moderator | Zeigt/ändert den Stream-Titel |
| `!game [kategorie]` | Moderator | Zeigt/ändert die Stream-Kategorie |
| `!so <kanal>` | Moderator | Shoutout für einen anderen Streamer |
| `!clip` | alle | Erstellt einen Clip vom aktuellen Moment |
| `!quote [nummer]` | alle | Zeigt ein zufälliges oder gezieltes Zitat |
| `!quote add <text>` | Moderator | Speichert ein neues Zitat |
| `!quote del <nummer>` | Moderator | Entfernt ein gespeichertes Zitat |
| `!quote list` | alle | Zeigt Anzahl und erste gespeicherte Zitate |
| `!poll start <frage> \| <opt1> \| <opt2> [\| ...]` | Moderator | Startet eine Chat-Umfrage (max. 5 Optionen) |
| `!poll results` | alle | Zeigt den aktuellen Zwischenstand der Umfrage |
| `!poll end` | Moderator | Beendet die Umfrage und zeigt das Endergebnis |
| `!vote <nummer>` | alle | Stimmt bei der laufenden Umfrage ab |

*Custom-Command-Antworten unterstützen die Platzhalter `{user}` und `{channel}`. Cooldown je Custom-Command
über `!command cooldown <name> <sekunden>`, Standard 5 Sekunden. `!ban`/`!timeout`/`!unban`/`!warn` schreiben
zusätzlich in die gemeinsame Fall-Historie und lösen - falls konfiguriert - eine Synchronisation zum verknüpften
Discord-Account aus (siehe [Erweiterte Moderation](#erweiterte-moderation)).*

### Zitat- und Umfrage-System

**Zitate (`!quote`):** Zitate werden pro Kanal fortlaufend nummeriert und dauerhaft in der SQLite-Datenbank
gespeichert (Tabelle `twitch_quotes`). `!quote` ohne Argument liefert ein zufälliges Zitat, `!quote <nummer>` ein
bestimmtes. Hinzufügen und Entfernen ist Moderatoren vorbehalten, jeder Vorgang wird im Event-Log festgehalten.

**Umfragen (`!poll` / `!vote`):** Pro Kanal kann jeweils eine Umfrage aktiv sein (2-5 Antwortoptionen, durch `|`
getrennt). Zuschauer stimmen mit `!vote <nummer>` ab, Mehrfachstimmen desselben Nutzers ersetzen die vorherige
Stimme. Umfragen leben nur im Arbeitsspeicher des laufenden Prozesses (kein Neustart-Überdauern) und werden beim
Start/Ende ins Event-Log geschrieben.

### Twitch-Autorisierung (Generierung der Tokens)

Da der Bot über die moderne Helix-API agiert, müssen **sowohl der Broadcaster als auch der Bot-Account** autorisiert
werden.

#### 1. Autorisierung durch den Broadcaster (Streamer)

Führe diesen URL-Aufruf im Browser aus, während du im **Streamer-Account** eingeloggt bist. Erlaube dem Bot den Zugriff
auf Mod- und Follower-Daten sowie auf das Ändern von Titel/Kategorie:

```text
https://id.twitch.tv/oauth2/authorize?client_id=<TWITCH_CLIENT_ID>&redirect_uri=http://localhost:8080&response_type=code&scope=channel:bot+moderation:read+channel:read:subscriptions+channel:read:vips+channel:manage:broadcast+moderator:read:followers+moderator:read:chatters+moderator:manage:chat_messages+moderator:manage:banned_users&force_verify=true
```

*Tausche den empfangenen `?code=` aus der Adresszeile gegen das Access- und Refresh-Token für den **Broadcaster** und
trage es unter `TWITCH_BROADCASTER_ACCESS_TOKEN`/`TWITCH_BROADCASTER_REFRESH_TOKEN` in die `.env` ein. Dieses Token wird
für `!title`/`!game` sowie für den periodischen Twitch-Rollen-Abgleich (Subscriber/VIP/Moderator, siehe
[Twitch-Rollen synchronisieren](#twitch-rollen-mit-discord-synchronisieren)) benötigt - ohne dieses Token bleiben
`!title`/`!game` im Lesemodus und der periodische Abgleich wird übersprungen (der laufende Chat-basierte Sync
funktioniert trotzdem weiter).*

#### 2. Autorisierung durch den Bot-Account

Logge dich im Browser in den **Bot-Account** ein und rufe folgende URL auf:

```text
https://id.twitch.tv/oauth2/authorize?client_id=<TWITCH_CLIENT_ID>&redirect_uri=http://localhost:8080&response_type=code&scope=user:bot+user:write:chat+user:read:chat+clips:edit+moderator:manage:shoutouts+moderator:manage:automod&force_verify=true
```

*Tausche den empfangenen `?code=` gegen das Access- und Refresh-Token für den **Bot** und trage es in die `.env`
ein. `clips:edit` wird für `!clip`, `moderator:manage:shoutouts` für `!so`, `moderator:manage:automod` für das
native Twitch-AutoMod-Logging (siehe [Discord-Logging](#discord-logging-twitch--discord)) benötigt.*

#### 3. Code gegen Access- und Refresh-Token tauschen

Nachdem du in Schritt 1 oder 2 auf "Autorisieren" geklickt hast, leitet Twitch dich zu `http://localhost:8080/?code=...`
weiter (die Seite lädt nicht, das ist normal). Kopiere den Wert hinter `code=` aus der Adresszeile und tausche ihn
**sofort** (Codes sind nur kurz gültig) gegen die Tokens ein.

**Windows / PowerShell:**

```powershell
Invoke-RestMethod -Uri "https://id.twitch.tv/oauth2/token" -Method Post -Body @{ client_id="<TWITCH_CLIENT_ID>"; client_secret="<TWITCH_CLIENT_SECRET>"; code="<CODE_AUS_DER_URL>"; grant_type="authorization_code"; redirect_uri="http://localhost:8080" }
```

**macOS / Linux (oder `curl.exe` unter Windows):**

```bash
curl -X POST https://id.twitch.tv/oauth2/token -d client_id=<TWITCH_CLIENT_ID> -d client_secret=<TWITCH_CLIENT_SECRET> -d code=<CODE_AUS_DER_URL> -d grant_type=authorization_code -d redirect_uri=http://localhost:8080
```

Die Antwort enthält `access_token` und `refresh_token`. Trage beide je nach Flow unter
`TWITCH_BROADCASTER_ACCESS_TOKEN`/`TWITCH_BROADCASTER_REFRESH_TOKEN` (Schritt 1) bzw. `TWITCH_BOT_ACCESS_TOKEN`/
`TWITCH_BOT_REFRESH_TOKEN` (Schritt 2) in die `.env` ein.

*Hinweis: Das Refresh-Token muss danach nicht mehr manuell erneuert werden - der Bot aktualisiert den Access-Token
automatisch im Hintergrund, solange das Refresh-Token gültig bleibt (siehe `TwitchUserTokenManager`).*

#### 4. Kanal-Rechte vergeben

1. Trage den Bot-Account im Kanal des Broadcasters als Moderator ein (`/mod <botname>`).
2. Trage den Zielkanal in den `twitchbot.properties` unter `twitch.chat.channels` ein.

### AutoMod-Mapping

| Discord-Key | Twitch-Bedeutung |
|---|---|
| `automod.exempt.role.ids` | Ausgenommene Twitch-Logins (Usernames) |
| `automod.exempt.channel.ids` | Ausgenommene Twitch-Kanäle |
| `automod.bypass.manage-server` | Bypass für Channel-Moderatoren |
| `automod.bypass.administrator` | Bypass für den Broadcaster (Kanalinhaber) |

### Persistenz

Eigene SQLite-DB (`twitch.database.file`), nutzt die identische `Database`/`Jdbc`/`SchemaMigrator`-Schicht wie der
Discord-Bot.
Tabellen: `twitch_channels`, `twitch_custom_commands` (inkl. `cooldown_seconds`), `twitch_watchtime`,
`twitch_broadcasts`, `twitch_event_log`, `twitch_quotes`, `moderation_cases`, `account_links`,
`pending_link_verifications`.

### Weitere Einstellungen

- `twitch.automod.permit.default.seconds` (Standard `30`): Dauer der AutoMod-Ausnahme, wenn `!permit <nutzer>` ohne
  Sekundenangabe genutzt wird.

---

## Discord ↔ Twitch Integration

### Discord-Logging (Twitch → Discord)

Der Twitch-Bot kann Chat-Nachrichten, AutoMod-Ereignisse (das eigene AutoMod **und** Twitchs natives AutoMod) sowie
Command-Nutzungen live per Discord-Webhook in einen Discord-Channel spiegeln. Da der Twitch-Bot als eigener Prozess
ohne Discord-Verbindung läuft, geschieht das unabhängig vom Discord-Bot über einen klassischen Discord-Webhook.

1. Lege in Discord im gewünschten Log-Channel einen Webhook an (Channel-Einstellungen → Integrationen →
   Webhooks → Neuer Webhook) und kopiere die Webhook-URL.
2. Trage die URL in `twitchbot.properties` unter `discord.log.webhook.url` ein.
3. Steuere über folgende Schalter, was geloggt wird:

| Property | Standard | Bedeutung |
|---|---|---|
| `discord.log.webhook.url` | *(leer)* | Discord-Webhook-URL, ohne URL ist das Feature deaktiviert |
| `discord.log.messages.enabled` | `false` | Loggt jede Chat-Nachricht (kann sehr viel Traffic erzeugen) |
| `discord.log.automod.enabled` | `true` | Loggt eigenes AutoMod **und** natives Twitch-AutoMod |
| `discord.log.commands.enabled` | `true` | Loggt jede Nutzung eines eingebauten oder Custom-Commands |

Für das native Twitch-AutoMod (Nachrichten, die Twitch selbst zur Prüfung zurückhält) abonniert der Bot
automatisch die EventSub-Typen `automod.message.hold` und `automod.message.update`. Dafür wird der Scope
`moderator:manage:automod` auf dem Bot-Account-Token benötigt (siehe Schritt 2 der Twitch-Autorisierung oben).

Auf der Discord-Bot-Seite wird die Nutzung von Discord-Slash-Commands über das bestehende Event-Log-System
geloggt (`/serverlog`, Event-Typ "Command-Nutzung") und benötigt keine zusätzliche Konfiguration.

### Erweiterte Moderation

Zusätzlich zum automatischen AutoMod gibt es ein vollständiges manuelles Moderationssystem mit eigener
Fall-Historie, konfigurierbaren Straf-/Moderator-Rollen und optionaler Synchronisation zwischen Discord und Twitch.

**Discord:** `/mod warn|timeout|untimeout|kick|ban|unban|cases` - Details siehe [Command-Referenz](#discord-command-referenz)
weiter oben. `/mod`-Befehle dürfen von Mitgliedern mit `ADMINISTRATOR`/`Mitglieder moderieren`-Rechten sowie allen
über `/modroles add-moderator` freigegebenen Rollen genutzt werden. `/modroles` selbst erfordert `ADMINISTRATOR`
oder eine über `add-admin` freigegebene Rolle. Bans und Timeouts erscheinen automatisch auch im normalen
`/serverlog` (Discord meldet diese Events selbst); Kicks und Verwarnungen werden zusätzlich als eigene Log-Typen
erfasst. Über `/modroles warn-role`/`mute-role`/`ban-role` legst du fest, welche Rolle bei Verwarnung/Timeout/Bann
automatisch vergeben (und bei Aufhebung wieder entfernt) wird.

**Twitch:** `!warn`, `!ban`, `!timeout`/`!to`, `!unban`/`!untimeout` schreiben in dieselbe gemeinsame Fall-Historie
und lösen - falls die Bridge konfiguriert ist - eine Synchronisation zum verknüpften Discord-Account aus. Ein Kick
hat kein Twitch-Äquivalent und wird nicht synchronisiert.

### Accounts verknüpfen

Die Verknüpfung ist für Nutzer komplett **freiwillig** - ohne sie funktionieren Server, Kanal und alle anderen
Features normal weiter. Sie lohnt sich, weil dadurch Twitch-Rollen (Subscriber, VIP, Moderator, Broadcaster)
automatisch als Discord-Rolle gespiegelt werden können und - falls die Bridge aktiviert ist - Moderationsmaßnahmen
zwischen Discord und Twitch synchronisiert werden. Gespeichert wird dabei ausschließlich die Zuordnung
Discord-ID ↔ Twitch-ID/-Login, keine Passwörter oder Tokens.

Die Verknüpfung funktioniert in beide Richtungen und erfordert immer eine Bestätigung auf der jeweils anderen
Plattform (kein reines Vertrauen auf Zuruf):

- **Panel (empfohlen):** `/link panel channel:<kanal>` (Admin) postet ein Embed mit einem "🔗 Verknüpfen"-Button
  in den angegebenen Kanal - ähnlich wie beim Ticket-System. Klick auf den Button öffnet ein kleines Formular
  für den Twitch-Loginnamen, der Rest läuft wie unten beschrieben weiter.
- **Start auf Discord (Command):** `/link twitch login:<name>` → Bot nennt einen Code → Nutzer postet
  `!verify <code>` im Twitch-Chat des angegebenen Kanals.
- **Start auf Twitch:** `!link` im Chat → Bot nennt einen Code → Nutzer führt `/link verify code:<code>` auf
  Discord aus.
- `/link status` / `/link unlink` verwalten die eigene Verknüpfung auf Discord-Seite.

### Twitch-Rollen mit Discord synchronisieren

Über `/modroles sync-role status:<subscriber|vip|moderator|broadcaster> role:<@rolle>` legst du fest, welche
Discord-Rolle für welchen Twitch-Status automatisch vergeben werden soll (Aufruf ohne `role` entfernt die
Zuordnung wieder). `/modroles status` zeigt die aktuelle Konfiguration.

Der Sync passiert auf zwei Wegen:
- **Sofort beim Schreiben:** Der Twitch-Bot erkennt den Status eines verknüpften Nutzers anhand der Chat-Badges,
  sobald dieser im Twitch-Chat schreibt, und gleicht die Discord-Rolle direkt ab.
- **Periodischer Abgleich:** Damit z. B. ein auslaufendes Abo auch dann erkannt wird, wenn die Person nicht mehr
  schreibt, fragt der Twitch-Bot zusätzlich alle `moderation.sync.reconcile.interval.minutes` (Standard `15`)
  die aktuelle Subscriber-/VIP-/Moderatoren-Liste per Twitch-API ab und gleicht **alle** verknüpften Accounts ab.
  Dafür wird der `TWITCH_BROADCASTER_ACCESS_TOKEN` benötigt (siehe oben) - ohne ihn fällt nur der periodische
  Abgleich weg, der Chat-basierte Sync funktioniert trotzdem.

### Die Bridge (Discord ↔ Twitch)

Da beide Bots als getrennte Prozesse - auch auf unterschiedlichen Servern - laufen können, sprechen sie über eine
kleine, in beide Bots eingebaute HTTP(S)-Schnittstelle miteinander ("Bridge"). Es wird **keine** zusätzliche Software
benötigt; jeder Bot kann optional einen kleinen HTTP(S)-Server starten und den des jeweils anderen Bots aufrufen.
Die Bridge unterstützt **TLS-Verschlüsselung und optional mutual TLS (mTLS)** für die höchstmögliche Absicherung der
Verbindung, insbesondere wenn beide Bots auf getrennten Servern laufen.

| Property (beide Bots) | Standard | Bedeutung |
|---|---|---|
| `bridge.enabled` | `false` | Startet den eigenen Bridge-Server (muss vom Peer erreichbar sein) |
| `bridge.bind.host` | `127.0.0.1` | Netzwerk-Interface, an das der Bridge-Server gebunden wird |
| `bridge.port` | `8082`/`8083` | Port des eigenen Bridge-Servers |
| `bridge.token` | *(leer)* | Gemeinsames Shared-Secret - **muss auf beiden Bots identisch sein** |
| `bridge.peer.url` | *(leer)* | Basis-URL des jeweils anderen Bots, z. B. `http://localhost:8083` |

Zusätzlich (jeweils nur auf einer Seite):

- **Discordbot:** `moderation.sync.guild.id` - die **Server-ID** (nicht Channel-ID!) des Discord-Servers, in dem
  synchronisierte Aktionen ausgeführt werden (dieses Bot-Setup ist auf eine Community/einen Server ausgelegt).
  Die ID bekommst du per Rechtsklick auf den **Servernamen** (nicht auf einen Kanal) → "ID kopieren"
  (Entwicklermodus muss dafür in den Discord-Einstellungen aktiviert sein).
- **Twitchbot:** `moderation.sync.channel` - der Twitch-Kanal-**Login** (Text, z. B. `xdestenyyy` - keine Zahl!),
  in dem synchronisierte Aktionen ausgeführt werden (leer = erster Kanal aus `twitch.chat.channels`).
- **Twitchbot:** `moderation.sync.reconcile.interval.minutes` (Standard `15`) - Intervall für den periodischen
  Rollen-Abgleich (siehe oben).

**Setup über den Launcher / auf demselben Server (Standard, empfohlen):** `bridge.bind.host` auf beiden Seiten bei
`127.0.0.1` belassen, `bridge.peer.url` jeweils auf `http://localhost:<port-des-anderen-bots>` setzen. Die Bridge ist
damit ausschließlich lokal erreichbar, selbst wenn der Server eine öffentliche IP hat. TLS ist hier optional, da der
Traffic den Rechner nie verlässt.

**Setup für zwei getrennte Server:** `bridge.bind.host=0.0.0.0` auf der Seite setzen, die erreichbar sein muss,
`bridge.token` auf beiden Seiten identisch setzen, `bridge.peer.url` auf die öffentlich erreichbare Adresse zeigen
lassen (mit `https://`, siehe unten). Zusätzlich unbedingt per Firewall auf die IP des jeweils anderen Servers
einschränken.

Ohne `bridge.peer.url` funktioniert alles andere (manuelle Commands, Rollen, Fall-Historie) weiterhin normal -
es wird lediglich keine Aktion zur anderen Plattform gespiegelt.

#### TLS-Verschlüsselung der Bridge (empfohlen bei getrennten Servern)

Läuft die Bridge über das offene Internet (zwei getrennte Server), sollte sie **immer** verschlüsselt werden - sonst
sind Shared-Secret, Discord-/Twitch-User-IDs und Moderationsaktionen im Klartext mitlesbar. Dafür stehen folgende
zusätzliche Properties zur Verfügung (auf beiden Bots identisch benannt, aber pro Bot mit eigenem Zertifikat):

| Property | Standard | Bedeutung |
|---|---|---|
| `bridge.tls.enabled` | `false` | Aktiviert HTTPS statt Klartext-HTTP für den eigenen Bridge-Server **und** für ausgehende Anfragen an den Peer |
| `bridge.tls.keystore.path` | *(leer)* | Pfad zur eigenen PKCS12-Zertifikatsdatei (`.p12`), die der Bridge-Server beim Handshake präsentiert |
| `bridge.tls.keystore.password` | *(leer)* | Passwort des Keystores. Unterstützt `env:VARNAME`, um das Passwort statt im Klartext über eine Umgebungsvariable bereitzustellen |
| `bridge.tls.key.password` | *(leer)* | Passwort des privaten Schlüssels im Keystore, falls abweichend vom Keystore-Passwort. Ebenfalls `env:VARNAME` möglich |
| `bridge.tls.truststore.path` | *(leer)* | Pfad zu einer PKCS12-Datei mit vertrauenswürdigen Zertifikaten. Nötig, sobald ein **selbstsigniertes** Zertifikat verwendet wird oder `mutual-auth` aktiv ist |
| `bridge.tls.truststore.password` | *(leer)* | Passwort des Truststores. Ebenfalls `env:VARNAME` möglich |
| `bridge.tls.mutual-auth` | `false` | Aktiviert mutual TLS (mTLS): Der Bridge-Server verlangt zusätzlich ein gültiges Client-Zertifikat vom Peer, nicht nur das Shared-Secret |

Erlaubt sind ausschließlich **TLS 1.2 und TLS 1.3**; ältere, unsichere Protokollversionen werden von der Bridge
grundsätzlich abgelehnt. Ist `bridge.tls.enabled=true`, muss `bridge.peer.url` mit `https://` beginnen - Anfragen an
eine `http://`-Peer-URL werden dann automatisch verweigert, um ein versehentliches Downgrade auf Klartext zu
verhindern.

**Schritt 1 - Zertifikat je Bot erzeugen** (auf jedem der beiden Server einmal, `keytool` ist Teil jeder Java-Installation):

```bash
keytool -genkeypair -alias bridge -keyalg EC -keysize 256 -validity 825 \
  -keystore bridge-keystore.p12 -storetype PKCS12 \
  -dname "CN=discordbot-bridge"        # bzw. "CN=twitchbot-bridge" auf dem anderen Server
```
`keytool` fragt dabei nach einem Passwort für den Keystore - dieses in `bridge.tls.keystore.password` eintragen
(oder besser per `env:BRIDGE_KEYSTORE_PASSWORD` als Umgebungsvariable setzen, siehe unten).

**Schritt 2 - Zertifikat exportieren und dem jeweils anderen Bot als vertrauenswürdig hinzufügen**, da es
selbstsigniert ist und nicht automatisch von der Standard-CA-Liste der JVM akzeptiert wird:

```bash
# Auf dem Discordbot-Server: eigenes Zertifikat exportieren ...
keytool -exportcert -alias bridge -keystore bridge-keystore.p12 -storetype PKCS12 -file discordbot.crt

# ... und auf den Twitchbot-Server kopieren (z. B. per scp), dort importieren:
keytool -importcert -alias discordbot-peer -file discordbot.crt \
  -keystore bridge-truststore.p12 -storetype PKCS12 -noprompt
```
Das Gleiche in die Gegenrichtung (Twitchbot-Zertifikat exportieren → auf dem Discordbot-Server in dessen
`bridge-truststore.p12` importieren). Jeder Bot bekommt so einen eigenen Truststore, der nur das Zertifikat
des jeweils anderen Bots enthält.

**Schritt 3 - Konfiguration** (Beispiel `discordbot.properties`, `twitchbot.properties` spiegelbildlich):

```properties
bridge.enabled=true
bridge.bind.host=0.0.0.0
bridge.peer.url=https://twitchbot.example.com:8083
bridge.tls.enabled=true
bridge.tls.keystore.path=/pfad/zu/bridge-keystore.p12
bridge.tls.keystore.password=env:BRIDGE_KEYSTORE_PASSWORD
bridge.tls.truststore.path=/pfad/zu/bridge-truststore.p12
bridge.tls.truststore.password=env:BRIDGE_TRUSTSTORE_PASSWORD
bridge.tls.mutual-auth=true
```
und beim Start die zugehörigen Umgebungsvariablen setzen (z. B. `export BRIDGE_KEYSTORE_PASSWORD=...`), statt die
Passwörter im Klartext in die Properties-Datei zu schreiben.

**Für maximale Sicherheit** zusätzlich `bridge.tls.mutual-auth=true` auf **beiden** Bots setzen: Der Bridge-Server
akzeptiert dann nur noch Anfragen, die sowohl ein gültiges, im Truststore hinterlegtes Client-Zertifikat als auch
das korrekte Shared-Secret mitbringen - zwei unabhängige Sicherheitsebenen zusätzlich zur Verschlüsselung selbst.

---

## Kombinierter Start (`launcher`)

Standardmäßig ist jeder Bot ein eigenständiger Prozess (eigene JAR, eigenes Logging). Wer Discord- und Twitch-Bot
auf demselben Server **in einem einzigen Prozess** betreiben möchte, nutzt stattdessen das Modul `launcher`.

Der Launcher enthält **keine eigene Bot-Logik**, sondern instanziiert und startet lediglich die bestehenden
`Bot`-Klassen aus `xBotenyyDiscordBot` und `xBotenyyTwitchBot` jeweils in einem eigenen Thread. Zusätzlich bringt
er eine interaktive **Konsole** mit, über die beide Bots zur Laufzeit gestartet, gestoppt und neugestartet werden
können, sowie Logging-Bündelung für beide Bots in einem Prozess. Die Konfiguration (`.env`, `discordbot.properties`,
`twitchbot.properties`) bleibt unverändert dieselbe wie beim getrennten Betrieb.

**Vollständige Dokumentation (Konsolen-Befehle, Architektur,
Logging-Details): [`launcher/README.md`](launcher/README.md)**

### Bauen und starten

```bash
mvn -pl launcher -am -DskipTests package
java -jar launcher/target/xBotenyyLauncher.jar
```

Optional kann über ein Startargument nur einer der beiden Bots aktiviert werden (der jeweils andere lässt sich
danach trotzdem jederzeit über die Konsole mit `start <bot>` dazu starten):

```bash
java -jar launcher/target/xBotenyyLauncher.jar --mode=discord
java -jar launcher/target/xBotenyyLauncher.jar --mode=twitch
```

Ohne Argument (oder mit `--mode=both`) starten beide Bots gemeinsam.

### Konsolen-Befehle

Sobald der Launcher läuft, akzeptiert er interaktiv Befehle über die Standard-Eingabe:

| Befehl | Beschreibung |
|---|---|
| `help` | Übersicht aller Befehle |
| `status` | Status, Neustart-Zähler und letzter Start pro Bot, aktuelle Einstellungen |
| `start <discord\|twitch\|all>` | Bot(s) starten |
| `stop <discord\|twitch\|all> [timeoutSekunden]` | Bot(s) geordnet stoppen (kein Auto-Neustart danach) |
| `restart <discord\|twitch\|all> [timeoutSekunden]` | Bot(s) stoppen und sofort wieder starten |
| `set maxrestarts <n>` / `set restartdelay <sekunden>` | Neustart-Verhalten zur Laufzeit ändern (siehe unten) |
| `schedule add <discord\|twitch\|all> <restart\|stop\|start> interval <wert> [timeoutSekunden]` | Wiederkehrende Aufgabe anlegen, z.B. `schedule add all restart interval 6h` |
| `schedule add <discord\|twitch\|all> <restart\|stop\|start> daily <HH:mm> [timeoutSekunden]` | Tägliche Aufgabe anlegen, z.B. `schedule add discord restart daily 04:30` |
| `schedule list` | Zeigt alle geplanten Aufgaben mit nächster/letzter Ausführung |
| `schedule remove <id>` | Entfernt eine geplante Aufgabe |
| `schedule enable <id>` / `schedule disable <id>` | Aktiviert/deaktiviert eine geplante Aufgabe |
| `exit` | Alle Bots geordnet stoppen und den Launcher beenden |

### Scheduler (automatische Restarts & mehr)

Der Launcher enthält einen eingebauten Scheduler, mit dem sich wiederkehrende Aktionen (`restart`, `stop`, `start`)
pro Bot oder für alle Bots gemeinsam planen lassen - z.B. ein täglicher Neustart um 04:30 Uhr oder ein Neustart alle
6 Stunden. Aufgaben werden über den Konsolen-Befehl `schedule` verwaltet und in `scheduler-tasks.txt`
(Pfad konfigurierbar über `LAUNCHER_SCHEDULER_FILE`) persistiert, sodass sie einen Neustart des Launchers
überleben. Der Scheduler prüft alle 10 Sekunden, ob eine Aufgabe fällig ist.

```
schedule add all restart daily 04:30
schedule add discord restart interval 6h 20
schedule list
schedule disable s1
schedule remove s1
```

### Logging im kombinierten Betrieb

Da beide Bots im selben Prozess laufen, bringt der Launcher ein eigenes, nach Modul getrenntes Logback-Setup mit
(`launcher/src/main/resources/logback.xml`), das anhand des Java-Package-Namens automatisch zuordnet, statt alles
unstrukturiert in eine Datei zu schreiben:

| Datei | Inhalt |
|---|---|
| `logs/xbotenyy-discord.log` | Alle Logs aus `discordbot` sowie der JDA-Bibliothek |
| `logs/xbotenyy-twitch.log` | Alle Logs aus `twitchbot` |
| `logs/xbotenyy-launcher.log` | Gemeinsame Infrastruktur aus `common` sowie der Launcher selbst |
| `logs/audit.log` | Audit-Log beider Bots (wie im getrennten Betrieb auch gemeinsam) |

Beim Bau der JAR sorgt ein eigener Assembly-Descriptor (`launcher/src/main/assembly/with-dependencies.xml`) dafür,
dass die `logback.xml`-Dateien von `discordbot` und `twitchbot` **nicht** mit ins Fat-Jar gepackt werden - so bleibt
ausschließlich die Konfiguration des `launcher`-Moduls aktiv und es entsteht keine Ressourcen-Kollision im Classpath.

### Ausfallsicherheit

Jeder Bot läuft in einem eigenen, vom Launcher überwachten Thread. Stürzt ein Bot ab (z. B. durch einen
Programmierfehler oder eine unerwartete Exception), betrifft das **ausschließlich diesen einen Bot** - der jeweils
andere läuft unbeeinflusst weiter, und der Prozess selbst bleibt am Leben. Der Launcher startet den abgestürzten Bot
zusätzlich automatisch neu (Standard: bis zu 5 Versuche, 15 Sekunden Pause dazwischen). Beide Werte lassen sich ohne
Neubau der JAR per Umgebungsvariable vorbelegen **und zusätzlich zur Laufzeit über die Konsole ändern**
(`set maxrestarts <n>`, `set restartdelay <sekunden>`):

```env
LAUNCHER_MAX_RESTART_ATTEMPTS=5
LAUNCHER_RESTART_DELAY_SECONDS=15
```

Sind alle automatischen Versuche aufgebraucht, bleibt der Bot stehen und kann jederzeit manuell per Konsolen-Befehl
(`start <bot>`) neugestartet werden - der Neustart-Zähler wird dabei zurückgesetzt.

*Einzige Grenze: Läuft die JVM selbst gegen ein hartes Problem (z. B. `OutOfMemoryError` durch zu wenig
zugewiesenen Arbeitsspeicher), kann das trotzdem den gesamten Prozess und damit beide Bots betreffen - das lässt sich
nur durch ausreichend RAM für den kombinierten Betrieb vermeiden, nicht durch Code.*

### Reicht die Launcher-JAR allein aus?

Ja. `xBotenyyLauncher.jar` ist bereits ein eigenständiges Fat-Jar und enthält `common`, `discordbot` und `twitchbot`
vollständig. Es müssen **keine** zusätzlichen JARs (`xBotenyyDiscordBot.jar`, `xBotenyyTwitchBot.jar`) auf den Server
kopiert werden - nur `xBotenyyLauncher.jar` sowie wie gewohnt `.env`, `discordbot.properties` und
`twitchbot.properties` im selben Verzeichnis.

---

## Automatische Builds und Releases

Drei GitHub-Actions-Workflows unter `.github/workflows/` bauen alle drei Fat-Jars automatisch:

- **`build.yml`** läuft bei jedem Push und Pull Request auf `main` (`mvn clean verify`, inkl. Tests, Checkstyle,
  SpotBugs) und lädt `xBotenyyDiscordBot.jar`, `xBotenyyTwitchBot.jar` sowie `xBotenyyLauncher.jar` als
  Workflow-Artefakte hoch (14 Tage abrufbar unter dem jeweiligen Workflow-Lauf, **kein** GitHub-Release).
- **`latest-release.yml`** läuft ebenfalls bei jedem Push auf `main` und aktualisiert automatisch eine rollierende
  Vorab-Release namens „Latest Build" (Tag `latest`) mit den drei aktuellen Fat-Jars - so bekommst du auch ohne
  eigenes Tagging nach jedem Push sofort eine herunterladbare Release.
- **`release.yml`** läuft **nur** bei einem echten Versions-Tag (z. B. `v3.0.0`) oder manuell über
  „Run workflow" und veröffentlicht ein offizielles GitHub-Release mit allen drei Fat-Jars als Anhang.

Ein offizielles, versioniertes Release erstellst du also per:

```bash
git tag v3.0.0
git push origin v3.0.0
```