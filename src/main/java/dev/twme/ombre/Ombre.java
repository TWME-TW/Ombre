package dev.twme.ombre;

import org.bukkit.plugin.java.JavaPlugin;

import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockpalettes.BlockPalettesFeature;
import dev.twme.ombre.color.ColorDataGenerator;
import dev.twme.ombre.color.ColorService;
import dev.twme.ombre.command.CommandHandler;
import dev.twme.ombre.gui.GUIManager;
import dev.twme.ombre.i18n.MessageManager;
import dev.twme.ombre.i18n.PlayerLocaleListener;
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
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        // 儲存預設配置
        saveDefaultConfig();
        
        // 初始化訊息管理器
        messageManager = new MessageManager(this);
        
        // 註冊玩家語言監聽器
        getServer().getPluginManager().registerEvents(new PlayerLocaleListener(messageManager), this);
        
        // 初始化顏色資料生成器
        colorDataGenerator = new ColorDataGenerator(this);
        
        // 如果不存在則生成 colors.yml
        int retryCount = getConfig().getInt("error-handling.color-generation-retry", 3);
        boolean colorGenerated = false;
        
        for (int i = 0; i < retryCount; i++) {
            if (colorDataGenerator.generateIfNotExists()) {
                colorGenerated = true;
                break;
            }
            getLogger().warning(String.format("Color file generation failed, retry %d/%d", i + 1, retryCount));
        }
        
        if (!colorGenerated) {
            getLogger().severe("Failed to generate colors.yml, plugin may not work properly");
            if (!getConfig().getBoolean("error-handling.regenerate-on-corruption", true)) {
                getLogger().severe("Auto-regeneration disabled, plugin will be disabled");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
        
        // 初始化顏色服務
        colorService = new ColorService(this);
        if (!colorService.loadColorsFromFile()) {
            getLogger().warning("Failed to load color file, will use dynamic color retrieval");
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
                getLogger().info("BlockColors feature enabled");
                
                // 註冊 BlockColors 指令
                getCommand("blockcolorsapp").setExecutor(blockColorsFeature.getCommandHandler());
                getCommand("blockcolorsapp").setTabCompleter(blockColorsFeature.getCommandHandler());
            } else {
                getLogger().warning("BlockColors feature initialization failed");
            }
        });
        
        // 初始化 BlockPalettes 功能
        blockPalettesFeature = new BlockPalettesFeature(this);
        blockPalettesFeature.initialize().thenAccept(success -> {
            if (success) {
                getLogger().info("Block Palettes feature enabled");
                
                // 註冊 Block Palettes 指令
                getCommand("blockpalettes").setExecutor(blockPalettesFeature.getCommandHandler());
                getCommand("blockpalettes").setTabCompleter(blockPalettesFeature.getCommandHandler());
            } else {
                getLogger().warning("Block Palettes feature initialization failed");
            }
        });
        
        getLogger().info("Ombre plugin enabled!");
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
        
        getLogger().info("Ombre plugin disabled!");
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
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
}
