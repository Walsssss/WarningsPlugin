package walson.me.walsondev.warningsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WarningsPlugin extends JavaPlugin {

    private static WarningsPlugin instance;

    private YamlConfiguration messagesConfig;
    private File messagesFile;

    private WarningManager warningManager;
    private DateTimeFormatter dateFormatter;

    private String warnsTitlePrefixStripped;
    private String panelTitleStripped;
    private String recentTitleStripped;
    private String playersTitleStripped;
    private String topTitleStripped;

    private NamespacedKey panelTargetKey;

    public static WarningsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadMessages();
        setupDateFormatter();

        this.warningManager = new WarningManager(this);
        this.panelTargetKey = new NamespacedKey(this, "panel-target");

        registerCommands();
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
    }

    @Override
    public void onDisable() {
        if (warningManager != null) {
            warningManager.save();
        }
    }

    private void setupDateFormatter() {
        FileConfiguration cfg = getConfig();
        String pattern = cfg.getString("date-format", "yyyy-MM-dd HH:mm:ss");
        this.dateFormatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
    }

    private void registerCommands() {
        PluginCommand warnCmd = getCommand("warn");
        if (warnCmd != null) {
            WarnCommand executor = new WarnCommand(this);
            warnCmd.setExecutor(executor);
            warnCmd.setTabCompleter(executor);
        }

        PluginCommand warnsCmd = getCommand("warns");
        if (warnsCmd != null) {
            WarnsCommand executor = new WarnsCommand(this);
            warnsCmd.setExecutor(executor);
            warnsCmd.setTabCompleter(executor);
        }

        PluginCommand removeWarnCmd = getCommand("removewarn");
        if (removeWarnCmd != null) {
            RemoveWarnCommand executor = new RemoveWarnCommand(this);
            removeWarnCmd.setExecutor(executor);
            removeWarnCmd.setTabCompleter(executor);
        }

        PluginCommand adminCmd = getCommand("walsonwarn");
        if (adminCmd != null) {
            WalsonWarnCommand executor = new WalsonWarnCommand(this);
            adminCmd.setExecutor(executor);
            adminCmd.setTabCompleter(executor);
        }
    }

    // ---- Config & messages ----

    private void loadMessages() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        String warnsTitleConf = messagesConfig.getString("gui.warns-title", "&8Warnings &7» &c%player%");
        String warnsWithoutPlayer = warnsTitleConf.replace("%player%", "");
        String warnsColored = ChatColor.translateAlternateColorCodes('&', warnsWithoutPlayer);
        this.warnsTitlePrefixStripped = ChatColor.stripColor(warnsColored);

        String panelTitleConf = messagesConfig.getString("gui.panel-title", "&8Warnings Panel");
        String panelColored = ChatColor.translateAlternateColorCodes('&', panelTitleConf);
        this.panelTitleStripped = ChatColor.stripColor(panelColored);

        String recentTitleConf = messagesConfig.getString("gui.recent-title", "&8Recent Warnings");
        String recentColored = ChatColor.translateAlternateColorCodes('&', recentTitleConf);
        this.recentTitleStripped = ChatColor.stripColor(recentColored);

        String playersTitleConf = messagesConfig.getString("gui.players-title", "&8Warned Players");
        String playersColored = ChatColor.translateAlternateColorCodes('&', playersTitleConf);
        this.playersTitleStripped = ChatColor.stripColor(playersColored);

        String topTitleConf = messagesConfig.getString("gui.top-title", "&8Top Warned Players");
        String topColored = ChatColor.translateAlternateColorCodes('&', topTitleConf);
        this.topTitleStripped = ChatColor.stripColor(topColored);
    }

    public void reloadAll() {
        reloadConfig();
        loadMessages();
        setupDateFormatter();
        if (warningManager != null) {
            warningManager.reload();
        }
    }

    public YamlConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public String getMessage(String path) {
        String prefix = messagesConfig.getString("prefix", "");
        String msg = messagesConfig.getString(path, "&cMissing message: " + path);
        if (msg == null) {
            msg = "&cMissing message: " + path;
        }
        msg = msg.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String msg = getMessage(path);
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                msg = msg.replace(e.getKey(), e.getValue());
            }
        }
        return msg;
    }

    private String formatGuiString(String path, String def, Map<String, String> placeholders) {
        String s = messagesConfig.getString(path, def);
        if (s == null) s = def;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                s = s.replace(e.getKey(), e.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private List<String> formatGuiLore(String path, Map<String, String> placeholders) {
        List<String> raw = messagesConfig.getStringList(path);
        List<String> out = new ArrayList<>();
        String prefix = messagesConfig.getString("prefix", "");
        for (String line : raw) {
            String s = line.replace("%prefix%", prefix);
            if (placeholders != null) {
                for (Map.Entry<String, String> e : placeholders.entrySet()) {
                    s = s.replace(e.getKey(), e.getValue());
                }
            }
            out.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return out;
    }

    public String formatTimestamp(long millis) {
        return dateFormatter.format(Instant.ofEpochMilli(millis));
    }

    public WarningManager getWarningManager() {
        return warningManager;
    }

    public String getWarnsTitlePrefixStripped() {
        return warnsTitlePrefixStripped;
    }

    public String getPanelTitleStripped() {
        return panelTitleStripped;
    }

    public String getRecentTitleStripped() {
        return recentTitleStripped;
    }

    public String getPlayersTitleStripped() {
        return playersTitleStripped;
    }

    public String getTopTitleStripped() {
        return topTitleStripped;
    }

    public NamespacedKey getPanelTargetKey() {
        return panelTargetKey;
    }

    // ---- GUI opening helpers ----

    public void openWarnsGui(Player viewer, UUID targetUuid) {
        List<Warning> warnings = warningManager.getWarnings(targetUuid);
        String targetName = warningManager.getLastKnownName(targetUuid);
        if (targetName == null) {
            targetName = "Unknown";
        }

        if (warnings.isEmpty()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", targetName);
            viewer.sendMessage(getMessage("no-warns", ph));
            return;
        }

        int size = getConfig().getInt("gui.warns.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) size = 54;

        String titleConf = messagesConfig.getString("gui.warns-title", "&8Warnings &7» &c%player%");
        String title = ChatColor.translateAlternateColorCodes('&',
                titleConf.replace("%player%", targetName));

        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (int i = 0; i < warnings.size() && slot < inv.getSize(); i++, slot++) {
            Warning w = warnings.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            String display = ChatColor.YELLOW + "#" + (i + 1) + ChatColor.GRAY + " - " +
                    ChatColor.WHITE + shorten(w.getReason(), 30);
            meta.setDisplayName(display);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Player: " + ChatColor.WHITE + w.getTargetName());
            lore.add(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + w.getReason());
            lore.add(ChatColor.GRAY + "By: " + ChatColor.WHITE + w.getWarnerName());
            lore.add(ChatColor.GRAY + "Date: " + ChatColor.WHITE + formatTimestamp(w.getTimestamp()));

            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }

        viewer.openInventory(inv);
    }

    public void openPanelGui(Player viewer) {
        FileConfiguration cfg = getConfig();

        int size = cfg.getInt("gui.panel.size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) size = 27;

        String titleConf = messagesConfig.getString("gui.panel-title", "&8Warnings Panel");
        String title = ChatColor.translateAlternateColorCodes('&', titleConf);

        Inventory inv = Bukkit.createInventory(null, size, title);

        // Recent button
        int recentSlot = cfg.getInt("gui.panel.buttons.recent.slot", 11);
        if (recentSlot >= 0 && recentSlot < size) {
            String matName = cfg.getString("gui.panel.buttons.recent.material", "CLOCK");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.CLOCK;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(formatGuiString("gui.panel-recent-name", "&6Recent Warnings", null));
            meta.setLore(formatGuiLore("gui.panel-recent-lore", null));
            item.setItemMeta(meta);
            inv.setItem(recentSlot, item);
        }

        // Players button
        int playersSlot = cfg.getInt("gui.panel.buttons.players.slot", 13);
        if (playersSlot >= 0 && playersSlot < size) {
            String matName = cfg.getString("gui.panel.buttons.players.material", "BOOK");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.BOOK;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(formatGuiString("gui.panel-players-name", "&6Warned Players", null));
            meta.setLore(formatGuiLore("gui.panel-players-lore", null));
            item.setItemMeta(meta);
            inv.setItem(playersSlot, item);
        }

        // Top button
        int topSlot = cfg.getInt("gui.panel.buttons.top.slot", 15);
        if (topSlot >= 0 && topSlot < size) {
            String matName = cfg.getString("gui.panel.buttons.top.material", "PLAYER_HEAD");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.PLAYER_HEAD;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(formatGuiString("gui.panel-top-name", "&6Top Warned Players", null));
            meta.setLore(formatGuiLore("gui.panel-top-lore", null));
            item.setItemMeta(meta);
            inv.setItem(topSlot, item);
        }

        viewer.openInventory(inv);
    }

    public void openRecentWarnsGui(Player viewer) {
        FileConfiguration cfg = getConfig();

        int size = cfg.getInt("gui.recent.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) size = 54;

        int limit = cfg.getInt("gui.recent.limit", size);
        if (limit <= 0 || limit > size) limit = size;

        List<Warning> recent = warningManager.getRecentWarnings(limit);
        if (recent.isEmpty()) {
            viewer.sendMessage(getMessage("no-warnings-data"));
            return;
        }

        String titleConf = messagesConfig.getString("gui.recent-title", "&8Recent Warnings");
        String title = ChatColor.translateAlternateColorCodes('&', titleConf);

        Inventory inv = Bukkit.createInventory(null, size, title);

        String matName = cfg.getString("gui.recent.material", "PAPER");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.PAPER;

        int slot = 0;
        for (Warning w : recent) {
            if (slot >= inv.getSize()) break;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", w.getTargetName());
            ph.put("%reason%", w.getReason());
            ph.put("%staff%", w.getWarnerName());
            ph.put("%date%", formatTimestamp(w.getTimestamp()));

            meta.setDisplayName(formatGuiString("gui.recent-item-name", "&e" + w.getTargetName(), ph));
            meta.setLore(formatGuiLore("gui.recent-item-lore", ph));

            meta.getPersistentDataContainer().set(
                    panelTargetKey,
                    PersistentDataType.STRING,
                    w.getTargetUuid().toString()
            );

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        viewer.openInventory(inv);
    }

    public void openPlayersGui(Player viewer) {
        FileConfiguration cfg = getConfig();

        int size = cfg.getInt("gui.players.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) size = 54;

        Map<UUID, String> players = warningManager.getAllWarnedPlayers();
        if (players.isEmpty()) {
            viewer.sendMessage(getMessage("no-warnings-data"));
            return;
        }

        String titleConf = messagesConfig.getString("gui.players-title", "&8Warned Players");
        String title = ChatColor.translateAlternateColorCodes('&', titleConf);

        Inventory inv = Bukkit.createInventory(null, size, title);

        String matName = cfg.getString("gui.players.material", "PLAYER_HEAD");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.PLAYER_HEAD;

        List<Map.Entry<UUID, String>> list = new ArrayList<>(players.entrySet());
        list.sort(Comparator.comparing(e -> e.getValue().toLowerCase(Locale.ROOT)));

        int slot = 0;
        for (Map.Entry<UUID, String> entry : list) {
            if (slot >= inv.getSize()) break;

            UUID uuid = entry.getKey();
            String name = entry.getValue();
            int count = warningManager.getWarningCount(uuid);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            if (mat == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                meta = skullMeta;
            }

            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", name);
            ph.put("%count%", String.valueOf(count));

            meta.setDisplayName(formatGuiString("gui.players-item-name", "&e" + name, ph));
            meta.setLore(formatGuiLore("gui.players-item-lore", ph));

            meta.getPersistentDataContainer().set(
                    panelTargetKey,
                    PersistentDataType.STRING,
                    uuid.toString()
            );

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        viewer.openInventory(inv);
    }

    public void openTopPlayersGui(Player viewer) {
        FileConfiguration cfg = getConfig();

        int size = cfg.getInt("gui.top.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) size = 54;

        int limit = cfg.getInt("gui.top.limit", size);
        if (limit <= 0 || limit > size) limit = size;

        Map<UUID, Integer> top = warningManager.getTopWarnCounts(limit);
        if (top.isEmpty()) {
            viewer.sendMessage(getMessage("no-warnings-data"));
            return;
        }

        String titleConf = messagesConfig.getString("gui.top-title", "&8Top Warned Players");
        String title = ChatColor.translateAlternateColorCodes('&', titleConf);

        Inventory inv = Bukkit.createInventory(null, size, title);

        String matName = cfg.getString("gui.top.material", "PLAYER_HEAD");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.PLAYER_HEAD;

        int slot = 0;
        for (Map.Entry<UUID, Integer> entry : top.entrySet()) {
            if (slot >= inv.getSize()) break;

            UUID uuid = entry.getKey();
            int count = entry.getValue();

            String name = warningManager.getLastKnownName(uuid);
            if (name == null) {
                name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name == null) name = "Unknown";
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            if (mat == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                meta = skullMeta;
            }

            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", name);
            ph.put("%count%", String.valueOf(count));

            meta.setDisplayName(formatGuiString("gui.top-item-name", "&e" + name, ph));
            meta.setLore(formatGuiLore("gui.top-item-lore", ph));

            meta.getPersistentDataContainer().set(
                    panelTargetKey,
                    PersistentDataType.STRING,
                    uuid.toString()
            );

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        viewer.openInventory(inv);
    }

    private String shorten(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}