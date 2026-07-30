package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.moderation.AccountLinkPanelFactory;
import de.destenylp.xBotenyy.discordbot.moderation.AccountLinkService;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountLinkListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountLinkListener.class);

    private final AccountLinkService accountLinkService;

    public AccountLinkListener(AccountLinkService accountLinkService) {
        this.accountLinkService = accountLinkService;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!AccountLinkPanelFactory.BUTTON_ID.equals(event.getComponentId())) {
            return;
        }
        try {
            TextInput login = TextInput.create(AccountLinkPanelFactory.MODAL_INPUT_ID, TextInputStyle.SHORT)
                    .setPlaceholder("z. B. destenylp")
                    .setMaxLength(30)
                    .build();

            Modal modal = Modal.create(AccountLinkPanelFactory.MODAL_ID, "Mit Twitch verknuepfen")
                    .addComponents(Label.of("Dein Twitch-Loginname", login))
                    .build();

            event.replyModal(modal).queue();
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler beim Oeffnen des Verknuepfungs-Modals: ", e);
            replyGenericError(event);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!AccountLinkPanelFactory.MODAL_ID.equals(event.getModalId())) {
            return;
        }
        try {
            ModalMapping mapping = event.getValue(AccountLinkPanelFactory.MODAL_INPUT_ID);
            if (mapping == null || mapping.getAsString().isBlank()) {
                event.reply("Bitte gib einen gueltigen Twitch-Loginnamen an.").setEphemeral(true).queue();
                return;
            }
            String login = mapping.getAsString().trim().toLowerCase().replace("@", "");
            String code = accountLinkService.initiate(event.getUser().getId(), event.getUser().getName(), login);
            event.reply("Fast fertig! Poste **`!verify " + code + "`** innerhalb der naechsten 10 Minuten im Twitch-Chat "
                    + "von **" + login + "**, um die Verknuepfung zu bestaetigen.").setEphemeral(true).queue();
            if (event.getGuild() != null) {
                AuditLog.record(event.getGuild().getId(), event.getUser().getId(), "ACCOUNT_LINK_INITIATE_PANEL", "twitch=" + login);
            }
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Verknuepfungs-Modal-Interaktion: ", e);
            replyGenericError(event);
        }
    }

    private void replyGenericError(IReplyCallback event) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage("Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es erneut.").queue();
        } else {
            event.reply("Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es erneut.").setEphemeral(true).queue();
        }
    }
}
