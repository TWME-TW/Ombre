package dev.twme.ombre.blockcolors.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * BlockColors GUI 事件監聽器
 * 統一處理所有 BlockColors 相關 GUI 的事件
 */
public class BlockColorsGUIListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof BlockColorsGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof PaletteGUI gui) {
            gui.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        // 可以在這裡加入關閉時的清理邏輯
        // 目前暫時不需要
    }
}
