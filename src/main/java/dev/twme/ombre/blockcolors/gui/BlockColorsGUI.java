package dev.twme.ombre.blockcolors.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockcolors.ColorMatcher;
import dev.twme.ombre.blockcolors.data.BlockCategory;
import dev.twme.ombre.blockcolors.data.ColorMatch;
import dev.twme.ombre.blockcolors.data.PlayerPalette;
import dev.twme.ombre.blockcolors.util.ColorConverter;
import dev.twme.ombre.blockcolors.util.GuiUtils;
import net.kyori.adventure.text.Component;

/**
 * BlockColors 主 GUI 介面
 * 提供顏色選擇和方塊匹配功能
 */
public class BlockColorsGUI implements InventoryHolder {
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    
    // 控制按鈕槽位
    private static final int SLOT_BACK = 0;
    private static final int SLOT_HELP = 7;
    private static final int SLOT_CLOSE = 8;
    
    // 顏色調整槽位（第2行）
    private static final int SLOT_COLOR_PICKER = 9;
    private static final int SLOT_R_PLUS = 10;
    private static final int SLOT_R_MINUS = 11;
    private static final int SLOT_G_PLUS = 12;
    private static final int SLOT_G_MINUS = 13;
    private static final int SLOT_B_PLUS = 14;
    private static final int SLOT_B_MINUS = 15;
    private static final int SLOT_HEX_INPUT = 16;
    private static final int SLOT_RESET = 17;
    
    // 當前顏色與篩選（第3行）
    private static final int SLOT_CURRENT_COLOR = 18;
    private static final int SLOT_FILTER_ALL = 20;
    private static final int SLOT_FILTER_BUILDING = 21;
    private static final int SLOT_FILTER_DECORATION = 22;
    
    // 方塊顯示區域（第4-5行，18個槽位）
    private static final int BLOCKS_START = 27;
    private static final int BLOCKS_END = 44;
    private static final int BLOCKS_PER_PAGE = 18;
    
    // 底部功能列（第6行）
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_PALETTE = 49;
    private static final int SLOT_NEXT_PAGE = 53;
    
    private final BlockColorsFeature feature;
    private final Player player;
    private final Inventory inventory;
    private final Ombre plugin;
    private final dev.twme.ombre.i18n.MessageManager msg;
    
    // 當前狀態
    private int currentRed = 255;
    private int currentGreen = 255;
    private int currentBlue = 255;
    private BlockCategory currentCategory = BlockCategory.ALL;
    private int currentPage = 0;
    private List<ColorMatch> currentMatches = new ArrayList<>();
    
    private final int rgbStep;

    public BlockColorsGUI(BlockColorsFeature feature, Player player) {
        this.feature = feature;
        this.player = player;
        this.plugin = (Ombre) feature.getPlugin();
        this.msg = plugin.getMessageManager();
        this.rgbStep = plugin.getConfig().getInt("blockcolors.gui.rgb-step", 5);
        
        this.inventory = Bukkit.createInventory(this, SIZE, 
            msg.getComponent("blockcolors.gui.title", player));
        
        // 初始化界面
        setupGUI();
        updateMatches();
    }

    /**
     * 設置 GUI 基礎佈局
     */
    private void setupGUI() {
        // 第1行：控制列
        inventory.setItem(SLOT_BACK, GuiUtils.createItem(
            Material.ARROW, msg.getMessage("blockcolors.gui.back", player), msg.getMessage("blockcolors.gui.back-lore", player)
        ));
        inventory.setItem(4, GuiUtils.createItem(
            Material.NAME_TAG, msg.getMessage("blockcolors.gui.title", player)
        ));
        List<String> helpLore = msg.getMessageList("blockcolors.gui.help-lore");
        inventory.setItem(SLOT_HELP, GuiUtils.createItem(
            Material.BOOK, msg.getMessage("blockcolors.gui.help", player),
            helpLore.toArray(new String[0])
        ));
        inventory.setItem(SLOT_CLOSE, GuiUtils.createItem(
            Material.BARRIER, msg.getMessage("blockcolors.gui.close", player)
        ));
        
        // 第2行：顏色調整
        updateColorControls();
        
        // 第3行：當前顏色與篩選
        updateCurrentColorDisplay();
        updateCategoryFilters();
        
        // 填充空格
        for (int i = 23; i < 27; i++) {
            inventory.setItem(i, GuiUtils.createFiller());
        }
        
        // 第6行：底部功能
        updatePagination();
        inventory.setItem(SLOT_PALETTE, GuiUtils.createItem(
            Material.PAINTING, msg.getMessage("blockcolors.gui.my-palette", player),
            msg.getMessage("blockcolors.gui.my-palette-lore", player)
        ));
    }

