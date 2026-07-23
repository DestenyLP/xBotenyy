package de.destenylp.xBotenyy.discordbot.placeholders;

import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public final class PlaceholderCatalog {
    private PlaceholderCatalog() {
    }

    public static MessageEmbed buildOverviewEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Verfügbare Platzhalter");
        embedBuilder.setColor(DiscordColors.brand());
        embedBuilder.setDescription("Diese Platzhalter kannst du in Titel, Nachrichtentext und Footer verwenden. " +
                "Nutze außerdem `\\n` an beliebiger Stelle für einen Zeilenumbruch.");
        embedBuilder.addField("{user}", "Erwähnt das Mitglied, das die Nachricht ausgelöst hat", true);
        embedBuilder.addField("{user.name}", "Anzeigename/Nickname des Mitglieds", true);
        embedBuilder.addField("{user.username}", "Discord-Benutzername (@handle)", true);
        embedBuilder.addField("{user.id}", "Discord-ID des Mitglieds", true);
        embedBuilder.addField("{user.avatar}", "Link zum Profilbild des Mitglieds", true);
        embedBuilder.addField("{server}", "Name des Servers", true);
        embedBuilder.addField("{server.id}", "ID des Servers", true);
        embedBuilder.addField("{membercount}", "Aktuelle Mitgliederzahl", true);
        embedBuilder.addField("{channel}", "Erwähnt den Ziel-/Willkommenskanal", true);
        embedBuilder.addField("{joined}", "Datum/Uhrzeit des Server-Beitritts", true);
        embedBuilder.addField("{created}", "Datum/Uhrzeit der Account-Erstellung", true);
        return embedBuilder.build();
    }
}
