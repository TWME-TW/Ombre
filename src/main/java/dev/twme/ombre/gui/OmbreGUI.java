package dev.twme.ombre.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import dev.twme.ombre.algorithm.GradientAlgorithm;
import dev.twme.ombre.data.GradientConfig;
import dev.twme.ombre.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * 漸層製作 GUI
 * 9列 × 6行的介面
 */
public class OmbreGUI implements InventoryHolder {
    
    private final Ombre plugin;
    private final Player player;
    private final Inventory inventory;
    private final GradientAlgorithm algorithm;
    private final ConfigManager configManager;
    
    // 輸入區域的方塊（前4行）
    private final Map<Integer, String> inputBlocks; // slot -> blockDataString
    private GradientConfig currentConfig;
    private String previousGUI; // "library" 或 "favorites" 或 "my"
    private String gradientName; // 當前漸層的自訂名稱
    
    // GUI 布局常量
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int INPUT_ROWS = 4;
    private static final int SEPARATOR_ROW = 4;
    private static final int BUTTON_ROW = 5;
    
    // 按鈕位置（第 6 行，索引 5）
    private static final int BUTTON_NAME = 45;     // 第6行第1列
    private static final int BUTTON_SAVE = 46;     // 第6行第2列
    private static final int BUTTON_PUBLISH = 47;  // 第6行第3列
    private static final int BUTTON_FAVORITE = 48; // 第6行第4列
    private static final int BUTTON_DELETE = 51;   // 第6行第7列
    private static final int BUTTON_CLEAR = 52;    // 第6行第8列
    private static final int BUTTON_BACK = 53;     // 第6行第9列（右下角，返回）
    
    public OmbreGUI(Ombre plugin, Player player) {
        this(plugin, player, null, null);
    }
    
    public OmbreGUI(Ombre plugin, Player player, GradientConfig config) {
        this(plugin, player, config, null);
    }
    
