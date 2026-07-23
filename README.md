# xBotenyy

Discord- und Twitch-Bot als eigenständige Maven-Module (`common`, `discordbot`, `twitchbot`).

## Voraussetzungen

- JDK 21
- Maven 3.9+
- Discord-Bot-Token (für `discordbot`)
- Twitch Client-ID + Client-Secret (für `twitchbot` & Twitch-Integration im `discordbot`)

### API-Portale konfigurieren
* **Discord Developer Portal:** `Server Members Intent` und `Message Content Intent` zwingend aktivieren.
* **Twitch Developer Console:** App registrieren und `OAuth Redirect URL` auf `http://localhost:8080` (oder die URL deines OAuth-Handlers) setzen.

## Setup

1. Repository klonen.
2. `.env` Datei im **Hauptverzeichnis (Projekt-Root)** erstellen. *Hinweis: Niemals unter `src/main/resources` ablegen, da die Werte sonst in die JAR kompiliert werden!*

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

   # Twitch Broadcaster-Account Tokens (für EventSub & Mod-Aktionen)
   TWITCH_BROADCASTER_ACCESS_TOKEN=
   TWITCH_BROADCASTER_REFRESH_TOKEN=
   ```

3. `discordbot.properties` / `twitchbot.properties` konfigurieren (werden beim ersten Start mit Standardwerten automatisch im jeweiligen Modulordner angelegt).

---

## Discord-Bot

### Features

| Slash-Command   | Funktion                                     |
|-----------------|-----------------------------------------------|
| `/ticket`       | Support-Ticket-System (Kategorien, Prioritäten, Auto-Close, Transcript) |
| `/report`       | Report-System für Mitglieder                  |
| `/reports`      | Eigene Reports & Status einsehen              |
| `/reactionrole` | Reaction Roles (Emoji oder Button)            |
| `/giveaway`     | Gewinnspiele                                  |
| `/welcome`      | Willkommensnachrichten (mit Varianten)        |
| `/serverlog`    | Event-Logging (Joins, Bans, Boosts, gelöschte Nachrichten, ...) |
| `/socials`      | YouTube- & Twitch-Benachrichtigungen          |
| `/automod`      | AutoMod-Diagnose (Konfiguration über `discordbot.properties`) |
| `/message`      | Freie Nachrichten mit Platzhaltern            |
| `/info`, `/ping`| Bot-Info / Latenz                             |

### AutoMod
Wortfilter, Invite-/Link-Erkennung, Mention-Spam, Caps-Filter, Spam-/Duplikat-Erkennung, KI-Moderation (Groq, `gpt-oss-safeguard-20b`), Strike-Eskalation (Warn → Timeout → Kick → Ban). Konfiguration ausschließlich über `automod.*` in `discordbot.properties`.

### Interne Systeme
- SQLite-Database mit automatischer Schema-Migration beim Start
- Automatische Backups (Rotation über `backup.*`)
- Audit-Log für administrative Aktionen
- Retry-Handling für Discord-API-Aufrufe
- Heartbeat-Logging mit Metriken pro Feature
- Automatische Bereinigung veralteter Datensätze

---

## Twitch-Bot

Eigenständiges Modul. Verbindet sich per modernen Helix-Chat-API + EventSub-Websockets.

### Chat-Commands
Prefix konfigurierbar (`twitch.chat.command.prefix`, Standard `!`):

| Befehl                      | Berechtigung | Funktion                                    |
|------------------------------|--------------|----------------------------------------------|
| `!ping`                      | alle         | Erreichbarkeits-Check                        |
| `!uptime`                     | alle         | Laufzeit des Bots                            |
| `!strikes`                    | alle         | Eigene AutoMod-Strikes                       |
| `!watchtime [nutzer]`         | alle         | Watchtime des Nutzers                        |
| `!followage [nutzer]`         | alle         | Seit wann jemand folgt                       |
| `!automod` / `!mod`           | Moderator    | AutoMod-Status abfragen                      |
| `!command add/remove/list`    | Moderator    | Custom-Commands verwalten                    |
| `!commands` / `!help`         | alle         | Listet Befehle auf                           |
| `!broadcast add/remove/list`  | Moderator    | Wiederkehrende Chat-Ansagen                  |
| `!eventlog`                   | Moderator    | Event-Log-Einstellungen                      |

*Custom-Command-Antworten unterstützen die Platzhalter `{user}` und `{channel}`.*

### Twitch-Autorisierung (Generierung der Tokens)

Da der Bot über die moderne Helix-API agiert, müssen **sowohl der Broadcaster als auch der Bot-Account** autorisiert werden.

#### 1. Autorisierung durch den Broadcaster (Streamer)
Führe diesen URL-Aufruf im Browser aus, während du im **Streamer-Account** eingeloggt bist. Erlaube dem Bot den Zugriff auf Mod- und Follower-Daten:
```text
https://id.twitch.tv/oauth2/authorize?client_id=<TWITCH_CLIENT_ID>&redirect_uri=http://localhost:8080&response_type=code&scope=channel:bot+channel:read:moderators+channel:read:subscriptions+moderator:read:followers+moderator:read:chatters+moderator:manage:chat_messages+moderator:manage:banned_users&force_verify=true
```
*Tausche den empfangenen `?code=` aus der Adresszeile gegen das Access- und Refresh-Token für den **Broadcaster** und trage es in die `.env` ein.*

#### 2. Autorisierung durch den Bot-Account
Logge dich im Browser in den **Bot-Account** ein und rufe folgende URL auf:
```text
https://id.twitch.tv/oauth2/authorize?client_id=<TWITCH_CLIENT_ID>&redirect_uri=http://localhost:8080&response_type=code&scope=user:bot+user:write:chat+user:read:chat&force_verify=true
```
*Tausche den empfangenen `?code=` gegen das Access- und Refresh-Token für den **Bot** und trage es in die `.env` ein.*

#### 3. Kanal-Rechte vergeben
1. Trage den Bot-Account im Kanal des Broadcasters als Moderator ein (`/mod <botname>`).
2. Trage den Zielkanal in den `twitchbot.properties` unter `twitch.chat.channels` ein.

### AutoMod-Mapping

| Discord-Key                     | Twitch-Bedeutung                          |
|----------------------------------|--------------------------------------------|
| `automod.exempt.role.ids`        | Ausgenommene Twitch-Logins (Usernames)     |
| `automod.exempt.channel.ids`     | Ausgenommene Twitch-Kanäle                 |
| `automod.bypass.manage-server`   | Bypass für Channel-Moderatoren             |
| `automod.bypass.administrator`   | Bypass für den Broadcaster (Kanalinhaber) |

### Persistenz
Eigene SQLite-DB (`twitch.database.file`), nutzt die identische `Database`/`Jdbc`/`SchemaMigrator`-Schicht wie der Discord-Bot.
Tabellen: `twitch_channels`, `twitch_custom_commands`, `twitch_watchtime`, `twitch_broadcasts`, `twitch_event_log`.
