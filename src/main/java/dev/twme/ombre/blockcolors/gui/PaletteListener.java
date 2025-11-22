package dev.twme.ombre.blockcolors.gui;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.twme.ombre.blockcolors.BlockColorsFeature;
import dev.twme.ombre.blockcolors.data.PlayerPalette;

/**
 * 調色盤事件監聽器
 * 負責在玩家登入時載入調色盤、離開時儲存調色盤
 */
public class PaletteListener implements Listener {
    private final BlockColorsFeature feature;

    public PaletteListener(BlockColorsFeature feature) {
        this.feature = feature;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // 載入玩家的調色盤資料
        feature.loadPlayerPalette(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // 儲存玩家的調色盤資料
        PlayerPalette palette = feature.getPlayerPaletteIfExists(playerId);
        if (palette != null) {
            feature.savePlayerPalette(playerId);
        }
        
        // 清除記憶體中的調色盤資料
        feature.unloadPlayerPalette(playerId);
    }
}
