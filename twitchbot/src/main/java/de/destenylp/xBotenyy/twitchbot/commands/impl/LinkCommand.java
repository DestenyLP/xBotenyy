package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.common.moderation.LinkCodeGenerator;
import de.destenylp.xBotenyy.common.moderation.ModerationPlatform;
import de.destenylp.xBotenyy.common.moderation.PendingLinkVerification;
import de.destenylp.xBotenyy.common.moderation.PendingLinkVerificationRepository;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class LinkCommand extends AbstractTwitchCommand {
    private final PendingLinkVerificationRepository pendingLinkVerificationRepository;

    public LinkCommand(PendingLinkVerificationRepository pendingLinkVerificationRepository) {
        super("link", "Startet die Verknuepfung mit deinem Discord-Account.", List.of(), CommandPermission.EVERYONE, 10);
        this.pendingLinkVerificationRepository = pendingLinkVerificationRepository;
    }

    @Override
    public void execute(TwitchCommandContext context) {
        String code = LinkCodeGenerator.generate();
        pendingLinkVerificationRepository.save(new PendingLinkVerification(code, null, null,
                context.message().userId(), context.message().userLogin(), ModerationPlatform.TWITCH,
                Instant.now().plus(Duration.ofMinutes(10)).toEpochMilli()));
        context.reply("@" + context.message().displayName() + " Fuehre innerhalb der naechsten 10 Minuten den Befehl "
                + "/link verify code:" + code + " auf Discord aus, um die Verknuepfung zu bestaetigen.");
    }
}

