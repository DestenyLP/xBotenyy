package de.destenylp.xBotenyy.twitchbot.commands.impl;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchQuoteRepository;
import de.destenylp.xBotenyy.twitchbot.persistence.TwitchQuoteRepository.QuoteRecord;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
public class QuoteCommand extends AbstractTwitchCommand {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault());
    private static final String USAGE = "Nutzung: !quote [nummer] | !quote add <text> | "
            + "!quote del <nummer> | !quote list";
    private final TwitchQuoteRepository repository;
    private final TwitchEventLogService eventLogService;
    public QuoteCommand(TwitchQuoteRepository repository, TwitchEventLogService eventLogService) {
        super("quote", "Zitat-System: zufaellige oder gezielte Zitate abrufen, hinzufuegen und entfernen.",
                List.of("zitat"), CommandPermission.EVERYONE, 3);
        this.repository = repository;
        this.eventLogService = eventLogService;
    }
    @Override
    public void execute(TwitchCommandContext context) {
        String channel = context.message().channelLogin();
        String first = context.arg(0);
        if (first == null) {
            handleRandom(context, channel);
            return;
        }
        switch (first.toLowerCase(Locale.ROOT)) {
            case "add" -> handleAdd(context, channel);
            case "del", "delete", "remove" -> handleDelete(context, channel);
            case "list" -> handleList(context, channel);
            case "random" -> handleRandom(context, channel);
            default -> handleByNumber(context, channel, first);
        }
    }
    private void handleAdd(TwitchCommandContext context, String channel) {
        if (!context.message().isPrivileged()) {
            context.reply("Nur Moderatoren koennen neue Zitate hinzufuegen.");
            return;
        }
        if (context.args().size() < 2) {
            context.reply(USAGE);
            return;
        }
        String content = String.join(" ", context.args().subList(1, context.args().size()));
        QuoteRecord quote = repository.add(channel, content, context.message().userLogin());
        eventLogService.record(channel, context.message().userId(), "QUOTE_ADDED", "number=" + quote.quoteNumber());
        context.reply("Zitat #" + quote.quoteNumber() + " wurde gespeichert.");
    }
    private void handleDelete(TwitchCommandContext context, String channel) {
        if (!context.message().isPrivileged()) {
            context.reply("Nur Moderatoren koennen Zitate entfernen.");
            return;
        }
        Integer number = parseNumber(context.arg(1));
        if (number == null) {
            context.reply("Nutzung: !quote del <nummer>");
            return;
        }
        boolean removed = repository.remove(channel, number);
        if (removed) {
            eventLogService.record(channel, context.message().userId(), "QUOTE_REMOVED", "number=" + number);
            context.reply("Zitat #" + number + " wurde entfernt.");
        } else {
            context.reply("Es gibt kein Zitat mit der Nummer #" + number + ".");
        }
    }
    private void handleList(TwitchCommandContext context, String channel) {
        int total = repository.count(channel);
        if (total == 0) {
            context.reply("Es sind noch keine Zitate fuer diesen Kanal gespeichert.");
            return;
        }
        List<QuoteRecord> preview = repository.list(channel, 5);
        String numbers = preview.stream()
                .map(quote -> "#" + quote.quoteNumber())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        context.reply(total + " Zitat(e) gespeichert. Erste: " + numbers
                + ". Nutze !quote <nummer> fuer ein bestimmtes Zitat.");
    }
    private void handleRandom(TwitchCommandContext context, String channel) {
        Optional<QuoteRecord> quote = repository.random(channel);
        if (quote.isEmpty()) {
            context.reply("Es sind noch keine Zitate fuer diesen Kanal gespeichert. Mit !quote add <text> anlegen.");
            return;
        }
        context.reply(formatQuote(quote.get()));
    }
    private void handleByNumber(TwitchCommandContext context, String channel, String raw) {
        Integer number = parseNumber(raw);
        if (number == null) {
            context.reply(USAGE);
            return;
        }
        Optional<QuoteRecord> quote = repository.get(channel, number);
        if (quote.isEmpty()) {
            context.reply("Es gibt kein Zitat mit der Nummer #" + number + ".");
            return;
        }
        context.reply(formatQuote(quote.get()));
    }
    private static Integer parseNumber(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw.replace("#", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static String formatQuote(QuoteRecord quote) {
        String date = DATE_FORMAT.format(Instant.ofEpochMilli(quote.createdAt()));
        return "Zitat #" + quote.quoteNumber() + ": \"" + quote.content() + "\" (" + date + ")";
    }
}
