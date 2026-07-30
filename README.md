# xBotenyy

Discord- und Twitch-Bot als eigenständige Maven-Module (`common`, `discordbot`, `twitchbot`). Über das zusätzliche
Modul `launcher` können beide Bots auch gemeinsam in **einem** Prozess/Server gestartet werden.

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

   # Twitch Broadcaster-Account Tokens (für EventSub & Mod-Aktionen)
   TWITCH_BROADCASTER_ACCESS_TOKEN=
   TWITCH_BROADCASTER_REFRESH_TOKEN=
   ```

3. `discordbot.properties` / `twitchbot.properties` konfigurieren (werden beim ersten Start mit Standardwerten
   automatisch im jeweiligen Modulordner angelegt).

---

## Discord-Bot

### Features

| Slash-Command    | Funktion                                                                |
|------------------|-------------------------------------------------------------------------|
| `/ticket`        | Support-Ticket-System (Kategorien, Prioritäten, Auto-Close, Transcript) |
| `/report`        | Report-System für Mitglieder                                            |
| `/reports`       | Eigene Reports & Status einsehen                                        |
| `/reactionrole`  | Reaction Roles (Emoji oder Button)                                      |
| `/giveaway`      | Gewinnspiele                                                            |
| `/welcome`       | Willkommensnachrichten (mit Varianten)                                  |
| `/serverlog`     | Event-Logging (Joins, Bans, Boosts, gelöschte Nachrichten, ...)         |
| `/socials`       | YouTube- & Twitch-Benachrichtigungen                                    |
| `/automod`       | AutoMod-Diagnose (Konfiguration über `discordbot.properties`)           |
| `/message`       | Freie Nachrichten mit Platzhaltern                                      |
| `/info`, `/ping` | Bot-Info / Latenz                                                       |

### AutoMod

Wortfilter, Invite-/Link-Erkennung, Mention-Spam, Caps-Filter, Spam-/Duplikat-Erkennung, KI-Moderation (Groq,
`gpt-oss-safeguard-20b`), Strike-Eskalation (Warn → Timeout → Kick → Ban). Konfiguration ausschließlich über `automod.*`
in `discordbot.properties`.

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

| Befehl                                             | Berechtigung | Funktion                                              |
|----------------------------------------------------|--------------|-------------------------------------------------------|
| `!ping`                                            | alle         | Erreichbarkeits-Check                                 |
| `!uptime`                                          | alle         | Laufzeit des Bots                                     |
| `!strikes`                                         | alle         | Eigene AutoMod-Strikes                                |
| `!watchtime [nutzer]`                              | alle         | Watchtime des Nutzers                                 |
| `!followage [nutzer]`                              | alle         | Seit wann jemand folgt                                |
| `!automod` / `!mod`                                | Moderator    | AutoMod-Status abfragen                               |
| `!command add/remove/list/cooldown`                | Moderator    | Custom-Commands verwalten (inkl. Cooldown je Command) |
| `!commands` / `!help`                              | alle         | Listet Befehle auf                                    |
| `!broadcast add/remove/list`                       | Moderator    | Wiederkehrende Chat-Ansagen                           |
| `!eventlog`                                        | Moderator    | Event-Log-Einstellungen                               |
| `!timeout <nutzer> <sek> [grund]`                  | Moderator    | Timeoutet einen Nutzer                                |
| `!ban <nutzer> [grund]`                            | Moderator    | Bannt einen Nutzer dauerhaft                          |
| `!unban <nutzer>`                                  | Moderator    | Hebt Bann/Timeout auf                                 |
| `!purge` / `!clear`                                | Moderator    | Leert den kompletten Chat                             |
| `!permit <nutzer> [sek]`                           | Moderator    | Voruebergehende AutoMod-Ausnahme fuer einen Nutzer    |
| `!title [neuer titel]`                             | Moderator    | Zeigt/aendert den Stream-Titel                        |
| `!game [kategorie]`                                | Moderator    | Zeigt/aendert die Stream-Kategorie                    |
| `!so <kanal>`                                      | Moderator    | Shoutout fuer einen anderen Streamer                  |
| `!clip`                                            | alle         | Erstellt einen Clip vom aktuellen Moment              |
| `!quote [nummer]`                                  | alle         | Zeigt ein zufaelliges oder gezieltes Zitat            |
| `!quote add <text>`                                | Moderator    | Speichert ein neues Zitat                             |
| `!quote del <nummer>`                              | Moderator    | Entfernt ein gespeichertes Zitat                      |
| `!quote list`                                      | alle         | Zeigt Anzahl und erste gespeicherte Zitate            |
| `!poll start <frage> \| <opt1> \| <opt2> [\| ...]` | Moderator    | Startet eine Chat-Umfrage (max. 5 Optionen)           |
| `!poll results`                                    | alle         | Zeigt den aktuellen Zwischenstand der Umfrage         |
| `!poll end`                                        | Moderator    | Beendet die Umfrage und zeigt das Endergebnis         |
| `!vote <nummer>`                                   | alle         | Stimmt bei der laufenden Umfrage ab                   |

*Custom-Command-Antworten unterstützen die Platzhalter `{user}` und `{channel}`. Cooldown je Custom-Command
ueber `!command cooldown <name> <sekunden>`, Standard 5 Sekunden.*

### Zitat-System (`!quote`)

Zitate werden pro Kanal fortlaufend nummeriert und dauerhaft in der SQLite-Datenbank gespeichert (Tabelle
`twitch_quotes`). `!quote` ohne Argument liefert ein zufaelliges Zitat, `!quote <nummer>` ein bestimmtes.
Hinzufuegen und Entfernen ist Moderatoren vorbehalten, jeder Vorgang wird im Event-Log festgehalten.

### Umfrage-System (`!poll` / `!vote`)

Pro Kanal kann jeweils eine Umfrage aktiv sein (2-5 Antwortoptionen, durch `|` getrennt). Zuschauer stimmen mit
`!vote <nummer>` ab, Mehrfachstimmen desselben Nutzers ersetzen die vorherige Stimme. Umfragen leben nur im
Arbeitsspeicher des laufenden Prozesses (kein Neustart-Ueberdauern) und werden beim Start/Ende ins Event-Log
geschrieben.

### Twitch-Autorisierung (Generierung der Tokens)

Da der Bot über die moderne Helix-API agiert, müssen **sowohl der Broadcaster als auch der Bot-Account** autorisiert
werden.

#### 1. Autorisierung durch den Broadcaster (Streamer)

Führe diesen URL-Aufruf im Browser aus, während du im **Streamer-Account** eingeloggt bist. Erlaube dem Bot den Zugriff
auf Mod- und Follower-Daten sowie auf das Aendern von Titel/Kategorie:

```text
https://id.twitch.tv/oauth2/authorize?client_id=<TWITCH_CLIENT_ID>&redirect_uri=http://localhost:8080&response_type=code&scope=channel:bot+moderation:read+channel:read:subscriptions+channel:manage:broadcast+moderator:read:followers+moderator:read:chatters+moderator:manage:chat_messages+moderator:manage:banned_users&force_verify=true
```

*Tausche den empfangenen `?code=` aus der Adresszeile gegen das Access- und Refresh-Token für den **Broadcaster** und
trage es unter `TWITCH_BROADCASTER_ACCESS_TOKEN`/`TWITCH_BROADCASTER_REFRESH_TOKEN` in die `.env` ein. Dieses Token wird
ausschließlich fuer `!title` und `!game` benoetigt - ohne dieses Token bleiben beide Befehle im Lesemodus (nur Anzeige,
keine Aenderung).*

#### 2. Autorisierung durch den Bot-Account

Logge dich im Browser in den **Bot-Account** ein und rufe folgende URL auf:

```text
https://id.twitch.tv/oauth2/authorize?client_id=<TWITCH_CLIENT_ID>&redirect_uri=http://localhost:8080&response_type=code&scope=user:bot+user:write:chat+user:read:chat+clips:edit+moderator:manage:shoutouts+moderator:manage:automod&force_verify=true
```

*Tausche den empfangenen `?code=` gegen das Access- und Refresh-Token für den **Bot** und trage es in die `.env`
ein. `clips:edit` wird fuer `!clip`, `moderator:manage:shoutouts` fuer `!so`, `moderator:manage:automod` fuer das
native Twitch-AutoMod-Logging (siehe Abschnitt "Discord-Logging") benoetigt.*

#### 3. Code gegen Access- und Refresh-Token tauschen

Nachdem du in Schritt 1 oder 2 auf "Autorisieren" geklickt hast, leitet Twitch dich zu `http://localhost:8080/?code=...`
weiter (die Seite lädt nicht, das ist normal). Kopiere den Wert hinter `code=` aus der Adresszeile und tausche ihn *
*sofort** (Codes sind nur kurz gültig) gegen die Tokens ein.

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

