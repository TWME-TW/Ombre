package dev.twme.ombre.blockpalettes.cache;

import java.util.UUID;

/**
 * 條款同意資料模型
 */
public class TermsAcceptanceData {
    
    public static final String CURRENT_TERMS_VERSION = "1.0";
    public static final String TERMS_URL = "https://www.blockpalettes.com/terms";
    public static final String PRIVACY_URL = "https://www.blockpalettes.com/privacy";
    
    private UUID playerUuid;
    private boolean agreedToTerms;
    private long agreedAt;           // Unix timestamp
    private String termsVersion;     // 條款版本 (如 "1.0")
    private String ipAddress;        // 可選：記錄 IP
    private String minecraftVersion; // 可選：Minecraft 版本
    
    public TermsAcceptanceData() {
    }
    
    public TermsAcceptanceData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.agreedToTerms = false;
        this.agreedAt = 0;
        this.termsVersion = "";
    }
    
    // Getters and Setters
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    public boolean isAgreedToTerms() {
        return agreedToTerms;
    }
    
    public void setAgreedToTerms(boolean agreedToTerms) {
        this.agreedToTerms = agreedToTerms;
    }
    
    public long getAgreedAt() {
        return agreedAt;
    }
    
    public void setAgreedAt(long agreedAt) {
        this.agreedAt = agreedAt;
    }
    
    public String getTermsVersion() {
        return termsVersion;
    }
    
    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
    
    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }
}
