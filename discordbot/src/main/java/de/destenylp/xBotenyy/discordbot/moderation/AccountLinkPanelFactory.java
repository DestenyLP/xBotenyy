package de.destenylp.xBotenyy.discordbot.moderation;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import java.awt.Color;
import java.util.List;
public final class AccountLinkPanelFactory {
    public static final String BUTTON_ID = "accountlink:start";
    public static final String MODAL_ID = "accountlink:modal";
    public static final String MODAL_INPUT_ID = "twitch_login";
    private AccountLinkPanelFactory() {
    }
    public static MessageEmbed buildPanelEmbed() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(145, 70, 255));
        eb.setTitle("\uD83D\uDD17 Discord- und Twitch-Account verknuepfen");
        eb.setDescription("Verknuepfe optional deinen Discord- mit deinem Twitch-Account. Das ist **freiwillig** "
                + "und nicht erforderlich, um den Server oder den Kanal nutzen zu koennen.\n\n"
                + "**Was wird gespeichert?**\nNur die Zuordnung zwischen deiner Discord- und deiner Twitch-ID "
                + "(sowie dein Twitch-Loginname). Keine Passwoerter, keine Tokens, keine Nachrichten.\n\n"
                + "**Wozu ist das gut?**\nDamit z. B. deine Twitch-Rollen (Subscriber, VIP, Moderator) automatisch "
                + "auch als Discord-Rolle sichtbar werden, und damit Moderationsmaßnahmen (Bann/Timeout/Verwarnung) "
                + "zwischen Discord und Twitch synchronisiert werden koennen, falls der Server das aktiviert hat.\n\n"
                + "Klicke unten auf **Verknuepfen**, um zu starten.");
        eb.setFooter("Du kannst die Verknuepfung jederzeit mit /link unlink wieder entfernen.");
        return eb.build();
    }
    public static List<ActionRow> buildPanelComponents() {
        return List.of(ActionRow.of(Button.primary(BUTTON_ID, "\uD83D\uDD17 Verknuepfen")));
    }
}