### Discord-Logging (Twitch → Discord)

Der Twitch-Bot kann Chat-Nachrichten, AutoMod-Ereignisse (das eigene AutoMod **und** Twitchs natives AutoMod) sowie
Command-Nutzungen live per Discord-Webhook in einen Discord-Channel spiegeln. Da der Twitch-Bot als eigener Prozess
ohne Discord-Verbindung laeuft, geschieht das unabhaengig vom Discord-Bot ueber einen klassischen Discord-Webhook.

1. Lege in Discord im gewuenschten Log-Channel einen Webhook an (Channel-Einstellungen → Integrationen →
   Webhooks → Neuer Webhook) und kopiere die Webhook-URL.
2. Trage die URL in `twitchbot.properties` unter `discord.log.webhook.url` ein.
3. Steuere ueber folgende Schalter, was geloggt wird:

| Property                       | Standard | Bedeutung                                                   |
|--------------------------------|----------|-------------------------------------------------------------|
| `discord.log.webhook.url`      | *(leer)* | Discord-Webhook-URL, ohne URL ist das Feature deaktiviert   |
| `discord.log.messages.enabled` | `false`  | Loggt jede Chat-Nachricht (kann sehr viel Traffic erzeugen) |
| `discord.log.automod.enabled`  | `true`   | Loggt eigenes AutoMod **und** natives Twitch-AutoMod        |
| `discord.log.commands.enabled` | `true`   | Loggt jede Nutzung eines eingebauten oder Custom-Commands   |

