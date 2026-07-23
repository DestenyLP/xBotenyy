package de.destenylp.xBotenyy.discordbot.socials;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SocialRepository {
    List<SocialAccount> getAccounts(String guildId);
    Optional<SocialAccount> getAccount(String guildId, String id);
    SocialAccount addAccount(String guildId, SocialAccount draft);
    void save(String guildId, SocialAccount account);
    boolean removeAccount(String guildId, String id);
    Map<String, List<SocialAccount>> findAccountsWithYoutube();
    Map<String, List<SocialAccount>> findAccountsWithTwitch();
}
