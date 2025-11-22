package dev.twme.ombre.blockpalettes.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockpalettes.BlockPalettesFeature;
import dev.twme.ombre.blockpalettes.api.PaletteData;
import dev.twme.ombre.blockpalettes.util.MaterialValidator;
import dev.twme.ombre.i18n.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 收藏管理 GUI
 * 顯示玩家收藏的調色板
 */
public class FavoritesGUI implements Listener {
    
    private final Ombre plugin;
    private final BlockPalettesFeature feature;
    private final Player player;
    private final Inventory inventory;
    private final MessageManager messageManager;
    private List<PaletteData> favorites;
    private int page;
    private String sortMode; // "newest", "oldest", "likes"
    
    public FavoritesGUI(Ombre plugin, BlockPalettesFeature feature, Player player) {
        this.plugin = plugin;
        this.feature = feature;
        this.player = player;
        this.messageManager = plugin.getMessageManager();
        this.page = 1;
        this.sortMode = "newest";
        this.favorites = new ArrayList<>();
        
        // 建立 54 格箱子 (6x9)
        this.inventory = Bukkit.createInventory(null, 54,
            messageManager.getComponent("blockpalettes.gui.favorites.title", player)
        );
        
        setupControlItems();
        loadFavorites();
    }
    
    /**
     * 設定控制項
     */
    private void setupControlItems() {
        // 排序切換 (格子 0)
        ItemStack sortItem = createItem(Material.CLOCK,
            messageManager.getComponent("blockpalettes.gui.favorites.sort.title", player, "mode", getSortDisplayName()),
            List.of(
                messageManager.getComponent("blockpalettes.gui.favorites.sort.current", player, "mode", getSortDisplayName()),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.favorites.sort.click", player)
            )
        );
        inventory.setItem(0, sortItem);
        
        // 清空收藏 (格子 1)
        ItemStack clearItem = createItem(Material.TNT,
            messageManager.getComponent("blockpalettes.gui.favorites.clear.title", player),
            List.of(
                messageManager.getComponent("blockpalettes.gui.favorites.clear.description", player),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.favorites.clear.confirm", player)
            )
        );
        inventory.setItem(1, clearItem);
        
        // 返回主選單 (格子 52)
        ItemStack backItem = createItem(Material.ARROW,
            messageManager.getComponent("blockpalettes.gui.favorites.back", player),
            List.of(
                messageManager.getComponent("blockpalettes.gui.favorites.back-lore", player)
            )
        );
        inventory.setItem(52, backItem);
        
        // 關閉 (格子 53)
        ItemStack closeItem = createItem(Material.RED_DYE,
            messageManager.getComponent("blockpalettes.gui.favorites.close", player),
            List.of(
                messageManager.getComponent("blockpalettes.gui.favorites.close-lore", player)
            )
        );
        inventory.setItem(53, closeItem);
    }
    
    /**
     * 載入收藏的調色板
     */
    private void loadFavorites() {
        List<PaletteData> cachedFavorites = feature.getFavoritesManager().getFavorites(player.getUniqueId());
        
        // 過濾掉包含無效物品的調色板（靜默失敗，不記錄 log）
        List<PaletteData> validFavorites = MaterialValidator.filterValidPalettes(cachedFavorites);
        
        if (validFavorites.isEmpty()) {
            showEmptyState();
            return;
        }
        
        // 直接從快取載入，不需要 API 請求
        favorites = new ArrayList<>(validFavorites);
        sortFavorites();
        displayFavorites();
        updatePageButtons();
    }

    
    /**
     * 排序收藏
     */
    private void sortFavorites() {
        switch (sortMode) {
            case "oldest" -> favorites.sort(Comparator.comparingInt(PaletteData::getId));
            case "likes" -> favorites.sort(Comparator.comparingInt(PaletteData::getLikes).reversed());
            default -> favorites.sort(Comparator.comparingInt(PaletteData::getId).reversed()); // newest
        }
    }
    