    /**
     * 更新顏色控制按鈕
     */
    private void updateColorControls() {
        // 方塊取色槽 - 只在空的或是玻璃片時才重置
        ItemStack currentPicker = inventory.getItem(SLOT_COLOR_PICKER);
        if (currentPicker == null || currentPicker.getType() == Material.AIR || currentPicker.getType() == Material.GLASS) {
            List<String> pickerLore = msg.getMessageList("blockcolors.gui.color-picker-lore");
            inventory.setItem(SLOT_COLOR_PICKER, GuiUtils.createItem(
                Material.GLASS, msg.getMessage("blockcolors.gui.color-picker"),
                pickerLore
            ));
        }
        
        // RGB 調整按鈕
        String currentValueR = msg.getMessage("blockcolors.gui.current-value", player, java.util.Map.of("<value>", String.valueOf(currentRed)));
        inventory.setItem(SLOT_R_PLUS, GuiUtils.createItem(
            Material.RED_WOOL, msg.getMessage("blockcolors.gui.r-plus", player, java.util.Map.of("<amount>", String.valueOf(rgbStep))),
            currentValueR
        ));
        inventory.setItem(SLOT_R_MINUS, GuiUtils.createItem(
            Material.RED_WOOL, msg.getMessage("blockcolors.gui.r-minus", player, java.util.Map.of("<amount>", String.valueOf(rgbStep))),
            currentValueR
        ));
        
        String currentValueG = msg.getMessage("blockcolors.gui.current-value", player, java.util.Map.of("<value>", String.valueOf(currentGreen)));
        inventory.setItem(SLOT_G_PLUS, GuiUtils.createItem(
            Material.LIME_WOOL, msg.getMessage("blockcolors.gui.g-plus", player, java.util.Map.of("<amount>", String.valueOf(rgbStep))),
            currentValueG
        ));
        inventory.setItem(SLOT_G_MINUS, GuiUtils.createItem(
            Material.LIME_WOOL, msg.getMessage("blockcolors.gui.g-minus", player, java.util.Map.of("<amount>", String.valueOf(rgbStep))),
            currentValueG
        ));
        
        String currentValueB = msg.getMessage("blockcolors.gui.current-value", player, java.util.Map.of("<value>", String.valueOf(currentBlue)));
        inventory.setItem(SLOT_B_PLUS, GuiUtils.createItem(
            Material.BLUE_WOOL, msg.getMessage("blockcolors.gui.b-plus", player, java.util.Map.of("<amount>", String.valueOf(rgbStep))),
            currentValueB
        ));
        inventory.setItem(SLOT_B_MINUS, GuiUtils.createItem(
            Material.BLUE_WOOL, msg.getMessage("blockcolors.gui.b-minus", player, java.util.Map.of("<amount>", String.valueOf(rgbStep))),
            currentValueB
        ));
        
        String hex = ColorConverter.rgbToHex(currentRed, currentGreen, currentBlue);
        List<String> hexLore = new ArrayList<>();
        hexLore.addAll(msg.getMessageList("blockcolors.gui.hex-input-lore"));
        hexLore = hexLore.stream().map(l -> l.replace("<hex>", hex)).collect(java.util.stream.Collectors.toList());
        inventory.setItem(SLOT_HEX_INPUT, GuiUtils.createItem(
            Material.PAPER, msg.getMessage("blockcolors.gui.hex-input", player),
            hexLore.toArray(new String[0])
        ));
        
        inventory.setItem(SLOT_RESET, GuiUtils.createItem(
            Material.WHITE_WOOL, msg.getMessage("blockcolors.gui.reset", player),
            msg.getMessage("blockcolors.gui.reset-lore", player)
        ));
    }

