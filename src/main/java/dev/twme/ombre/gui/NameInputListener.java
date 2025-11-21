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
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * 漸層名稱輸入監聽器
 * 處理玩家在聊天欄輸入的漸層名稱
 */
public class NameInputListener implements Listener {
    
    private final Ombre plugin;
    private final GUIManager guiManager;
    
    // 等待輸入名稱的玩家
    private final Map<UUID, OmbreGUI> awaitingInput;
    
    public NameInputListener(Ombre plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
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
        
        // 取消事件，避免訊息被發送到聊天
        event.setCancelled(true);
        
        OmbreGUI gui = awaitingInput.remove(playerId);
        
        // 將 Component 訊息轉換為純文字
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        
        // 在主執行緒中處理名稱設定
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
        // 檢查取消命令
        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("取消")) {
            player.sendMessage(Component.text("已取消命名").color(NamedTextColor.YELLOW));
            gui.open(player);
            return;
        }
        
        // 驗證名稱長度
        if (input.isEmpty()) {
            player.sendMessage(Component.text("名稱不能為空！請重新輸入或輸入 'cancel' 取消").color(NamedTextColor.RED));
            awaitingInput.put(player.getUniqueId(), gui);
            return;
        }
        
        if (input.length() > 32) {
            player.sendMessage(Component.text("名稱太長！請輸入不超過32字元的名稱或輸入 'cancel' 取消").color(NamedTextColor.RED));
            awaitingInput.put(player.getUniqueId(), gui);
            return;
        }
        
        // 設定名稱
        gui.setGradientName(input);
        player.sendMessage(Component.text("已將漸層命名為: ").color(NamedTextColor.GREEN)
            .append(Component.text(input).color(NamedTextColor.YELLOW)));
        
        // 重新打開 GUI
        gui.open(player);
    }
}