    public OmbreGUI(Ombre plugin, Player player, GradientConfig config, String previousGUI) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, ROWS * COLS, 
            Component.text("漸層製作").color(NamedTextColor.DARK_PURPLE));
        this.algorithm = new GradientAlgorithm(plugin, plugin.getColorService(), 
            plugin.getBlockFilterManager(), INPUT_ROWS, COLS);
        this.configManager = plugin.getConfigManager();
        this.inputBlocks = new HashMap<>();
        this.currentConfig = config;
        this.previousGUI = previousGUI;
        this.gradientName = (config != null) ? config.getName() : null;
        
        // 設定當前玩家以便算法使用玩家的色表設定
        this.algorithm.setCurrentPlayer(player.getUniqueId());
        
        setupGUI();
        
        // 如果有配置，載入它
        if (config != null) {
            loadConfigurationData(config);
        }
    }
    
    /**
     * 設置 GUI 初始狀態
     */
    private void setupGUI() {
        // 設置分隔行（白色玻璃）
        for (int col = 0; col < COLS; col++) {
            int slot = SEPARATOR_ROW * COLS + col;
            inventory.setItem(slot, createItem(Material.WHITE_STAINED_GLASS_PANE, " "));
        }
        
        // 設置按鈕
        setupButtons();
    }
    
    /**
     * 設置底部按鈕
     */
    private void setupButtons() {
        // 命名按鈕
        updateNameButton();
        
        // 儲存按鈕
        inventory.setItem(BUTTON_SAVE, createItem(Material.BOOK, "§a儲存", 
            "§7儲存當前漸層配置"));
        
        // 發布按鈕
        inventory.setItem(BUTTON_PUBLISH, createItem(Material.WRITABLE_BOOK, "§b發布", 
            "§7發布到共享庫"));
        
        // 收藏按鈕
        inventory.setItem(BUTTON_FAVORITE, createItem(Material.NETHER_STAR, "§6收藏", 
            "§7加入我的最愛"));
        
        // 刪除按鈕
        inventory.setItem(BUTTON_DELETE, createItem(Material.RED_WOOL, "§c刪除", 
            "§7刪除當前配置"));
        
        // 清除按鈕
        inventory.setItem(BUTTON_CLEAR, createItem(Material.BARRIER, "§c清除調色板", 
            "§7清除當前所有方塊"));
        
        // 返回按鈕（如果有前一個 GUI）
        if (previousGUI != null) {
            String guiName = switch (previousGUI) {
                case "library" -> "共享庫";
                case "favorites" -> "我的最愛";
                case "my" -> "我的漸層";
                default -> "上一頁";
            };
            inventory.setItem(BUTTON_BACK, createItem(Material.RED_WOOL, "§c返回", 
                "§7返回到 " + guiName));
        }
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
        int slot = event.getRawSlot();
        
        // 點擊在 GUI 外或無效的 slot
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        
        // 點擊輸入區域
        if (slot < INPUT_ROWS * COLS) {
            handleInputAreaClick(event);
            return;
        }
        
        // 點擊分隔行 - 取消
        if (slot >= SEPARATOR_ROW * COLS && slot < BUTTON_ROW * COLS) {
            event.setCancelled(true);
            return;
        }
        
        // 點擊按鈕區域
        if (slot >= BUTTON_ROW * COLS) {
            event.setCancelled(true);
            handleButtonClick(slot);
        }
    }
    
    /**
     * 處理輸入區域點擊
     */
    private void handleInputAreaClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        // 如果是放置方塊
        if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
            ItemStack cursor = event.getCursor();
            
            if (cursor != null && cursor.getType().isBlock() && cursor.getType() != Material.AIR) {
                // 玩家正在放置方塊
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    updateInputBlock(slot);
                    recalculateGradient();
                }, 1L);
            } else {
                // 玩家取出方塊
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    inputBlocks.remove(slot);
                    recalculateGradient();
                }, 1L);
            }
        }
    }
    
    /**
     * 更新輸入方塊
     */
    private void updateInputBlock(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item != null && item.getType().isBlock() && item.getType() != Material.AIR) {
            try {
                String blockDataString = item.getType().createBlockData().getAsString();
                inputBlocks.put(slot, blockDataString);
            } catch (Exception e) {
                plugin.getLogger().warning("無法獲取方塊數據: " + item.getType());
            }
        }
    }
    
    /**
     * 重新計算漸層
     */
    private void recalculateGradient() {
        // 將 slot 映射轉換為 Position 映射
        Map<GradientConfig.Position, String> positionMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : inputBlocks.entrySet()) {
            int slot = entry.getKey();
            int row = slot / COLS;
            int col = slot % COLS;
            positionMap.put(new GradientConfig.Position(row, col), entry.getValue());
        }
        
        // 檢查配置是否有效
        if (!algorithm.isValidConfiguration(positionMap, 
                plugin.getConfig().getInt("settings.gradient.min-blocks", 2),
                plugin.getConfig().getBoolean("settings.gradient.ignore-transparent", true),
                plugin.getConfig().getBoolean("settings.gradient.ignore-same-color", true))) {
            return;
        }
        
        // 計算漸層
        Map<GradientConfig.Position, String> gradient = algorithm.calculateGradient(positionMap);
        
        // 填充漸層到 GUI
        for (Map.Entry<GradientConfig.Position, String> entry : gradient.entrySet()) {
            GradientConfig.Position pos = entry.getKey();
            String blockDataString = entry.getValue();
            
            int slot = pos.getRow() * COLS + pos.getCol();
            
            try {
                Material material = Bukkit.createBlockData(blockDataString).getMaterial();
                inventory.setItem(slot, new ItemStack(material));
            } catch (Exception e) {
                // 某些方塊狀態無法作為物品顯示，使用 FINE 級別記錄
                plugin.getLogger().fine("無法設置方塊: " + blockDataString);
            }
        }
    }
    
    /**
     * 處理按鈕點擊
     */
    private void handleButtonClick(int slot) {
        if (slot == BUTTON_NAME) {
            handleNameButton();
        } else if (slot == BUTTON_SAVE) {
            handleSaveButton();
        } else if (slot == BUTTON_PUBLISH) {
            handlePublishButton();
        } else if (slot == BUTTON_FAVORITE) {
            handleFavoriteButton();
        } else if (slot == BUTTON_DELETE) {
            handleDeleteButton();
        } else if (slot == BUTTON_CLEAR) {
            handleClearButton();
        } else if (slot == BUTTON_BACK) {
            handleBackButton();
        }
    }
    
    /**
     * 處理命名按鈕
     */
    private void handleNameButton() {
        player.closeInventory();
        
        // 如果已有名稱，顯示當前名稱
        if (gradientName != null && !gradientName.isEmpty()) {
            player.sendMessage(Component.text("目前名稱: ").color(NamedTextColor.GRAY)
                .append(Component.text(gradientName).color(NamedTextColor.YELLOW)));
        }
        
        player.sendMessage(Component.text("請在聊天欄輸入漸層名稱（最多32字元）").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("輸入 'cancel' 取消命名").color(NamedTextColor.GRAY));
        
        // 註冊到名稱輸入監聽器
        plugin.getGuiManager().getNameInputListener().startWaitingForInput(player, this);
    }
    
    /**
     * 處理儲存按鈕
     */
    private void handleSaveButton() {
        if (inputBlocks.isEmpty()) {
            player.sendMessage(Component.text("沒有可儲存的漸層配置").color(NamedTextColor.RED));
            return;
        }
        
        // 創建新配置
        int configNumber = configManager.getPlayerGradientCount(player.getUniqueId()) + 1;
        GradientConfig config = new GradientConfig(player.getUniqueId(), player.getName(), configNumber);
        
        // 設定自訂名稱（如果有）
        if (gradientName != null && !gradientName.isEmpty()) {
            config.setName(gradientName);
        }
        
        // 設置方塊配置
        Map<GradientConfig.Position, String> positionMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : inputBlocks.entrySet()) {
            int slot = entry.getKey();
            int row = slot / COLS;
            int col = slot % COLS;
            positionMap.put(new GradientConfig.Position(row, col), entry.getValue());
        }
        
        // 計算並儲存完整漸層
        Map<GradientConfig.Position, String> gradient = algorithm.calculateGradient(positionMap);
        for (Map.Entry<GradientConfig.Position, String> entry : gradient.entrySet()) {
            config.setBlock(entry.getKey().getRow(), entry.getKey().getCol(), entry.getValue());
        }
        
        // 儲存配置
        if (configManager.saveGradient(config)) {
            currentConfig = config;
            player.sendMessage(Component.text("漸層配置已儲存為: " + config.getDisplayName())
                .color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("儲存失敗").color(NamedTextColor.RED));
        }
    }
    
    /**
     * 處理發布按鈕
     */
    private void handlePublishButton() {
        if (currentConfig == null) {
            player.sendMessage(Component.text("請先儲存配置").color(NamedTextColor.RED));
            return;
        }
        
        if (!player.hasPermission("ombre.publish")) {
            player.sendMessage(Component.text("你沒有發布權限").color(NamedTextColor.RED));
            return;
        }
        
        if (configManager.publishGradient(currentConfig.getId(), player.getUniqueId())) {
            player.sendMessage(Component.text("配置已發布到共享庫").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("發布失敗").color(NamedTextColor.RED));
        }
    }
    
    /**
     * 處理收藏按鈕
     */
    private void handleFavoriteButton() {
        if (currentConfig == null) {
            player.sendMessage(Component.text("請先儲存配置").color(NamedTextColor.RED));
            return;
        }
        
        if (configManager.isFavorited(player, currentConfig.getId())) {
            configManager.removeFavorite(player.getUniqueId(), currentConfig.getId());
            player.sendMessage(Component.text("已從我的最愛移除").color(NamedTextColor.YELLOW));
        } else {
            configManager.addFavorite(player.getUniqueId(), currentConfig.getId());
            player.sendMessage(Component.text("已加入我的最愛").color(NamedTextColor.GREEN));
        }
        
        // 更新按鈕外觀
        setupButtons();
    }
    
    /**
     * 處理刪除按鈕
     */
    private void handleDeleteButton() {
        if (currentConfig == null) {
            player.sendMessage(Component.text("沒有可刪除的配置").color(NamedTextColor.RED));
            return;
        }
        
        if (configManager.deleteGradient(currentConfig.getId(), player)) {
            player.sendMessage(Component.text("配置已刪除").color(NamedTextColor.GREEN));
            currentConfig = null;
            clearInputArea();
        } else {
            player.sendMessage(Component.text("刪除失敗（可能沒有權限）").color(NamedTextColor.RED));
        }
    }
    
    /**
     * 處理清除按鈕
     */
    private void handleClearButton() {
        if (inputBlocks.isEmpty()) {
            player.sendMessage(Component.text("調色板已經是空的").color(NamedTextColor.YELLOW));
            return;
        }
        
        clearInputArea();
        player.sendMessage(Component.text("調色板已清除").color(NamedTextColor.GREEN));
    }
    
    /**
     * 處理返回按鈕
     */
    private void handleBackButton() {
        if (previousGUI == null) {
            return;
        }
        
        player.closeInventory();
        
        // 根據 previousGUI 的值打開相應的 GUI
        GUIManager guiManager = plugin.getGuiManager();
        if (previousGUI.equals("library")) {
            guiManager.openLibraryGUI(player);
        } else if (previousGUI.equals("favorites")) {
            guiManager.openFavoritesGUI(player);
        } else if (previousGUI.equals("my")) {
            guiManager.openMyGradientsGUI(player);
        }
    }
    
    /**
     * 清空輸入區域
     */
    private void clearInputArea() {
        inputBlocks.clear();
        for (int slot = 0; slot < INPUT_ROWS * COLS; slot++) {
            inventory.setItem(slot, null);
        }
    }
    
    /**
     * 載入配置到 GUI
     */
    public void loadConfig(GradientConfig config) {
        this.currentConfig = config;
        clearInputArea();
        
        // 載入方塊配置
        for (Map.Entry<GradientConfig.Position, String> entry : config.getBlockConfiguration().entrySet()) {
            GradientConfig.Position pos = entry.getKey();
            String blockDataString = entry.getValue();
            
            int slot = pos.getRow() * COLS + pos.getCol();
            
            try {
                Material material = Bukkit.createBlockData(blockDataString).getMaterial();
                inventory.setItem(slot, new ItemStack(material));
                
                // 記錄到 inputBlocks（假設前2行是輸入，後2行是輸出）
                if (pos.getRow() < 2) {
                    inputBlocks.put(slot, blockDataString);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("無法載入方塊: " + blockDataString);
            }
        }
        
        // 增加載入次數
        if (config.isPublished()) {
            configManager.incrementLoadCount(config.getId());
        }
    }
    
    /**
     * 載入配置（別名）
     */
    public void loadConfiguration(GradientConfig config) {
        loadConfig(config);
    }
    
    /**
     * 載入配置數據（內部使用，用於構造函數）
     */
    private void loadConfigurationData(GradientConfig config) {
        loadConfig(config);
    }
    
    /**
     * 打開 GUI
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * 打開 GUI
     */
    public void open(Player player) {
        player.openInventory(inventory);
    }
    
    /**
     * 設定漸層名稱
     */
    public void setGradientName(String name) {
        this.gradientName = name;
        // 更新按鈕顯示
        updateNameButton();
    }
    
    /**
     * 更新命名按鈕的顯示
     */
    private void updateNameButton() {
        ItemStack nameButton;
        if (gradientName != null && !gradientName.isEmpty()) {
            nameButton = createItem(Material.NAME_TAG, "§6命名",
                "§7目前名稱: §e" + gradientName,
                "§7點擊修改名稱");
        } else {
            nameButton = createItem(Material.NAME_TAG, "§6命名",
                "§7為漸層設定自訂名稱",
                "§7未設定時將使用預設格式");
        }
        inventory.setItem(BUTTON_NAME, nameButton);
    }
}
