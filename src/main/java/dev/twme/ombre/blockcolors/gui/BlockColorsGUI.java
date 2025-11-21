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
        this.rgbStep = plugin.getConfig().getInt("blockcolors.gui.rgb-step", 5);
        
        this.inventory = Bukkit.createInventory(this, SIZE, "§6§lBlockColors.app");
        
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
            Material.ARROW, "§e返回", "§7返回 Ombre 主選單"
        ));
        inventory.setItem(4, GuiUtils.createItem(
            Material.NAME_TAG, "§6§lBlockColors.app"
        ));
        inventory.setItem(SLOT_HELP, GuiUtils.createItem(
            Material.BOOK, "§e說明",
            "§7點擊調整 RGB 值來選擇顏色",
            "§7或使用方塊取色槽",
            "§7系統會顯示最接近的方塊"
        ));
        inventory.setItem(SLOT_CLOSE, GuiUtils.createItem(
            Material.BARRIER, "§c關閉"
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
            Material.PAINTING, "§d我的調色盤",
            "§7點擊管理你的調色盤"
        ));
    }

    /**
     * 更新顏色控制按鈕
     */
    private void updateColorControls() {
        // 方塊取色槽 - 只在空的或是玻璃片時才重置
        ItemStack currentPicker = inventory.getItem(SLOT_COLOR_PICKER);
        if (currentPicker == null || currentPicker.getType() == Material.AIR || currentPicker.getType() == Material.GLASS) {
            inventory.setItem(SLOT_COLOR_PICKER, GuiUtils.createItem(
                Material.GLASS, "§e方塊取色槽",
                "§7將方塊放入此槽以提取顏色",
                "§7再次點擊可移除方塊"
            ));
        }
        
        // RGB 調整按鈕
        inventory.setItem(SLOT_R_PLUS, GuiUtils.createItem(
            Material.RED_WOOL, "§cR +§f" + rgbStep,
            "§7當前: §c" + currentRed
        ));
        inventory.setItem(SLOT_R_MINUS, GuiUtils.createItem(
            Material.RED_WOOL, "§cR -§f" + rgbStep,
            "§7當前: §c" + currentRed
        ));
        
        inventory.setItem(SLOT_G_PLUS, GuiUtils.createItem(
            Material.LIME_WOOL, "§aG +§f" + rgbStep,
            "§7當前: §a" + currentGreen
        ));
        inventory.setItem(SLOT_G_MINUS, GuiUtils.createItem(
            Material.LIME_WOOL, "§aG -§f" + rgbStep,
            "§7當前: §a" + currentGreen
        ));
        
        inventory.setItem(SLOT_B_PLUS, GuiUtils.createItem(
            Material.BLUE_WOOL, "§9B +§f" + rgbStep,
            "§7當前: §9" + currentBlue
        ));
        inventory.setItem(SLOT_B_MINUS, GuiUtils.createItem(
            Material.BLUE_WOOL, "§9B -§f" + rgbStep,
            "§7當前: §9" + currentBlue
        ));
        
        inventory.setItem(SLOT_HEX_INPUT, GuiUtils.createItem(
            Material.PAPER, "§eHEX 輸入",
            "§7點擊輸入 HEX 色碼",
            "§7當前: §f" + ColorConverter.rgbToHex(currentRed, currentGreen, currentBlue)
        ));
        
        inventory.setItem(SLOT_RESET, GuiUtils.createItem(
            Material.WHITE_WOOL, "§f重置",
            "§7重置為白色 (255, 255, 255)"
        ));
    }

    /**
     * 更新當前顏色顯示
     */
    private void updateCurrentColorDisplay() {
        int rgb = ColorConverter.createRgb(currentRed, currentGreen, currentBlue);
        String hex = ColorConverter.rgbToHex(currentRed, currentGreen, currentBlue);
        
        inventory.setItem(SLOT_CURRENT_COLOR, GuiUtils.createColoredPane(
            rgb,
            "§e當前顏色",
            "§7HEX: §f" + hex,
            "§7RGB: §f(" + currentRed + ", " + currentGreen + ", " + currentBlue + ")"
        ));
    }

    /**
     * 更新類別篩選按鈕
     */
    private void updateCategoryFilters() {
        boolean isAll = currentCategory == BlockCategory.ALL;
        boolean isBuilding = currentCategory == BlockCategory.BUILDING;
        boolean isDecoration = currentCategory == BlockCategory.DECORATION;
        
        inventory.setItem(SLOT_FILTER_ALL, GuiUtils.createItem(
            Material.CHEST,
            isAll ? "§a§l全部方塊" : "§7全部方塊",
            isAll ? "§a✓ 當前篩選" : "§7點擊切換"
        ));
        
        inventory.setItem(SLOT_FILTER_BUILDING, GuiUtils.createItem(
            Material.BRICKS,
            isBuilding ? "§a§l建築方塊" : "§7建築方塊",
            isBuilding ? "§a✓ 當前篩選" : "§7點擊切換"
        ));
        
        inventory.setItem(SLOT_FILTER_DECORATION, GuiUtils.createItem(
            Material.FLOWER_POT,
            isDecoration ? "§a§l裝飾方塊" : "§7裝飾方塊",
            isDecoration ? "§a✓ 當前篩選" : "§7點擊切換"
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
            
            List<String> lore = new ArrayList<>();
            lore.add("§7" + match.getBlock().getDisplayName());
            lore.add("");
            lore.add("§e相似度: §f" + match.getSimilarityPercentage());
            lore.add(match.getSimilarityLevel());
            lore.add("");
            lore.add("§7HEX: §f" + match.getBlock().getHexColor());
            lore.add("§7RGB: §f(" + match.getBlock().getRed() + ", " + 
                     match.getBlock().getGreen() + ", " + match.getBlock().getBlue() + ")");
            lore.add("");
            lore.add("§a左鍵 §7加入調色盤");
            lore.add("§e右鍵 §7拿取方塊");
            
            ItemStack item = GuiUtils.createItem(material, "§b" + match.getBlock().getDisplayName(), lore);
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
            inventory.setItem(SLOT_PREV_PAGE, GuiUtils.createItem(
                Material.ARROW, "§e◀ 上一頁",
                "§7頁數: §f" + (currentPage) + "/" + totalPages
            ));
        } else {
            inventory.setItem(SLOT_PREV_PAGE, GuiUtils.createItem(
                Material.GRAY_STAINED_GLASS_PANE, " "
            ));
        }
        
        if (currentPage < totalPages - 1) {
            inventory.setItem(SLOT_NEXT_PAGE, GuiUtils.createItem(
                Material.ARROW, "§e下一頁 ▶",
                "§7頁數: §f" + (currentPage + 2) + "/" + totalPages
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
            player.sendMessage("§6§l========== BlockColors 說明 ==========");
            player.sendMessage("§e使用方法:");
            player.sendMessage("§7- 點擊 RGB 按鈕調整顏色");
            player.sendMessage("§7- 將方塊放入取色槽提取顏色");
            player.sendMessage("§7- 點擊 HEX 輸入使用 HEX 色碼");
            player.sendMessage("§7- 系統會自動顯示最相似的方塊");
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
            player.sendMessage("§a✓ 已重置為白色");
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
                        player.sendMessage("§a✓ 已獲得 " + match.getBlock().getDisplayName());
                        
                        // 延遲補上相同方塊
                        final int targetSlot = slot;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            List<String> lore = new ArrayList<>();
                            lore.add("§7" + match.getBlock().getDisplayName());
                            lore.add("");
                            lore.add("§e相似度: §f" + match.getSimilarityPercentage());
                            lore.add(match.getSimilarityLevel());
                            lore.add("");
                            lore.add("§7HEX: §f" + match.getBlock().getHexColor());
                            lore.add("§7RGB: §f(" + match.getBlock().getRed() + ", " + 
                                     match.getBlock().getGreen() + ", " + match.getBlock().getBlue() + ")");
                            lore.add("");
                            lore.add("§a左鍵 §7加入調色盤");
                            lore.add("§e右鍵 §7拿取方塊");
                            
                            ItemStack item = GuiUtils.createItem(material, "§b" + match.getBlock().getDisplayName(), lore);
                            inventory.setItem(targetSlot, item);
                        }, 1L);
                    } else {
                        // 左鍵 - 加入調色盤
                        PlayerPalette palette = feature.getPlayerPalette(player.getUniqueId());
                        if (palette.addBlock(material)) {
                            player.sendMessage("§a✓ 已加入 " + match.getBlock().getDisplayName() + " 到調色盤");
                        } else if (palette.isFull()) {
                            player.sendMessage("§c調色盤已滿!");
                        } else {
                            player.sendMessage("§c此方塊已在調色盤中");
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
                    
                    player.sendMessage("§a✓ 已從 " + blockToPlace.getType().name() + " 提取顏色");
                    player.sendMessage("§7顏色: §f" + ColorConverter.rgbToHex(currentRed, currentGreen, currentBlue));
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
