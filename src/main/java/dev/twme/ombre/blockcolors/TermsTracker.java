package dev.twme.ombre.blockcolors;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

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
                plugin.getLogger().log(Level.SEVERE, "無法建立條款檔案", e);
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
                    plugin.getLogger().warning("無效的 UUID: " + uuidString);
                }
            }
        }
        
        plugin.getLogger().info("已載入 " + acceptedPlayers.size() + " 位玩家的條款接受記錄");
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
        plugin.getLogger().info("玩家 " + playerId + " 已接受 BlockColors 使用條款");
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
            plugin.getLogger().log(Level.SEVERE, "儲存條款檔案失敗", e);
        }
    }

    /**
     * 取得條款文字內容
     * 
     * @return 條款內容字串陣列
     */
    public static String[] getTermsText() {
        return new String[] {
            "§6§l===========================================",
            "§6§l  BlockColors.app 功能使用條款",
            "§6§l===========================================",
            "",
            "§e本功能基於 BlockColors.app 網站的資料。",
            "",
            "§e在使用此功能前，請先查看並同意以下網站的使用條款：",
            "§b§nhttps://blockcolors.app/",
            "",
            "§e資料來源: §b§nhttps://blockcolors.app/",
            "§e網站作者: §fmcndt §7(§b§nhttps://mcndt.dev/§7)",
            "",
            "§a輸入 §f'accept' §a表示您已查看並同意使用條款",
            "§c輸入 §f'decline' §c拒絕並關閉",
            "",
            "§7(60秒內未回應將自動關閉)"
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
