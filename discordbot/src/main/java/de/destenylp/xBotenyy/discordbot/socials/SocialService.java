package de.destenylp.xBotenyy.discordbot.socials;

import de.destenylp.xBotenyy.discordbot.core.GuildService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SocialService implements GuildService {
    private final SocialRepository manager;
    private final int maxAccountsPerGuild;

    public SocialService(SocialRepository manager, int maxAccountsPerGuild) {
        this.manager = manager;
        this.maxAccountsPerGuild = maxAccountsPerGuild;
    }

    @Override
    public String getServiceName() {
        return "Socials";
    }

    public List<SocialAccount> getAccounts(String guildId) {
        return manager.getAccounts(guildId);
    }

    public Optional<SocialAccount> getAccount(String guildId, String id) {
        return manager.getAccount(guildId, id);
    }

    public boolean isAtCapacity(String guildId) {
        return manager.getAccounts(guildId).size() >= maxAccountsPerGuild;
    }

    public int getMaxAccountsPerGuild() {
        return maxAccountsPerGuild;
    }

    public SocialAccount addAccount(String guildId, SocialAccount draft) {
        return manager.addAccount(guildId, draft);
    }

    public boolean removeAccount(String guildId, String id) {
        return manager.removeAccount(guildId, id);
    }

    public void saveAccount(String guildId, SocialAccount account) {
        manager.save(guildId, account);
    }

    public Map<String, List<SocialAccount>> findAccountsWithYoutube() {
        return manager.findAccountsWithYoutube();
    }

    public Map<String, List<SocialAccount>> findAccountsWithTwitch() {
        return manager.findAccountsWithTwitch();
    }
}