Fuer das native Twitch-AutoMod (Nachrichten, die Twitch selbst zur Pruefung zurueckhaelt) abonniert der Bot
automatisch die EventSub-Typen `automod.message.hold` und `automod.message.update`. Dafuer wird der Scope
`moderator:manage:automod` auf dem Bot-Account-Token benoetigt (siehe Schritt 2 der Twitch-Autorisierung oben).

Auf der Discord-Bot-Seite wird die Nutzung von Discord-Slash-Commands ueber das bestehende Event-Log-System
geloggt (`/eventlog` bzw. `/serverlog`, Event-Typ "Command-Nutzung") und benoetigt keine zusaetzliche Konfiguration.

### AutoMod-Mapping

| Discord-Key                    | Twitch-Bedeutung                          |
|--------------------------------|-------------------------------------------|
| `automod.exempt.role.ids`      | Ausgenommene Twitch-Logins (Usernames)    |
| `automod.exempt.channel.ids`   | Ausgenommene Twitch-Kanäle                |
| `automod.bypass.manage-server` | Bypass für Channel-Moderatoren            |
| `automod.bypass.administrator` | Bypass für den Broadcaster (Kanalinhaber) |

### Persistenz

Eigene SQLite-DB (`twitch.database.file`), nutzt die identische `Database`/`Jdbc`/`SchemaMigrator`-Schicht wie der
Discord-Bot.
Tabellen: `twitch_channels`, `twitch_custom_commands` (inkl. `cooldown_seconds`), `twitch_watchtime`,
`twitch_broadcasts`, `twitch_event_log`, `twitch_quotes`.

