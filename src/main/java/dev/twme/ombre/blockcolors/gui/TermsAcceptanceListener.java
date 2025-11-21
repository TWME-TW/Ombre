package dev.twme.ombre.blockcolors.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import dev.twme.ombre.blockcolors.BlockColorsFeature;

/**
 * 監聽玩家聊天以處理條款接受
 */
public class TermsAcceptanceListener implements Listener {
    private final BlockColorsFeature feature;

    public TermsAcceptanceListener(BlockColorsFeature feature) {
        this.feature = feature;
    }

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
            
            player.sendMessage("§a✓ 感謝您的同意！");
            player.sendMessage("§7請再次執行 §e/bca §7開啟功能");
            
        } else if (message.equals("decline")) {
            // 拒絕條款
            event.setCancelled(true);
            feature.removePendingTermsAcceptance(player.getUniqueId());
            
            player.sendMessage("§c您已拒絕使用條款");
            player.sendMessage("§7若要使用此功能，請重新執行 §e/bca §7並接受條款");
        }
    }
}
