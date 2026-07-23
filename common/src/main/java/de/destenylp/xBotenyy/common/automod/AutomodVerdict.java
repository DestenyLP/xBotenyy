package de.destenylp.xBotenyy.common.automod;

public record AutomodVerdict(AutomodRuleType ruleType, AutomodAction action, String reason, String matchedContent) {
}
