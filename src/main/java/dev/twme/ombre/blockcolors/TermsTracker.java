package dev.twme.ombre.blockcolors;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.i18n.MessageManager;

/**
 * 使用條款追蹤器
 * 追蹤並記錄玩家是否已接受使用條款
 */
public class TermsTracker {
    private final JavaPlugin plugin;
    private final Set<UUID> acceptedPlayers;
    private static final String TERMS_FILE = "blockcolors_terms.yml";
    private static final String TERMS_VERSION = "1.0";
    
    private File termsFile;
    private YamlConfiguration config;

    public TermsTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.acceptedPlayers = new HashSet<>();
        this.termsFile = new File(plugin.getDataFolder(), TERMS_FILE);
        
        loadTermsData();
    }

    /**
     * 載入條款資料
     */
    private void loadTermsData() {
        if (!termsFile.exists()) {
            try {
                termsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Unable to create terms file", e);
                return;
            }
        }
        
        config = YamlConfiguration.loadConfiguration(termsFile);
        
        // 載入已接受的玩家列表
        if (config.contains("accepted_players")) {
            for (String uuidString : config.getStringList("accepted_players")) {
                try {
                    acceptedPlayers.add(UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID: " + uuidString);
                }
            }
        }
        
        plugin.getLogger().info("Loaded terms acceptance records for " + acceptedPlayers.size() + " players");
    }

    /**
     * 檢查玩家是否已接受條款
     * 
     * @param playerId 玩家 UUID
     * @return 是否已接受
     */
    public boolean hasAcceptedTerms(UUID playerId) {
        return acceptedPlayers.contains(playerId);
    }

    /**
     * 記錄玩家接受條款
     * 
     * @param playerId 玩家 UUID
     */
    public void acceptTerms(UUID playerId) {
        acceptedPlayers.add(playerId);
        saveTermsData();
        plugin.getLogger().info("Player " + playerId + " has accepted BlockColors terms of service");
    }

    /**
     * 儲存條款資料
     */
    public void saveTermsData() {
        try {
            config.set("version", "1.0.0");
            config.set("terms_version", TERMS_VERSION);
            
            // 儲存 UUID 列表
            config.set("accepted_players", 
                acceptedPlayers.stream()
                    .map(UUID::toString)
                    .toList()
            );
            
            config.save(termsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save terms file", e);
        }
    }

    /**
     * 取得條款文字內容
     * 
     * @param plugin Ombre plugin instance for MessageManager
     * @return 條款內容字串陣列
     */
    public static String[] getTermsText(Ombre plugin) {
        MessageManager msg = plugin.getMessageManager();
        
        return new String[] {
            msg.getMessage("terms.blockcolors.title"),
            msg.getMessage("terms.blockcolors.subtitle"),
            msg.getMessage("terms.blockcolors.separator"),
            "",
            msg.getMessage("terms.blockcolors.line1"),
            "",
            msg.getMessage("terms.blockcolors.line2"),
            msg.getMessage("terms.blockcolors.website"),
            "",
            msg.getMessage("terms.blockcolors.data-source"),
            msg.getMessage("terms.blockcolors.author"),
            "",
            msg.getMessage("terms.blockcolors.accept"),
            msg.getMessage("terms.blockcolors.decline"),
            "",
            msg.getMessage("terms.blockcolors.timeout")
        };
    }

    /**
     * 清除所有條款記錄
     */
    public void clearAll() {
        acceptedPlayers.clear();
        saveTermsData();
    }

    /**
     * 取得已接受條款的玩家數量
     */
    public int getAcceptedCount() {
        return acceptedPlayers.size();
    }
}