### Weitere Einstellungen

- `twitch.automod.permit.default.seconds` (Standard `30`): Dauer der AutoMod-Ausnahme, wenn `!permit <nutzer>` ohne
  Sekundenangabe genutzt wird.

---

## Erweiterte Moderation (Discord ↔ Twitch)

Zusaetzlich zum automatischen AutoMod gibt es ein vollstaendiges manuelles Moderationssystem mit eigener
Fall-Historie, konfigurierbaren Straf-/Moderator-Rollen und optionaler Synchronisation zwischen Discord und Twitch.

### Discord: `/mod` und `/modroles`

| Befehl                                   | Beschreibung                                                      |
|------------------------------------------|-------------------------------------------------------------------|
| `/mod warn user:<@nutzer>`               | Verwarnt ein Mitglied, vergibt optional die Warn-Rolle            |
| `/mod timeout user: dauer:`              | Timeout (z. B. `10m`, `1h`, `1d`, `1w`, max. 28 Tage)             |
| `/mod untimeout user:`                   | Hebt einen Timeout auf                                            |
| `/mod kick user:`                        | Kickt ein Mitglied                                                |
| `/mod ban user:`                         | Bannt einen Nutzer (auch per ID, ohne dass er Mitglied sein muss) |
| `/mod unban user:`                       | Hebt einen Bann auf                                               |
| `/mod cases user:`                       | Zeigt die vollstaendige Moderations-Historie eines Nutzers        |
| `/modroles warn-role/mute-role/ban-role` | Legt die drei automatisch vergebenen Straf-Rollen fest            |
| `/modroles add-moderator/add-admin`      | Erlaubt zusaetzlichen Rollen die Nutzung von `/mod`/`/modroles`   |
| `/modroles status`                       | Zeigt die aktuelle Rollen-Konfiguration                           |

`/mod`-Befehle duerfen von Mitgliedern mit `ADMINISTRATOR`/`Mitglieder moderieren`-Rechten sowie allen ueber
`/modroles add-moderator` freigegebenen Rollen genutzt werden. `/modroles` selbst erfordert `ADMINISTRATOR` oder
eine ueber `add-admin` freigegebene Rolle. Bans und Timeouts erscheinen automatisch auch im normalen `/serverlog`
(Discord meldet diese Events selbst); Kicks und Verwarnungen werden zusaetzlich als eigene Log-Typen erfasst.

### Twitch: `!warn`, `!ban`, `!timeout`, `!unban`

Die bereits vorhandenen Mod-Commands (`!ban`, `!timeout`/`!to`, `!unban`/`!untimeout`) sowie das neue `!warn`
schreiben jetzt zusaetzlich in die gemeinsame Fall-Historie und loesen - falls konfiguriert - eine Synchronisation
zum verknuepften Discord-Account aus.

### Accounts verknuepfen

Die Verknuepfung ist fuer Nutzer komplett **freiwillig** - ohne sie funktionieren Server, Kanal und alle anderen
Features normal weiter. Sie lohnt sich, weil dadurch Twitch-Rollen (Subscriber, VIP, Moderator, Broadcaster)
automatisch als Discord-Rolle gespiegelt werden koennen und - falls die Bridge aktiviert ist - Moderationsmaßnahmen
zwischen Discord und Twitch synchronisiert werden. Gespeichert wird dabei ausschließlich die Zuordnung
Discord-ID ↔ Twitch-ID/-Login, keine Passwoerter oder Tokens.

Die Verknuepfung funktioniert in beide Richtungen und erfordert immer eine Bestaetigung auf der jeweils anderen
Plattform (kein reines Vertrauen auf Zuruf):

