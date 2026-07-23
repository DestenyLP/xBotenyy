package de.destenylp.xBotenyy.discordbot.placeholders;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderContext {
    private final Member member;
    private final Guild guild;
    private final TextChannel channel;
    private final Map<String, String> values = new HashMap<>();

    private PlaceholderContext(Member member, Guild guild, TextChannel channel) {
        this.member = member;
        this.guild = guild;
        this.channel = channel;
    }

    public static PlaceholderContext empty() {
        return new PlaceholderContext(null, null, null);
    }

    public static PlaceholderContext of(Guild guild) {
        return new PlaceholderContext(null, guild, null);
    }

    public static PlaceholderContext of(Member member, Guild guild) {
        return new PlaceholderContext(member, guild, null);
    }

    public static PlaceholderContext of(Member member, Guild guild, TextChannel channel) {
        return new PlaceholderContext(member, guild, channel);
    }

    public PlaceholderContext with(String key, String value) {
        values.put(key, value);
        return this;
    }

    public Member getMember() {
        return member;
    }

    public Guild getGuild() {
        return guild;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public Map<String, String> getValues() {
        return values;
    }
}