    /**
     * 更新當前顏色顯示
     */
    private void updateCurrentColorDisplay() {
        int rgb = ColorConverter.createRgb(currentRed, currentGreen, currentBlue);
        String hex = ColorConverter.rgbToHex(currentRed, currentGreen, currentBlue);
        String rgbStr = "(" + currentRed + ", " + currentGreen + ", " + currentBlue + ")";
        
        List<String> lore = new ArrayList<>();
        lore.addAll(msg.getMessageList("blockcolors.gui.current-color-lore"));
        lore = lore.stream().map(l -> l.replace("<hex>", hex).replace("<rgb>", rgbStr)).collect(java.util.stream.Collectors.toList());
        
        inventory.setItem(SLOT_CURRENT_COLOR, GuiUtils.createColoredPane(
            rgb,
            msg.getMessage("blockcolors.gui.current-color", player),
            lore.toArray(new String[0])
        ));
    }

    /**
     * 更新類別篩選按鈕
     */
    private void updateCategoryFilters() {
        boolean isAll = currentCategory == BlockCategory.ALL;
        boolean isBuilding = currentCategory == BlockCategory.BUILDING;
        boolean isDecoration = currentCategory == BlockCategory.DECORATION;
        
        String filterHintAll = isAll ? msg.getMessage("blockcolors.gui.filter-current", player) : msg.getMessage("blockcolors.gui.filter-hint", player);
        inventory.setItem(SLOT_FILTER_ALL, GuiUtils.createItem(
            Material.CHEST,
            msg.getMessage("blockcolors.gui.filter-all", player),
            filterHintAll
        ));
        
        String filterHintBuilding = isBuilding ? msg.getMessage("blockcolors.gui.filter-current", player) : msg.getMessage("blockcolors.gui.filter-hint", player);
        inventory.setItem(SLOT_FILTER_BUILDING, GuiUtils.createItem(
            Material.BRICKS,
            msg.getMessage("blockcolors.gui.filter-building", player),
            filterHintBuilding
        ));
        
        String filterHintDecoration = isDecoration ? msg.getMessage("blockcolors.gui.filter-current", player) : msg.getMessage("blockcolors.gui.filter-hint", player);
        inventory.setItem(SLOT_FILTER_DECORATION, GuiUtils.createItem(
            Material.FLOWER_POT,
            msg.getMessage("blockcolors.gui.filter-decoration", player),
            filterHintDecoration
        )); 
    }

    /**
     * 更新方塊匹配結果
     */
    private void updateMatches() {
        int rgb = ColorConverter.createRgb(currentRed, currentGreen, currentBlue);
        currentMatches = ColorMatcher.findMatchingBlocks(rgb, currentCategory, 100);
        currentPage = 0;
        updateBlockDisplay();
    }

    /**
     * 更新方塊顯示區域
     */
    private void updateBlockDisplay() {
        // 清空方塊顯示區域
        for (int i = BLOCKS_START; i <= BLOCKS_END; i++) {
            inventory.setItem(i, null);
        }
        
        // 計算當前頁的方塊
        int startIndex = currentPage * BLOCKS_PER_PAGE;
        int endIndex = Math.min(startIndex + BLOCKS_PER_PAGE, currentMatches.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            ColorMatch match = currentMatches.get(i);
            Material material = match.getBlock().getMaterial();
            
            if (material == null) continue;
            
            // 為 ColorMatch 設置 MessageManager 以便正確顯示相似度
            match.setMessageManager(msg);
            
            List<String> lore = new ArrayList<>();
            lore.add(msg.getMessage("blockcolors.gui.block-info.display-name", player, java.util.Map.of("<name>", match.getBlock().getDisplayName())));
            lore.add("");
            lore.add(msg.getMessage("blockcolors.gui.block-info.similarity", player, java.util.Map.of("<percentage>", match.getSimilarityPercentage())));
            lore.add("  " + msg.getMessage("blockcolors.similarity." + getSimilarityKey(match.getSimilarity())));
            lore.add("");
            lore.add(msg.getMessage("blockcolors.gui.block-info.hex", player, java.util.Map.of("<hex>", match.getBlock().getHexColor())));
            String rgbStr = "(" + match.getBlock().getRed() + ", " + match.getBlock().getGreen() + ", " + match.getBlock().getBlue() + ")";
            lore.add(msg.getMessage("blockcolors.gui.block-info.rgb", player, java.util.Map.of("<rgb>", rgbStr)));
            lore.add("");
            lore.add(msg.getMessage("blockcolors.gui.block-info.left-click"));
            lore.add(msg.getMessage("blockcolors.gui.block-info.right-click"));
            
            ItemStack item = GuiUtils.createItem(material, "\u00a7b" + match.getBlock().getDisplayName(), lore);
            inventory.setItem(BLOCKS_START + (i - startIndex), item);
        }
        
        updatePagination();
    }