- **Panel (empfohlen):** `/link panel channel:<kanal>` (Admin) postet ein Embed mit einem "🔗 Verknuepfen"-Button
  in den angegebenen Kanal - aehnlich wie beim Ticket-System. Klick auf den Button oeffnet ein kleines Formular
  fuer den Twitch-Loginnamen, der Rest laeuft wie unten beschrieben weiter.
- **Start auf Discord (Command):** `/link twitch login:<name>` → Bot nennt einen Code → Nutzer postet
  `!verify <code>` im Twitch-Chat des angegebenen Kanals.
- **Start auf Twitch:** `!link` im Chat → Bot nennt einen Code → Nutzer fuehrt `/link verify code:<code>` auf
  Discord aus.
- `/link status` / `/link unlink` verwalten die eigene Verknuepfung auf Discord-Seite.

### Twitch-Rollen mit Discord synchronisieren

Ueber `/modroles sync-role status:<subscriber|vip|moderator|broadcaster> role:<@rolle>` legst du fest, welche
Discord-Rolle fuer welchen Twitch-Status automatisch vergeben werden soll (Aufruf ohne `role` entfernt die
Zuordnung wieder). `/modroles status` zeigt die aktuelle Konfiguration.

Der Twitch-Bot erkennt den Status eines verknuepften Nutzers automatisch anhand der Chat-Badges, sobald dieser
im Twitch-Chat schreibt (kein Polling noetig) und gleicht die Discord-Rolle darueber die Bridge ab - sowohl beim
Erhalten als auch beim Verlieren eines Status (z. B. nach Ablauf des Abos, sobald erneut geschrieben wird).

### Die Bridge (Discord ↔ Twitch)

Da beide Bots als getrennte Prozesse - auch auf unterschiedlichen Servern - laufen koennen, sprechen sie ueber eine
kleine, in beide Bots eingebaute HTTP-Schnittstelle miteinander ("Bridge"). Es wird **keine** zusaetzliche Software
benoetigt; jeder Bot kann optional einen kleinen HTTP-Server starten und den des jeweils anderen Bots aufrufen.

| Property (beide Bots) | Standard      | Bedeutung                                                           |
|-----------------------|---------------|---------------------------------------------------------------------|
| `bridge.enabled`      | `false`       | Startet den eigenen Bridge-Server (muss vom Peer erreichbar sein)   |
| `bridge.port`         | `8082`/`8083` | Port des eigenen Bridge-Servers                                     |
| `bridge.token`        | *(leer)*      | Gemeinsames Shared-Secret - **muss auf beiden Bots identisch sein** |
| `bridge.peer.url`     | *(leer)*      | Basis-URL des jeweils anderen Bots, z. B. `http://localhost:8083`   |

Zusaetzlich (jeweils nur auf einer Seite):

- **Discordbot:** `moderation.sync.guild.id` - die Discord-Server-ID, in der synchronisierte Aktionen ausgefuehrt
  werden (dieses Bot-Setup ist auf eine Community/einen Server ausgelegt).
- **Twitchbot:** `moderation.sync.channel` - der Twitch-Kanal, in dem synchronisierte Aktionen ausgefuehrt werden
  (leer = erster Kanal aus `twitch.chat.channels`).

**Setup fuer zwei getrennte Server:** `bridge.enabled=true` auf beiden Seiten, `bridge.token` identisch setzen,
`bridge.peer.url` jeweils auf die oeffentlich erreichbare Adresse des anderen Bots zeigen lassen (Firewall/Reverse
Proxy beachten - die Bridge hat außer dem Shared-Secret keine weitere Absicherung).
**Setup ueber den Launcher (ein Server):** `bridge.peer.url` einfach auf `http://localhost:<port-des-anderen-bots>`
setzen, beide Ports muessen sich unterscheiden.

