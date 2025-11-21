package dev.twme.ombre.gui;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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
 * 我的最愛 GUI
 * 顯示玩家收藏的漸層配置
 */
public class FavoritesGUI implements InventoryHolder {
    
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
    private static final int BUTTON_CLEAR_ALL = 49;       // 第 6 行第 5 列
    private static final int BUTTON_NEXT_PAGE = 52;       // 第 6 行第 8 列
    private static final int BUTTON_BACK = 53;            // 第 6 行第 9 列（右下角）
    
    private final Ombre plugin;
    private final Player player;
    private final ConfigManager configManager;
    private final GUIManager guiManager;
    private final Inventory inventory;
    
    private List<GradientConfig> favoriteConfigs;
    private int currentPage;
    private SortMode sortMode;
    
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
    
    public FavoritesGUI(Ombre plugin, Player player, GUIManager guiManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.player = player;
        this.guiManager = guiManager;
        this.configManager = configManager;
        this.favoriteConfigs = new ArrayList<>();
        this.currentPage = 0;
        this.sortMode = SortMode.TIME;
        
        this.inventory = Bukkit.createInventory(this, ROWS * COLS, 
            Component.text("我的最愛").color(NamedTextColor.GOLD));
        
        loadFavorites();
        setupGUI();
    }
    
    /**
     * 載入收藏的配置
     */
    private void loadFavorites() {
        favoriteConfigs.clear();
        
        // 獲取玩家的收藏列表
        Set<UUID> favoriteIds = configManager.getFavorites(player.getUniqueId());
        
        // 載入每個收藏的配置
        for (UUID configId : favoriteIds) {
            GradientConfig config = configManager.loadGradient(configId, player.getUniqueId());
            if (config != null) {
                favoriteConfigs.add(config);
            }
        }
        
        // 排序
        sortConfigs();
    }
    
    /**
     * 排序配置
     */
    private void sortConfigs() {
        switch (sortMode) {
            case TIME:
                favoriteConfigs.sort(Comparator.comparing(GradientConfig::getTimestamp).reversed());
                break;
            case POPULAR:
                favoriteConfigs.sort(Comparator.comparing(GradientConfig::getFavoriteCount).reversed());
                break;
            case CREATOR:
                favoriteConfigs.sort(Comparator.comparing(GradientConfig::getCreatorName));
                break;
        }
    }
    
    /**
     * 設置 GUI
     */
    private void setupGUI() {
        inventory.clear();
        
        // 導航按鈕
        setupNavigationButtons();
        
        // 顯示配置
        displayConfigs();
        
        // 控制按鈕
        setupControlButtons();
    }
    
    /**
     * 設置導航按鈕
     */
    private void setupNavigationButtons() {
        int totalPages = getTotalPages();
        
        // 頁碼顯示（第 1 行中間）
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.displayName(Component.text("§6我的最愛")
            .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("共 " + favoriteConfigs.size() + " 個收藏").color(NamedTextColor.GRAY));
        lore.add(Component.text("第 " + (currentPage + 1) + " / " + Math.max(1, totalPages) + " 頁").color(NamedTextColor.GRAY));
        pageMeta.lore(lore);
        pageInfo.setItemMeta(pageMeta);
        inventory.setItem(BUTTON_PAGE_INFO, pageInfo);
        
        // 上一頁按鈕（第 6 行）
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            meta.displayName(Component.text("§a上一頁")
                .decoration(TextDecoration.ITALIC, false));
            List<Component> prevLore = new ArrayList<>();
            prevLore.add(Component.text("§7第 " + currentPage + " 頁"));
            meta.lore(prevLore);
            prevPage.setItemMeta(meta);
            inventory.setItem(BUTTON_PREV_PAGE, prevPage);
        }
        
