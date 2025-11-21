package dev.twme.ombre.gui;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.data.GradientConfig;
import dev.twme.ombre.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * 共享庫 GUI
 * 顯示所有發布的漸層配置
 */
public class LibraryGUI implements InventoryHolder {
    
    private static final int ROWS = 6;
    private static final int COLS = 9;
    
    // 配置顯示區域（第 2-5 行，9×4 網格）
    private static final int CONFIG_START_ROW = 1; // 從第二橫排（索引 1）開始
    private static final int CONFIG_ROWS = 4;
    private static final int CONFIG_COLS = 9;
    private static final int CONFIGS_PER_PAGE = CONFIG_ROWS * CONFIG_COLS; // 36
    
    // 按鈕位置（第 1 行和第 6 行）
    private static final int BUTTON_PAGE_INFO = 4;
    private static final int BUTTON_PREV_PAGE = 45;       // 第 6 行第 1 列
    private static final int BUTTON_SORT_TIME = 46;       // 第 6 行第 2 列
    private static final int BUTTON_SORT_POPULAR = 47;    // 第 6 行第 3 列
    private static final int BUTTON_SORT_CREATOR = 48;    // 第 6 行第 4 列
    private static final int BUTTON_FILTER = 49;          // 第 6 行第 5 列
    private static final int BUTTON_NEXT_PAGE = 52;       // 第 6 行第 8 列
    private static final int BUTTON_BACK = 53;            // 第 6 行第 9 列（右下角）
    
    private final Ombre plugin;
    private final Player player;
    private final ConfigManager configManager;
    private final GUIManager guiManager;
    private final Inventory inventory;
    
    private List<GradientConfig> allConfigs;
    private List<GradientConfig> displayConfigs;
    private int currentPage;
    private SortMode sortMode;
    private FilterMode filterMode;
    private Set<UUID> favorites;
    
    public enum SortMode {
        TIME("時間"),
        POPULAR("熱度"),
        CREATOR("建立者");
        
        private final String displayName;
        
        SortMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum FilterMode {
        ALL("全部"),
        FAVORITES("我的最愛"),
        MY_CREATIONS("我的創作"),
        WEEKLY_HOT("本週熱門");
        
        private final String displayName;
        
        FilterMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public LibraryGUI(Ombre plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.configManager = plugin.getConfigManager();
        this.guiManager = plugin.getGuiManager();
        this.inventory = Bukkit.createInventory(this, ROWS * COLS,
            Component.text("共享庫").color(NamedTextColor.GOLD));
        this.currentPage = 0;
        this.sortMode = SortMode.TIME;
        this.filterMode = FilterMode.ALL;
        this.favorites = configManager.getFavorites(player.getUniqueId());
        
        loadConfigs();
        setupGUI();
    }
    
    /**
     * 載入配置列表
     */
    private void loadConfigs() {
        allConfigs = configManager.getSharedConfigs();
        applyFilterAndSort();
    }
    
    /**
     * 應用篩選和排序
     */
    private void applyFilterAndSort() {
        // 篩選
        displayConfigs = new ArrayList<>(allConfigs);
        
        switch (filterMode) {
            case FAVORITES:
                displayConfigs = displayConfigs.stream()
                    .filter(config -> favorites.contains(config.getId()))
                    .collect(Collectors.toList());
                break;
            case MY_CREATIONS:
                displayConfigs = displayConfigs.stream()
                    .filter(config -> config.getCreatorUuid().equals(player.getUniqueId()))
                    .collect(Collectors.toList());
                break;
            case WEEKLY_HOT:
                long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
                displayConfigs = displayConfigs.stream()
                    .filter(config -> config.getTimestamp() >= weekAgo)
                    .sorted(Comparator.comparingInt(GradientConfig::getFavoriteCount).reversed())
                    .limit(30)
                    .collect(Collectors.toList());
                return; // 本週熱門已排序，直接返回
            case ALL:
            default:
                break;
        }
        
        // 排序
        switch (sortMode) {
            case TIME:
                displayConfigs.sort(Comparator.comparingLong(GradientConfig::getTimestamp).reversed());
                break;
            case POPULAR:
                displayConfigs.sort(Comparator.comparingInt(GradientConfig::getFavoriteCount).reversed());
                break;
            case CREATOR:
                displayConfigs.sort(Comparator.comparing(GradientConfig::getCreatorName));
                break;
        }
    }
    
    /**
     * 設置 GUI
     */
    private void setupGUI() {
        inventory.clear();
        
        // 設置導航按鈕
        setupNavigationButtons();
        
        // 設置底部控制按鈕
        setupControlButtons();
        
        // 顯示配置
        displayCurrentPage();
    }
    
    /**
     * 設置導航按鈕
     */
    private void setupNavigationButtons() {
        int totalPages = getTotalPages();
        
        // 頁碼顯示（第 1 行中間）
        inventory.setItem(BUTTON_PAGE_INFO, createItem(Material.BOOK,
            "§6共享庫",
            Arrays.asList(
                "§7共 " + displayConfigs.size() + " 個配置",
                "§7第 " + (currentPage + 1) + " / " + Math.max(1, totalPages) + " 頁"
            )));
        
        // 上一頁按鈕（第 6 行）
        if (currentPage > 0) {
            inventory.setItem(BUTTON_PREV_PAGE, createItem(Material.ARROW,
                "§a上一頁", Arrays.asList("§7第 " + currentPage + " 頁")));
        }
        
        // 下一頁按鈕（第 6 行）
        if (currentPage < totalPages - 1) {
            inventory.setItem(BUTTON_NEXT_PAGE, createItem(Material.ARROW,
                "§a下一頁", Arrays.asList("§7第 " + (currentPage + 2) + " 頁")));
        }
    }
    
    /**
     * 設置控制按鈕
     */
    private void setupControlButtons() {
        // 返回按鈕
        inventory.setItem(BUTTON_BACK, createItem(Material.BARRIER,
            "關閉", Collections.emptyList()));
        
        // 排序按鈕
        inventory.setItem(BUTTON_SORT_TIME, createItem(Material.CLOCK,
            "按時間排序", Arrays.asList(
                sortMode == SortMode.TIME ? "§a✓ 當前排序" : "§7點擊切換"
            )));
        
        inventory.setItem(BUTTON_SORT_POPULAR, createItem(Material.BLAZE_POWDER,
            "按熱度排序", Arrays.asList(
                sortMode == SortMode.POPULAR ? "§a✓ 當前排序" : "§7點擊切換"
            )));
        
        inventory.setItem(BUTTON_SORT_CREATOR, createItem(Material.PLAYER_HEAD,
            "按建立者排序", Arrays.asList(
                sortMode == SortMode.CREATOR ? "§a✓ 當前排序" : "§7點擊切換"
            )));
        
        // 篩選按鈕
        List<String> filterLore = new ArrayList<>();
        filterLore.add("§e當前: §f" + filterMode.getDisplayName());
        filterLore.add("");
        filterLore.add("§7點擊切換篩選模式:");
        filterLore.add("§8• 全部配置");
        filterLore.add("§8• 我的最愛");
        filterLore.add("§8• 我的創作");
        filterLore.add("§8• 本週熱門");
        
        inventory.setItem(BUTTON_FILTER, createItem(Material.HOPPER,
            "篩選", filterLore));
    }
    
    /**
     * 顯示當前頁配置
     */
    private void displayCurrentPage() {
        int startIndex = currentPage * CONFIGS_PER_PAGE;
        int endIndex = Math.min(startIndex + CONFIGS_PER_PAGE, displayConfigs.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GradientConfig config = displayConfigs.get(i);
            int displayIndex = i - startIndex;
            int slot = getConfigSlot(displayIndex);
            
            if (slot >= 0) {
                displayConfig(slot, config);
            }
        }
    }
    
    /**
     * 根據顯示索引計算 slot 位置
     * 9×4 網格，從第二橫排開始，從左到右、從上到下
     */
    private int getConfigSlot(int displayIndex) {
        int row = displayIndex / CONFIG_COLS;
        int col = displayIndex % CONFIG_COLS;
        
        if (row >= CONFIG_ROWS || col >= CONFIG_COLS) {
            return -1;
        }
        
        // 從第二橫排（索引 1）開始
        return (CONFIG_START_ROW + row) * COLS + col;
    }
    
    /**
     * 顯示單個配置
     */
    private void displayConfig(int slot, GradientConfig config) {
        // 獲取配置中最常見的方塊
        Material displayMaterial = getMostCommonBlock(config);
        
        // 創建顯示物品
        ItemStack item = new ItemStack(displayMaterial);
        ItemMeta meta = item.getItemMeta();
        
        // 設置名稱
        String displayName = config.getName() != null ? config.getName() :
            config.getCreatorName() + "#" + config.getConfigNumber();
        meta.displayName(Component.text(displayName)
            .color(NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false));
        
        // 設置 Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("建立者: " + config.getCreatorName())
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        // 格式化時間
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(config.getTimestamp()),
            ZoneId.systemDefault()
        );
        String formattedTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        lore.add(Component.text("建立時間: " + formattedTime)
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        // 收藏狀態
        boolean isFavorite = favorites.contains(config.getId());
        String favoriteIcon = isFavorite ? "⭐" : "☆";
        lore.add(Component.text(favoriteIcon + " 收藏數: " + config.getFavoriteCount())
            .color(isFavorite ? NamedTextColor.YELLOW : NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        lore.add(Component.text("載入次數: " + config.getLoadCount())
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        
        lore.add(Component.empty());
        lore.add(Component.text("左鍵: 載入配置")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("右鍵: " + (isFavorite ? "取消收藏" : "加入收藏"))
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        // 放置到指定 slot
        inventory.setItem(slot, item);
    }
    
    /**
     * 獲取配置中最常見的方塊
     */
    private Material getMostCommonBlock(GradientConfig config) {
        Map<String, Integer> blockCounts = new HashMap<>();
        
        for (String blockDataString : config.getBlockConfiguration().values()) {
            blockCounts.merge(blockDataString, 1, Integer::sum);
        }
        
        if (blockCounts.isEmpty()) {
            return Material.GRASS_BLOCK;
        }
        
        String mostCommon = blockCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("minecraft:grass_block");
        
        try {
            return Bukkit.createBlockData(mostCommon).getMaterial();
        } catch (Exception e) {
            return Material.GRASS_BLOCK;
        }
    }
    
    /**
     * 創建物品
     */
    private ItemStack createItem(Material material, String name, List<String> loreStrings) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(name)
            .decoration(TextDecoration.ITALIC, false));
        
        if (!loreStrings.isEmpty()) {
            List<Component> lore = loreStrings.stream()
                .map(s -> Component.text(s)
                    .decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
            meta.lore(lore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 獲取總頁數
     */
    private int getTotalPages() {
        return (int) Math.ceil((double) displayConfigs.size() / CONFIGS_PER_PAGE);
    }
    
    /**
     * 處理點擊事件
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        // 無效的 slot 或點擊在 GUI 外
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        
        // 導航按鈕
        if (slot == BUTTON_PREV_PAGE) {
            if (currentPage > 0) {
                currentPage--;
                setupGUI();
            }
            return;
        }
        
        if (slot == BUTTON_NEXT_PAGE) {
            if (currentPage < getTotalPages() - 1) {
                currentPage++;
                setupGUI();
            }
            return;
        }
        
        // 控制按鈕
        if (slot == BUTTON_BACK) {
            player.closeInventory();
            return;
        }
        
        if (slot == BUTTON_SORT_TIME) {
            sortMode = SortMode.TIME;
            currentPage = 0;
            applyFilterAndSort();
            setupGUI();
            return;
        }
        
        if (slot == BUTTON_SORT_POPULAR) {
            sortMode = SortMode.POPULAR;
            currentPage = 0;
            applyFilterAndSort();
            setupGUI();
            return;
        }
        
        if (slot == BUTTON_SORT_CREATOR) {
            sortMode = SortMode.CREATOR;
            currentPage = 0;
            applyFilterAndSort();
            setupGUI();
            return;
        }
        
        if (slot == BUTTON_FILTER) {
            cycleFilterMode();
            currentPage = 0;
            applyFilterAndSort();
            setupGUI();
            return;
        }
        
        // 配置點擊
        handleConfigClick(event, slot);
    }
    
    /**
     * 處理配置點擊
     */
    private void handleConfigClick(InventoryClickEvent event, int slot) {
        // 找到點擊的配置
        GradientConfig clickedConfig = getConfigAtSlot(slot);
        
        if (clickedConfig == null) return;
        
        // 左鍵: 載入配置
        if (event.isLeftClick()) {
            loadConfiguration(clickedConfig);
        }
        // 右鍵: 切換收藏
        else if (event.isRightClick()) {
            toggleFavorite(clickedConfig);
        }
    }
    
    /**
     * 獲取指定 slot 的配置
     */
    private GradientConfig getConfigAtSlot(int slot) {
        int startIndex = currentPage * CONFIGS_PER_PAGE;
        
        for (int i = 0; i < CONFIGS_PER_PAGE; i++) {
            int configIndex = startIndex + i;
            if (configIndex >= displayConfigs.size()) {
                break;
            }
            
            int configSlot = getConfigSlot(i);
            if (configSlot == slot) {
                return displayConfigs.get(configIndex);
            }
        }
        
        return null;
    }
    
    /**
     * 載入配置
     */
    private void loadConfiguration(GradientConfig config) {
        player.closeInventory();
        
        // 增加載入次數
        configManager.incrementLoadCount(config.getId());
        
        // 打開漸層製作 GUI 並載入配置（帶返回按鈕）
        guiManager.openOmbreGUI(player, config, "library");
        
        player.sendMessage(Component.text("已載入配置: " + 
            (config.getName() != null ? config.getName() : 
                config.getCreatorName() + "#" + config.getConfigNumber()))
            .color(NamedTextColor.GREEN));
    }
    
    /**
     * 切換收藏狀態
     */
    private void toggleFavorite(GradientConfig config) {
        if (favorites.contains(config.getId())) {
            favorites.remove(config.getId());
            configManager.removeFavorite(player.getUniqueId(), config.getId());
            configManager.decrementFavoriteCount(config.getId());
            player.sendMessage(Component.text("已取消收藏")
                .color(NamedTextColor.YELLOW));
        } else {
            favorites.add(config.getId());
            configManager.addFavorite(player.getUniqueId(), config.getId());
            configManager.incrementFavoriteCount(config.getId());
            player.sendMessage(Component.text("已加入收藏")
                .color(NamedTextColor.GREEN));
        }
        
        // 重新載入配置以更新收藏數
        loadConfigs();
        setupGUI();
    }
    
    /**
     * 切換篩選模式
     */
    private void cycleFilterMode() {
        FilterMode[] modes = FilterMode.values();
        int currentIndex = filterMode.ordinal();
        filterMode = modes[(currentIndex + 1) % modes.length];
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public void open() {
        player.openInventory(inventory);
    }
}