Ohne `bridge.peer.url` funktioniert alles andere (manuelle Commands, Rollen, Fall-Historie) weiterhin normal -
es wird lediglich keine Aktion zur anderen Plattform gespiegelt.

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

| Befehl                                                | Beschreibung                                                              |
|-------------------------------------------------------|---------------------------------------------------------------------------|
| `help`                                                | Übersicht aller Befehle                                                   |
| `status`                                              | Status, Neustart-Zähler und letzter Start pro Bot, aktuelle Einstellungen |
| `start <discord\|twitch\|all>`                        | Bot(s) starten                                                            |
| `stop <discord\|twitch\|all> [timeoutSekunden]`       | Bot(s) geordnet stoppen (kein Auto-Neustart danach)                       |
| `restart <discord\|twitch\|all> [timeoutSekunden]`    | Bot(s) stoppen und sofort wieder starten                                  |
| `set maxrestarts <n>` / `set restartdelay <sekunden>` | Neustart-Verhalten zur Laufzeit ändern (siehe unten)                      |
| `exit`                                                | Alle Bots geordnet stoppen und den Launcher beenden                       |

### Logging im kombinierten Betrieb

Da beide Bots im selben Prozess laufen, bringt der Launcher ein eigenes, nach Modul getrenntes Logback-Setup mit
(`launcher/src/main/resources/logback.xml`), das anhand des Java-Package-Namens automatisch zuordnet, statt alles
unstrukturiert in eine Datei zu schreiben:

| Datei                        | Inhalt                                                           |
|------------------------------|------------------------------------------------------------------|
| `logs/xbotenyy-discord.log`  | Alle Logs aus `discordbot` sowie der JDA-Bibliothek              |
| `logs/xbotenyy-twitch.log`   | Alle Logs aus `twitchbot`                                        |
| `logs/xbotenyy-launcher.log` | Gemeinsame Infrastruktur aus `common` sowie der Launcher selbst  |
| `logs/audit.log`             | Audit-Log beider Bots (wie im getrennten Betrieb auch gemeinsam) |

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

### Automatische Builds und Releases

Drei GitHub-Actions-Workflows unter `.github/workflows/` bauen alle drei Fat-Jars automatisch:

- **`build.yml`** läuft bei jedem Push und Pull Request auf `main` (`mvn clean verify`, inkl. Tests, Checkstyle,
  SpotBugs) und lädt `xBotenyyDiscordBot.jar`, `xBotenyyTwitchBot.jar` sowie `xBotenyyLauncher.jar` als
  Workflow-Artefakte hoch (14 Tage abrufbar unter dem jeweiligen Workflow-Lauf, **kein** GitHub-Release).
- **`latest-release.yml`** läuft ebenfalls bei jedem Push auf `main` und aktualisiert automatisch eine rollierende
  Vorab-Release namens „Latest Build“ (Tag `latest`) mit den drei aktuellen Fat-Jars - so bekommst du auch ohne
  eigenes Tagging nach jedem Push sofort eine herunterladbare Release.
- **`release.yml`** läuft **nur** bei einem echten Versions-Tag (z. B. `v3.0.0`) oder manuell über
  „Run workflow“ und veröffentlicht ein offizielles GitHub-Release mit allen drei Fat-Jars als Anhang.

Ein offizielles, versioniertes Release erstellst du also per:

```bash
git tag v3.0.0
git push origin v3.0.0
```

> ⚠️ **IntelliJ-Hinweis:** Ein Tag über *Git → New Tag* anlegen reicht nicht - IntelliJ pusht Tags standardmäßig
> **nicht** automatisch mit. Im Push-Dialog musst du unter „Push Tags“ explizit „All“ auswählen, oder den Tag
> per Terminal pushen (`git push origin v3.0.0`). Ein normaler Commit-Push ohne Tag löst nur `build.yml` und
> `latest-release.yml` aus, aber kein offizielles, versioniertes Release.
