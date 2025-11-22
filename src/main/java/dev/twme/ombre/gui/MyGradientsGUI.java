package dev.twme.ombre.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import dev.twme.ombre.i18n.MessageManager;
import dev.twme.ombre.manager.ConfigManager;
import net.kyori.adventure.text.Component;

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
    private final MessageManager messageManager;
    private final Inventory inventory;
    
    private List<GradientConfig> myConfigs;
    private int currentPage;
    private SortMode sortMode;
    
    /**
     * 排序模式
     */
    private enum SortMode {
        TIME("gui.my-gradients.sort.time", "gui.my-gradients.sort.time-desc"),
        NAME("gui.my-gradients.sort.name", "gui.my-gradients.sort.name-desc"),
        POPULARITY("gui.my-gradients.sort.popularity", "gui.my-gradients.sort.popularity-desc");
        
        private final String displayNameKey;
        private final String descriptionKey;
        
        SortMode(String displayNameKey, String descriptionKey) {
            this.displayNameKey = displayNameKey;
            this.descriptionKey = descriptionKey;
        }
        
        public String getDisplayNameKey() {
            return displayNameKey;
        }
        
        public String getDescriptionKey() {
            return descriptionKey;
        }
    }
    
    public MyGradientsGUI(Ombre plugin, Player player, GUIManager guiManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.player = player;
        this.guiManager = guiManager;
        this.configManager = configManager;
        this.messageManager = plugin.getMessageManager();
        this.currentPage = 0;
        this.sortMode = SortMode.TIME;
        this.myConfigs = new ArrayList<>();
        
        this.inventory = Bukkit.createInventory(this, ROWS * COLS,
            messageManager.getComponent("gui.my-gradients.title"));
        
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
                plugin.getLogger().warning("Failed to load config file: " + file.getName());
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
                messageManager.getMessage("gui.my-gradients.config.default-name", player,
                    "number", String.valueOf(config.getConfigNumber()));
            Map<String, Object> nameParams = new HashMap<>();
            nameParams.put("name", displayName);
            meta.displayName(messageManager.getComponent("gui.my-gradients.config.display-name", player, nameParams));
            
            // 說明文字
            List<Component> lore = new ArrayList<>();
            lore.add(messageManager.getComponent("gui.my-gradients.config.creator", player,
                "creator", config.getCreatorName()));
            lore.add(messageManager.getComponent("gui.my-gradients.config.blocks", player,
                "count", String.valueOf(config.getBlockConfiguration().size())));
            lore.add(messageManager.getComponent("gui.my-gradients.config.favorites", player,
                "count", String.valueOf(config.getFavoriteCount())));
            
            if (config.isPublished()) {
                lore.add(messageManager.getComponent("gui.my-gradients.config.published", player));
                lore.add(messageManager.getComponent("gui.my-gradients.config.loads", player,
                    "count", String.valueOf(config.getLoadCount())));
            } else {
                lore.add(messageManager.getComponent("gui.my-gradients.config.not-published", player));
            }
            
            lore.add(Component.empty());
            lore.add(messageManager.getComponent("gui.my-gradients.config.action.left-click", player));
            lore.add(messageManager.getComponent("gui.my-gradients.config.action.shift-left-click", player));
            lore.add(messageManager.getComponent(config.isPublished() ? 
                "gui.my-gradients.config.action.right-click-unpublish" : 
                "gui.my-gradients.config.action.right-click-publish", player));
            lore.add(messageManager.getComponent("gui.my-gradients.config.action.shift-right-click", player));
            
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
                messageManager.getMessage("gui.my-gradients.button.prev-page", player),
                messageManager.getMessage("gui.my-gradients.button.prev-page-lore", player,
                    "page", String.valueOf(currentPage))));
        }
        
        // 排序按鈕
        inventory.setItem(BUTTON_SORT, createItem(Material.COMPARATOR, 
            messageManager.getMessage("gui.my-gradients.button.sort", player,
                "mode", messageManager.getMessage(sortMode.getDisplayNameKey(), player)),
            messageManager.getMessage(sortMode.getDescriptionKey(), player),
            messageManager.getMessage("gui.my-gradients.button.sort-hint", player)));
        
        // 下一頁按鈕
        int totalPages = (int) Math.ceil((double) myConfigs.size() / CONFIGS_PER_PAGE);
        if (currentPage < totalPages - 1) {
            inventory.setItem(BUTTON_NEXT_PAGE, createItem(Material.ARROW, 
                messageManager.getMessage("gui.my-gradients.button.next-page", player),
                messageManager.getMessage("gui.my-gradients.button.next-page-lore", player,
                    "page", String.valueOf(currentPage + 2))));
        }
        
        // 頁碼資訊
        inventory.setItem(4, createItem(Material.BOOK, 
            messageManager.getMessage("gui.my-gradients.page-info.title", player),
            messageManager.getMessage("gui.my-gradients.page-info.total", player,
                "count", String.valueOf(myConfigs.size())),
            messageManager.getMessage("gui.my-gradients.page-info.page", player,
                "current", String.valueOf(currentPage + 1),
                "total", String.valueOf(Math.max(1, totalPages)))));
        
        // 返回按鈕（右下角）
        inventory.setItem(BUTTON_BACK, createItem(Material.BARRIER, 
            messageManager.getMessage("gui.my-gradients.button.back", player),
            messageManager.getMessage("gui.my-gradients.button.back-lore", player)));
    }
    
    /**
     * 創建物品
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 使用 MiniMessage 解析名稱
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name));
            if (lore.length > 0) {
                // 使用 MiniMessage 解析 Lore
                meta.lore(Arrays.stream(lore)
                    .map(line -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line))
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
        
        String configName = config.getName() != null ? config.getName() : 
            messageManager.getMessage("gui.my-gradients.config.default-name", player,
                "number", String.valueOf(config.getConfigNumber()));
        messageManager.sendMessage(player, "gui.my-gradients.messages.loaded", "name", configName);
    }
    
    /**
     * 編輯配置
     */
    private void editConfiguration(GradientConfig config) {
        player.closeInventory();
        
        // 打開漸層製作 GUI 並載入配置（帶返回按鈕）
        guiManager.openOmbreGUI(player, config, "my");
        
        messageManager.sendMessage(player, "gui.my-gradients.messages.edit-mode");
    }
    
    /**
     * 切換發布狀態
     */
    private void togglePublish(GradientConfig config) {
        if (config.isPublished()) {
            // 取消發布
            config.setPublished(false);
            configManager.saveGradient(config);
            messageManager.sendMessage(player, "gui.my-gradients.messages.unpublished");
        } else {
            // 發布到共享庫
            config.setPublished(true);
            configManager.saveGradient(config);
            messageManager.sendMessage(player, "gui.my-gradients.messages.published");
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
            messageManager.sendMessage(player, "gui.my-gradients.messages.deleted");
            
            // 重新載入配置
            loadMyConfigs();
            
            // 如果當前頁已無配置且不是第一頁，返回上一頁
            if (myConfigs.size() <= currentPage * CONFIGS_PER_PAGE && currentPage > 0) {
                currentPage--;
            }
            
            setupGUI();
        } else {
            messageManager.sendMessage(player, "gui.my-gradients.messages.delete-failed");
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
        
        messageManager.sendMessage(player, "gui.my-gradients.messages.sort-changed", 
            "mode", messageManager.getMessage(sortMode.getDisplayNameKey(), player));
    }
}