    /**
     * 顯示收藏的調色板
     */
    private void displayFavorites() {
        // 清除調色板區域 (格子 9-44)
        for (int i = 9; i <= 44; i++) {
            inventory.setItem(i, null);
        }
        
        // 計算分頁 (每頁 4 個調色板)
        int startIdx = (page - 1) * 4;
        int endIdx = Math.min(startIdx + 4, favorites.size());
        
        // 每個調色板佔用 2行4列 = 8個格子
        // 6個方塊 + 資訊按鈕 + 移除按鈕
        int[][] paletteSlots = {
            {9, 10, 11, 18, 19, 20},   // 調色板 1 的方塊
            {14, 15, 16, 23, 24, 25},  // 調色板 2 的方塊
            {27, 28, 29, 36, 37, 38},  // 調色板 3 的方塊
            {32, 33, 34, 41, 42, 43}   // 調色板 4 的方塊
        };
        
        // 資訊按鈕格子（每個調色板右側上方）
        int[] infoSlots = {12, 17, 30, 35};
        
        // 移除按鈕格子（每個調色板右側下方）
        int[] removeSlots = {21, 26, 39, 44};
        
        for (int i = startIdx; i < endIdx; i++) {
            PaletteData palette = favorites.get(i);
            int displayIdx = i - startIdx;
            List<Material> materials = palette.getMaterials();
            
            // 顯示 6 個方塊（可拿取）
            if (materials != null && materials.size() >= 6) {
                for (int j = 0; j < 6; j++) {
                    ItemStack blockItem = new ItemStack(materials.get(j));
                    inventory.setItem(paletteSlots[displayIdx][j], blockItem);
                }
            }
            
            // 資訊按鈕
            ItemStack infoItem = createInfoButton(palette);
            inventory.setItem(infoSlots[displayIdx], infoItem);
            
            // 移除按鈕
            ItemStack removeItem = createRemoveButton(palette);
            inventory.setItem(removeSlots[displayIdx], removeItem);
        }
        
        // 更新統計
        updateStatistics();
    }
    
    /**
     * 建立資訊按鈕
     */
    private ItemStack createInfoButton(PaletteData palette) {
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("blockpalettes.gui.favorites.info.palette-id", player, "id", String.valueOf(palette.getId())));
        lore.add(Component.empty());
        
        String author = palette.getAuthor();
        lore.add(messageManager.getComponent("blockpalettes.gui.favorites.info.author", player, "author", author));
        
        String uploadTime = palette.getUploadTime();
        if (uploadTime != null && !uploadTime.isEmpty()) {
            lore.add(messageManager.getComponent("blockpalettes.gui.favorites.info.time", "time", uploadTime));
        }
        
        lore.add(messageManager.getComponent("blockpalettes.gui.favorites.info.likes", player, "likes", String.valueOf(palette.getLikes())));
        lore.add(Component.empty());
        lore.add(messageManager.getComponent("blockpalettes.gui.favorites.info.click-hint", player));
        
