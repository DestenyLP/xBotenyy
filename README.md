# xBotenyy

Discord and Twitch bot as standalone Maven modules (`common`, `discordbot`, `twitchbot`). Via the additional
module `launcher`, both bots can also be started together in **one** process/server. Both bots can
optionally talk to each other via a built-in bridge (moderation sync, role sync, account linking).

## Table of contents

- [Requirements](#requirements)
- [Setup](#setup)
- [Discord Bot](#discord-bot)
    - [Command reference](#discord-command-reference)
    - [AutoMod](#discord-automod)
    - [Event logging (`/serverlog`)](#event-logging-serverlog)
    - [Internal systems](#internal-systems)
- [Twitch Bot](#twitch-bot)
    - [Command reference](#twitch-command-reference)
    - [Quote & poll system](#quote-and-poll-system)
    - [Twitch authorization](#twitch-authorization-generating-the-tokens)
    - [AutoMod mapping](#automod-mapping)
    - [Persistence](#persistence)
- [Discord ↔ Twitch integration](#discord--twitch-integration)
    - [Discord logging (Twitch → Discord)](#discord-logging-twitch--discord)
    - [Follow / Subscribe / Raid alerts](#follow--subscribe--raid-alerts)
    - [Advanced moderation](#advanced-moderation)
    - [Linking accounts](#linking-accounts)
    - [Syncing Twitch roles](#syncing-twitch-roles-with-discord)
    - [The bridge](#the-bridge-discord--twitch)
- [Combined start (`launcher`)](#combined-start-launcher)
- [Automatic builds and releases](#automatic-builds-and-releases)

---

## Requirements

- JDK 21 (to run the bot, whether via a downloaded JAR or a build you did yourself)
- Maven 3.9+ (only needed if you build from source yourself - not required if you just download a
  ready-made JAR from [Releases](../../releases))
- Discord bot token (for `discordbot`)
- Twitch client ID + client secret (for `twitchbot` & the Twitch integration in `discordbot`)

### Configuring the API portals

* **Discord Developer Portal:** you must enable `Server Members Intent` and `Message Content Intent`.
* **Twitch Developer Console:** register an app and set the `OAuth Redirect URL` to `http://localhost:8080`
  (or the URL of your OAuth handler).

## Setup

There are two ways to get a running bot: download a ready-made JAR (no build tools needed), or build the
project yourself from source.

### Option A: Download a ready-made JAR (recommended for most users)

1. Go to the [Releases](../../releases) page and download the JAR you need from the latest release:
    - `xBotenyyDiscordBot.jar` - Discord bot only
    - `xBotenyyTwitchBot.jar` - Twitch bot only
    - `xBotenyyLauncher.jar` - both bots combined in one process (see
      [Combined start](#combined-start-launcher)); this single JAR already contains everything, you do
      **not** need the other two JARs alongside it
2. Put the JAR in an empty folder on your server/PC.
3. Create an `.env` file **in that same folder, right next to the JAR**. *Note: this is different from
   building from source - there is no `src/main/resources` here, the `.env` just needs to sit next to the
   JAR file.*

   ```env
   # Discord configuration
   BOT_TOKEN=
   GROQ_API_KEY=

   # Twitch app credentials
   TWITCH_CLIENT_ID=
   TWITCH_CLIENT_SECRET=

   # Twitch bot account tokens
   TWITCH_BOT_ACCESS_TOKEN=
   TWITCH_BOT_REFRESH_TOKEN=

   # Twitch broadcaster account tokens (for EventSub, mod actions & role sync)
   TWITCH_BROADCASTER_ACCESS_TOKEN=
   TWITCH_BROADCASTER_REFRESH_TOKEN=
   ```

4. Start the bot:

   ```bash
   java -jar xBotenyyDiscordBot.jar
   # or xBotenyyTwitchBot.jar / xBotenyyLauncher.jar, depending on what you downloaded
   ```

5. Configure `discordbot.properties` / `twitchbot.properties` (these are created automatically with default
   values in the same folder on first start).

### Option B: Build from source

1. Clone the repository.
2. Create an `.env` file in the **root directory (project root)**. *Note: never place it under `src/main/resources`,
   otherwise the values will be compiled into the JAR!*

   ```env
   # Discord configuration
   BOT_TOKEN=
   GROQ_API_KEY=

   # Twitch app credentials
   TWITCH_CLIENT_ID=
   TWITCH_CLIENT_SECRET=

   # Twitch bot account tokens
   TWITCH_BOT_ACCESS_TOKEN=
   TWITCH_BOT_REFRESH_TOKEN=

   # Twitch broadcaster account tokens (for EventSub, mod actions & role sync)
   TWITCH_BROADCASTER_ACCESS_TOKEN=
   TWITCH_BROADCASTER_REFRESH_TOKEN=
   ```

3. Build and run, e.g. for the Discord bot:

   ```bash
   mvn -pl discordbot -am -DskipTests package
   java -jar discordbot/target/xBotenyyDiscordBot.jar
   ```

4. Configure `discordbot.properties` / `twitchbot.properties` (these are created automatically with default
   values in the respective module folder on first start).

---

## Discord Bot

Systems: support tickets, report system, reaction roles, giveaways, welcome messages, event logging,
YouTube/Twitch announcements, AI-powered AutoMod, advanced manual moderation with punishment roles and
Discord ↔ Twitch linking.

### Discord command reference

#### Server & community

| Command | Subcommands | Function |
|---|---|---|
| `/ticket` | `panel`, `settings`, `list`, `claim`, `unclaim`, `close`, `priority`, `add`, `remove` | Support ticket system with categories, priorities, claiming, auto-close and transcripts |
| `/report` | `send`, `settings` | Report system - members report incidents via a guided dialog, the team configures the channel/role |
| `/reports` | – | Shows your own submitted reports and their status |
| `/reactionrole` | `add`, `remove`, `list` | Reaction roles via emoji or button on any message |
| `/giveaway` | `start`, `end`, `reroll`, `cancel`, `list` | Giveaway system with multiple winners |
| `/welcome` | `settings`, `add`, `edit`, `remove`, `list`, `test`, `placeholders` | Welcome messages with multiple variants and placeholders |
| `/socials` | `add`, `edit`, `message`, `remove`, `list`, `status`, `test`, `placeholders` | YouTube, Twitch & TikTok announcements |
| `/message` | `create`, `edit`, `placeholders` | Freeform messages with placeholders (e.g. as a basis for reaction roles) |

#### Moderation

| Command | Subcommands | Function |
|---|---|---|
| `/mod` | `warn`, `timeout`, `untimeout`, `kick`, `ban`, `unban`, `cases` | Manual moderation with case history (see [Advanced moderation](#advanced-moderation)) |
| `/modroles` | `warn-role`, `mute-role`, `ban-role`, `add-moderator`, `remove-moderator`, `add-admin`, `remove-admin`, `sync-role`, `status` | Configures punishment roles, who may use `/mod`, and the Twitch role sync |
| `/link` | `twitch`, `verify`, `status`, `unlink`, `panel` | Discord ↔ Twitch account linking (optional, see [Linking accounts](#linking-accounts)) |
| `/automod` | `status`, `test` | AutoMod diagnostics (configuration exclusively via `discordbot.properties`) |
| `/serverlog` | `setup`, `channel`, `toggle`, `event-channel`, `status` | Configures event logging (see [Event logging](#event-logging-serverlog)) |

#### Utility

| Command | Function |
|---|---|
| `/info` | Bot information |
| `/ping` | Bot latency |

*Permissions: `/mod` is available to members with `ADMINISTRATOR`/`Moderate Members` as well as any
role granted via `/modroles add-moderator`. `/modroles`, `/serverlog`, `/automod` and the
team subcommands of `/ticket`/`/report` require `ADMINISTRATOR`/`Manage Server` (or, in the case of `/modroles`,
also roles granted via `add-admin`).*

### Discord AutoMod

Word filter, invite/link detection, mention spam, caps filter, spam/duplicate detection, AI moderation (Groq,
`gpt-oss-safeguard-20b`), strike escalation (warn → timeout → kick → ban). Configuration exclusively via
`automod.*` in `discordbot.properties`. Diagnostics and testing of individual texts via `/automod status` / `/automod test`.

### Event logging (`/serverlog`)

Freely configurable per event type: default channel, an individual channel per type, and can be toggled on/off individually.
`/serverlog setup` sets everything up with a default channel, `/serverlog status` shows the current configuration.

| Event type | Meaning |
|---|---|
| Member joined / left | Server joins and leaves |
| Server boosted / boost removed | Nitro boost activity |
| Member banned / ban lifted | Discord reports this natively, regardless of the source (AutoMod, `/mod`, manual) |
| Member timed out / timeout lifted | Also captured natively, regardless of the source |
| Member kicked | Only via `/mod kick`, since Discord does not natively distinguish kicks from normal leaves |
| Member warned | Only via `/mod warn`, since warnings are not a native Discord concept |
| Nickname changed | Changes to a member's server name |
| Roles updated | Role changes on a member |
| Voice activity | Joining/leaving voice channels |
| Channel created / deleted | Server structure changes |
| Message deleted | Deleted messages including content (up to `eventlog.message-delete.content-max-length` characters) |
| Command usage | Every slash command execution including status (executed/no permission/cooldown/error) |

### Internal systems

- SQLite database with automatic schema migration on startup
- Automatic backups (rotation via `backup.*`)
- Audit log for administrative actions
- Retry handling for Discord API calls
- Heartbeat logging with metrics per feature
- Automatic cleanup of stale records

---

## Twitch Bot

Standalone module. Connects via the modern Helix chat API + EventSub websockets. Systems: custom commands,
quotes, polls, recurring announcements, watchtime tracking, AI-powered AutoMod, advanced manual
moderation, as well as optional Discord integration (logging, moderation sync, role sync).

### Twitch command reference

Prefix configurable (`twitch.chat.command.prefix`, default `!`):

| Command                                               | Permission | Function                                                 |
|-------------------------------------------------------|---|----------------------------------------------------------|
| `!ping`                                               | everyone | Reachability check                                       |
| `!uptime`                                             | everyone | Bot uptime                                               |
| `!strikes`                                            | everyone | Your own AutoMod strikes                                 |
| `!watchtime [user]`                                   | everyone | Watchtime of the user                                    |
| `!followage [user]`                                   | everyone | How long someone has been following                      |
| `!link`                                               | everyone | Starts linking with your own Discord account             |
| `!verify <code>`                                      | everyone | Confirms a code generated on Discord with `/link twitch` |
| `!automod` / `!mod`                                   | moderator | Queries AutoMod status                                   |
| `!command add/remove/list/cooldown`                   | moderator | Manages custom commands (incl. cooldown per command)     |
| `!commands` / `!help`                                 | everyone | Lists commands                                           |
| `!broadcast add/remove/list`                          | moderator | Recurring chat announcements                             |
| `!eventlog`                                           | moderator | Event log settings                                       |
| `!warn <user> [reason]`                               | moderator | Manually warns a user                                    |
| `!timeout <user> <sec> [reason]`                      | moderator | Times out a user                                         |
| `!ban <user> [reason]`                                | moderator | Permanently bans a user                                  |
| `!unban <user>`                                       | moderator | Lifts a ban/timeout                                      |
| `!purge` / `!clear`                                   | moderator | Clears the entire chat                                   |
| `!permit <user> [sec]`                                | moderator | Temporary AutoMod exemption for a user                   |
| `!title [new title]`                                  | moderator | Shows/changes the stream title                           |
| `!game [category]`                                    | moderator | Shows/changes the stream category                        |
| `!so <channel>`                                       | moderator | Shoutout for another streamer                            |
| `!clip`                                               | everyone | Creates a clip of the current moment                     |
| `!quote [number]`                                     | everyone | Shows a random or specific quote                         |
| `!quote add <text>`                                   | moderator | Saves a new quote                                        |
| `!quote del <number>`                                 | moderator | Removes a saved quote                                    |
| `!quote list`                                         | everyone | Shows the count and first saved quotes                   |
| `!poll start <question> \| <opt1> \| <opt2> [\| ...]` | moderator | Starts a chat poll (max. 5 options)                      |
| `!poll results`                                       | everyone | Shows the current interim results of the poll            |
| `!poll end`                                           | moderator | Ends the poll and shows the final result                 |
| `!vote <number>`                                      | everyone | Votes in the running poll                                |
| `!v`                                                  | everyone | Vanish from the chat for ever                            |

*Custom command responses support the placeholders `{user}` and `{channel}`. Cooldown per custom command
via `!command cooldown <name> <seconds>`, default 5 seconds. `!ban`/`!timeout`/`!unban`/`!warn` also write
to the shared case history and - if configured - trigger a sync to the linked
Discord account (see [Advanced moderation](#advanced-moderation)).*

### Quote and poll system

**Quotes (`!quote`):** Quotes are numbered sequentially per channel and stored permanently in the SQLite database
(table `twitch_quotes`). `!quote` without an argument returns a random quote, `!quote <number>` a
specific one. Adding and removing is reserved for moderators, and every action is recorded in the event log.

**Polls (`!poll` / `!vote`):** One poll can be active per channel at a time (2-5 answer options, separated by `|`).
Viewers vote with `!vote <number>`; multiple votes from the same user replace the previous
vote. Polls live only in the running process's memory (they do not survive a restart) and are written to
the event log on start/end.

### Twitch authorization (generating the tokens)

Since the bot operates via the modern Helix API, **both the broadcaster and the bot account** need to be
authorized.

#### 1. Authorization by the broadcaster (streamer)

Run this URL call in your browser while logged into the **streamer account**. Grant the bot access
to mod and follower data as well as to changing the title/category:

```text
https://id.twitch.tv/oauth2/authorize?client_id=<TWITCH_CLIENT_ID>&redirect_uri=http://localhost:8080&response_type=code&scope=channel:bot+moderation:read+channel:read:subscriptions+channel:read:vips+channel:manage:broadcast+moderator:read:followers+moderator:read:chatters+moderator:manage:chat_messages+moderator:manage:banned_users&force_verify=true
```

*Exchange the `?code=` received in the address bar for the access and refresh token for the **broadcaster** and
enter it under `TWITCH_BROADCASTER_ACCESS_TOKEN`/`TWITCH_BROADCASTER_REFRESH_TOKEN` in the `.env` file. This token is
required for `!title`/`!game` as well as for the periodic Twitch role sync (subscriber/VIP/moderator, see
[Syncing Twitch roles](#syncing-twitch-roles-with-discord)) - without this token,
`!title`/`!game` stay in read-only mode and the periodic sync is skipped (the ongoing chat-based sync
still works).*

#### 2. Authorization by the bot account

Log into your browser with the **bot account** and open the following URL:

```text
https://id.twitch.tv/oauth2/authorize?client_id=<TWITCH_CLIENT_ID>&redirect_uri=http://localhost:8080&response_type=code&scope=user:bot+user:write:chat+user:read:chat+clips:edit+moderator:manage:shoutouts+moderator:manage:automod&force_verify=true
```

*Exchange the received `?code=` for the access and refresh token for the **bot** and enter it in the `.env`
file. `clips:edit` is required for `!clip`, `moderator:manage:shoutouts` for `!so`, `moderator:manage:automod` for the
native Twitch AutoMod logging (see [Discord logging](#discord-logging-twitch--discord)).*

#### 3. Exchanging the code for an access and refresh token

After clicking "Authorize" in step 1 or 2, Twitch redirects you to `http://localhost:8080/?code=...`
(the page won't load, that's normal). Copy the value after `code=` from the address bar and exchange it
**immediately** (codes are only valid briefly) for the tokens.

**Windows / PowerShell:**

```powershell
Invoke-RestMethod -Uri "https://id.twitch.tv/oauth2/token" -Method Post -Body @{ client_id="<TWITCH_CLIENT_ID>"; client_secret="<TWITCH_CLIENT_SECRET>"; code="<CODE_FROM_THE_URL>"; grant_type="authorization_code"; redirect_uri="http://localhost:8080" }
```

**macOS / Linux (or `curl.exe` on Windows):**

```bash
curl -X POST https://id.twitch.tv/oauth2/token -d client_id=<TWITCH_CLIENT_ID> -d client_secret=<TWITCH_CLIENT_SECRET> -d code=<CODE_FROM_THE_URL> -d grant_type=authorization_code -d redirect_uri=http://localhost:8080
```

The response contains `access_token` and `refresh_token`. Enter both, depending on the flow, under
`TWITCH_BROADCASTER_ACCESS_TOKEN`/`TWITCH_BROADCASTER_REFRESH_TOKEN` (step 1) or `TWITCH_BOT_ACCESS_TOKEN`/
`TWITCH_BOT_REFRESH_TOKEN` (step 2) in the `.env` file.

*Note: The refresh token does not need to be renewed manually afterwards - the bot automatically refreshes the access token
in the background as long as the refresh token remains valid (see `TwitchUserTokenManager`).*

#### 4. Granting channel permissions

1. Add the bot account as a moderator in the broadcaster's channel (`/mod <botname>`).
2. Enter the target channel in `twitchbot.properties` under `twitch.chat.channels`.

### AutoMod mapping

| Discord key | Twitch meaning |
|---|---|
| `automod.exempt.role.ids` | Exempt Twitch logins (usernames) |
| `automod.exempt.channel.ids` | Exempt Twitch channels |
| `automod.bypass.manage-server` | Bypass for channel moderators |
| `automod.bypass.administrator` | Bypass for the broadcaster (channel owner) |

### Persistence

Own SQLite DB (`twitch.database.file`), uses the identical `Database`/`Jdbc`/`SchemaMigrator` layer as the
Discord bot.
Tables: `twitch_channels`, `twitch_custom_commands` (incl. `cooldown_seconds`), `twitch_watchtime`,
`twitch_broadcasts`, `twitch_event_log`, `twitch_quotes`, `moderation_cases`, `account_links`,
`pending_link_verifications`.

### Other settings

- `twitch.automod.permit.default.seconds` (default `30`): duration of the AutoMod exemption when `!permit <user>` is used
  without specifying seconds.

---

## Discord ↔ Twitch integration

### Discord logging (Twitch → Discord)

The Twitch bot can mirror chat messages, AutoMod events (both its own AutoMod **and** Twitch's native AutoMod) as well as
command usages live to a Discord channel via a Discord webhook. Since the Twitch bot runs as its own process
without a Discord connection, this happens independently of the Discord bot via a classic Discord webhook.

1. Create a webhook in Discord in the desired log channel (channel settings → integrations →
   webhooks → new webhook) and copy the webhook URL.
2. Enter the URL in `twitchbot.properties` under `discord.log.webhook.url`.
3. Control what gets logged via the following switches:

| Property | Default | Meaning |
|---|---|---|
| `discord.log.webhook.url` | *(empty)* | Discord webhook URL; without a URL the feature is disabled |
| `discord.log.messages.enabled` | `false` | Logs every chat message (can generate a lot of traffic) |
| `discord.log.automod.enabled` | `true` | Logs both its own AutoMod **and** native Twitch AutoMod |
| `discord.log.commands.enabled` | `true` | Logs every use of a built-in or custom command |

For native Twitch AutoMod (messages that Twitch itself holds back for review) the bot
automatically subscribes to the EventSub types `automod.message.hold` and `automod.message.update`. For this, the scope
`moderator:manage:automod` is required on the bot account token (see step 2 of the Twitch authorization above).

On the Discord bot side, the use of Discord slash commands is logged via the existing event log system
(`/serverlog`, event type "Command usage") and requires no additional configuration.

### Follow / Subscribe / Raid alerts

The Twitch bot can announce new followers, new subscribers, and incoming raids - in the Twitch chat, as a
Discord embed via webhook, or both at once, independently per event type.

| Property | Default | Meaning |
|---|---|---|
| `twitch.alert.follow.chat.enabled` | `true` | Posts a chat message for every new follow |
| `twitch.alert.follow.chat.message` | `🎉 Danke für den Follow, {user}!` | Chat message template, placeholders: `{user}`, `{channel}` |
| `twitch.alert.follow.discord.enabled` | `false` | Posts a Discord embed for every new follow |
| `twitch.alert.subscribe.chat.enabled` | `true` | Posts a chat message for every new/renewed subscription |
| `twitch.alert.subscribe.chat.message` | `🎉 Vielen Dank für den Sub, {user}! ({tier})` | Placeholders: `{user}`, `{channel}`, `{tier}` (`Tier 1`/`2`/`3`) |
| `twitch.alert.subscribe.discord.enabled` | `false` | Posts a Discord embed for every new/renewed subscription |
| `twitch.alert.raid.chat.enabled` | `true` | Posts a chat message for every incoming raid |
| `twitch.alert.raid.chat.message` | `🚀 Danke für den Raid, {user}! {viewers} Zuschauer sind mit dabei, sagt Hallo!` | Placeholders: `{user}`, `{channel}`, `{viewers}` |
| `twitch.alert.raid.discord.enabled` | `false` | Posts a Discord embed for every incoming raid |
| `discord.alert.webhook.url` | *(empty)* | Webhook URL for the Discord alerts; if left empty, `discord.log.webhook.url` is reused |

Only subscriptions that are actually enabled (chat and/or Discord) get set up with Twitch, so nothing is
subscribed to unnecessarily. Follow alerts require the `moderator:read:followers` scope and subscribe alerts
require `channel:read:subscriptions` - both are already part of the broadcaster authorization URL from step 1
above, so the **broadcaster token** (`TWITCH_BROADCASTER_ACCESS_TOKEN`/`TWITCH_BROADCASTER_REFRESH_TOKEN`) must be
configured for these two alert types; without it, follow/subscribe alerts are skipped with a warning log on
startup (raid alerts don't need any extra scope and always work with just the bot token).

### Advanced moderation

In addition to the automatic AutoMod, there is a full manual moderation system with its own
case history, configurable punishment/moderator roles, and optional synchronization between Discord and Twitch.

**Discord:** `/mod warn|timeout|untimeout|kick|ban|unban|cases` - see [command reference](#discord-command-reference)
above for details. `/mod` commands may be used by members with `ADMINISTRATOR`/`Moderate Members` permissions as well as any
role granted via `/modroles add-moderator`. `/modroles` itself requires `ADMINISTRATOR`
or a role granted via `add-admin`. Bans and timeouts also automatically appear in the normal
`/serverlog` (Discord reports these events itself); kicks and warnings are additionally
recorded as their own log types. Via `/modroles warn-role`/`mute-role`/`ban-role` you define which role is
automatically granted on warn/timeout/ban (and removed again when it is lifted).

**Twitch:** `!warn`, `!ban`, `!timeout`/`!to`, `!unban`/`!untimeout` write to the same shared case history
and - if the bridge is configured - trigger a synchronization to the linked Discord account. A kick
has no Twitch equivalent and is not synchronized.

### Linking accounts

Linking is completely **optional** for users - without it, the server, channel, and all other
features continue to work normally. It's worthwhile because it allows Twitch roles (subscriber, VIP, moderator, broadcaster)
to be automatically mirrored as a Discord role and - if the bridge is enabled - moderation actions
to be synchronized between Discord and Twitch. Only the mapping between the
Discord ID and Twitch ID/login is stored, no passwords or tokens.

Linking works in both directions and always requires confirmation on the respective other
platform (no relying purely on the person's word):

- **Panel (recommended):** `/link panel channel:<channel>` (admin) posts an embed with a "🔗 Link"
  button in the specified channel - similar to the ticket system. Clicking the button opens a small form
  for the Twitch login name; the rest continues as described below.
- **Starting on Discord (command):** `/link twitch login:<name>` → the bot provides a code → the user posts
  `!verify <code>` in the Twitch chat of the specified channel.
- **Starting on Twitch:** `!link` in chat → the bot provides a code → the user runs `/link verify code:<code>` on
  Discord.
- `/link status` / `/link unlink` manage your own link on the Discord side.

### Syncing Twitch roles with Discord

Via `/modroles sync-role status:<subscriber|vip|moderator|broadcaster> role:<@role>` you define which
Discord role should be automatically granted for which Twitch status (calling it without `role` removes the
mapping again). `/modroles status` shows the current configuration.

The sync happens in two ways:
- **Immediately on chat message:** The Twitch bot recognizes the status of a linked user from the chat badges
  as soon as they write in Twitch chat, and syncs the Discord role directly.
- **Periodic reconciliation:** So that, for example, an expiring subscription is also detected when the person no longer
  writes in chat, the Twitch bot additionally queries the current subscriber/VIP/moderator list via the Twitch API every
  `moderation.sync.reconcile.interval.minutes` (default `15`) and syncs **all** linked accounts.
  This requires the `TWITCH_BROADCASTER_ACCESS_TOKEN` (see above) - without it, only the periodic
  reconciliation is skipped; the chat-based sync still works.

### The bridge (Discord ↔ Twitch)

Since both bots can run as separate processes - even on different servers - they talk to each other via a
small HTTP(S) interface built into both bots ("bridge"). **No** additional software is
required; each bot can optionally start a small HTTP(S) server and call the other bot's server.
The bridge supports **TLS encryption and optionally mutual TLS (mTLS)** for the highest possible security of the
connection, especially when both bots run on separate servers.

| Property (both bots) | Default | Meaning |
|---|---|---|
| `bridge.enabled` | `false` | Starts its own bridge server (must be reachable by the peer) |
| `bridge.bind.host` | `127.0.0.1` | Network interface the bridge server binds to |
| `bridge.port` | `8082`/`8083` | Port of its own bridge server |
| `bridge.token` | *(empty)* | Shared secret - **must be identical on both bots** |
| `bridge.peer.url` | *(empty)* | Base URL of the respective other bot, e.g. `http://localhost:8083` |

Additionally (only on one side each):

- **Discordbot:** `moderation.sync.guild.id` - the **server ID** (not channel ID!) of the Discord server in which
  synchronized actions are executed (this bot setup is designed for one community/server).
  You get the ID by right-clicking on the **server name** (not a channel) → "Copy ID"
  (Developer Mode must be enabled in Discord settings for this).
- **Twitchbot:** `moderation.sync.channel` - the Twitch channel **login** (text, e.g. `xdestenyyy` - not a number!),
  in which synchronized actions are executed (empty = first channel from `twitch.chat.channels`).
- **Twitchbot:** `moderation.sync.reconcile.interval.minutes` (default `15`) - interval for the periodic
  role reconciliation (see above).

**Setup via the launcher / on the same server (default, recommended):** leave `bridge.bind.host` on both sides at
`127.0.0.1`, set `bridge.peer.url` to `http://localhost:<port-of-the-other-bot>` respectively. The bridge is
then only reachable locally, even if the server has a public IP. TLS is optional here, since the
traffic never leaves the machine.

**Setup for two separate servers:** set `bridge.bind.host=0.0.0.0` on the side that needs to be reachable,
set `bridge.token` identically on both sides, and point `bridge.peer.url` to the publicly reachable
address (with `https://`, see below). Additionally, be sure to restrict access via firewall to the IP of the
respective other server.

Without `bridge.peer.url`, everything else (manual commands, roles, case history) continues to work normally -
it just means no action is mirrored to the other platform.

#### TLS encryption for the bridge (recommended for separate servers)

If the bridge runs over the open internet (two separate servers), it should **always** be encrypted - otherwise
the shared secret, Discord/Twitch user IDs, and moderation actions can be read in plaintext. The following
additional properties are available for this (named identically on both bots, but with its own certificate per bot):

| Property | Default | Meaning |
|---|---|---|
| `bridge.tls.enabled` | `false` | Enables HTTPS instead of plaintext HTTP for its own bridge server **and** for outgoing requests to the peer |
| `bridge.tls.keystore.path` | *(empty)* | Path to its own PKCS12 certificate file (`.p12`) that the bridge server presents during the handshake |
| `bridge.tls.keystore.password` | *(empty)* | Password of the keystore. Supports `env:VARNAME` to provide the password via an environment variable instead of in plaintext |
| `bridge.tls.key.password` | *(empty)* | Password of the private key in the keystore, if different from the keystore password. `env:VARNAME` is also possible |
| `bridge.tls.truststore.path` | *(empty)* | Path to a PKCS12 file with trusted certificates. Needed as soon as a **self-signed** certificate is used or `mutual-auth` is active |
| `bridge.tls.truststore.password` | *(empty)* | Password of the truststore. `env:VARNAME` is also possible |
| `bridge.tls.mutual-auth` | `false` | Enables mutual TLS (mTLS): the bridge server additionally requires a valid client certificate from the peer, not just the shared secret |

Only **TLS 1.2 and TLS 1.3** are allowed; older, insecure protocol versions are always
rejected by the bridge. If `bridge.tls.enabled=true`, `bridge.peer.url` must start with `https://` - requests to
an `http://` peer URL are then automatically refused, to prevent an accidental downgrade to plaintext.

**Step 1 - Generate a certificate per bot** (once on each of the two servers, `keytool` is part of every Java installation):

```bash
keytool -genkeypair -alias bridge -keyalg EC -keysize 256 -validity 825 \
  -keystore bridge-keystore.p12 -storetype PKCS12 \
  -dname "CN=discordbot-bridge"        # or "CN=twitchbot-bridge" on the other server
```
`keytool` will ask for a password for the keystore - enter this in `bridge.tls.keystore.password`
(or better, set it as an environment variable via `env:BRIDGE_KEYSTORE_PASSWORD`, see below).

**Step 2 - Export the certificate and add it to the other bot as trusted**, since it is
self-signed and is not automatically accepted by the JVM's default CA list:

```bash
# On the Discordbot server: export its own certificate ...
keytool -exportcert -alias bridge -keystore bridge-keystore.p12 -storetype PKCS12 -file discordbot.crt

# ... and copy it to the Twitchbot server (e.g. via scp), then import it there:
keytool -importcert -alias discordbot-peer -file discordbot.crt \
  -keystore bridge-truststore.p12 -storetype PKCS12 -noprompt
```
The same in the opposite direction (export the Twitchbot certificate → import it on the Discordbot server into its
`bridge-truststore.p12`). Each bot thus gets its own truststore that contains only the certificate
of the respective other bot.

**Step 3 - Configuration** (example `discordbot.properties`, `twitchbot.properties` is a mirror image):

```properties
bridge.enabled=true
bridge.bind.host=0.0.0.0
bridge.peer.url=https://twitchbot.example.com:8083
bridge.tls.enabled=true
bridge.tls.keystore.path=/path/to/bridge-keystore.p12
bridge.tls.keystore.password=env:BRIDGE_KEYSTORE_PASSWORD
bridge.tls.truststore.path=/path/to/bridge-truststore.p12
bridge.tls.truststore.password=env:BRIDGE_TRUSTSTORE_PASSWORD
bridge.tls.mutual-auth=true
```
and set the corresponding environment variables at startup (e.g. `export BRIDGE_KEYSTORE_PASSWORD=...`), instead of writing the
passwords in plaintext in the properties file.

**For maximum security**, additionally set `bridge.tls.mutual-auth=true` on **both** bots: the bridge server
will then only accept requests that carry both a valid client certificate stored in the truststore and
the correct shared secret - two independent layers of security in addition to the encryption itself.

---

## Combined start (`launcher`)

By default, each bot is a standalone process (its own JAR, its own logging). Anyone who wants to run the Discord and Twitch bot
on the same server **in a single process** should use the `launcher` module instead.

The launcher contains **no bot logic of its own**, but merely instantiates and starts the existing
`Bot` classes from `xBotenyyDiscordBot` and `xBotenyyTwitchBot`, each in its own thread. In addition, it
comes with an interactive **console** through which both bots can be started, stopped, and restarted
at runtime, as well as combined logging for both bots in one process. The configuration (`.env`, `discordbot.properties`,
`twitchbot.properties`) remains unchanged, the same as in separate operation.

**Full documentation (console commands, architecture,
logging details): [`launcher/README.md`](launcher/README.md)**

### Building and starting

```bash
mvn -pl launcher -am -DskipTests package
java -jar launcher/target/xBotenyyLauncher.jar
```

Optionally, only one of the two bots can be enabled via a startup argument (the respective other one can
still be started at any time afterwards via the console with `start <bot>`):

```bash
java -jar launcher/target/xBotenyyLauncher.jar --mode=discord
java -jar launcher/target/xBotenyyLauncher.jar --mode=twitch
```

Without an argument (or with `--mode=both`), both bots start together.

### Console commands

Once the launcher is running, it interactively accepts commands via standard input:

| Command | Description |
|---|---|
| `help` | Overview of all commands |
| `status` | Status, restart counter, and last start per bot, current settings |
| `start <discord\|twitch\|all>` | Start bot(s) |
| `stop <discord\|twitch\|all> [timeoutSeconds]` | Stop bot(s) gracefully (no auto-restart afterward) |
| `restart <discord\|twitch\|all> [timeoutSeconds]` | Stop bot(s) and immediately start them again |
| `set maxrestarts <n>` / `set restartdelay <seconds>` | Change restart behavior at runtime (see below) |
| `schedule add <discord\|twitch\|all> <restart\|stop\|start> interval <value> [timeoutSeconds]` | Create a recurring task, e.g. `schedule add all restart interval 6h` |
| `schedule add <discord\|twitch\|all> <restart\|stop\|start> daily <HH:mm> [timeoutSeconds]` | Create a daily task, e.g. `schedule add discord restart daily 04:30` |
| `schedule list` | Shows all scheduled tasks with next/last execution |
| `schedule remove <id>` | Removes a scheduled task |
| `schedule enable <id>` / `schedule disable <id>` | Enables/disables a scheduled task |
| `exit` | Stops all bots gracefully and shuts down the launcher |

### Scheduler (automatic restarts & more)

The launcher includes a built-in scheduler with which recurring actions (`restart`, `stop`, `start`)
can be scheduled per bot or for all bots together - e.g. a daily restart at 04:30 or a restart every
6 hours. Tasks are managed via the console command `schedule` and persisted in `scheduler-tasks.txt`
(path configurable via `LAUNCHER_SCHEDULER_FILE`), so they survive a restart of the launcher.
The scheduler checks every 10 seconds whether a task is due.

```
schedule add all restart daily 04:30
schedule add discord restart interval 6h 20
schedule list
schedule disable s1
schedule remove s1
```

### Logging in combined operation

Since both bots run in the same process, the launcher comes with its own Logback setup that is separated by module
(`launcher/src/main/resources/logback.xml`), which automatically routes logs based on the Java package name instead of
writing everything unstructured into one file:

| File | Content |
|---|---|
| `logs/xbotenyy-discord.log` | All logs from `discordbot` as well as the JDA library |
| `logs/xbotenyy-twitch.log` | All logs from `twitchbot` |
| `logs/xbotenyy-launcher.log` | Shared infrastructure from `common` as well as the launcher itself |
| `logs/audit.log` | Audit log of both bots (shared, as in separate operation too) |

When building the JAR, a dedicated assembly descriptor (`launcher/src/main/assembly/with-dependencies.xml`) ensures
that the `logback.xml` files from `discordbot` and `twitchbot` are **not** packed into the fat JAR - this way,
only the `launcher` module's configuration remains active and there is no resource collision on the classpath.

### Fault tolerance

Each bot runs in its own thread, supervised by the launcher. If a bot crashes (e.g. due to a
programming error or an unexpected exception), this affects **exclusively that one bot** - the
other one keeps running unaffected, and the process itself stays alive. The launcher additionally automatically
restarts the crashed bot (default: up to 5 attempts, 15 seconds pause in between). Both values can be pre-set via
environment variable without rebuilding the JAR, **and can also be changed at runtime via the console**
(`set maxrestarts <n>`, `set restartdelay <seconds>`):

```env
LAUNCHER_MAX_RESTART_ATTEMPTS=5
LAUNCHER_RESTART_DELAY_SECONDS=15
```

Once all automatic attempts are exhausted, the bot stays stopped and can be restarted manually at any time via a console command
(`start <bot>`) - the restart counter is reset in the process.

*The only limit: if the JVM itself hits a hard problem (e.g. an `OutOfMemoryError` due to too little
allocated memory), that can still affect the entire process and thus both bots - this can
only be avoided with sufficient RAM for combined operation, not through code.*

### Is the launcher JAR alone sufficient?

Yes. `xBotenyyLauncher.jar` is already a standalone fat JAR and contains `common`, `discordbot`, and `twitchbot`
in full. **No** additional JARs (`xBotenyyDiscordBot.jar`, `xBotenyyTwitchBot.jar`) need to be
copied to the server - only `xBotenyyLauncher.jar` as well as, as usual, `.env`, `discordbot.properties`, and
`twitchbot.properties` in the same directory.

---

## Automatic builds and releases

A single GitHub Actions workflow, `.github/workflows/release.yml`, builds all three fat JARs and publishes
a GitHub release fully automatically - the version in the root `pom.xml` is the single source of truth.

### How it works

1. You change `<version>` in the root `pom.xml` (e.g. from `1.0-RELEASE` to `1.1.0`).
2. You commit and push to `main`.
3. The workflow triggers (it watches for changes to `pom.xml` on `main`) and:
    - reads the new version directly from the root `pom.xml`,
    - checks whether a tag for that version already exists (skips everything below if it does, so pushes
      that don't touch the version number don't create duplicate releases),
    - synchronizes the `<parent><version>` reference in `common`, `discordbot`, `twitchbot`, and `launcher`
      so all modules match the new version,
    - creates and pushes the tag (`v` + version, e.g. `v1.1.0`),
    - builds all three fat JARs (`mvn clean package`),
    - publishes a GitHub release with `xBotenyyDiscordBot.jar`, `xBotenyyTwitchBot.jar`, and
      `xBotenyyLauncher.jar` attached (GitHub automatically adds a source code ZIP/TAR.GZ to every release
      as well),
    - commits the synchronized module `pom.xml` files back to `main`.

### Manual trigger

The workflow can also be started by hand under **Actions → Release → Run workflow**, using whatever
version is currently set in the root `pom.xml`. This is mainly useful for re-running a release after a
fixed workflow error, without having to bump the version again.

### Everyday release

For a normal new release, all you need is:

```bash
# edit <version> in the root pom.xml, then:
git add pom.xml
git commit -m "Version 1.1.0"
git push origin main
```


---

This Documentation has been generated by AI

