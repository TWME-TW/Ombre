package dev.twme.ombre.palette;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * 方塊過濾管理器
 * 負責管理排除列表和色表
 */
public class BlockFilterManager {
    
    private final Plugin plugin;
    private final File exclusionsFolder;
    private final File palettesFolder;
    
    private final Map<String, BlockList> exclusionLists;
    private final Map<String, BlockList> colorPalettes;
    
    // 玩家的色表選擇（玩家UUID -> 選擇的色表ID集合）
    private final Map<UUID, Set<String>> playerPaletteChoices;
    // 玩家的排除列表選擇
    private final Map<UUID, Set<String>> playerExclusionChoices;
    
    public BlockFilterManager(Plugin plugin) {
        this.plugin = plugin;
        this.exclusionsFolder = new File(plugin.getDataFolder(), "exclusions");
        this.palettesFolder = new File(plugin.getDataFolder(), "palettes");
        this.exclusionLists = new HashMap<>();
        this.colorPalettes = new HashMap<>();
        this.playerPaletteChoices = new HashMap<>();
        this.playerExclusionChoices = new HashMap<>();
        
        // 確保資料夾存在
        if (!exclusionsFolder.exists()) {
            exclusionsFolder.mkdirs();
        }
        if (!palettesFolder.exists()) {
            palettesFolder.mkdirs();
        }
    }
    
    /**
     * 初始化預設的排除列表和色表
     */
    public void initializeDefaults() {
        // 生成預設排除列表
        createDefaultExclusionLists();
        
        // 生成預設色表
        createDefaultPalettes();
        
        // 載入所有列表
        loadAllLists();
    }
    
    /**
     * 創建預設排除列表
     */
    private void createDefaultExclusionLists() {
        createTechnicalBlocksExclusion();
        createUnplaceableBlocksExclusion();
        createFluidsExclusion();
        createDecorativeExclusion();
        createPlantsExclusion();
        createRedstoneExclusion();
    }
    
