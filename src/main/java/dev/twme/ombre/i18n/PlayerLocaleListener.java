package dev.twme.ombre.i18n;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for handling player locale changes
 * Automatically updates each player's language based on their client settings
 */
public class PlayerLocaleListener implements Listener {
    
    private final MessageManager messageManager;
    
    public PlayerLocaleListener(MessageManager messageManager) {
        this.messageManager = messageManager;
    }
    
    /**
     * When a player joins, set their locale based on their client language
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String clientLocale = player.locale().toString();
        
        // Set player's locale in MessageManager
        messageManager.setPlayerLocale(player, clientLocale);
    }
    
    /**
     * When a player changes their language in-game, update their locale
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        Player player = event.getPlayer();
        String newLocale = event.locale().toString();
        
        // Update player's locale in MessageManager
        messageManager.setPlayerLocale(player, newLocale);
    }
    
    /**
     * When a player quits, remove their locale data to save memory
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Remove player's locale from MessageManager
        messageManager.removePlayerLocale(player);
    }
}
