package dev.twme.ombre.blockpalettes.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    private PaletteFilter filter;
    private APIResponse currentResponse;
    private boolean loading;
    
    public PalettesListGUI(Ombre plugin, BlockPalettesFeature feature, Player player) {
        this.plugin = plugin;
        this.feature = feature;
        this.player = player;
        this.filter = new PaletteFilter();
        this.filter.setLimit(4);  // 每頁只顯示 4 個調色板
        this.loading = false;
        
        // 建立 54 格箱子 (6x9)
        this.inventory = Bukkit.createInventory(null, 54,
            Component.text("Block Palettes")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
        );
        
        setupControlItems();
        loadPalettes(false);
    }
    
    /**
     * 設定控制項
     */
    private void setupControlItems() {
        // 搜尋 (格子 0)
        String searchDisplay = filter.getBlockSearch().isEmpty() ? "無" : filter.getBlockSearch();
        ItemStack searchItem = createItem(Material.WRITABLE_BOOK,
            Component.text("搜尋方塊: " + searchDisplay).color(NamedTextColor.YELLOW),
            List.of(
                Component.text("當前搜尋: ").color(NamedTextColor.GRAY)
                    .append(Component.text(searchDisplay).color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("▸ 點擊開啟搜尋").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(0, searchItem);
        
        // 排序 (格子 1)
        ItemStack sortItem = createItem(Material.CLOCK,
            Component.text("排序: " + getSortDisplayName()).color(NamedTextColor.YELLOW),
            List.of(
                Component.text("當前排序: ").color(NamedTextColor.GRAY)
                    .append(Component.text(getSortDisplayName()).color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("▸ 點擊切換排序").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(1, sortItem);
        
        // 顏色篩選 (格子 2)
        String colorDisplay = getColorDisplayName(filter.getColor());
        ItemStack colorItem = createItem(Material.LIME_DYE,
            Component.text("顏色篩選: " + colorDisplay).color(NamedTextColor.YELLOW),
            List.of(
                Component.text("當前顏色: ").color(NamedTextColor.GRAY)
                    .append(Component.text(colorDisplay).color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("▸ 點擊開啟顏色選單").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(2, colorItem);
        
        // 熱門方塊 (格子 3)
        String blockDisplay = filter.getBlockSearch().isEmpty() ? "無" : getBlockDisplayName(filter.getBlockSearch());
        ItemStack blockItem = createItem(Material.DIAMOND_BLOCK,
            Component.text("方塊篩選: " + blockDisplay).color(NamedTextColor.YELLOW),
            List.of(
                Component.text("當前方塊: ").color(NamedTextColor.GRAY)
                    .append(Component.text(blockDisplay).color(NamedTextColor.WHITE)),
                Component.empty(),
                Component.text("▸ 點擊開啟熱門方塊").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(3, blockItem);
        
        // 上一頁 (格子 7)
        updatePageButtons();
        
        // 重新整理 (格子 52)
        ItemStack refreshItem = createItem(Material.LIME_DYE,
            Component.text("重新整理").color(NamedTextColor.GREEN),
            List.of(
                Component.text("重新載入最新資料").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("▸ 點擊重新整理").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(52, refreshItem);
        
        // 關閉 (格子 53)
        ItemStack closeItem = createItem(Material.RED_DYE,
            Component.text("關閉").color(NamedTextColor.RED),
            List.of(
                Component.text("▸ 點擊關閉介面").color(NamedTextColor.YELLOW)
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
                Component.text("上一頁").color(NamedTextColor.YELLOW),
                List.of(
                    Component.text("第 " + (filter.getPage() - 1) + " 頁").color(NamedTextColor.GRAY)
                )
            );
            inventory.setItem(7, prevItem);
        } else {
            inventory.setItem(7, createItem(Material.GRAY_DYE,
                Component.text("上一頁").color(NamedTextColor.DARK_GRAY),
                List.of(Component.text("已經是第一頁").color(NamedTextColor.GRAY))
            ));
        }
        
        // 下一頁 (格子 8)
        if (filter.getPage() < currentResponse.getTotalPages()) {
            ItemStack nextItem = createItem(Material.ARROW,
                Component.text("下一頁").color(NamedTextColor.YELLOW),
                List.of(
                    Component.text("第 " + (filter.getPage() + 1) + " 頁").color(NamedTextColor.GRAY)
                )
            );
            inventory.setItem(8, nextItem);
        } else {
            inventory.setItem(8, createItem(Material.GRAY_DYE,
                Component.text("下一頁").color(NamedTextColor.DARK_GRAY),
                List.of(Component.text("已經是最後一頁").color(NamedTextColor.GRAY))
            ));
        }
        
        // 統計資訊 (格子 45)
        int favoriteCount = feature.getFavoritesManager().getFavoriteCount(player.getUniqueId());
        ItemStack statsInfo = createItem(Material.PAPER,
            Component.text("統計資訊").color(NamedTextColor.YELLOW),
            List.of(
                Component.text("當前頁數: ").color(NamedTextColor.GRAY)
                    .append(Component.text(filter.getPage() + "/" + currentResponse.getTotalPages()).color(NamedTextColor.WHITE)),
                Component.text("總調色板數: ").color(NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(currentResponse.getTotalResults())).color(NamedTextColor.WHITE)),
                Component.text("我的收藏: ").color(NamedTextColor.GRAY)
                    .append(Component.text(favoriteCount + "/100").color(NamedTextColor.GOLD))
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
                showError("載入失敗: " + ex.getMessage());
            });
            return null;
        });
    }
    
    /**
     * 顯示調色板
     */
    private void displayPalettes(List<PaletteData> palettes) {
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
        
        for (int i = 0; i < Math.min(palettes.size(), 4); i++) {
            PaletteData palette = palettes.get(i);
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
        if (palettes.isEmpty()) {
            ItemStack noResults = createItem(Material.BARRIER,
                Component.text("沒有找到符合條件的調色板").color(NamedTextColor.GRAY),
                List.of(
                    Component.text("嘗試調整搜尋條件").color(NamedTextColor.GRAY)
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
        lore.add(Component.text("Palette #" + palette.getId()).color(NamedTextColor.GOLD));
        lore.add(Component.empty());
        lore.add(Component.text("作者: ").color(NamedTextColor.GRAY)
            .append(Component.text(palette.getAuthor()).color(NamedTextColor.WHITE)));
        lore.add(Component.text("時間: ").color(NamedTextColor.GRAY)
            .append(Component.text(palette.getUploadTime()).color(NamedTextColor.WHITE)));
        lore.add(Component.text("喜歡: ").color(NamedTextColor.GRAY)
            .append(Component.text("❤ " + palette.getLikes()).color(NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        lore.add(Component.text("▸ 點擊複製方塊清單").color(NamedTextColor.YELLOW));
        
        return createItem(Material.BOOK,
            Component.text("資訊").color(NamedTextColor.AQUA)
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
        String text = isFavorite ? "已收藏" : "收藏";
        NamedTextColor color = isFavorite ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Palette #" + palette.getId()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        
        if (isFavorite) {
            lore.add(Component.text("此調色板已在收藏中").color(NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("▸ 點擊移除收藏").color(NamedTextColor.RED));
        } else {
            lore.add(Component.text("將此調色板加入收藏").color(NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("▸ 點擊加入收藏").color(NamedTextColor.YELLOW));
        }
        
        return createItem(material,
            Component.text(text).color(color)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false),
            lore
        );
    }
    
    /**
     * 顯示載入中
     */
    private void showLoading() {
        ItemStack loading = createItem(Material.HOPPER,
            Component.text("載入中...").color(NamedTextColor.YELLOW),
            List.of(
                Component.text("正在從 Block Palettes 載入資料").color(NamedTextColor.GRAY)
            )
        );
        inventory.setItem(22, loading);
    }
    
    /**
     * 顯示錯誤
     */
    private void showError(String error) {
        ItemStack errorItem = createItem(Material.BARRIER,
            Component.text("載入失敗").color(NamedTextColor.RED),
            List.of(
                Component.text(error).color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("▸ 點擊重試").color(NamedTextColor.YELLOW)
            )
        );
        inventory.setItem(22, errorItem);
    }
    
    /**
     * 取得排序顯示名稱
     */
    private String getSortDisplayName() {
        return switch (filter.getSortBy()) {
            case "recent" -> "最新";
            case "popular" -> "最受歡迎";
            case "oldest" -> "最舊";
            case "trending" -> "趨勢";
            default -> "最新";
        };
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
        if (isBlockSlot && currentResponse != null && paletteIndex < currentResponse.getPalettes().size()) {
            PaletteData palette = currentResponse.getPalettes().get(paletteIndex);
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
        
        // 熱門方塊 (格子 3)
        if (slot == 3) {
            openPopularBlocks(clicker);
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
        if (currentResponse != null && !currentResponse.getPalettes().isEmpty()) {
            int[] infoSlots = {12, 17, 30, 35};  // 資訊按鈕
            int[] favoriteSlots = {21, 26, 39, 44};  // 收藏按鈕
            
            // 資訊按鈕 - 複製方塊清單
            for (int i = 0; i < infoSlots.length && i < currentResponse.getPalettes().size(); i++) {
                if (slot == infoSlots[i]) {
                    PaletteData palette = currentResponse.getPalettes().get(i);
                    copyBlockList(clicker, palette);
                    return;
                }
            }
            
            // 收藏按鈕 - 切換收藏狀態
            for (int i = 0; i < favoriteSlots.length && i < currentResponse.getPalettes().size(); i++) {
                if (slot == favoriteSlots[i]) {
                    PaletteData palette = currentResponse.getPalettes().get(i);
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
        return switch (colorId) {
            case "all" -> "全部";
            case "red" -> "紅色";
            case "orange" -> "橙色";
            case "yellow" -> "黃色";
            case "green" -> "綠色";
            case "blue" -> "藍色";
            case "purple" -> "紫色";
            case "black" -> "黑色";
            case "white" -> "白色";
            default -> "全部";
        };
    }
    
    /**
     * 取得方塊顯示名稱
     */
    private String getBlockDisplayName(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return "無";
        }
        return Arrays.stream(blockId.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
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
     * 開啟熱門方塊選單
     */
    private void openPopularBlocks(Player player) {
        player.closeInventory();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PopularBlocksGUI blocksGUI = new PopularBlocksGUI(plugin, feature, player, filter, () -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    setupControlItems();  // 更新控制項顯示
                    loadPalettes(true);
                    player.openInventory(inventory);
                }, 2L);
            });
            Bukkit.getPluginManager().registerEvents(blocksGUI, plugin);
            blocksGUI.open();
        }, 2L);
    }
    
    /**
     * 複製方塊清單到聊天欄
     */
    private void copyBlockList(Player player, PaletteData palette) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
        player.sendMessage(
            Component.text("Palette #" + palette.getId())
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
        );
        player.sendMessage(Component.text("作者: ").color(NamedTextColor.GRAY)
            .append(Component.text(palette.getAuthor()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("時間: ").color(NamedTextColor.GRAY)
            .append(Component.text(palette.getUploadTime()).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("喜歡: ").color(NamedTextColor.GRAY)
            .append(Component.text("❤ " + palette.getLikes()).color(NamedTextColor.YELLOW)));
        player.sendMessage(Component.empty());
        
        for (String blockName : palette.getDisplayNames()) {
            player.sendMessage(Component.text("• " + blockName).color(NamedTextColor.WHITE));
        }
        
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("✓ 已複製方塊清單").color(NamedTextColor.GREEN));
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
            player.sendMessage(Component.text("✓ 已從收藏中移除 Palette #" + palette.getId()).color(NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
        } else {
            // 加入收藏
            boolean success = feature.getFavoritesManager().addFavorite(player.getUniqueId(), palette);
            if (success) {
                player.sendMessage(Component.text("✓ 已加入收藏 Palette #" + palette.getId()).color(NamedTextColor.GREEN));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            } else {
                player.sendMessage(Component.text("✗ 收藏已滿 (最多 100 個)").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
        
        // 重新載入以更新收藏按鈕
        displayPalettes(currentResponse.getPalettes());
    }
}
