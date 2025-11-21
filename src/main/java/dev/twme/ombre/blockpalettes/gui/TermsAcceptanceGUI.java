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
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * 條款同意介面
 * 首次使用 Block Palettes 時透過聊天欄文字互動顯示
 */
public class TermsAcceptanceGUI implements Listener {
    
    private final Plugin plugin;
    private final Player player;
    private final Runnable onAccept;
    private final Runnable onDecline;
    private static final Map<UUID, TermsAcceptanceGUI> awaitingResponse = new HashMap<>();
    
    public TermsAcceptanceGUI(Plugin plugin, Player player, Runnable onAccept, Runnable onDecline) {
        this.plugin = plugin;
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
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        player.sendMessage(
            Component.text("Block Palettes 使用條款與隱私政策")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GOLD));
        player.sendMessage(Component.empty());
        
        // 功能說明
        player.sendMessage(
            Component.text("本功能使用 Block Palettes 網站提供的公開 API")
                .color(NamedTextColor.GRAY)
        );
        player.sendMessage(
            Component.text("來瀏覽社群創作的方塊配色方案。")
                .color(NamedTextColor.GRAY)
        );
        player.sendMessage(Component.empty());
        
        // 條款說明
        player.sendMessage(
            Component.text("在使用前，您需要同意以下條款:")
                .color(NamedTextColor.YELLOW)
        );
        player.sendMessage(Component.empty());
        
        // 使用條款連結
        player.sendMessage(
            Component.text("1. ")
                .color(NamedTextColor.WHITE)
                .append(Component.text("Block Palettes 使用條款")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true))
        );
        player.sendMessage(
            Component.text("   ")
                .append(Component.text("[點擊開啟使用條款]")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.UNDERLINED, true)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(TermsAcceptanceData.TERMS_URL)))
        );
        player.sendMessage(
            Component.text("   " + TermsAcceptanceData.TERMS_URL)
                .color(NamedTextColor.DARK_GRAY)
        );
        player.sendMessage(Component.empty());
        
        // 隱私政策連結
        player.sendMessage(
            Component.text("2. ")
                .color(NamedTextColor.WHITE)
                .append(Component.text("Block Palettes 隱私政策")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.BOLD, true))
        );
        player.sendMessage(
            Component.text("   ")
                .append(Component.text("[點擊開啟隱私政策]")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.UNDERLINED, true)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(TermsAcceptanceData.PRIVACY_URL)))
        );
        player.sendMessage(
            Component.text("   " + TermsAcceptanceData.PRIVACY_URL)
                .color(NamedTextColor.DARK_GRAY)
        );
        player.sendMessage(Component.empty());
        
        // 免責聲明
        player.sendMessage(
            Component.text("這些條款由 Block Palettes 網站制定，與本伺服器無關。")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true)
        );
        player.sendMessage(Component.empty());
        
        // 選項說明
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
        player.sendMessage(
            Component.text("請在聊天欄輸入您的選擇:")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
        );
        player.sendMessage(Component.empty());
        player.sendMessage(
            Component.text("  ✓ ")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text("輸入 ")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, false))
                .append(Component.text("agree")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" 或 ")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, false))
                .append(Component.text("同意")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" - 同意條款並繼續")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.empty());
        player.sendMessage(
            Component.text("  ✗ ")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text("輸入 ")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, false))
                .append(Component.text("decline")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" 或 ")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, false))
                .append(Component.text("不同意")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" - 拒絕條款並關閉")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
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
        if (message.equals("agree") || message.equals("同意") || message.equals("yes") || message.equals("是")) {
            // 同意條款
            awaitingResponse.remove(playerId);
            
            player.sendMessage(Component.empty());
            player.sendMessage(
                Component.text("✓ 已同意 Block Palettes 使用條款與隱私政策")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
            );
            player.sendMessage(
                Component.text("感謝您的同意,現在可以使用 Block Palettes 功能了!")
                    .color(NamedTextColor.GRAY)
            );
            player.sendMessage(Component.empty());
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            if (gui.onAccept != null) {
                gui.onAccept.run();
            }
            
        } else if (message.equals("decline") || message.equals("不同意") || message.equals("no") || message.equals("否")) {
            // 不同意條款
            awaitingResponse.remove(playerId);
            
            player.sendMessage(Component.empty());
            player.sendMessage(
                Component.text("✗ 您已拒絕 Block Palettes 使用條款")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
            );
            player.sendMessage(
                Component.text("您需要同意條款才能使用 Block Palettes 瀏覽功能")
                    .color(NamedTextColor.GRAY)
            );
            player.sendMessage(
                Component.text("不過您仍然可以使用伺服器的其他功能")
                    .color(NamedTextColor.GRAY)
            );
            player.sendMessage(Component.empty());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            
            if (gui.onDecline != null) {
                gui.onDecline.run();
            }
            
        } else {
            // 無效輸入
            player.sendMessage(Component.empty());
            player.sendMessage(
                Component.text("⚠ 無效的輸入")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
            );
            player.sendMessage(
                Component.text("請輸入 ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text("agree")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true))
                    .append(Component.text("/")
                        .color(NamedTextColor.GRAY))
                    .append(Component.text("同意")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true))
                    .append(Component.text(" 或 ")
                        .color(NamedTextColor.GRAY))
                    .append(Component.text("decline")
                        .color(NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true))
                    .append(Component.text("/")
                        .color(NamedTextColor.GRAY))
                    .append(Component.text("不同意")
                        .color(NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true))
            );
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
