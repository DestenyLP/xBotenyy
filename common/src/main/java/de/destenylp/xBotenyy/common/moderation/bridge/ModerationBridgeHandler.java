package de.destenylp.xBotenyy.common.moderation.bridge;

public interface ModerationBridgeHandler {
    BridgeActionResult applyAction(BridgeActionRequest request);

    BridgeLinkConfirmResult confirmLink(BridgeLinkConfirmRequest request);

    BridgeRoleSyncResult syncRoles(BridgeRoleSyncRequest request);
}
