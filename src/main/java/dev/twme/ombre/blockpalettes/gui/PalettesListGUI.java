package dev.twme.ombre.blockpalettes.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import dev.twme.ombre.blockpalettes.api.APIResponse;
import dev.twme.ombre.blockpalettes.api.PaletteData;
import dev.twme.ombre.blockpalettes.api.PaletteFilter;
import dev.twme.ombre.blockpalettes.util.MaterialValidator;
import dev.twme.ombre.i18n.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 調色板列表 GUI
 * 顯示所有調色板的瀏覽介面
 */
public class PalettesListGUI implements Listener {
    
    private final Ombre plugin;
    private final BlockPalettesFeature feature;
    private final Player player;
    private final Inventory inventory;
    private final MessageManager messageManager;
    private PaletteFilter filter;
    private APIResponse currentResponse;
    private boolean loading;
    
    public PalettesListGUI(Ombre plugin, BlockPalettesFeature feature, Player player) {
        this.plugin = plugin;
        this.feature = feature;
        this.player = player;
        this.messageManager = plugin.getMessageManager();
        this.filter = new PaletteFilter();
        this.filter.setLimit(4);  // 每頁只顯示 4 個調色板
        this.loading = false;
        
        // 建立 54 格箱子 (6x9)
        this.inventory = Bukkit.createInventory(null, 54,
            messageManager.getComponent("blockpalettes.gui.list.title", player)
        );
        
        setupControlItems();
        loadPalettes(false);
    }
    
    /**
     * 設定控制項
     */
    private void setupControlItems() {
        // 搜尋 (格子 0)
        String searchDisplay = filter.getBlockSearch().isEmpty() ? 
            messageManager.getMessage("blockpalettes.gui.list.control.none", player) : filter.getBlockSearch();
        ItemStack searchItem = createItem(Material.WRITABLE_BOOK,
            messageManager.getComponent("blockpalettes.gui.list.control.search.title", player, "search", searchDisplay),
            List.of(
                messageManager.getComponent("blockpalettes.gui.list.control.search.current", player, "search", searchDisplay),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.list.control.search.click", player)
            )
        );
        inventory.setItem(0, searchItem);
        
        // 排序 (格子 1)
        ItemStack sortItem = createItem(Material.CLOCK,
            messageManager.getComponent("blockpalettes.gui.list.control.sort.title", player, "mode", getSortDisplayName()),
            List.of(
                messageManager.getComponent("blockpalettes.gui.list.control.sort.current", player, "mode", getSortDisplayName()),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.list.control.sort.click", player)
            )
        );
        inventory.setItem(1, sortItem);
        
        // 顏色篩選 (格子 2)
        String colorDisplay = getColorDisplayName(filter.getColor());
        ItemStack colorItem = createItem(Material.LIME_DYE,
            messageManager.getComponent("blockpalettes.gui.list.control.color.title", player, "color", colorDisplay),
            List.of(
                messageManager.getComponent("blockpalettes.gui.list.control.color.current", player, "color", colorDisplay),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.list.control.color.click", player)
            )
        );
        inventory.setItem(2, colorItem);
        
        // 上一頁 (格子 7)
        updatePageButtons();
        
        // 重新整理 (格子 52)
        ItemStack refreshItem = createItem(Material.LIME_DYE,
            messageManager.getComponent("blockpalettes.gui.list.control.refresh.title", player),
            List.of(
                messageManager.getComponent("blockpalettes.gui.list.control.refresh.description", player),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.list.control.refresh.click", player)
            )
        );
        inventory.setItem(52, refreshItem);
        
        // 關閉 (格子 53)
        ItemStack closeItem = createItem(Material.RED_DYE,
            messageManager.getComponent("blockpalettes.gui.list.control.close.title", player),
            List.of(
                messageManager.getComponent("blockpalettes.gui.list.control.close.click", player)
            )
        );
        inventory.setItem(53, closeItem);
    }
    
