package dev.twme.ombre.blockcolors.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockcolors.util.ColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * HEX 色碼輸入 GUI
 * 使用 Anvil 介面讓玩家輸入 HEX 色碼
 */
public class HexInputGUI implements Listener {
    private final BlockColorsFeature feature;
    private final Player player;
    private final BlockColorsGUI parentGUI;
    private boolean processed = false;

    public HexInputGUI(BlockColorsFeature feature, Player player, BlockColorsGUI parentGUI) {
        this.feature = feature;
        this.player = player;
        this.parentGUI = parentGUI;
    }

    /**
     * 開啟 Anvil GUI
     */
    public void open() {
        // 註冊事件監聽器
        Bukkit.getPluginManager().registerEvents(this, feature.getPlugin());
        
        // 開啟 Anvil GUI
        InventoryView view = player.openAnvil(null, true);
        if (view != null) {
            Inventory anvil = view.getTopInventory();
            
            // 設置預設的 HEX 輸入項
            ItemStack input = new ItemStack(Material.PAPER);
            ItemMeta meta = input.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("#FFFFFF"));
                input.setItemMeta(meta);
            }
            
            anvil.setItem(0, input);
            
            player.sendMessage("§e請輸入 HEX 色碼 (例如: #FF5733 或 FF5733)");
            player.sendMessage("§7完成後點擊結果槽");
        } else {
            player.sendMessage("§c無法開啟 HEX 輸入介面");
            cleanup();
        }
    }

    /**
     * 處理 Anvil 點擊
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (processed) return;
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!p.equals(player)) return;
        
        // 檢查是否是 Anvil 介面
        if (event.getView().getTopInventory().getSize() != 3) return;
        
        // 只處理結果槽（槽位 2）
        if (event.getRawSlot() != 2) return;
        
        event.setCancelled(true);
        
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            player.sendMessage("§c請先輸入 HEX 色碼");
            return;
        }
        
        // 獲取輸入的文字
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;
        
        Component displayName = meta.displayName();
        if (displayName == null) return;
        
        // 使用 PlainTextComponentSerializer 正確提取純文字
        String input = PlainTextComponentSerializer.plainText().serialize(displayName);
        
        // 移除可能的格式化代碼
        input = input.replaceAll("§[0-9a-fk-or]", "");
        input = input.trim();
        
        // 驗證並解析 HEX
        if (!ColorConverter.isValidHex(input)) {
            player.sendMessage("§c無效的 HEX 色碼格式");
            player.sendMessage("§7請使用格式: #RRGGBB 或 RRGGBB");
            return;
        }
        
        int rgb = ColorConverter.hexToRgb(input);
        if (rgb == -1) {
            player.sendMessage("§c無法解析 HEX 色碼");
            return;
        }
        
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        
        processed = true;
        
        player.closeInventory();
        player.sendMessage("§a✓ 已設定顏色為 " + ColorConverter.rgbToHex(red, green, blue));
        player.sendMessage("§7RGB: (" + red + ", " + green + ", " + blue + ")");
        
        // 延遲開啟主 GUI 並設定顏色
        Bukkit.getScheduler().runTaskLater(feature.getPlugin(), () -> {
            if (parentGUI != null) {
                parentGUI.setColor(red, green, blue);
                parentGUI.open();
            }
            cleanup();
        }, 1L);
    }

    /**
     * 處理關閉事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        if (!p.equals(player)) return;
        
        // 如果還沒處理完就關閉了，清理並返回主 GUI
        if (!processed) {
            Bukkit.getScheduler().runTaskLater(feature.getPlugin(), () -> {
                if (parentGUI != null) {
                    parentGUI.open();
                }
                cleanup();
            }, 1L);
        }
    }

    /**
     * 清理事件監聽器
     */
    private void cleanup() {
        HandlerList.unregisterAll(this);
    }
}
