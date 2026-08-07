package de.destenylp.xBotenyy.discordbot.moderation;

import de.destenylp.xBotenyy.common.moderation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class AccountLinkService {
    private final AccountLinkRepository accountLinkRepository;
    private final PendingLinkVerificationRepository pendingLinkVerificationRepository;

    public AccountLinkService(AccountLinkRepository accountLinkRepository,
                              PendingLinkVerificationRepository pendingLinkVerificationRepository) {
        this.accountLinkRepository = accountLinkRepository;
        this.pendingLinkVerificationRepository = pendingLinkVerificationRepository;
    }

    public String initiate(String discordUserId, String discordUsername, String twitchLogin) {
        String code = LinkCodeGenerator.generate();
        pendingLinkVerificationRepository.save(new PendingLinkVerification(code, discordUserId, discordUsername,
                null, twitchLogin, ModerationPlatform.DISCORD,
                Instant.now().plus(Duration.ofMinutes(10)).toEpochMilli()));
        return code;
    }

    public Optional<AccountLink> findLink(String discordUserId) {
        return accountLinkRepository.findByDiscordUserId(discordUserId);
    }

    public void unlink(String discordUserId) {
        accountLinkRepository.delete(discordUserId);
    }
}

