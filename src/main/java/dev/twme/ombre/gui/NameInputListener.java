package dev.twme.ombre.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.i18n.MessageManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * 漸層名稱輸入監聽器
 * 處理玩家在聊天欄輸入的漸層名稱
 */
public class NameInputListener implements Listener {
    
    private final Ombre plugin;
    private final MessageManager messageManager;
    
    // 等待輸入名稱的玩家
    private final Map<UUID, OmbreGUI> awaitingInput;
    
    public NameInputListener(Ombre plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.awaitingInput = new HashMap<>();
    }
    
    /**
     * 開始等待玩家輸入名稱
     */
    public void startWaitingForInput(Player player, OmbreGUI gui) {
        awaitingInput.put(player.getUniqueId(), gui);
    }
    
    /**
     * 取消等待玩家輸入
     */
    public void cancelWaitingForInput(Player player) {
        awaitingInput.remove(player.getUniqueId());
    }
    
    /**
     * 檢查玩家是否正在等待輸入
     */
    public boolean isWaitingForInput(Player player) {
        return awaitingInput.containsKey(player.getUniqueId());
    }
    
    /**
     * 處理玩家聊天事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 檢查玩家是否正在等待輸入名稱
        if (!awaitingInput.containsKey(playerId)) {
            return;
        }
        
        // Cancel the event to prevent the message from being sent to chat
        event.setCancelled(true);
        
        OmbreGUI gui = awaitingInput.remove(playerId);
        
        // Convert Component message to plain text
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        
        // Handle name input on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            handleNameInput(player, gui, input);
        });
    }
    
    /**
     * 處理玩家離線事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        awaitingInput.remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * 處理名稱輸入（必須在主執行緒執行）
     */
    private void handleNameInput(Player player, OmbreGUI gui, String input) {
        // Check for cancel command
        if (input.equalsIgnoreCase("cancel")) {
            messageManager.sendMessage(player, "messages.naming.name-input.cancelled");
            gui.open(player);
            return;
        }
        
        // Validate name length
        if (input.isEmpty()) {
            messageManager.sendMessage(player, "messages.naming.name-input.empty");
            awaitingInput.put(player.getUniqueId(), gui);
            return;
        }
        
        if (input.length() > 32) {
            messageManager.sendMessage(player, "messages.naming.name-input.too-long");
            awaitingInput.put(player.getUniqueId(), gui);
            return;
        }
        
        // Set the name
        gui.setGradientName(input);
        messageManager.sendMessage(player, "messages.naming.name-input.success", "name", input);
        
        // Reopen the GUI
        gui.open(player);
    }
}
