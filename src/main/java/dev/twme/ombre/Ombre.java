package dev.twme.ombre;

import org.bukkit.plugin.java.JavaPlugin;

import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockpalettes.BlockPalettesFeature;
import dev.twme.ombre.color.ColorDataGenerator;
import dev.twme.ombre.color.ColorService;
import dev.twme.ombre.command.CommandHandler;
import dev.twme.ombre.gui.GUIManager;
import dev.twme.ombre.manager.ConfigManager;
import dev.twme.ombre.palette.BlockFilterManager;

public final class Ombre extends JavaPlugin {
    
    private ColorDataGenerator colorDataGenerator;
    private ColorService colorService;
    private ConfigManager configManager;
    private BlockFilterManager blockFilterManager;
    private GUIManager guiManager;
    private CommandHandler commandHandler;
    private BlockColorsFeature blockColorsFeature;
    private BlockPalettesFeature blockPalettesFeature;

    @Override
    public void onEnable() {
        // 儲存預設配置
        saveDefaultConfig();
        
        // 初始化顏色資料生成器
        colorDataGenerator = new ColorDataGenerator(this);
        
        // 生成 colors.yml（如果不存在）
        int retryCount = getConfig().getInt("error-handling.color-generation-retry", 3);
        boolean colorGenerated = false;
        
        for (int i = 0; i < retryCount; i++) {
            if (colorDataGenerator.generateIfNotExists()) {
                colorGenerated = true;
                break;
            }
            getLogger().warning(String.format("顏色檔案生成失敗，重試 %d/%d", i + 1, retryCount));
        }
        
        if (!colorGenerated) {
            getLogger().severe("無法生成 colors.yml，插件可能無法正常運作");
            if (!getConfig().getBoolean("error-handling.regenerate-on-corruption", true)) {
                getLogger().severe("已禁用自動重新生成，插件將停用");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
        
        // 初始化顏色服務
        colorService = new ColorService(this);
        if (!colorService.loadColorsFromFile()) {
            getLogger().warning("載入顏色檔案失敗，將使用動態顏色獲取");
        }
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 初始化方塊過濾管理器
        blockFilterManager = new BlockFilterManager(this);
        blockFilterManager.initializeDefaults();
        
        // 初始化 GUI 管理器
        guiManager = new GUIManager(this);
        
        // 初始化指令處理器
        commandHandler = new CommandHandler(this, guiManager, configManager);
        
        // 註冊指令
        getCommand("ombre").setExecutor(commandHandler);
        getCommand("ombre").setTabCompleter(commandHandler);
        
        // 初始化 BlockColors 功能
        blockColorsFeature = new BlockColorsFeature(this);
        blockColorsFeature.initialize().thenAccept(success -> {
            if (success) {
                getLogger().info("BlockColors 功能已啟用");
                
                // 註冊 BlockColors 指令
                getCommand("blockcolorsapp").setExecutor(blockColorsFeature.getCommandHandler());
                getCommand("blockcolorsapp").setTabCompleter(blockColorsFeature.getCommandHandler());
            } else {
                getLogger().warning("BlockColors 功能初始化失敗");
            }
        });
        
        // 初始化 BlockPalettes 功能
        blockPalettesFeature = new BlockPalettesFeature(this);
        blockPalettesFeature.initialize().thenAccept(success -> {
            if (success) {
                getLogger().info("Block Palettes 功能已啟用");
                
                // 註冊 Block Palettes 指令
                getCommand("blockpalettes").setExecutor(blockPalettesFeature.getCommandHandler());
                getCommand("blockpalettes").setTabCompleter(blockPalettesFeature.getCommandHandler());
            } else {
                getLogger().warning("Block Palettes 功能初始化失敗");
            }
        });
        
        getLogger().info("Ombre 插件已啟用！");
    }

    @Override
    public void onDisable() {
        // 關閉 BlockColors 功能
        if (blockColorsFeature != null) {
            blockColorsFeature.shutdown();
        }
        
        // 關閉 BlockPalettes 功能
        if (blockPalettesFeature != null) {
            blockPalettesFeature.shutdown();
        }
        
        // 清理 GUI
        if (guiManager != null) {
            guiManager.cleanup();
        }
        
        getLogger().info("Ombre 插件已停用！");
    }
    
    // Getters
    public ColorDataGenerator getColorDataGenerator() {
        return colorDataGenerator;
    }
    
    public ColorService getColorService() {
        return colorService;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public GUIManager getGuiManager() {
        return guiManager;
    }
    
    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
    
    public BlockFilterManager getBlockFilterManager() {
        return blockFilterManager;
    }
    
    public BlockColorsFeature getBlockColorsFeature() {
        return blockColorsFeature;
    }
    
    public BlockPalettesFeature getBlockPalettesFeature() {
        return blockPalettesFeature;
    }
}