    /**
     * 更新分頁按鈕
     */
    private void updatePageButtons() {
        if (currentResponse == null) {
            return;
        }
        
        // 上一頁 (格子 7)
        if (filter.getPage() > 1) {
            ItemStack prevItem = createItem(Material.ARROW,
                messageManager.getComponent("blockpalettes.gui.list.page.prev", player),
                List.of(
                    messageManager.getComponent("blockpalettes.gui.list.page.number", player, "page", String.valueOf(filter.getPage() - 1))
                )
            );
            inventory.setItem(7, prevItem);
        } else {
            inventory.setItem(7, createItem(Material.GRAY_DYE,
                messageManager.getComponent("blockpalettes.gui.list.page.prev", player),
                List.of(messageManager.getComponent("blockpalettes.gui.list.page.first", player))
            ));
        }
        
        // 下一頁 (格子 8)
        if (filter.getPage() < currentResponse.getTotalPages()) {
            ItemStack nextItem = createItem(Material.ARROW,
                messageManager.getComponent("blockpalettes.gui.list.page.next", player),
                List.of(
                    messageManager.getComponent("blockpalettes.gui.list.page.number", player, "page", String.valueOf(filter.getPage() + 1))
                )
            );
            inventory.setItem(8, nextItem);
        } else {
            inventory.setItem(8, createItem(Material.GRAY_DYE,
                messageManager.getComponent("blockpalettes.gui.list.page.next", player),
                List.of(messageManager.getComponent("blockpalettes.gui.list.page.last", player))
            ));
        }
        
        // 統計資訊 (格子 45)
        int favoriteCount = feature.getFavoritesManager().getFavoriteCount(player.getUniqueId());
        ItemStack statsInfo = createItem(Material.PAPER,
            messageManager.getComponent("blockpalettes.gui.list.stats.title", player),
            List.of(
                messageManager.getComponent("blockpalettes.gui.list.stats.current-page", player, Map.of("page", filter.getPage() + "/" + currentResponse.getTotalPages())),
                messageManager.getComponent("blockpalettes.gui.list.stats.total", player, Map.of("total", String.valueOf(currentResponse.getTotalResults()))),
                messageManager.getComponent("blockpalettes.gui.list.stats.favorites", player, Map.of("count", favoriteCount + "/100"))
            )
        );
        inventory.setItem(45, statsInfo);
    }
    
