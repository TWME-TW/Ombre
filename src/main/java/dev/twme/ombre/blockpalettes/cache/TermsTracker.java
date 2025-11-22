package dev.twme.ombre.blockpalettes.cache;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.twme.ombre.Ombre;

/**
 * 條款同意追蹤器
 * 管理玩家對 Block Palettes 使用條款的同意狀態
 */
public class TermsTracker {
    
    private final Ombre plugin;
    private final Map<UUID, TermsAcceptanceData> acceptanceRecords;
    private final File dataFolder;
    private final Gson gson;
    private final String currentVersion;
    private final boolean recordIp;
    
    public TermsTracker(Ombre plugin) {
        this.plugin = plugin;
        this.acceptanceRecords = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // 從設定讀取
        this.currentVersion = plugin.getConfig().getString(
            "block-palettes.terms.version", 
            TermsAcceptanceData.CURRENT_TERMS_VERSION
        );
        this.recordIp = plugin.getConfig().getBoolean("block-palettes.terms.record-ip", false);
        
        // 確保資料目錄存在
        this.dataFolder = new File(plugin.getDataFolder(), "blockpalettes/players");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    /**
     * 檢查玩家是否已同意條款
     */
    public boolean hasAgreed(UUID playerUuid) {
        if (!acceptanceRecords.containsKey(playerUuid)) {
            loadPlayerData(playerUuid);
        }
        
        TermsAcceptanceData data = acceptanceRecords.get(playerUuid);
        if (data == null) {
            return false;
        }
        
        // 檢查是否同意且版本匹配
        boolean agreed = data.isAgreedToTerms() && currentVersion.equals(data.getTermsVersion());
        
        return agreed;
    }
    
    /**
     * 記錄玩家同意條款
     */
    public void recordAgreement(UUID playerUuid, String ipAddress) {
        TermsAcceptanceData data = new TermsAcceptanceData();
        data.setPlayerUuid(playerUuid);
        data.setAgreedToTerms(true);
        data.setAgreedAt(System.currentTimeMillis());
        data.setTermsVersion(currentVersion);
        
        if (recordIp && ipAddress != null) {
            data.setIpAddress(ipAddress);
        }
        
        // 記錄 Minecraft 版本
        data.setMinecraftVersion(plugin.getServer().getVersion());
        
        acceptanceRecords.put(playerUuid, data);
        savePlayerData(playerUuid, data);
    }
    
    /**
     * 撤銷同意（用於管理員操作）
     */
    public void revokeAgreement(UUID playerUuid) {
        acceptanceRecords.remove(playerUuid);
        File playerFile = new File(dataFolder, playerUuid + ".json");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }
    
    /**
     * 從檔案載入玩家資料
     */
    private void loadPlayerData(UUID playerUuid) {
        File playerFile = new File(dataFolder, playerUuid + ".json");
        if (!playerFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(playerFile)) {
            TermsAcceptanceData data = gson.fromJson(reader, TermsAcceptanceData.class);
            if (data != null) {
                acceptanceRecords.put(playerUuid, data);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load terms data for player " + playerUuid + ": " + e.getMessage());
        }
    }
    
    /**
     * 儲存玩家資料到檔案
     */
    private void savePlayerData(UUID playerUuid, TermsAcceptanceData data) {
        File playerFile = new File(dataFolder, playerUuid + ".json");
        
        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save terms data for player " + playerUuid + ": " + e.getMessage());
        }
    }
    
    /**
     * 取得同意統計資料
     */
    public Map<String, Object> getStatistics() {
        long totalAgreed = acceptanceRecords.values().stream()
            .filter(TermsAcceptanceData::isAgreedToTerms)
            .count();
        
        return Map.of(
            "total_agreed", totalAgreed,
            "current_version", currentVersion,
            "terms_url", TermsAcceptanceData.TERMS_URL,
            "privacy_url", TermsAcceptanceData.PRIVACY_URL
        );
    }
    
    /**
     * 取得當前條款版本
     */
    public String getCurrentVersion() {
        return currentVersion;
    }
}
