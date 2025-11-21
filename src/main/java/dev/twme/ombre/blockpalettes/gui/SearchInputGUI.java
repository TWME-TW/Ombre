package dev.twme.ombre.blockpalettes.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockpalettes.BlockPalettesFeature;
import dev.twme.ombre.blockpalettes.api.PaletteFilter;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * 搜尋輸入 GUI
 * 提供方塊名稱搜尋功能
 */
public class SearchInputGUI implements Listener {
    
    private final Ombre plugin;
    private final BlockPalettesFeature feature;
    private final Player player;
    private final PaletteFilter filter;
    private final Runnable onSearchApplied;
    private final Inventory inventory;
    
    // 全域追蹤等待輸入的玩家
    private static final Map<UUID, SearchInputGUI> waitingForInput = new HashMap<>();
    
    public SearchInputGUI(Ombre plugin, BlockPalettesFeature feature, Player player, 
                          PaletteFilter filter, Runnable onSearchApplied) {
        this.plugin = plugin;
        this.feature = feature;
        this.player = player;
        this.filter = filter;
        this.onSearchApplied = onSearchApplied;
        this.inventory = Bukkit.createInventory(null, 27, 
            Component.text("方塊搜尋").color(NamedTextColor.DARK_PURPLE));
        
        setupItems();
    }
    
    /**
     * 設置 GUI 物品
     */
    private void setupItems() {
        // 搜尋資訊 (格子 13)
        ItemStack infoItem = createItem(Material.WRITABLE_BOOK,
            Component.text("搜尋方塊").color(NamedTextColor.YELLOW),
            List.of(
                Component.text("在聊天輸入方塊名稱").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("範例:").color(NamedTextColor.WHITE),
                Component.text("  • oak_planks").color(NamedTextColor.GRAY),
                Component.text("  • stone").color(NamedTextColor.GRAY),
                Component.text("  • brick").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("▸ 點擊開始搜尋").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(13, infoItem);
        
        // 當前搜尋 (格子 11)
        String currentSearch = filter.getBlockSearch().isEmpty() ? "無" : filter.getBlockSearch();
        ItemStack currentItem = createItem(Material.PAPER,
            Component.text("當前搜尋").color(NamedTextColor.YELLOW),
            List.of(
                Component.text("搜尋內容: ").color(NamedTextColor.GRAY)
                    .append(Component.text(currentSearch).color(NamedTextColor.WHITE))
            )
        );
        inventory.setItem(11, currentItem);
        
        // 清除搜尋 (格子 15)
        if (!filter.getBlockSearch().isEmpty()) {
            ItemStack clearItem = createItem(Material.BARRIER,
                Component.text("清除搜尋").color(NamedTextColor.RED),
                List.of(
                    Component.text("▸ 點擊清除當前搜尋").color(NamedTextColor.YELLOW)
                )
            );
            inventory.setItem(15, clearItem);
        }
        
        // 返回 (格子 22)
        ItemStack backItem = createItem(Material.ARROW,
            Component.text("返回").color(NamedTextColor.YELLOW),
            List.of(
                Component.text("▸ 點擊返回列表").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(22, backItem);
    }
    
    /**
     * 建立物品
     */
    private ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 開啟 GUI
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * 處理點擊事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player clicker)) {
            return;
        }
        
        if (!clicker.equals(player)) {
            return;
        }
        
        // 檢查是否點擊的是 GUI 而不是玩家背包
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) {
            return;
        }
        
        int slot = event.getSlot();
        
        // 搜尋資訊 (格子 13)
        if (slot == 13) {
            startChatInput(clicker);
            return;
        }
        
        // 清除搜尋 (格子 15)
        if (slot == 15 && !filter.getBlockSearch().isEmpty()) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            filter.setBlockSearch("");
            filter.setPage(1);
            clicker.closeInventory();
            if (onSearchApplied != null) {
                onSearchApplied.run();
            }
            return;
        }
        
        // 返回 (格子 22)
        if (slot == 22) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            clicker.closeInventory();
            if (onSearchApplied != null) {
                onSearchApplied.run();
            }
            return;
        }
    }
    
    /**
     * 開始聊天輸入
     */
    private void startChatInput(Player player) {
        player.closeInventory();
        waitingForInput.put(player.getUniqueId(), this);
        
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  請在聊天輸入方塊名稱").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  範例: ").color(NamedTextColor.GRAY)
            .append(Component.text("oak_planks, stone, brick").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  輸入 ").color(NamedTextColor.GRAY)
            .append(Component.text("cancel").color(NamedTextColor.RED))
            .append(Component.text(" 取消搜尋").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY));
        
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }
    
    /**
     * 全域聊天事件處理器
     */
    public static class ChatListener implements Listener {
        
        @EventHandler
        public void onAsyncChat(AsyncChatEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            
            // 檢查玩家是否在等待輸入
            if (!waitingForInput.containsKey(uuid)) {
                return;
            }
            
            event.setCancelled(true);
            SearchInputGUI gui = waitingForInput.remove(uuid);
            
            // 取得輸入的文字
            String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            
            // 同步執行（因為這是異步事件）
            Bukkit.getScheduler().runTask(gui.plugin, () -> {
                gui.handleChatInput(player, input);
            });
        }
    }
    
    /**
     * 處理聊天輸入
     */
    private void handleChatInput(Player player, String input) {
        // 取消搜尋
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("✘ ").color(NamedTextColor.RED)
                .append(Component.text("已取消搜尋").color(NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (onSearchApplied != null) {
                    onSearchApplied.run();
                }
            }, 5L);
            return;
        }
        
        // 驗證輸入
        if (input.isEmpty()) {
            player.sendMessage(Component.text("✘ ").color(NamedTextColor.RED)
                .append(Component.text("搜尋內容不能為空").color(NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                open();
            }, 5L);
            return;
        }
        
        // 套用搜尋
        filter.setBlockSearch(input.toLowerCase());
        filter.setPage(1);
        
        player.sendMessage(Component.text("✓ ").color(NamedTextColor.GREEN)
            .append(Component.text("搜尋: ").color(NamedTextColor.GRAY))
            .append(Component.text(input).color(NamedTextColor.WHITE)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        
        // 返回並重新載入
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (onSearchApplied != null) {
                onSearchApplied.run();
            }
        }, 5L);
    }
    
    /**
     * 處理 GUI 關閉事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        
        // 註銷監聽器
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }
    
    /**
     * 清理等待輸入的玩家（當玩家離線時）
     */
    public static void removeWaitingPlayer(UUID uuid) {
        waitingForInput.remove(uuid);
    }
}