        return createItem(Material.BOOK,
            messageManager.getComponent("blockpalettes.gui.favorites.info.title", player)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false),
            lore
        );
    }
    
    /**
     * 建立移除按鈕
     */
    private ItemStack createRemoveButton(PaletteData palette) {
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("blockpalettes.gui.favorites.remove.palette-id", player, "id", String.valueOf(palette.getId())));
        lore.add(Component.empty());
        lore.add(messageManager.getComponent("blockpalettes.gui.favorites.remove.description", player));
        lore.add(Component.empty());
        lore.add(messageManager.getComponent("blockpalettes.gui.favorites.remove.click-hint", player));
        
        return createItem(Material.BARRIER,
            messageManager.getComponent("blockpalettes.gui.favorites.remove.title", player)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false),
            lore
        );
    }
    
    /**
     * 建立收藏物品（已棄用，保留作為備用）
     */
    private ItemStack createFavoriteItem(PaletteData palette) {
        // 安全地取得材料，如果列表為空則使用預設材料
        Material material = Material.BARRIER;
        if (palette.getMaterials() != null && !palette.getMaterials().isEmpty()) {
            material = palette.getMaterials().get(0);
        }
        
        List<Component> lore = new ArrayList<>();
        
        // 安全地取得作者名稱
        String author = palette.getAuthor();
        if (author == null || author.isEmpty()) {
            author = "未知";
        }
        lore.add(Component.text("作者: ").color(NamedTextColor.GRAY)
            .append(Component.text(author).color(NamedTextColor.WHITE)));
        
        lore.add(Component.text("喜歡: ").color(NamedTextColor.GRAY)
            .append(Component.text("❤ " + palette.getLikes()).color(NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        
        // 檢查是否有方塊名稱
        if (palette.getDisplayNames() != null && !palette.getDisplayNames().isEmpty()) {
            lore.add(Component.text("包含方塊:").color(NamedTextColor.GRAY));
            for (String blockName : palette.getDisplayNames()) {
                if (blockName != null && !blockName.isEmpty()) {
                    lore.add(Component.text("- " + blockName).color(NamedTextColor.WHITE));
                }
            }
        } else {
            lore.add(Component.text("無方塊資訊").color(NamedTextColor.GRAY));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("▸ 左鍵查看詳細").color(NamedTextColor.YELLOW));
        lore.add(Component.text("▸ 右鍵移除收藏").color(NamedTextColor.RED));
        
        return createItem(material,
            Component.text("Palette #" + palette.getId()).color(NamedTextColor.GOLD),
            lore
        );
    }
    
    /**
     * 顯示空狀態
     */
    private void showEmptyState() {
        ItemStack emptyItem = createItem(Material.BOOK,
            messageManager.getComponent("blockpalettes.gui.favorites.empty.title"),
            List.of(
                messageManager.getComponent("blockpalettes.gui.favorites.empty.line1"),
                messageManager.getComponent("blockpalettes.gui.favorites.empty.line2"),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.favorites.empty.limit")
            )
        );
        inventory.setItem(22, emptyItem);
        
        // 更新統計
        updateStatistics();
    }
    
    /**
     * 顯示載入中
     */
    private void showLoading() {
        ItemStack loading = createItem(Material.HOPPER,
            messageManager.getComponent("blockpalettes.gui.favorites.loading.title"),
            List.of(
                messageManager.getComponent("blockpalettes.gui.favorites.loading.description")
            )
        );
        inventory.setItem(22, loading);
    }
    
    /**
     * 顯示錯誤訊息
     */
    private void showError(String message) {
        ItemStack errorItem = createItem(Material.BARRIER,
            messageManager.getComponent("blockpalettes.gui.favorites.error.title"),
            List.of(
                Component.text(message).color(NamedTextColor.GRAY),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.favorites.error.retry")
            )
        );
        inventory.setItem(22, errorItem);
    }
    
    /**
     * 更新分頁按鈕
     */
    private void updatePageButtons() {
        int totalPages = (int) Math.ceil(favorites.size() / 4.0);  // 每頁 4 個調色板
        
        // 上一頁 (格子 7)
        if (page > 1) {
            ItemStack prevItem = createItem(Material.ARROW,
                messageManager.getComponent("blockpalettes.gui.favorites.page.prev"),
                List.of(messageManager.getComponent("blockpalettes.gui.favorites.page.number", "page", String.valueOf(page - 1)))
            );
            inventory.setItem(7, prevItem);
        } else {
            inventory.setItem(7, createItem(Material.GRAY_DYE,
                messageManager.getComponent("blockpalettes.gui.favorites.page.prev"),
                List.of(messageManager.getComponent("blockpalettes.gui.favorites.page.first"))
            ));
        }
        
        // 下一頁 (格子 8)
        if (page < totalPages) {
            ItemStack nextItem = createItem(Material.ARROW,
                messageManager.getComponent("blockpalettes.gui.favorites.page.next"),
                List.of(messageManager.getComponent("blockpalettes.gui.favorites.page.number", "page", String.valueOf(page + 1)))
            );
            inventory.setItem(8, nextItem);
        } else {
            inventory.setItem(8, createItem(Material.GRAY_DYE,
                messageManager.getComponent("blockpalettes.gui.favorites.page.next"),
                List.of(messageManager.getComponent("blockpalettes.gui.favorites.page.last"))
            ));
        }
    }
    
    /**
     * 更新統計資訊
     */
    private void updateStatistics() {
        int count = feature.getFavoritesManager().getFavoriteCount(player.getUniqueId());
        
        ItemStack statsItem = createItem(Material.PAPER,
            messageManager.getComponent("blockpalettes.gui.favorites.stats.title"),
            List.of(
                messageManager.getComponent("blockpalettes.gui.favorites.stats.count", "count", count + "/100"),
                messageManager.getComponent("blockpalettes.gui.favorites.stats.sort", "mode", getSortDisplayName())
            )
        );
        inventory.setItem(45, statsItem);
    }
    
    /**
     * 取得排序顯示名稱
     */
    private String getSortDisplayName() {
        return messageManager.getMessage("blockpalettes.gui.favorites.sort-mode." + sortMode, player);
    }
    
    /**
     * 切換排序
     */
    private void toggleSort() {
        sortMode = switch (sortMode) {
            case "newest" -> "oldest";
            case "oldest" -> "likes";
            case "likes" -> "newest";
            default -> "newest";
        };
        
        sortFavorites();
        setupControlItems();
        displayFavorites();
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
     * 開啟介面
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
        
        // 檢查是否點擊方塊格子（允許拿取並補充）
        int[][] paletteSlots = {
            {9, 10, 11, 18, 19, 20},   // 調色板 1
            {14, 15, 16, 23, 24, 25},  // 調色板 2
            {27, 28, 29, 36, 37, 38},  // 調色板 3
            {32, 33, 34, 41, 42, 43}   // 調色板 4
        };
        
        // 資訊和移除按鈕
        int[] infoSlots = {12, 17, 30, 35};
        int[] removeSlots = {21, 26, 39, 44};
        
        // 計算當前頁的調色板索引
        int startIdx = (page - 1) * 4;
        
        // 檢查是否點擊方塊並處理補充
        boolean isBlockSlot = false;
        for (int i = 0; i < paletteSlots.length; i++) {
            for (int j = 0; j < paletteSlots[i].length; j++) {
                if (slot == paletteSlots[i][j]) {
                    int paletteIdx = startIdx + i;
                    if (paletteIdx < favorites.size()) {
                        PaletteData palette = favorites.get(paletteIdx);
                        if (palette.getMaterials() != null && j < palette.getMaterials().size()) {
                            Material blockMaterial = palette.getMaterials().get(j);
                            
                            // 延遲補充方塊
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                ItemStack newBlock = new ItemStack(blockMaterial);
                                inventory.setItem(slot, newBlock);
                            }, 1L);
                            
                            isBlockSlot = true;
                        }
                    }
                    break;
                }
            }
            if (isBlockSlot) break;
        }
        
        // 如果是方塊格子，允許拿取
        if (isBlockSlot) {
            return;  // 不取消事件，允許玩家拿取
        }
        
        // 其他格子取消事件
        event.setCancelled(true);
        
        // 排序 (格子 0)
        if (slot == 0) {
            toggleSort();
            return;
        }
        
        // 清空收藏 (格子 1)
        if (slot == 1 && event.isShiftClick() && event.isLeftClick()) {
            clearAllFavorites(clicker);
            return;
        }
        
        // 上一頁 (格子 7)
        if (slot == 7 && page > 1) {
            page--;
            displayFavorites();
            updatePageButtons();
            return;
        }
        
        // 下一頁 (格子 8)
        if (slot == 8) {
            int totalPages = (int) Math.ceil(favorites.size() / 4.0);  // 每頁 4 個調色板
            if (page < totalPages) {
                page++;
                displayFavorites();
                updatePageButtons();
            }
            return;
        }
        
        // 返回主選單 (格子 52)
        if (slot == 52) {
            clicker.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                PalettesListGUI listGUI = new PalettesListGUI(plugin, feature, clicker);
                Bukkit.getPluginManager().registerEvents(listGUI, plugin);
                listGUI.open();
            }, 2L);
            return;
        }
        
        // 關閉 (格子 53)
        if (slot == 53) {
            clicker.closeInventory();
            return;
        }
        
        // 處理資訊按鈕點擊
        for (int i = 0; i < infoSlots.length; i++) {
            if (slot == infoSlots[i]) {
                int paletteIdx = startIdx + i;
                if (paletteIdx < favorites.size()) {
                    showPaletteInfo(clicker, favorites.get(paletteIdx));
                }
                return;
            }
        }
        
        // 處理移除按鈕點擊
        for (int i = 0; i < removeSlots.length; i++) {
            if (slot == removeSlots[i]) {
                int paletteIdx = startIdx + i;
                if (paletteIdx < favorites.size()) {
                    removeFavorite(clicker, favorites.get(paletteIdx));
                }
                return;
            }
        }
    }
    
    /**
     * 顯示調色板資訊
     */
    private void showPaletteInfo(Player player, PaletteData palette) {
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.favorites.detail.separator"));
        player.sendMessage(
            messageManager.getComponent("blockpalettes.gui.favorites.detail.title", "id", String.valueOf(palette.getId()))
        );
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.favorites.detail.author", "author", palette.getAuthor()));
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.favorites.detail.time", "time", palette.getUploadTime()));
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.favorites.detail.likes", "likes", String.valueOf(palette.getLikes())));
        player.sendMessage(Component.empty());
        
        for (String blockName : palette.getDisplayNames()) {
            player.sendMessage(Component.text("• " + blockName).color(NamedTextColor.WHITE));
        }
        
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.favorites.detail.separator"));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    
    /**
     * 移除收藏
     */
    private void removeFavorite(Player player, PaletteData palette) {
        feature.getFavoritesManager().removeFavorite(player.getUniqueId(), palette.getId());
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.favorites.messages.removed", "id", String.valueOf(palette.getId())));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
        
        // 重新載入
        loadFavorites();
    }
    
    /**
     * 清空所有收藏
     */
    private void clearAllFavorites(Player player) {
        feature.getFavoritesManager().clearFavorites(player.getUniqueId());
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.favorites.messages.cleared"));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        
        favorites.clear();
        showEmptyState();
    }
}
