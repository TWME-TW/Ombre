package dev.twme.ombre.blockcolors.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.i18n.MessageManager;

/**
 * 監聽玩家聊天以處理條款接受
 */
public class TermsAcceptanceListener implements Listener {
    private final BlockColorsFeature feature;
    private final MessageManager msg;

    public TermsAcceptanceListener(BlockColorsFeature feature) {
        this.feature = feature;
        this.msg = ((Ombre) feature.getPlugin()).getMessageManager();
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // 檢查玩家是否正在等待條款回應
        if (!feature.isPendingTermsAcceptance(player.getUniqueId())) {
            return;
        }
        
        String message = event.getMessage().toLowerCase().trim();
        
        if (message.equals("accept")) {
            // 接受條款
            event.setCancelled(true);
            feature.getTermsTracker().acceptTerms(player.getUniqueId());
            feature.removePendingTermsAcceptance(player.getUniqueId());
            
            msg.sendMessage(player, "terms.blockcolors.accepted");
            msg.sendMessage(player, "terms.blockcolors.accepted-reminder");
            
        } else if (message.equals("decline")) {
            // 拒絕條款
            event.setCancelled(true);
            feature.removePendingTermsAcceptance(player.getUniqueId());
            
            msg.sendMessage(player, "terms.blockcolors.declined");
            msg.sendMessage(player, "terms.blockcolors.declined-reminder");
        }
    }
}