    private void createTechnicalBlocksExclusion() {
        File file = new File(exclusionsFolder, "technical_blocks.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Technical Blocks");
        config.set("description", "Command blocks, barriers, and other technical blocks");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK",
            "BARRIER", "STRUCTURE_BLOCK", "STRUCTURE_VOID", "JIGSAW",
            "BEDROCK", "END_PORTAL_FRAME", "END_PORTAL", "NETHER_PORTAL"
        ));
        
        saveConfig(file, config);
    }
    
    private void createUnplaceableBlocksExclusion() {
        File file = new File(exclusionsFolder, "unplaceable_blocks.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Unplaceable Blocks");
        config.set("description", "Blocks that cannot be placed normally");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "FROSTED_ICE",
            "FIRE",
            "SOUL_FIRE",
            "MOVING_PISTON",
            "PISTON_HEAD",
            "POWDER_SNOW",
            "CAVE_AIR",
            "VOID_AIR",
            "BUBBLE_COLUMN",
            "END_GATEWAY",
            "END_PORTAL",
            "NETHER_PORTAL",
            "SPAWNER",
            "FARMLAND",
            "CARROTS",
            "POTATOES",
            "BEETROOTS",
            "WHEAT",
            "COCOA",
            "SWEET_BERRY_BUSH",
            "ATTACHED_MELON_STEM",
            "ATTACHED_PUMPKIN_STEM",
            "MELON_STEM",
            "PUMPKIN_STEM",
            "BAMBOO_SAPLING",
            "TRIPWIRE",
            "KELP_PLANT",
            "TALL_SEAGRASS",
            "LARGE_FERN",
            "TALL_GRASS",
            "SUNFLOWER",
            "LILAC",
            "ROSE_BUSH",
            "PEONY",
            "PITCHER_PLANT",
            "BIG_DRIPLEAF_STEM",
            "CAVE_VINES_PLANT"
        ));
        
        saveConfig(file, config);
    }
    
    private void createFluidsExclusion() {
        File file = new File(exclusionsFolder, "fluids.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Fluids");
        config.set("description", "Water and lava blocks");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList("WATER", "LAVA", "BUBBLE_COLUMN"));
        
        saveConfig(file, config);
    }
    
    private void createDecorativeExclusion() {
        File file = new File(exclusionsFolder, "decorative.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Decorative Blocks");
        config.set("description", "Torches, doors, buttons, and other decorative blocks");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "ALL:TORCH", "LADDER", "LEVER",
            "ALL:DOOR", "ALL:TRAPDOOR", "ALL:BUTTON", "ALL:PRESSURE_PLATE"
        ));
        
        saveConfig(file, config);
    }
    
    private void createPlantsExclusion() {
        File file = new File(exclusionsFolder, "plants.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Plants");
        config.set("description", "Saplings, leaves, grass, and flowers");
        config.set("enabled", false);
        config.set("blocks", Arrays.asList(
            "ALL:SAPLING", "ALL:LEAVES", "GRASS", "TALL_GRASS",
            "FERN", "LARGE_FERN", "DANDELION", "POPPY"
        ));
        
        saveConfig(file, config);
    }
    
    private void createRedstoneExclusion() {
        File file = new File(exclusionsFolder, "redstone.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Redstone Components");
        config.set("description", "Redstone wire, repeaters, and other redstone components");
        config.set("enabled", false);
        config.set("blocks", Arrays.asList(
            "REDSTONE_WIRE", "REPEATER", "COMPARATOR", "REDSTONE_LAMP",
            "PISTON", "STICKY_PISTON", "OBSERVER", "HOPPER", "DROPPER", "DISPENSER"
        ));
        
        saveConfig(file, config);
    }
    
    /**
     * 創建預設色表
     */
    private void createDefaultPalettes() {
        createWoolPalette();
        createConcretePalette();
        createTerracottaPalette();
        createStainedGlassPalette();
        createNaturalStonePalette();
        createWoodPalette();
    }
    
    private void createWoolPalette() {
        File file = new File(palettesFolder, "wool.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Wool Blocks");
        config.set("description", "All colored wool blocks");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "WHITE_WOOL", "ORANGE_WOOL", "MAGENTA_WOOL", "LIGHT_BLUE_WOOL",
            "YELLOW_WOOL", "LIME_WOOL", "PINK_WOOL", "GRAY_WOOL",
            "LIGHT_GRAY_WOOL", "CYAN_WOOL", "PURPLE_WOOL", "BLUE_WOOL",
            "BROWN_WOOL", "GREEN_WOOL", "RED_WOOL", "BLACK_WOOL"
        ));
        
        saveConfig(file, config);
    }
    
    private void createConcretePalette() {
        File file = new File(palettesFolder, "concrete.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Concrete Blocks");
        config.set("description", "All colored concrete blocks");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "WHITE_CONCRETE", "ORANGE_CONCRETE", "MAGENTA_CONCRETE", "LIGHT_BLUE_CONCRETE",
            "YELLOW_CONCRETE", "LIME_CONCRETE", "PINK_CONCRETE", "GRAY_CONCRETE",
            "LIGHT_GRAY_CONCRETE", "CYAN_CONCRETE", "PURPLE_CONCRETE", "BLUE_CONCRETE",
            "BROWN_CONCRETE", "GREEN_CONCRETE", "RED_CONCRETE", "BLACK_CONCRETE"
        ));
        
        saveConfig(file, config);
    }
    
    private void createTerracottaPalette() {
        File file = new File(palettesFolder, "terracotta.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Terracotta Blocks");
        config.set("description", "All colored terracotta blocks");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "WHITE_TERRACOTTA", "ORANGE_TERRACOTTA", "MAGENTA_TERRACOTTA", "LIGHT_BLUE_TERRACOTTA",
            "YELLOW_TERRACOTTA", "LIME_TERRACOTTA", "PINK_TERRACOTTA", "GRAY_TERRACOTTA",
            "LIGHT_GRAY_TERRACOTTA", "CYAN_TERRACOTTA", "PURPLE_TERRACOTTA", "BLUE_TERRACOTTA",
            "BROWN_TERRACOTTA", "GREEN_TERRACOTTA", "RED_TERRACOTTA", "BLACK_TERRACOTTA"
        ));
        
        saveConfig(file, config);
    }
    
    private void createStainedGlassPalette() {
        File file = new File(palettesFolder, "stained_glass.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Stained Glass");
        config.set("description", "All colored stained glass blocks");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "WHITE_STAINED_GLASS", "ORANGE_STAINED_GLASS", "MAGENTA_STAINED_GLASS", "LIGHT_BLUE_STAINED_GLASS",
            "YELLOW_STAINED_GLASS", "LIME_STAINED_GLASS", "PINK_STAINED_GLASS", "GRAY_STAINED_GLASS",
            "LIGHT_GRAY_STAINED_GLASS", "CYAN_STAINED_GLASS", "PURPLE_STAINED_GLASS", "BLUE_STAINED_GLASS",
            "BROWN_STAINED_GLASS", "GREEN_STAINED_GLASS", "RED_STAINED_GLASS", "BLACK_STAINED_GLASS"
        ));
        
        saveConfig(file, config);
    }
    
    private void createNaturalStonePalette() {
        File file = new File(palettesFolder, "natural_stone.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Natural Stone");
        config.set("description", "Natural stone blocks including granite, diorite, and andesite");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "STONE", "GRANITE", "POLISHED_GRANITE", "DIORITE", "POLISHED_DIORITE",
            "ANDESITE", "POLISHED_ANDESITE", "COBBLESTONE", "MOSSY_COBBLESTONE",
            "STONE_BRICKS", "MOSSY_STONE_BRICKS", "DEEPSLATE", "COBBLED_DEEPSLATE",
            "POLISHED_DEEPSLATE", "DEEPSLATE_BRICKS", "BLACKSTONE", "POLISHED_BLACKSTONE",
            "POLISHED_BLACKSTONE_BRICKS"
        ));
        
        saveConfig(file, config);
    }
    
    private void createWoodPalette() {
        File file = new File(palettesFolder, "wood.yml");
        if (file.exists()) return;
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Wood Blocks");
        config.set("description", "Wooden planks and logs");
        config.set("enabled", true);
        config.set("blocks", Arrays.asList(
            "OAK_PLANKS", "SPRUCE_PLANKS", "BIRCH_PLANKS", "JUNGLE_PLANKS",
            "ACACIA_PLANKS", "DARK_OAK_PLANKS", "MANGROVE_PLANKS", "CHERRY_PLANKS",
            "BAMBOO_PLANKS", "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG",
            "ACACIA_LOG", "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG", "BAMBOO_BLOCK"
        ));
        
        saveConfig(file, config);
    }
    
    /**
     * 儲存配置檔案
     */
    private void saveConfig(File file, YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration file: " + file.getName(), e);
        }
    }
    
    /**
     * 載入所有列表
     */
    public void loadAllLists() {
        loadExclusionLists();
        loadColorPalettes();
    }
    
    /**
     * 載入排除列表
     */
    private void loadExclusionLists() {
        exclusionLists.clear();
        File[] files = exclusionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                BlockList list = loadBlockList(file);
                if (list != null) {
                    exclusionLists.put(list.getId(), list);
                }
            }
        }
        
        plugin.getLogger().info(String.format("Loaded %d exclusion list(s)", exclusionLists.size()));
    }
    
    /**
     * 載入色表
     */
    private void loadColorPalettes() {
        colorPalettes.clear();
        File[] files = palettesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                BlockList list = loadBlockList(file);
                if (list != null) {
                    colorPalettes.put(list.getId(), list);
                }
            }
        }
        
        plugin.getLogger().info(String.format("Loaded %d palette(s)", colorPalettes.size()));
    }
    
    /**
     * 從檔案載入方塊列表
     */
    private BlockList loadBlockList(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            String id = file.getName().replace(".yml", "");
            String name = config.getString("name", id);
            String description = config.getString("description", "");
            boolean enabled = config.getBoolean("enabled", false);
            List<String> blocks = config.getStringList("blocks");
            
            return new BlockList(id, name, description, enabled, blocks);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load list: " + file.getName(), e);
            return null;
        }
    }
    
    /**
     * 獲取玩家可用的方塊集合
     */
    public Set<Material> getAvailableBlocks(UUID playerUuid) {
        Set<Material> availableBlocks = new HashSet<>();
        
        // 1. 檢查玩家是否選擇了色表
        Set<String> playerPalettes = playerPaletteChoices.get(playerUuid);
        
        if (playerPalettes == null || playerPalettes.isEmpty()) {
            // 使用所有方塊（僅包含可作為物品的方塊）
            for (Material material : Material.values()) {
                if (material.isBlock() && material.isItem()) {
                    availableBlocks.add(material);
                }
            }
        } else {
            // 使用選擇的色表
            for (String paletteId : playerPalettes) {
                BlockList palette = colorPalettes.get(paletteId);
                if (palette != null) {
                    for (String blockName : palette.getBlocks()) {
                        Set<Material> materials = expandBlockPattern(blockName);
                        availableBlocks.addAll(materials);
                    }
                }
            }
        }
        
        // 2. 應用排除規則
        Set<Material> excludedBlocks = getExcludedBlocks(playerUuid);
        availableBlocks.removeAll(excludedBlocks);
        
        return availableBlocks;
    }
    
    /**
     * 獲取排除的方塊集合
     */
    private Set<Material> getExcludedBlocks(UUID playerUuid) {
        Set<Material> excludedBlocks = new HashSet<>();
        
        // 獲取玩家的排除列表選擇
        Set<String> playerExclusions = playerExclusionChoices.get(playerUuid);
        
        // 如果玩家沒有自訂，使用預設啟用的排除列表
        if (playerExclusions == null) {
            playerExclusions = exclusionLists.values().stream()
                .filter(BlockList::isEnabled)
                .map(BlockList::getId)
                .collect(Collectors.toSet());
        }
        
        // 收集所有排除的方塊
        for (String exclusionId : playerExclusions) {
            BlockList exclusion = exclusionLists.get(exclusionId);
            if (exclusion != null) {
                for (String blockName : exclusion.getBlocks()) {
                    Set<Material> materials = expandBlockPattern(blockName);
                    excludedBlocks.addAll(materials);
                }
            }
        }
        
        return excludedBlocks;
    }
    
    /**
     * 展開方塊模式（支援 ALL: 通配符）
     */
    private Set<Material> expandBlockPattern(String pattern) {
        Set<Material> materials = new HashSet<>();
        
        if (pattern.startsWith("ALL:")) {
            // 通配符模式
            String suffix = pattern.substring(4);
            for (Material material : Material.values()) {
                if (material.name().contains(suffix) && material.isBlock() && material.isItem()) {
                    materials.add(material);
                }
            }
        } else {
            // 直接材質名稱
            try {
                Material material = Material.valueOf(pattern);
                // 檢查是否為可用的方塊物品
                if (material.isBlock() && material.isItem()) {
                    materials.add(material);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(String.format("Unknown block type: %s", pattern));
            }
        }
        
        return materials;
    }
    
    // 玩家選擇管理
    public void enablePaletteForPlayer(UUID playerUuid, String paletteId) {
        playerPaletteChoices.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(paletteId);
    }
    
    public void disablePaletteForPlayer(UUID playerUuid, String paletteId) {
        Set<String> palettes = playerPaletteChoices.get(playerUuid);
        if (palettes != null) {
            palettes.remove(paletteId);
        }
    }
    
    public void resetPalettesForPlayer(UUID playerUuid) {
        playerPaletteChoices.remove(playerUuid);
    }
    
    public void enableExclusionForPlayer(UUID playerUuid, String exclusionId) {
        playerExclusionChoices.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(exclusionId);
    }
    
    public void disableExclusionForPlayer(UUID playerUuid, String exclusionId) {
        Set<String> exclusions = playerExclusionChoices.get(playerUuid);
        if (exclusions != null) {
            exclusions.remove(exclusionId);
        }
    }
    
    // Getters
    public Map<String, BlockList> getExclusionLists() {
        return new HashMap<>(exclusionLists);
    }
    
    public Map<String, BlockList> getColorPalettes() {
        return new HashMap<>(colorPalettes);
    }
    
    public Set<String> getPlayerPalettes(UUID playerUuid) {
        Set<String> palettes = playerPaletteChoices.get(playerUuid);
        return palettes != null ? new HashSet<>(palettes) : new HashSet<>();
    }
    
    public Set<String> getPlayerExclusions(UUID playerUuid) {
        Set<String> exclusions = playerExclusionChoices.get(playerUuid);
        return exclusions != null ? new HashSet<>(exclusions) : new HashSet<>();
    }
}
