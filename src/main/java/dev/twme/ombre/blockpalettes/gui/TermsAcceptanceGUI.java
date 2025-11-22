package dev.twme.ombre.blockpalettes.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import dev.twme.ombre.blockpalettes.cache.TermsAcceptanceData;
import dev.twme.ombre.i18n.MessageManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * 條款同意介面
 * 首次使用 Block Palettes 時透過聊天欄文字互動顯示
 */
public class TermsAcceptanceGUI implements Listener {
    
    private final Plugin plugin;
    private final MessageManager messageManager;
    private final Player player;
    private final Runnable onAccept;
    private final Runnable onDecline;
    private static final Map<UUID, TermsAcceptanceGUI> awaitingResponse = new HashMap<>();
    
    public TermsAcceptanceGUI(Plugin plugin, MessageManager messageManager, Player player, Runnable onAccept, Runnable onDecline) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.player = player;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
    }
    
    /**
     * 顯示條款同意訊息
     */
    public void show() {
        // 記錄正在等待回應的玩家
        awaitingResponse.put(player.getUniqueId(), this);
        
        // 顯示標題
        player.sendMessage(Component.empty());
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.separator", player));
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.title", player));
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.separator", player));
        player.sendMessage(Component.empty());
        
        // 功能說明
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.description.line1", player));
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.description.line2", player));
        player.sendMessage(Component.empty());
        
        // 條款說明
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.before-continue", player));
        player.sendMessage(Component.empty());
        
        // 使用條款連結
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.terms.number", player));
        player.sendMessage(
            messageManager.getComponent("terms.blockpalettes.terms.click", player)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(TermsAcceptanceData.TERMS_URL))
        );
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.terms.url", player,
            "url", TermsAcceptanceData.TERMS_URL));
        player.sendMessage(Component.empty());
        
        // 隱私政策連結
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.privacy.number", player));
        player.sendMessage(
            messageManager.getComponent("terms.blockpalettes.privacy.click", player)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(TermsAcceptanceData.PRIVACY_URL))
        );
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.privacy.url", player,
            "url", TermsAcceptanceData.PRIVACY_URL));
        player.sendMessage(Component.empty());
        
        // 免責聲明
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.disclaimer", player));
        player.sendMessage(Component.empty());
        
        // 選項說明
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.choice-separator", player));
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.choice-prompt", player));
        player.sendMessage(Component.empty());
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.choice.accept", player));
        player.sendMessage(Component.empty());
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.choice.decline", player));
        player.sendMessage(Component.empty());
        player.sendMessage(messageManager.getComponent("terms.blockpalettes.choice-separator", player));
        player.sendMessage(Component.empty());
    }
    
    /**
     * 處理聊天事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player eventPlayer = event.getPlayer();
        UUID playerId = eventPlayer.getUniqueId();
        
        // 檢查是否是等待回應的玩家
        if (!awaitingResponse.containsKey(playerId)) {
            return;
        }
        
        // 取消聊天事件,避免訊息被廣播
        event.setCancelled(true);
        
        TermsAcceptanceGUI gui = awaitingResponse.get(playerId);
        
        // 將 Component 訊息轉換為純文字
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim().toLowerCase();
        
        // 在主執行緒處理回應
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            handleResponse(eventPlayer, gui, message);
        });
    }
    
    /**
     * 處理玩家回應 (必須在主執行緒執行)
     */
    private void handleResponse(Player player, TermsAcceptanceGUI gui, String message) {
        UUID playerId = player.getUniqueId();
        
        // 處理回應
        if (message.equals("agree")) {
            // 同意條款
            awaitingResponse.remove(playerId);
            
            player.sendMessage(Component.empty());
            player.sendMessage(messageManager.getComponent("terms.blockpalettes.response.accepted.title", player));
            player.sendMessage(messageManager.getComponent("terms.blockpalettes.response.accepted.message", player));
            player.sendMessage(Component.empty());
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            if (gui.onAccept != null) {
                gui.onAccept.run();
            }
            
        } else if (message.equals("decline")) {
            // 不同意條款
            awaitingResponse.remove(playerId);
            
            player.sendMessage(Component.empty());
            player.sendMessage(messageManager.getComponent("terms.blockpalettes.response.declined.title", player));
            player.sendMessage(messageManager.getComponent("terms.blockpalettes.response.declined.message1", player));
            player.sendMessage(messageManager.getComponent("terms.blockpalettes.response.declined.message2", player));
            player.sendMessage(Component.empty());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            
            if (gui.onDecline != null) {
                gui.onDecline.run();
            }
            
        } else {
            // 無效輸入
            player.sendMessage(Component.empty());
            player.sendMessage(messageManager.getComponent("terms.blockpalettes.response.invalid.title", player));
            player.sendMessage(messageManager.getComponent("terms.blockpalettes.response.invalid.message", player));
            player.sendMessage(Component.empty());
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * 清除等待回應的玩家記錄
     */
    public static void clearAwaitingResponse(UUID playerId) {
        awaitingResponse.remove(playerId);
    }
    
    /**
     * 檢查玩家是否正在等待回應
     */
    public static boolean isAwaitingResponse(UUID playerId) {
        return awaitingResponse.containsKey(playerId);
    }
}
