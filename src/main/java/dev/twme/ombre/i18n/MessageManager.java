package dev.twme.ombre.i18n;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Message manager for handling internationalization (i18n)
 * Uses MiniMessage format for all messages
 */
public class MessageManager {
    
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private FileConfiguration languageConfig;
    private String currentLocale;
    private final Map<String, FileConfiguration> loadedLanguages;
    private final Map<UUID, String> playerLocales;
    
    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.loadedLanguages = new HashMap<>();
        this.playerLocales = new HashMap<>();
        
        // Initialize language files
        initializeLanguageFiles();
        
        // Load default language from config (for console)
        String configLocale = plugin.getConfig().getString("settings.language", "en_US");
        loadLanguage(configLocale);
    }
    
    /**
     * Initialize language files (copy from JAR if not exists)
     */
    private void initializeLanguageFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // Copy default language files from JAR
        String[] defaultLanguages = {"en_US.yml", "zh_TW.yml"};
        for (String langFile : defaultLanguages) {
            File file = new File(langDir, langFile);
            if (!file.exists()) {
                try (InputStream in = plugin.getResource("lang/" + langFile)) {
                    if (in != null) {
                        Files.copy(in, file.toPath());
                        plugin.getLogger().info("Created language file: " + langFile);
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to create language file: " + langFile, e);
                }
            }
        }
    }
    
    /**
     * Load a language file
     * @param locale Language code (e.g., "en_US", "zh_TW")
     * @return true if loaded successfully
     */
    public boolean loadLanguage(String locale) {
        // Check if already loaded
        if (loadedLanguages.containsKey(locale)) {
            this.languageConfig = loadedLanguages.get(locale);
            this.currentLocale = locale;
            return true;
        }
        
        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + locale + ".yml, falling back to en_US");
            // Fall back to en_US
            if (!locale.equals("en_US")) {
                return loadLanguage("en_US");
            }
            return false;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            loadedLanguages.put(locale, config);
            this.languageConfig = config;
            this.currentLocale = locale;
            plugin.getLogger().info("Loaded language: " + locale);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load language file: " + locale + ".yml", e);
            return false;
        }
    }
    
    /**
     * Reload current language
     */
    public void reload() {
        loadedLanguages.clear();
        String configLocale = plugin.getConfig().getString("settings.language", "en_US");
        loadLanguage(configLocale);
    }
    
    /**
     * Get a message by key (for console, uses default language)
     * @param key Message key (e.g., "general.no-permission")
     * @return Message string
     */
    public String getMessage(String key) {
        if (languageConfig == null) {
            return key;
        }
        
        String message = languageConfig.getString(key);
        if (message == null) {
            plugin.getLogger().warning("Missing translation key: " + key);
            return key;
        }
        
        return message;
    }
    
    /**
     * Get a message by key for a specific player
     * @param key Message key
     * @param player Player (uses player's locale)
     * @return Message string
     */
    public String getMessage(String key, Player player) {
        String locale = getPlayerLocale(player);
        FileConfiguration config = loadedLanguages.get(locale);
        
        if (config == null) {
            return getMessage(key);
        }
        
        String message = config.getString(key);
        if (message == null) {
            plugin.getLogger().warning("Missing translation key: " + key + " for locale: " + locale);
            return key;
        }
        
        return message;
    }
    
    /**
     * Get a message with variable replacements for a specific player
     * @param key Message key
     * @param player Player (uses player's locale)
     * @param replacements Variable replacements (pairs of placeholder name and value)
     * @return Formatted message string
     */
    public String getMessage(String key, Player player, Object... replacements) {
        String message = getMessage(key, player);
        
        if (replacements.length > 0) {
            // If odd number of arguments, treat as simple indexed replacements
            if (replacements.length % 2 != 0) {
                for (int i = 0; i < replacements.length; i++) {
                    message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
                }
            } else {
                // Treat as key-value pairs
                for (int i = 0; i < replacements.length; i += 2) {
                    String placeholder = String.valueOf(replacements[i]);
                    String value = String.valueOf(replacements[i + 1]);
                    message = message.replace("<" + placeholder + ">", value);
                }
            }
        }
        
        return message;
    }
    
    /**
     * Get a message with named variable replacements for a specific player
     * @param key Message key
     * @param player Player (uses player's locale)
     * @param placeholders Map of placeholder names to values
     * @return Formatted message string
     */
    public String getMessage(String key, Player player, Map<String, Object> placeholders) {
        String message = getMessage(key, player);
        
        if (placeholders != null) {
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                message = message.replace("<" + entry.getKey() + ">", String.valueOf(entry.getValue()));
            }
        }
        
        return message;
    }
    
    /**
     * Get a Component from a message key using MiniMessage (for console)
     * @param key Message key
     * @return Adventure Component
     */
    public Component getComponent(String key) {
        String message = getMessage(key);
        return miniMessage.deserialize(message);
    }
    
    /**
     * Get a Component from a message key for a specific player
     * @param key Message key
     * @param player Player (uses player's locale)
     * @return Adventure Component
     */
    public Component getComponent(String key, Player player) {
        String message = getMessage(key, player);
        return miniMessage.deserialize(message);
    }
    
    /**
     * Get a Component with variable replacements using MiniMessage (for console)
     * @param key Message key
     * @param replacements Variable replacements (pairs of placeholder name and value)
     * @return Adventure Component
     */
    public Component getComponent(String key, Object... replacements) {
        // Check if first parameter is a Player
        if (replacements.length > 0 && replacements[0] instanceof Player) {
            Player player = (Player) replacements[0];
            Object[] actualReplacements = new Object[replacements.length - 1];
            System.arraycopy(replacements, 1, actualReplacements, 0, actualReplacements.length);
            return getComponentForPlayer(key, player, actualReplacements);
        }
        
        String message = getMessage(key);
        
        if (replacements.length == 0) {
            return miniMessage.deserialize(message);
        }
        
        // Build tag resolvers for MiniMessage
        List<TagResolver> resolvers = new ArrayList<>();
        
        // If odd number of arguments, treat as simple indexed replacements
        if (replacements.length % 2 != 0) {
            for (int i = 0; i < replacements.length; i++) {
                resolvers.add(Placeholder.unparsed(String.valueOf(i), String.valueOf(replacements[i])));
            }
        } else {
            // Treat as key-value pairs
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = String.valueOf(replacements[i]);
                String value = String.valueOf(replacements[i + 1]);
                resolvers.add(Placeholder.unparsed(placeholder, value));
            }
        }
        
        return miniMessage.deserialize(message, TagResolver.resolver(resolvers));
    }
    
    /**
     * Get a Component with variable replacements for a specific player
     * @param key Message key
     * @param player Player
     * @param replacements Variable replacements
     * @return Adventure Component
     */
    private Component getComponentForPlayer(String key, Player player, Object... replacements) {
        String message = getMessage(key, player);
        
        if (replacements.length == 0) {
            return miniMessage.deserialize(message);
        }
        
        // Build tag resolvers for MiniMessage
        List<TagResolver> resolvers = new ArrayList<>();
        
        // If odd number of arguments, treat as simple indexed replacements
        if (replacements.length % 2 != 0) {
            for (int i = 0; i < replacements.length; i++) {
                resolvers.add(Placeholder.unparsed(String.valueOf(i), String.valueOf(replacements[i])));
            }
        } else {
            // Treat as key-value pairs
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = String.valueOf(replacements[i]);
                String value = String.valueOf(replacements[i + 1]);
                resolvers.add(Placeholder.unparsed(placeholder, value));
            }
        }
        
        return miniMessage.deserialize(message, TagResolver.resolver(resolvers));
    }
    
    /**
     * Get a Component with named variable replacements using MiniMessage (for console)
     * @param key Message key
     * @param placeholders Map of placeholder names to values
     * @return Adventure Component
     */
    public Component getComponent(String key, Map<String, Object> placeholders) {
        String message = getMessage(key);
        
        if (placeholders == null || placeholders.isEmpty()) {
            return miniMessage.deserialize(message);
        }
        
        // Build tag resolvers for MiniMessage
        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            resolvers.add(Placeholder.unparsed(entry.getKey(), String.valueOf(entry.getValue())));
        }
        
        return miniMessage.deserialize(message, TagResolver.resolver(resolvers));
    }
    
    /**
     * Get a Component with named variable replacements for a specific player
     * @param key Message key
     * @param player Player
     * @param placeholders Map of placeholder names to values
     * @return Adventure Component
     */
    public Component getComponent(String key, Player player, Map<String, Object> placeholders) {
        String message = getMessage(key, player);
        
        if (placeholders == null || placeholders.isEmpty()) {
            return miniMessage.deserialize(message);
        }
        
        // Build tag resolvers for MiniMessage
        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            resolvers.add(Placeholder.unparsed(entry.getKey(), String.valueOf(entry.getValue())));
        }
        
        return miniMessage.deserialize(message, TagResolver.resolver(resolvers));
    }
    
    /**
     * Get a list of messages
     * @param key Message key (should point to a list in the YAML)
     * @return List of message strings
     */
    public List<String> getMessageList(String key) {
        if (languageConfig == null) {
            return new ArrayList<>();
        }
        
        List<String> messages = languageConfig.getStringList(key);
        if (messages.isEmpty()) {
            plugin.getLogger().warning("Missing translation key or empty list: " + key);
        }
        
        return messages;
    }
    
    /**
     * Get a list of Components using MiniMessage
     * @param key Message key
     * @return List of Adventure Components
     */
    public List<Component> getComponentList(String key) {
        List<String> messages = getMessageList(key);
        List<Component> components = new ArrayList<>();
        
        for (String message : messages) {
            components.add(miniMessage.deserialize(message));
        }
        
        return components;
    }
    
    /**
     * Get a list of Components with placeholders using MiniMessage
     * @param key Message key
     * @param placeholders Map of placeholder names to values
     * @return List of Adventure Components
     */
    public List<Component> getComponentList(String key, Map<String, Object> placeholders) {
        List<String> messages = getMessageList(key);
        List<Component> components = new ArrayList<>();
        
        if (placeholders == null || placeholders.isEmpty()) {
            for (String message : messages) {
                components.add(miniMessage.deserialize(message));
            }
        } else {
            // Build tag resolvers
            List<TagResolver> resolvers = new ArrayList<>();
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                resolvers.add(Placeholder.unparsed(entry.getKey(), String.valueOf(entry.getValue())));
            }
            TagResolver resolver = TagResolver.resolver(resolvers);
            
            for (String message : messages) {
                components.add(miniMessage.deserialize(message, resolver));
            }
        }
        
        return components;
    }
    
    /**
     * Get a list of Components with placeholders for a specific player using MiniMessage
     * @param key Message key
     * @param player Player (uses player's locale)
     * @param placeholders Map of placeholder names to values
     * @return List of Adventure Components
     */
    public List<Component> getComponentList(String key, Player player, Map<String, Object> placeholders) {
        String locale = getPlayerLocale(player);
        FileConfiguration config = loadedLanguages.get(locale);
        
        if (config == null) {
            return getComponentList(key, placeholders);
        }
        
        List<String> messages = config.getStringList(key);
        if (messages.isEmpty()) {
            plugin.getLogger().warning("Missing translation key or empty list: " + key + " for locale: " + locale);
            return new ArrayList<>();
        }
        
        List<Component> components = new ArrayList<>();
        
        if (placeholders == null || placeholders.isEmpty()) {
            for (String message : messages) {
                components.add(miniMessage.deserialize(message));
            }
        } else {
            // Build tag resolvers
            List<TagResolver> resolvers = new ArrayList<>();
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                resolvers.add(Placeholder.unparsed(entry.getKey(), String.valueOf(entry.getValue())));
            }
            TagResolver resolver = TagResolver.resolver(resolvers);
            
            for (String message : messages) {
                components.add(miniMessage.deserialize(message, resolver));
            }
        }
        
        return components;
    }
    
    /**
     * Send a message to a player (uses player's locale)
     * @param player Player to send message to
     * @param key Message key
     * @param replacements Variable replacements
     */
    public void sendMessage(Player player, String key, Object... replacements) {
        Component message = getComponentForPlayer(key, player, replacements);
        player.sendMessage(message);
    }
    
    /**
     * Send a message to a player with named placeholders (uses player's locale)
     * @param player Player to send message to
     * @param key Message key
     * @param placeholders Map of placeholder names to values
     */
    public void sendMessage(Player player, String key, Map<String, Object> placeholders) {
        Component message = getComponent(key, player, placeholders);
        player.sendMessage(message);
    }
    
    /**
     * Check if a message key exists
     * @param key Message key
     * @return true if key exists
     */
    public boolean hasMessage(String key) {
        return languageConfig != null && languageConfig.contains(key);
    }
    
    /**
     * Get current locale (for console)
     * @return Current locale code
     */
    public String getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * Set player's locale based on their client language
     * @param player Player
     * @param locale Language code from client
     */
    public void setPlayerLocale(Player player, String locale) {
        // Convert client locale format to our format if needed
        // e.g., "zh_tw" -> "zh_TW", "en_us" -> "en_US"
        String normalizedLocale = normalizeLocale(locale);
        
        // Load the language if not already loaded
        if (!loadedLanguages.containsKey(normalizedLocale)) {
            // Try to load, if fails, fall back to default
            if (!loadLanguage(normalizedLocale)) {
                normalizedLocale = currentLocale; // Use server default
            }
        }
        
        playerLocales.put(player.getUniqueId(), normalizedLocale);
    }
    
    /**
     * Get player's locale
     * @param player Player
     * @return Player's locale code
     */
    public String getPlayerLocale(Player player) {
        return playerLocales.getOrDefault(player.getUniqueId(), currentLocale);
    }
    
    /**
     * Remove player's locale when they leave
     * @param player Player
     */
    public void removePlayerLocale(Player player) {
        playerLocales.remove(player.getUniqueId());
    }
    
    /**
     * Normalize locale format (e.g., zh_tw -> zh_TW)
     * @param locale Raw locale string
     * @return Normalized locale string
     */
    private String normalizeLocale(String locale) {
        if (locale == null || locale.isEmpty()) {
            return currentLocale;
        }
        
        // Replace hyphens with underscores
        locale = locale.replace('-', '_');
        
        // Split by underscore
        String[] parts = locale.split("_");
        if (parts.length == 2) {
            return parts[0].toLowerCase() + "_" + parts[1].toUpperCase();
        }
        
        return locale;
    }
    
    /**
     * Get the prefix for messages (for console)
     * @return Prefix component
     */
    public Component getPrefix() {
        return getComponent("general.prefix");
    }
    
    /**
     * Get the prefix for a specific player
     * @param player Player
     * @return Prefix component
     */
    public Component getPrefix(Player player) {
        return getComponent("general.prefix", player);
    }
    
    /**
     * Send a message with prefix (uses player's locale)
     * @param player Player to send message to
     * @param key Message key
     * @param replacements Variable replacements
     */
    public void sendMessageWithPrefix(Player player, String key, Object... replacements) {
        Component prefix = getPrefix(player);
        Component message = getComponentForPlayer(key, player, replacements);
        player.sendMessage(prefix.append(Component.text(" ")).append(message));
    }
    
    /**
     * Send a message with prefix and named placeholders (uses player's locale)
     * @param player Player to send message to
     * @param key Message key
     * @param placeholders Map of placeholder names to values
     */
    public void sendMessageWithPrefix(Player player, String key, Map<String, Object> placeholders) {
        Component prefix = getPrefix(player);
        Component message = getComponent(key, player, placeholders);
        player.sendMessage(prefix.append(Component.text(" ")).append(message));
    }
}