    /**
     * 更新分頁按鈕
     */
    private void updatePagination() {
        int totalPages = (int) Math.ceil((double) currentMatches.size() / BLOCKS_PER_PAGE);
        
        if (currentPage > 0) {
            String pageInfo = msg.getMessage("blockcolors.gui.page-info", player, java.util.Map.of("<current>", String.valueOf(currentPage), "<total>", String.valueOf(totalPages)));
            inventory.setItem(SLOT_PREV_PAGE, GuiUtils.createItem(
                Material.ARROW, msg.getMessage("blockcolors.gui.prev-page"),
                pageInfo
            ));
        } else {
            inventory.setItem(SLOT_PREV_PAGE, GuiUtils.createItem(
                Material.GRAY_STAINED_GLASS_PANE, " "
            ));
        }
        
        if (currentPage < totalPages - 1) {
            String pageInfo = msg.getMessage("blockcolors.gui.page-info", player, java.util.Map.of("<current>", String.valueOf(currentPage + 2), "<total>", String.valueOf(totalPages)));
            inventory.setItem(SLOT_NEXT_PAGE, GuiUtils.createItem(
                Material.ARROW, msg.getMessage("blockcolors.gui.next-page"),
                pageInfo
            ));
        } else {
            inventory.setItem(SLOT_NEXT_PAGE, GuiUtils.createItem(
                Material.GRAY_STAINED_GLASS_PANE, " "
            ));
        }
    }

