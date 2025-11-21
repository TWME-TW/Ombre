package dev.twme.ombre.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

/**
 * 我的漸層 GUI
 * 顯示玩家自己儲存的所有漸層配置
 */
public class MyGradientsGUI implements InventoryHolder {
    
    private static final int ROWS = 6;
    private static final int COLS = 9;
    
    // 配置顯示區域（第 2-5 行，9×4 網格）
    private static final int CONFIG_START_ROW = 1; // 從第二橫排（索引 1）開始
    private static final int CONFIG_ROWS = 4;
    private static final int CONFIG_COLS = 9;
    private static final int CONFIGS_PER_PAGE = CONFIG_ROWS * CONFIG_COLS; // 36
    
    // 按鈕位置（第 6 行）
    private static final int BUTTON_PREVIOUS_PAGE = 45; // 第 6 行第 1 列
    private static final int BUTTON_SORT = 49;          // 第 6 行第 5 列
    private static final int BUTTON_NEXT_PAGE = 52;     // 第 6 行第 8 列
    private static final int BUTTON_BACK = 53;          // 第 6 行第 9 列（右下角）
    
    private final Ombre plugin;
    private final Player player;
    private final GUIManager guiManager;
    private final ConfigManager configManager;
    private final Inventory inventory;
    
    private List<GradientConfig> myConfigs;
    private int currentPage;
    private SortMode sortMode;
    
    /**
     * 排序模式
     */
    private enum SortMode {
        TIME("按時間", "最新的在前面"),
        NAME("按名稱", "按字母順序排序"),
        POPULARITY("按人氣", "最多收藏在前面");
        
        private final String displayName;
        private final String description;
        
        SortMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public MyGradientsGUI(Ombre plugin, Player player, GUIManager guiManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.player = player;
        this.guiManager = guiManager;
        this.configManager = configManager;
        this.currentPage = 0;
        this.sortMode = SortMode.TIME;
        this.myConfigs = new ArrayList<>();
        
        this.inventory = Bukkit.createInventory(this, ROWS * COLS,
            Component.text("我的漸層").color(NamedTextColor.GOLD));
        
        loadMyConfigs();
        setupGUI();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * 載入玩家的配置
     */
    private void loadMyConfigs() {
        myConfigs = loadPlayerGradients();
        sortConfigs();
    }
    
    /**
     * 載入玩家配置
     */
    private List<GradientConfig> loadPlayerGradients() {
        List<GradientConfig> configs = new ArrayList<>();
        
        // 獲取玩家資料夾
        java.io.File playersFolder = new java.io.File(plugin.getDataFolder(), "players");
        java.io.File playerFolder = new java.io.File(playersFolder, player.getUniqueId().toString());
        
        if (!playerFolder.exists() || !playerFolder.isDirectory()) {
            return configs;
        }
        
        // 掃描所有 .yml 檔案
        java.io.File[] files = playerFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return configs;
        }
        
        // 載入每個配置
        for (java.io.File file : files) {
            try {
                String fileName = file.getName();
                String configIdStr = fileName.substring(0, fileName.length() - 4); // 移除 .yml
                UUID configId = UUID.fromString(configIdStr);
                
                GradientConfig config = configManager.loadGradient(configId, player.getUniqueId());
                if (config != null) {
                    configs.add(config);
                }
            } catch (Exception e) {
                // 忽略無法載入的配置
                plugin.getLogger().warning("無法載入配置檔案: " + file.getName());
            }
        }
        
        return configs;
    }
    
    /**
     * 排序配置
     */
    private void sortConfigs() {
        switch (sortMode) {
            case TIME:
                myConfigs.sort(Comparator.comparingLong(GradientConfig::getTimestamp).reversed());
                break;
            case NAME:
                myConfigs.sort(Comparator.comparing(config -> {
                    String name = config.getName();
                    return name != null ? name.toLowerCase() : 
                        config.getCreatorName() + "#" + config.getConfigNumber();
                }));
                break;
            case POPULARITY:
                myConfigs.sort(Comparator.comparingInt(GradientConfig::getFavoriteCount).reversed());
                break;
        }
    }
    
    /**
     * 設置 GUI
     */
    private void setupGUI() {
        inventory.clear();
        
        displayConfigs();
        setupNavigationButtons();
    }
    