        // 下一頁按鈕（第 6 行）
        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            meta.displayName(Component.text("§a下一頁")
                .decoration(TextDecoration.ITALIC, false));
            List<Component> nextLore = new ArrayList<>();
            nextLore.add(Component.text("§7第 " + (currentPage + 2) + " 頁"));
            meta.lore(nextLore);
            nextPage.setItemMeta(meta);
            inventory.setItem(BUTTON_NEXT_PAGE, nextPage);
        }
    }
    
    /**
     * 顯示配置
     */
    private void displayConfigs() {
        int startIndex = currentPage * CONFIGS_PER_PAGE;
        int endIndex = Math.min(startIndex + CONFIGS_PER_PAGE, favoriteConfigs.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GradientConfig config = favoriteConfigs.get(i);
            int displayIndex = i - startIndex;
            int slot = getConfigSlot(displayIndex);
            
            if (slot >= 0) {
                displayConfig(config, slot);
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
    private void displayConfig(GradientConfig config, int slot) {
        // 獲取配置的第一個方塊作為圖示
        Material displayMaterial = getDisplayMaterial(config);
        
        // 創建顯示物品
        ItemStack display = new ItemStack(displayMaterial);
        ItemMeta meta = display.getItemMeta();
        
        // 設置名稱
        String displayName = config.getName() != null ? config.getName() : 
            config.getCreatorName() + "#" + config.getConfigNumber();
        meta.displayName(Component.text(displayName)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false)
            .decorate(TextDecoration.BOLD));
        
        // 設置 Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("建立者: " + config.getCreatorName()).color(NamedTextColor.GRAY));
        
        // 格式化時間
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(config.getTimestamp()), 
            ZoneId.systemDefault()
        );
        String formattedTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        lore.add(Component.text("時間: " + formattedTime).color(NamedTextColor.GRAY));
        
        // 收藏狀態（我的最愛中一定是已收藏）
        lore.add(Component.text("⭐ 已收藏 (" + config.getFavoriteCount() + " 人收藏)")
            .color(NamedTextColor.YELLOW));
        
        lore.add(Component.text("載入次數: " + config.getLoadCount()).color(NamedTextColor.GREEN));
        lore.add(Component.empty());
        lore.add(Component.text("左鍵載入配置").color(NamedTextColor.DARK_GRAY));
        lore.add(Component.text("右鍵取消收藏").color(NamedTextColor.DARK_GRAY));
        
        meta.lore(lore);
        display.setItemMeta(meta);
        
        // 放置到指定 slot
        inventory.setItem(slot, display);
    }
    
    /**
     * 獲取配置的顯示材質
     */
    private Material getDisplayMaterial(GradientConfig config) {
        var blocks = config.getBlockConfiguration();
        if (blocks.isEmpty()) {
            return Material.BARRIER;
        }
        
        // 獲取第一個方塊
        String firstBlock = blocks.values().iterator().next();
        try {
            return Bukkit.createBlockData(firstBlock).getMaterial();
        } catch (Exception e) {
            return Material.STONE;
        }
    }
    
    /**
     * 設置控制按鈕
     */
    private void setupControlButtons() {
        // 返回按鈕
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("返回").color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        inventory.setItem(BUTTON_BACK, back);
        
        // 排序按鈕 - 時間
        ItemStack sortTime = new ItemStack(Material.CLOCK);
        ItemMeta sortTimeMeta = sortTime.getItemMeta();
        sortTimeMeta.displayName(Component.text("按時間排序")
            .color(sortMode == SortMode.TIME ? NamedTextColor.GREEN : NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        sortTime.setItemMeta(sortTimeMeta);
        inventory.setItem(BUTTON_SORT_TIME, sortTime);
        
        // 排序按鈕 - 熱度
        ItemStack sortPopular = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta sortPopularMeta = sortPopular.getItemMeta();
        sortPopularMeta.displayName(Component.text("按熱度排序")
            .color(sortMode == SortMode.POPULAR ? NamedTextColor.GREEN : NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        sortPopular.setItemMeta(sortPopularMeta);
        inventory.setItem(BUTTON_SORT_POPULAR, sortPopular);
        
        // 排序按鈕 - 建立者
        ItemStack sortCreator = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta sortCreatorMeta = sortCreator.getItemMeta();
        sortCreatorMeta.displayName(Component.text("按建立者排序")
            .color(sortMode == SortMode.CREATOR ? NamedTextColor.GREEN : NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        sortCreator.setItemMeta(sortCreatorMeta);
        inventory.setItem(BUTTON_SORT_CREATOR, sortCreator);
        
        // 清空收藏按鈕
        ItemStack clearAll = new ItemStack(Material.TNT);
        ItemMeta clearAllMeta = clearAll.getItemMeta();
        clearAllMeta.displayName(Component.text("清空所有收藏").color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));
        List<Component> clearLore = new ArrayList<>();
        clearLore.add(Component.text("注意：此操作不可撤銷！").color(NamedTextColor.DARK_RED));
        clearLore.add(Component.text("再次點擊確認清空").color(NamedTextColor.GRAY));
        clearAllMeta.lore(clearLore);
        clearAll.setItemMeta(clearAllMeta);
        inventory.setItem(BUTTON_CLEAR_ALL, clearAll);
    }
    
    /**
     * 獲取總頁數
     */
    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) favoriteConfigs.size() / CONFIGS_PER_PAGE));
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
            sortConfigs();
            currentPage = 0;
            setupGUI();
            return;
        }
        
        if (slot == BUTTON_SORT_POPULAR) {
            sortMode = SortMode.POPULAR;
            sortConfigs();
            currentPage = 0;
            setupGUI();
            return;
        }
        
        if (slot == BUTTON_SORT_CREATOR) {
            sortMode = SortMode.CREATOR;
            sortConfigs();
            currentPage = 0;
            setupGUI();
            return;
        }
        
        if (slot == BUTTON_CLEAR_ALL) {
            handleClearAll();
            return;
        }
        
        // 配置點擊
        handleConfigClick(slot, event.getClick());
    }
    
    /**
     * 處理配置點擊
     */
    private void handleConfigClick(int slot, ClickType clickType) {
        // 找到被點擊的配置
        GradientConfig config = getConfigAtSlot(slot);
        
        if (config != null) {
            if (clickType == ClickType.LEFT) {
                // 左鍵 - 載入配置
                loadConfiguration(config);
            } else if (clickType == ClickType.RIGHT) {
                // 右鍵 - 取消收藏
                removeFavorite(config);
            }
        }
    }
    
    /**
     * 獲取指定 slot 的配置
     */
    private GradientConfig getConfigAtSlot(int slot) {
        int startIndex = currentPage * CONFIGS_PER_PAGE;
        
        for (int i = 0; i < CONFIGS_PER_PAGE; i++) {
            int configIndex = startIndex + i;
            if (configIndex >= favoriteConfigs.size()) {
                break;
            }
            
            int configSlot = getConfigSlot(i);
            if (configSlot == slot) {
                return favoriteConfigs.get(configIndex);
            }
        }
        
        return null;
    }
    
    /**
     * 載入配置
     */
    private void loadConfiguration(GradientConfig config) {
        player.closeInventory();
        
        // 打開漸層製作 GUI 並載入配置（帶返回按鈕）
        guiManager.openOmbreGUI(player, config, "favorites");
        
        // 增加載入次數
        configManager.incrementLoadCount(config.getId());
        
        player.sendMessage(Component.text("已載入配置: " + 
            (config.getName() != null ? config.getName() : config.getCreatorName() + "#" + config.getConfigNumber()))
            .color(NamedTextColor.GREEN));
    }
    
    /**
     * 取消收藏
     */
    private void removeFavorite(GradientConfig config) {
        configManager.removeFavorite(player.getUniqueId(), config.getId());
        configManager.decrementFavoriteCount(config.getId());
        
        player.sendMessage(Component.text("已從收藏中移除")
            .color(NamedTextColor.YELLOW));
        
        // 重新載入並刷新 GUI
        loadFavorites();
        
        // 如果當前頁沒有配置了，返回上一頁
        if (favoriteConfigs.isEmpty()) {
            currentPage = 0;
        } else if (currentPage >= getTotalPages()) {
            currentPage = getTotalPages() - 1;
        }
        
        setupGUI();
    }
    
    /**
     * 處理清空所有收藏
     */
    private void handleClearAll() {
        if (favoriteConfigs.isEmpty()) {
            player.sendMessage(Component.text("收藏列表已經是空的").color(NamedTextColor.YELLOW));
            return;
        }
        
        // 需要二次確認
        player.sendMessage(Component.text("確定要清空所有收藏嗎？").color(NamedTextColor.RED));
        player.sendMessage(Component.text("這將移除 " + favoriteConfigs.size() + " 個收藏，此操作不可撤銷！")
            .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("使用 /ombre favorites clear confirm 確認清空")
            .color(NamedTextColor.GRAY));
    }
    
    /**
     * 確認清空所有收藏
     */
    public void confirmClearAll() {
        int count = favoriteConfigs.size();
        
        // 移除所有收藏
        for (GradientConfig config : favoriteConfigs) {
            configManager.removeFavorite(player.getUniqueId(), config.getId());
            configManager.decrementFavoriteCount(config.getId());
        }
        
        player.sendMessage(Component.text("已清空 " + count + " 個收藏").color(NamedTextColor.GREEN));
        
        // 重新載入
        loadFavorites();
        currentPage = 0;
        setupGUI();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