    /**
     * 處理點擊事件
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        // 如果點擊的是玩家背包區域(下方),允許操作
        if (slot >= SIZE) {
            return;
        }
        
        // 如果點擊的是取色槽,允許放置/取出方塊
        if (slot == SLOT_COLOR_PICKER) {
            handleColorPicker(event);
            return;
        }
        
        // 檢查是否是方塊區域的右鍵點擊
        boolean isBlockRightClick = (slot >= BLOCKS_START && slot <= BLOCKS_END) && event.isRightClick();
        
        // 方塊區域右鍵不取消事件,允許拿取
        if (!isBlockRightClick) {
            event.setCancelled(true);
        }
        
        if (slot < 0) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // 控制按鈕
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        
        if (slot == SLOT_BACK) {
            player.closeInventory();
            player.performCommand("ombre");
            return;
        }
        
        if (slot == SLOT_HELP) {
            msg.sendMessage(player, "blockcolors.gui.help-title");
            for (Component line : msg.getComponentList("blockcolors.gui.help-content")) {
                player.sendMessage(line);
            }
            return;
        }
        
        // 顏色調整
        if (slot == SLOT_R_PLUS) {
            adjustColor(rgbStep, 0, 0);
            return;
        }
        if (slot == SLOT_R_MINUS) {
            adjustColor(-rgbStep, 0, 0);
            return;
        }
        if (slot == SLOT_G_PLUS) {
            adjustColor(0, rgbStep, 0);
            return;
        }
        if (slot == SLOT_G_MINUS) {
            adjustColor(0, -rgbStep, 0);
            return;
        }
        if (slot == SLOT_B_PLUS) {
            adjustColor(0, 0, rgbStep);
            return;
        }
        if (slot == SLOT_B_MINUS) {
            adjustColor(0, 0, -rgbStep);
            return;
        }
        
        if (slot == SLOT_RESET) {
            currentRed = 255;
            currentGreen = 255;
            currentBlue = 255;
            updateColorControls();
            updateCurrentColorDisplay();
            updateMatches();
            msg.sendMessage(player, "blockcolors.gui.messages.reset");
            return;
        }
        
        if (slot == SLOT_HEX_INPUT) {
            player.closeInventory();
            new HexInputGUI(feature, player, this).open();
            return;
        }
        
        // 類別篩選
        if (slot == SLOT_FILTER_ALL) {
            currentCategory = BlockCategory.ALL;
            updateCategoryFilters();
            updateMatches();
            return;
        }
        if (slot == SLOT_FILTER_BUILDING) {
            currentCategory = BlockCategory.BUILDING;
            updateCategoryFilters();
            updateMatches();
            return;
        }
        if (slot == SLOT_FILTER_DECORATION) {
            currentCategory = BlockCategory.DECORATION;
            updateCategoryFilters();
            updateMatches();
            return;
        }
        
        // 調色盤
        if (slot == SLOT_PALETTE) {
            player.closeInventory();
            new PaletteGUI(feature, player, this).open();
            return;
        }
        
        // 分頁
        if (slot == SLOT_PREV_PAGE && currentPage > 0) {
            currentPage--;
            updateBlockDisplay();
            return;
        }
        if (slot == SLOT_NEXT_PAGE) {
            int totalPages = (int) Math.ceil((double) currentMatches.size() / BLOCKS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateBlockDisplay();
            }
            return;
        }
        
        // 方塊點擊
        if (slot >= BLOCKS_START && slot <= BLOCKS_END) {
            int index = currentPage * BLOCKS_PER_PAGE + (slot - BLOCKS_START);
            if (index < currentMatches.size()) {
                ColorMatch match = currentMatches.get(index);
                Material material = match.getBlock().getMaterial();
                
                if (material != null) {
                    if (event.isRightClick()) {
                        // 右鍵 - 允許拿取方塊(事件已不取消)
                        msg.sendMessage(player, "blockcolors.gui.messages.obtained", java.util.Map.of("<block>", match.getBlock().getDisplayName()));
                        
                        // 延遲補上相同方塊
                        final int targetSlot = slot;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            match.setMessageManager(msg);
                            List<String> lore = new ArrayList<>();
                            lore.add(msg.getMessage("blockcolors.gui.block-info.display-name", player, java.util.Map.of("<name>", match.getBlock().getDisplayName())));
                            lore.add("");
                            lore.add(msg.getMessage("blockcolors.gui.block-info.similarity", player, java.util.Map.of("<percentage>", match.getSimilarityPercentage())));
                            lore.add("  " + msg.getMessage("blockcolors.similarity." + getSimilarityKey(match.getSimilarity())));
                            lore.add("");
                            lore.add(msg.getMessage("blockcolors.gui.block-info.hex", player, java.util.Map.of("<hex>", match.getBlock().getHexColor())));
                            String rgbStr = "(" + match.getBlock().getRed() + ", " + match.getBlock().getGreen() + ", " + match.getBlock().getBlue() + ")";
                            lore.add(msg.getMessage("blockcolors.gui.block-info.rgb", player, java.util.Map.of("<rgb>", rgbStr)));
                            lore.add("");
                            lore.add(msg.getMessage("blockcolors.gui.block-info.left-click"));
                            lore.add(msg.getMessage("blockcolors.gui.block-info.right-click"));
                            
                            ItemStack item = GuiUtils.createItem(material, "\u00a7b" + match.getBlock().getDisplayName(), lore);
                            inventory.setItem(targetSlot, item);
                        }, 1L);
                    } else {
                        // 左鍵 - 加入調色盤
                        PlayerPalette palette = feature.getPlayerPalette(player.getUniqueId());
                        if (palette.addBlock(material)) {
                            msg.sendMessage(player, "blockcolors.gui.messages.added-palette", java.util.Map.of("<block>", match.getBlock().getDisplayName()));
                        } else if (palette.isFull()) {
                            msg.sendMessage(player, "blockcolors.gui.messages.palette-full");
                        } else {
                            msg.sendMessage(player, "blockcolors.gui.messages.already-in-palette");
                        }
                    }
                }
            }
        }
    }

    /**
     * 處理方塊取色槽
     */
    private void handleColorPicker(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // 如果是點擊玻璃片但手上有方塊,清空槽位讓方塊能放入
        if (currentItem != null && currentItem.getType() == Material.GLASS) {
            // 如果手上有方塊,清空槽位並取消事件,然後手動放入方塊
            if (cursorItem != null && cursorItem.getType() != Material.AIR && cursorItem.getType().isBlock()) {
                event.setCancelled(true);
                // 清空槽位
                inventory.setItem(SLOT_COLOR_PICKER, null);
                // 將玩家手上的方塊放入槽位
                ItemStack blockToPlace = cursorItem.clone();
                blockToPlace.setAmount(1);
                inventory.setItem(SLOT_COLOR_PICKER, blockToPlace);
                // 減少手上的方塊數量
                if (cursorItem.getAmount() > 1) {
                    cursorItem.setAmount(cursorItem.getAmount() - 1);
                } else {
                    event.getWhoClicked().setItemOnCursor(null);
                }
                
                // 提取顏色
                int rgb = ColorMatcher.extractBlockColor(blockToPlace.getType());
                if (rgb != -1) {
                    currentRed = (rgb >> 16) & 0xFF;
                    currentGreen = (rgb >> 8) & 0xFF;
                    currentBlue = rgb & 0xFF;
                    
                    updateColorControls();
                    updateCurrentColorDisplay();
                    updateMatches();
                    
                    String hex = ColorConverter.rgbToHex(currentRed, currentGreen, currentBlue);
                    msg.sendMessage(player, "blockcolors.gui.messages.color-extracted", java.util.Map.of("<block>", blockToPlace.getType().name()));
                    msg.sendMessage(player, "blockcolors.gui.messages.color-value", java.util.Map.of("<hex>", hex));
                }
                return;
            } else {
                // 手上沒東西,防止拿走玻璃片
                event.setCancelled(true);
                return;
            }
        }
        
        // 如果是取出真正的方塊,允許操作
        if (currentItem != null && currentItem.getType() != Material.AIR && currentItem.getType() != Material.GLASS) {
            // 允許取出,不取消事件
            // 延遲 1 tick 後重置槽位為玻璃片
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ItemStack slotItem = inventory.getItem(SLOT_COLOR_PICKER);
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    inventory.setItem(SLOT_COLOR_PICKER, GuiUtils.createItem(
                        Material.GLASS, "§e方塊取色槽",
                        "§7將方塊放入此槽以提取顏色",
                        "§7再次點擊可移除方塊"
                    ));
                }
            }, 1L);
        }
    }

    /**
     * 調整顏色值
     */
    private void adjustColor(int rDelta, int gDelta, int bDelta) {
        currentRed = ColorConverter.clampRgb(currentRed + rDelta);
        currentGreen = ColorConverter.clampRgb(currentGreen + gDelta);
        currentBlue = ColorConverter.clampRgb(currentBlue + bDelta);
        
        updateColorControls();
        updateCurrentColorDisplay();
        updateMatches();
    }

    /**
     * 設定顏色（從 HEX 輸入）
     */
    public void setColor(int red, int green, int blue) {
        this.currentRed = ColorConverter.clampRgb(red);
        this.currentGreen = ColorConverter.clampRgb(green);
        this.currentBlue = ColorConverter.clampRgb(blue);
        
        updateColorControls();
        updateCurrentColorDisplay();
        updateMatches();
    }

    /**
     * 開啟 GUI
     */
    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Get similarity level key for message lookup
     */
    private String getSimilarityKey(double similarity) {
        if (similarity >= 95) return "perfect";
        if (similarity >= 85) return "very-similar";
        if (similarity >= 70) return "quite-similar";
        if (similarity >= 50) return "similar";
        return "not-similar";
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