    /**
     * 顯示配置
     */
    private void displayConfigs() {
        int startIndex = currentPage * CONFIGS_PER_PAGE;
        int endIndex = Math.min(startIndex + CONFIGS_PER_PAGE, myConfigs.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GradientConfig config = myConfigs.get(i);
            int displayIndex = i - startIndex;
            int slot = getConfigSlot(displayIndex);
            
            if (slot >= 0) {
                inventory.setItem(slot, createConfigItem(config));
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
     * 創建配置物品
     */
    private ItemStack createConfigItem(GradientConfig config) {
        // 使用配置中的第一個方塊作為圖標
        Material material = Material.PAPER;
        if (!config.getBlockConfiguration().isEmpty()) {
            try {
                String firstBlock = config.getBlockConfiguration().values().iterator().next();
                material = Bukkit.createBlockData(firstBlock).getMaterial();
            } catch (Exception e) {
                // 使用預設材質
            }
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 顯示名稱
            String displayName = config.getName() != null ? config.getName() : 
                "配置 #" + config.getConfigNumber();
            meta.displayName(Component.text(displayName).color(NamedTextColor.AQUA));
            
            // 說明文字
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("創建者: " + config.getCreatorName()).color(NamedTextColor.GRAY));
            lore.add(Component.text("方塊數: " + config.getBlockConfiguration().size()).color(NamedTextColor.GRAY));
            lore.add(Component.text("收藏數: " + config.getFavoriteCount()).color(NamedTextColor.YELLOW));
            
            if (config.isPublished()) {
                lore.add(Component.text("已發布").color(NamedTextColor.GREEN));
                lore.add(Component.text("載入次數: " + config.getLoadCount()).color(NamedTextColor.GRAY));
            } else {
                lore.add(Component.text("未發布").color(NamedTextColor.RED));
            }
            
            lore.add(Component.text(""));
            lore.add(Component.text("§e左鍵 §7載入配置"));
            lore.add(Component.text("§eShift+左鍵 §7編輯配置"));
            lore.add(Component.text("§e右鍵 §7" + (config.isPublished() ? "取消發布" : "發布到共享庫")));
            lore.add(Component.text("§eShift+右鍵 §7刪除配置"));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 設置導航按鈕
     */
    private void setupNavigationButtons() {
        // 上一頁按鈕
        if (currentPage > 0) {
            inventory.setItem(BUTTON_PREVIOUS_PAGE, createItem(Material.ARROW, 
                "§a上一頁", "§7第 " + currentPage + " 頁"));
        }
        
        // 排序按鈕
        inventory.setItem(BUTTON_SORT, createItem(Material.COMPARATOR, 
            "§b排序: " + sortMode.getDisplayName(), 
            "§7" + sortMode.getDescription(),
            "§e點擊切換排序方式"));
        
        // 下一頁按鈕
        int totalPages = (int) Math.ceil((double) myConfigs.size() / CONFIGS_PER_PAGE);
        if (currentPage < totalPages - 1) {
            inventory.setItem(BUTTON_NEXT_PAGE, createItem(Material.ARROW, 
                "§a下一頁", "§7第 " + (currentPage + 2) + " 頁"));
        }
        
        // 頁碼資訊
        inventory.setItem(4, createItem(Material.BOOK, 
            "§6我的漸層配置",
            "§7共 " + myConfigs.size() + " 個配置",
            "§7第 " + (currentPage + 1) + " / " + Math.max(1, totalPages) + " 頁"));
        
        // 返回按鈕（右下角）
        inventory.setItem(BUTTON_BACK, createItem(Material.BARRIER, 
            "§c返回", "§7關閉此界面"));
    }
    
    /**
     * 創建物品
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            if (lore.length > 0) {
                meta.lore(Arrays.stream(lore)
                    .map(Component::text)
                    .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 處理點擊事件
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        // 點擊在 GUI 外
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        
        // 導航按鈕
        if (slot == BUTTON_PREVIOUS_PAGE) {
            if (currentPage > 0) {
                currentPage--;
                setupGUI();
            }
            return;
        }
        
        if (slot == BUTTON_NEXT_PAGE) {
            int totalPages = (int) Math.ceil((double) myConfigs.size() / CONFIGS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                setupGUI();
            }
            return;
        }
        
        if (slot == BUTTON_SORT) {
            cycleSortMode();
            return;
        }
        
        if (slot == BUTTON_BACK) {
            player.closeInventory();
            return;
        }
        
        // 配置點擊
        GradientConfig clickedConfig = getConfigAtSlot(slot);
        if (clickedConfig != null) {
            handleConfigClick(event, clickedConfig);
        }
    }
    
    /**
     * 獲取指定 slot 的配置
     */
    private GradientConfig getConfigAtSlot(int slot) {
        int startIndex = currentPage * CONFIGS_PER_PAGE;
        
        for (int i = 0; i < CONFIGS_PER_PAGE; i++) {
            int configIndex = startIndex + i;
            if (configIndex >= myConfigs.size()) {
                break;
            }
            
            int configSlot = getConfigSlot(i);
            if (configSlot == slot) {
                return myConfigs.get(configIndex);
            }
        }
        
        return null;
    }
    
    /**
     * 處理配置點擊
     */
    private void handleConfigClick(InventoryClickEvent event, GradientConfig config) {
        // 左鍵: 載入配置
        if (event.isLeftClick() && !event.isShiftClick()) {
            loadConfiguration(config);
        }
        // Shift+左鍵: 編輯配置
        else if (event.isLeftClick() && event.isShiftClick()) {
            editConfiguration(config);
        }
        // 右鍵: 切換發布狀態
        else if (event.isRightClick() && !event.isShiftClick()) {
            togglePublish(config);
        }
        // Shift+右鍵: 刪除配置
        else if (event.isRightClick() && event.isShiftClick()) {
            deleteConfiguration(config);
        }
    }
    
    /**
     * 載入配置
     */
    private void loadConfiguration(GradientConfig config) {
        player.closeInventory();
        
        // 打開漸層製作 GUI 並載入配置（帶返回按鈕）
        guiManager.openOmbreGUI(player, config, "my");
        
        player.sendMessage(Component.text("已載入配置: " + 
            (config.getName() != null ? config.getName() : "配置 #" + config.getConfigNumber()))
            .color(NamedTextColor.GREEN));
    }
    
    /**
     * 編輯配置
     */
    private void editConfiguration(GradientConfig config) {
        player.closeInventory();
        
        // 打開漸層製作 GUI 並載入配置（帶返回按鈕）
        guiManager.openOmbreGUI(player, config, "my");
        
        player.sendMessage(Component.text("進入編輯模式")
            .color(NamedTextColor.YELLOW));
    }
    
    /**
     * 切換發布狀態
     */
    private void togglePublish(GradientConfig config) {
        if (config.isPublished()) {
            // 取消發布
            config.setPublished(false);
            configManager.saveGradient(config);
            player.sendMessage(Component.text("已取消發布")
                .color(NamedTextColor.YELLOW));
        } else {
            // 發布到共享庫
            config.setPublished(true);
            configManager.saveGradient(config);
            player.sendMessage(Component.text("已發布到共享庫")
                .color(NamedTextColor.GREEN));
        }
        
        // 重新載入並更新顯示
        loadMyConfigs();
        setupGUI();
    }
    
    /**
     * 刪除配置
     */
    private void deleteConfiguration(GradientConfig config) {
        if (configManager.deleteGradient(config.getId(), player)) {
            player.sendMessage(Component.text("配置已刪除")
                .color(NamedTextColor.RED));
            
            // 重新載入配置
            loadMyConfigs();
            
            // 如果當前頁已無配置且不是第一頁，返回上一頁
            if (myConfigs.size() <= currentPage * CONFIGS_PER_PAGE && currentPage > 0) {
                currentPage--;
            }
            
            setupGUI();
        } else {
            player.sendMessage(Component.text("刪除失敗")
                .color(NamedTextColor.RED));
        }
    }
    
    /**
     * 循環切換排序模式
     */
    private void cycleSortMode() {
        SortMode[] modes = SortMode.values();
        int currentIndex = sortMode.ordinal();
        sortMode = modes[(currentIndex + 1) % modes.length];
        
        loadMyConfigs();
        currentPage = 0; // 重置到第一頁
        setupGUI();
        
        player.sendMessage(Component.text("排序方式: " + sortMode.getDisplayName())
            .color(NamedTextColor.YELLOW));
    }
}
