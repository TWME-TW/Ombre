package dev.twme.ombre.example;

import dev.twme.ombre.Ombre;
import dev.twme.ombre.i18n.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example class demonstrating MessageManager usage with MiniMessage format
 * 
 * This is a reference implementation showing best practices for:
 * - Command message handling
 * - GUI creation with i18n
 * - Variable replacement
 */
public class MessageManagerExample implements CommandExecutor, Listener {
    
    private final Ombre plugin;
    private final MessageManager msg;
    
    public MessageManagerExample(Ombre plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
    }
    
    // ============================================
    // Example 1: Simple Command Messages
    // ============================================
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            // Send message to console/command block
            sender.sendMessage(msg.getMessage("general.player-only"));
            return true;
        }
        
        // Check permission
        if (!player.hasPermission("ombre.example")) {
            msg.sendMessage(player, "general.no-permission");
            return true;
        }
        
        // Send success message
        msg.sendMessage(player, "general.reload-success");
        
        return true;
    }
    
    // ============================================
    // Example 2: Messages with Single Variable
    // ============================================
    
    public void sendGradientSavedMessage(Player player, String gradientName) {
        // Using key-value pairs for named placeholders
        msg.sendMessage(player, "messages.gradient.saved", 
            "name", gradientName);
    }
    
    // ============================================
    // Example 3: Messages with Multiple Variables
    // ============================================
    
    public void sendPaletteFullMessage(Player player, int current, int max) {
        msg.sendMessage(player, "gui.blockcolors.palette.full",
            "current", current,
            "max", max);
    }
    
    // ============================================
    // Example 4: Messages with Map (Complex)
    // ============================================
    
    public void sendDetailedStats(Player player, int gradientCount, String lastCreated) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("count", gradientCount);
        placeholders.put("name", lastCreated);
        placeholders.put("date", java.time.LocalDate.now().toString());
        
        msg.sendMessage(player, "messages.stats.summary", placeholders);
    }
    
    // ============================================
    // Example 5: GUI with i18n
    // ============================================
    
    public static class ExampleGUI implements InventoryHolder {
        private final Inventory inventory;
        private final MessageManager msg;
        private final Player player;
        
        public ExampleGUI(Ombre plugin, Player player) {
            this.msg = plugin.getMessageManager();
            this.player = player;
            
            // Create inventory with translated title
            Component title = msg.getComponent("gui.ombre.title");
            this.inventory = Bukkit.createInventory(this, 54, title);
            
            // Setup GUI items
            setupItems();
        }
        
        private void setupItems() {
            // Example: Save button
            ItemStack saveButton = createButton(
                Material.BOOK,
                "gui.ombre.buttons.save",
                "gui.ombre.lore.save"
            );
            inventory.setItem(10, saveButton);
            
            // Example: Delete button
            ItemStack deleteButton = createButton(
                Material.BARRIER,
                "gui.ombre.buttons.delete",
                "gui.ombre.lore.delete"
            );
            inventory.setItem(11, deleteButton);
            
            // Example: Item with variables
            ItemStack infoItem = createInfoItem();
            inventory.setItem(13, infoItem);
        }
        
        private ItemStack createButton(Material material, String nameKey, String loreKey) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            // Set display name using MiniMessage
            meta.displayName(msg.getComponent(nameKey));
            
            // Set lore
            List<Component> lore = new ArrayList<>();
            lore.add(msg.getComponent(loreKey));
            meta.lore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        private ItemStack createInfoItem() {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            
            // Display name with variable
            meta.displayName(msg.getComponent("items.config.creator", 
                "creator", player.getName()));
            
            // Lore with multiple variables
            List<Component> lore = new ArrayList<>();
            lore.add(msg.getComponent("items.config.blocks", "count", 10));
            lore.add(msg.getComponent("items.config.favorites", "count", 5));
            lore.add(msg.getComponent("items.config.published"));
            
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }
        
        public void open() {
            player.openInventory(inventory);
        }
        
        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
    
    // ============================================
    // Example 6: Event Handler with i18n
    // ============================================
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!(event.getInventory().getHolder() instanceof ExampleGUI)) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        // Handle different button clicks
        switch (clicked.getType()) {
            case BOOK:
                // Save button clicked
                msg.sendMessage(player, "messages.gradient.saved", "name", "Example");
                player.closeInventory();
                break;
                
            case BARRIER:
                // Delete button clicked
                msg.sendMessage(player, "messages.gradient.deleted");
                player.closeInventory();
                break;
                
            default:
                break;
        }
    }
    
    // ============================================
    // Example 7: Prefix Messages
    // ============================================
    
    public void sendPrefixedMessage(Player player, String messageKey) {
        // Sends message with the plugin prefix
        msg.sendMessageWithPrefix(player, messageKey);
    }
    
    // ============================================
    // Example 8: Multi-line Messages
    // ============================================
    
    public void sendHelp(Player player) {
        // Send multiple help lines
        Component title = msg.getComponent("commands.ombre.help.title");
        player.sendMessage(title);
        
        player.sendMessage(msg.getComponent("commands.ombre.help.main"));
        player.sendMessage(msg.getComponent("commands.ombre.help.library"));
        player.sendMessage(msg.getComponent("commands.ombre.help.favorites"));
        player.sendMessage(msg.getComponent("commands.ombre.help.my"));
    }
    
    // ============================================
    // Example 9: Conditional Messages
    // ============================================
    
    public void sendStatusMessage(Player player, boolean published) {
        String key = published ? "items.config.published" : "items.config.not-published";
        msg.sendMessage(player, key);
    }
    
    // ============================================
    // Example 10: Error Handling
    // ============================================
    
    public void sendErrorMessage(Player player, String errorType, String details) {
        switch (errorType) {
            case "file":
                msg.sendMessage(player, "errors.file.load-failed", "file", details);
                break;
            case "api":
                msg.sendMessage(player, "errors.api.connection-failed");
                break;
            case "permission":
                msg.sendMessage(player, "general.no-permission");
                break;
            default:
                msg.sendMessage(player, "general.unknown-command");
                break;
        }
    }
    
    // ============================================
    // Example 11: Getting Raw Message Strings
    // ============================================
    
    public String getMessageString(String key) {
        // Get message as string (useful for non-player contexts like console)
        return msg.getMessage(key);
    }
    
    // ============================================
    // Example 12: Component Lists for Lore
    // ============================================
    
    public ItemStack createItemWithMultiLineLore(String configKey) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        // Get multi-line lore from config
        List<Component> lore = msg.getComponentList(configKey);
        meta.lore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    // ============================================
    // Example 13: Dynamic Locale
    // ============================================
    
    public void demonstrateLanguageReload() {
        // Reload current language (useful after config changes)
        msg.reload();
        
        // Get current locale
        String currentLocale = msg.getCurrentLocale();
        plugin.getLogger().info("Current locale: " + currentLocale);
    }
}
