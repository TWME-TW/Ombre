package dev.twme.ombre.blockcolors.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 調色盤資料管理器
 * 負責儲存和載入玩家的調色盤資料
 */
public class PaletteDataManager {
    private final JavaPlugin plugin;
    private final File paletteFolder;

    public PaletteDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.paletteFolder = new File(plugin.getDataFolder(), "palettes");
        
        // 確保資料夾存在
        if (!paletteFolder.exists()) {
            paletteFolder.mkdirs();
        }
    }

    /**
     * 儲存玩家的調色盤資料
     */
    public void savePalette(UUID playerId, PlayerPalette palette) {
        File paletteFile = new File(paletteFolder, playerId.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        // 將方塊列表轉換為字串列表
        List<String> blockNames = new ArrayList<>();
        for (Material material : palette.getBlocks()) {
            blockNames.add(material.name());
        }
        
        config.set("blocks", blockNames);
        config.set("max-size", palette.getMaxSize());
        
        try {
            config.save(paletteFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, 
                "Failed to save palette for player " + playerId, e);
        }
    }

    /**
     * 載入玩家的調色盤資料
     */
    public PlayerPalette loadPalette(UUID playerId, int defaultMaxSize) {
        File paletteFile = new File(paletteFolder, playerId.toString() + ".yml");
        PlayerPalette palette = new PlayerPalette(defaultMaxSize);
        
        if (!paletteFile.exists()) {
            return palette;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(paletteFile);
            
            // 載入最大容量
            int maxSize = config.getInt("max-size", defaultMaxSize);
            PlayerPalette loadedPalette = new PlayerPalette(maxSize);
            
            // 載入方塊列表
            List<String> blockNames = config.getStringList("blocks");
            List<Material> blocks = new ArrayList<>();
            
            for (String blockName : blockNames) {
                try {
                    Material material = Material.valueOf(blockName);
                    blocks.add(material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(
                        "Invalid material in palette for player " + playerId + ": " + blockName);
                }
            }
            
            loadedPalette.setBlocks(blocks);
            return loadedPalette;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "Failed to load palette for player " + playerId, e);
            return palette;
        }
    }

    /**
     * 刪除玩家的調色盤資料
     */
    public boolean deletePalette(UUID playerId) {
        File paletteFile = new File(paletteFolder, playerId.toString() + ".yml");
        if (paletteFile.exists()) {
            return paletteFile.delete();
        }
        return false;
    }

    /**
     * 檢查玩家是否有調色盤資料
     */
    public boolean hasPalette(UUID playerId) {
        File paletteFile = new File(paletteFolder, playerId.toString() + ".yml");
        return paletteFile.exists();
    }

    /**
     * 取得調色盤資料夾
     */
    public File getPaletteFolder() {
        return paletteFolder;
    }
}