    /**
     * 載入調色板資料
     */
    private void loadPalettes(boolean forceRefresh) {
        if (loading) {
            return;
        }
        
        loading = true;
        showLoading();
        
        feature.getCache().getPalettes(filter, forceRefresh).thenAccept(response -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                this.currentResponse = response;
                loading = false;
                
                if (response.isSuccess()) {
                    displayPalettes(response.getPalettes());
                    updatePageButtons();
                } else {
                    showError(response.getError());
                }
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                loading = false;
                showError(messageManager.getMessage("blockpalettes.gui.list.error.load-failed", player, "error", ex.getMessage()));
            });
            return null;
        });
    }
    
    /**
     * 顯示調色板
     */
    private void displayPalettes(List<PaletteData> palettes) {
        // 過濾掉包含無效物品的調色板（靜默失敗，不記錄 log）
        List<PaletteData> validPalettes = MaterialValidator.filterValidPalettes(palettes);
        
        // 清除調色板區域 (格子 9-44)
        for (int i = 9; i <= 44; i++) {
            inventory.setItem(i, null);
        }
        
        // 每個調色板佔用 2行4列 = 8個格子
        // 6個方塊 + 資訊按鈕 + 收藏按鈕
        // 一次顯示 4 個調色板（2x2 佈局）
        // 調色板 1: 左上 (格子 9-12, 18-21)
        // 調色板 2: 右上 (格子 14-17, 23-26) 
        // 調色板 3: 左下 (格子 27-30, 36-39)
        // 調色板 4: 右下 (格子 32-35, 41-44)
        
        int[][] paletteSlots = {
            {9, 10, 11, 18, 19, 20},   // 調色板 1 的方塊
            {14, 15, 16, 23, 24, 25},  // 調色板 2 的方塊
            {27, 28, 29, 36, 37, 38},  // 調色板 3 的方塊
            {32, 33, 34, 41, 42, 43}   // 調色板 4 的方塊
        };
        
        // 資訊按鈕格子（每個調色板右側上方）
        int[] infoSlots = {12, 17, 30, 35};
        
        // 收藏按鈕格子（每個調色板右側下方）
        int[] favoriteSlots = {21, 26, 39, 44};
        
        for (int i = 0; i < Math.min(validPalettes.size(), 4); i++) {
            PaletteData palette = validPalettes.get(i);
            List<Material> materials = palette.getMaterials();
            
            // 顯示 6 個方塊（可拿取）
            for (int j = 0; j < 6; j++) {
                ItemStack blockItem = new ItemStack(materials.get(j));
                inventory.setItem(paletteSlots[i][j], blockItem);
            }
            
            // 資訊按鈕
            ItemStack infoItem = createPaletteInfoButton(palette);
            inventory.setItem(infoSlots[i], infoItem);
            
            // 收藏按鈕
            ItemStack favoriteItem = createFavoriteButton(palette);
            inventory.setItem(favoriteSlots[i], favoriteItem);
        }
        
        // 如果沒有結果
        if (validPalettes.isEmpty()) {
            ItemStack noResults = createItem(Material.BARRIER,
                messageManager.getComponent("blockpalettes.gui.list.no-results.title", player),
                List.of(
                    messageManager.getComponent("blockpalettes.gui.list.no-results.hint", player)
                )
            );
            inventory.setItem(22, noResults);
        }
    }
    
    /**
     * 建立調色板資訊按鈕
     */
    private ItemStack createPaletteInfoButton(PaletteData palette) {
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.info.id", player, "id", String.valueOf(palette.getId())));
        lore.add(Component.empty());
        lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.info.author", player, "author", palette.getAuthor()));
        lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.info.time", player, "time", palette.getUploadTime()));
        lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.info.likes", player, "likes", String.valueOf(palette.getLikes())));
        lore.add(Component.empty());
        lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.info.click", player));
        
        return createItem(Material.BOOK,
            messageManager.getComponent("blockpalettes.gui.list.palette.info.title", player)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false),
            lore
        );
    }
    
    /**
     * 建立收藏按鈕
     */
    private ItemStack createFavoriteButton(PaletteData palette) {
        boolean isFavorite = feature.getFavoritesManager().isFavorite(player.getUniqueId(), palette.getId());
        
        Material material = isFavorite ? Material.NETHER_STAR : Material.GOLD_NUGGET;
        Component titleComponent = isFavorite ? 
            messageManager.getComponent("blockpalettes.gui.list.palette.favorite.favorited", player) :
            messageManager.getComponent("blockpalettes.gui.list.palette.favorite.title", player);
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.favorite.id", player, "id", String.valueOf(palette.getId())));
        lore.add(Component.empty());
        
        if (isFavorite) {
            lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.favorite.already", player));
            lore.add(Component.empty());
            lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.favorite.remove-click", player));
        } else {
            lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.favorite.description", player));
            lore.add(Component.empty());
            lore.add(messageManager.getComponent("blockpalettes.gui.list.palette.favorite.add-click", player));
        }
        
        return createItem(material,
            titleComponent.decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false),
            lore
        );
    }
    
    /**
     * 顯示載入中
     */
    private void showLoading() {
        ItemStack loading = createItem(Material.HOPPER,
            messageManager.getComponent("blockpalettes.gui.list.loading.title", player),
            List.of(
                messageManager.getComponent("blockpalettes.gui.list.loading.description", player)
            )
        );
        inventory.setItem(22, loading);
    }
    
    /**
     * 顯示錯誤
     */
    private void showError(String error) {
        ItemStack errorItem = createItem(Material.BARRIER,
            messageManager.getComponent("blockpalettes.gui.list.error.title", player),
            List.of(
                Component.text(error).color(NamedTextColor.GRAY),
                Component.empty(),
                messageManager.getComponent("blockpalettes.gui.list.error.retry", player)
            )
        );
        inventory.setItem(22, errorItem);
    }
    
    /**
     * 取得排序顯示名稱
     */
    private String getSortDisplayName() {
        String sortKey = filter.getSortBy();
        if (sortKey == null || sortKey.isEmpty()) {
            sortKey = "recent";
        }
        return messageManager.getMessage("blockpalettes.gui.list.sort-mode." + sortKey, player);
    }
    
    /**
     * 切換排序
     */
    private void toggleSort() {
        String currentSort = filter.getSortBy();
        String newSort = switch (currentSort) {
            case "recent" -> "popular";
            case "popular" -> "oldest";
            case "oldest" -> "trending";
            case "trending" -> "recent";
            default -> "recent";
        };
        filter.setSortBy(newSort);
        filter.setPage(1);
        setupControlItems();
        loadPalettes(true);
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
        
        boolean isBlockSlot = false;
        int paletteIndex = -1;
        int blockIndex = -1;
        
        for (int i = 0; i < paletteSlots.length; i++) {
            for (int j = 0; j < paletteSlots[i].length; j++) {
                if (slot == paletteSlots[i][j]) {
                    isBlockSlot = true;
                    paletteIndex = i;
                    blockIndex = j;
                    break;
                }
            }
            if (isBlockSlot) break;
        }
        
        // 如果點擊方塊格子，允許拿取並補充
        List<PaletteData> displayedPalettes = MaterialValidator.filterValidPalettes(currentResponse != null ? currentResponse.getPalettes() : List.of());
        if (isBlockSlot && !displayedPalettes.isEmpty() && paletteIndex < displayedPalettes.size()) {
            PaletteData palette = displayedPalettes.get(paletteIndex);
            Material blockMaterial = palette.getMaterials().get(blockIndex);
            
            // 延遲補充方塊
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack newBlock = new ItemStack(blockMaterial);
                inventory.setItem(slot, newBlock);
            }, 1L);
            
            // 不取消事件，允許玩家拿取
            return;
        }
        
        // 其他格子取消事件
        event.setCancelled(true);
        
        // 搜尋 (格子 0)
        if (slot == 0) {
            openSearchInput(clicker);
            return;
        }
        
        // 顏色篩選 (格子 2)
        if (slot == 2) {
            openColorFilter(clicker);
            return;
        }
        
        // 排序 (格子 1)
        if (slot == 1) {
            toggleSort();
            return;
        }
        
        // 上一頁 (格子 7)
        if (slot == 7 && filter.getPage() > 1) {
            filter.setPage(filter.getPage() - 1);
            loadPalettes(false);
            return;
        }
        
        // 下一頁 (格子 8)
        if (slot == 8 && currentResponse != null && filter.getPage() < currentResponse.getTotalPages()) {
            filter.setPage(filter.getPage() + 1);
            loadPalettes(false);
            return;
        }
        
        // 重新整理 (格子 52)
        if (slot == 52) {
            loadPalettes(true);
            return;
        }
        
        // 關閉 (格子 53)
        if (slot == 53) {
            clicker.closeInventory();
            return;
        }
        
        // 調色板資訊和收藏按鈕
        List<PaletteData> displayedPalettes2 = MaterialValidator.filterValidPalettes(currentResponse != null ? currentResponse.getPalettes() : List.of());
        if (!displayedPalettes2.isEmpty()) {
            int[] infoSlots = {12, 17, 30, 35};  // 資訊按鈕
            int[] favoriteSlots = {21, 26, 39, 44};  // 收藏按鈕
            
            // 資訊按鈕 - 複製方塊清單
            for (int i = 0; i < infoSlots.length && i < displayedPalettes2.size(); i++) {
                if (slot == infoSlots[i]) {
                    PaletteData palette = displayedPalettes2.get(i);
                    copyBlockList(clicker, palette);
                    return;
                }
            }
            
            // 收藏按鈕 - 切換收藏狀態
            for (int i = 0; i < favoriteSlots.length && i < displayedPalettes2.size(); i++) {
                if (slot == favoriteSlots[i]) {
                    PaletteData palette = displayedPalettes2.get(i);
                    toggleFavorite(clicker, palette);
                    return;
                }
            }
        }
    }
    
    /**
     * 取得顏色顯示名稱
     */
    private String getColorDisplayName(String colorId) {
        if (colorId == null || colorId.isEmpty()) {
            colorId = "all";
        }
        return messageManager.getMessage("blockpalettes.gui.color-filter.color." + colorId, player);
    }
    
    /**
     * 開啟搜尋輸入選單
     */
    private void openSearchInput(Player player) {
        player.closeInventory();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            SearchInputGUI searchGUI = new SearchInputGUI(plugin, feature, player, filter, () -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    setupControlItems();  // 更新控制項顯示
                    loadPalettes(true);
                    player.openInventory(inventory);
                }, 2L);
            });
            Bukkit.getPluginManager().registerEvents(searchGUI, plugin);
            searchGUI.open();
        }, 2L);
    }
    
    /**
     * 開啟顏色篩選選單
     */
    private void openColorFilter(Player player) {
        player.closeInventory();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ColorFilterGUI colorGUI = new ColorFilterGUI(plugin, feature, player, filter, () -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    setupControlItems();  // 更新控制項顯示
                    loadPalettes(true);
                    player.openInventory(inventory);
                }, 2L);
            });
            Bukkit.getPluginManager().registerEvents(colorGUI, plugin);
            colorGUI.open();
        }, 2L);
    }
    
    /**
     * 複製方塊清單到聊天欄
     */
    private void copyBlockList(Player player, PaletteData palette) {
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.copy.separator", player));
        player.sendMessage(
            messageManager.getComponent("blockpalettes.gui.list.copy.title", player, "id", String.valueOf(palette.getId()))
        );
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.copy.author", player, "author", palette.getAuthor()));
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.copy.time", player, "time", palette.getUploadTime()));
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.copy.likes", player, "likes", String.valueOf(palette.getLikes())));
        player.sendMessage(Component.empty());
        
        for (String blockName : palette.getDisplayNames()) {
            player.sendMessage(Component.text("• " + blockName).color(NamedTextColor.WHITE));
        }
        
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.copy.separator", player));
        player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.copy.success", player));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    
    /**
     * 切換收藏狀態
     */
    private void toggleFavorite(Player player, PaletteData palette) {
        boolean isFavorite = feature.getFavoritesManager().isFavorite(player.getUniqueId(), palette.getId());
        
        if (isFavorite) {
            // 移除收藏
            feature.getFavoritesManager().removeFavorite(player.getUniqueId(), palette.getId());
            player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.messages.removed", player, "id", String.valueOf(palette.getId())));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
        } else {
            // 加入收藏
            boolean success = feature.getFavoritesManager().addFavorite(player.getUniqueId(), palette);
            if (success) {
                player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.messages.added", player, "id", String.valueOf(palette.getId())));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            } else {
                player.sendMessage(messageManager.getComponent("blockpalettes.gui.list.messages.full", player));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
        
        // 重新載入以更新收藏按鈕
        List<PaletteData> refreshPalettes = MaterialValidator.filterValidPalettes(currentResponse.getPalettes());
        displayPalettes(refreshPalettes);
    }
}
