package walson.me.walsondev.warningsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
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
            // ensure plugin data dir exists
            getDataFolder().mkdirs();
        }

        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // update GUI title detection strings
        String warnsTitleConf = messagesConfig.getString("gui.warns-title", "&8Warnings &7» &c%player%");
        String warnsWithoutPlayer = warnsTitleConf.replace("%player%", "");
        String warnsColored = ChatColor.translateAlternateColorCodes('&', warnsWithoutPlayer);
        this.warnsTitlePrefixStripped = ChatColor.stripColor(warnsColored);

        String panelTitleConf = messagesConfig.getString("gui.panel-title", "&8Warnings Panel");
        String panelColored = ChatColor.translateAlternateColorCodes('&', panelTitleConf);
        this.panelTitleStripped = ChatColor.stripColor(panelColored);
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
        int size = getConfig().getInt("gui.panel.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) size = 54;

        String titleConf = messagesConfig.getString("gui.panel-title", "&8Warnings Panel");
        String title = ChatColor.translateAlternateColorCodes('&', titleConf);

        int recentLimit = getConfig().getInt("gui.panel.recent-warns-limit", 27);
        int topLimit = getConfig().getInt("gui.panel.top-warned-limit", 9);

        List<Warning> recent = warningManager.getRecentWarnings(recentLimit);
        Map<UUID, Integer> top = warningManager.getTopWarnCounts(topLimit);

        Inventory inv = Bukkit.createInventory(null, size, title);

        // Recent warns as papers
        int slot = 0;
        for (Warning w : recent) {
            if (slot >= inv.getSize()) break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            String display = ChatColor.GOLD + w.getTargetName();
            meta.setDisplayName(display);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Reason: " + ChatColor.WHITE + w.getReason());
            lore.add(ChatColor.GRAY + "By: " + ChatColor.WHITE + w.getWarnerName());
            lore.add(ChatColor.GRAY + "Date: " + ChatColor.WHITE + formatTimestamp(w.getTimestamp()));

            String hint = messagesConfig.getString("gui.open-warns-hint", "&7Click to open this player's warns.");
            lore.add(ChatColor.translateAlternateColorCodes('&', hint));

            meta.setLore(lore);

            // store target uuid in PDC
            meta.getPersistentDataContainer().set(
                    panelTargetKey,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    w.getTargetUuid().toString()
            );

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // Top warned players as heads (start at bottom row)
        int topStart = Math.max(size - 9, slot + 1);
        int used = 0;
        for (Map.Entry<UUID, Integer> entry : top.entrySet()) {
            if (topStart + used >= inv.getSize()) break;

            UUID uuid = entry.getKey();
            int count = entry.getValue();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta =
                    (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));

            String name = warningManager.getLastKnownName(uuid);
            if (name == null) {
                name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name == null) name = "Unknown";
            }

            meta.setDisplayName(ChatColor.GOLD + name);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Warnings: " + ChatColor.RED + count);
            String hint = messagesConfig.getString("gui.open-warns-hint", "&7Click to open this player's warns.");
            lore.add(ChatColor.translateAlternateColorCodes('&', hint));
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(
                    panelTargetKey,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    uuid.toString()
            );

            head.setItemMeta(meta);

            inv.setItem(topStart + used, head);
            used++;
            if (used >= topLimit) break;
        }

        viewer.openInventory(inv);
    }

    private String shorten(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    // Convenience for saving messages if you ever modify them at runtime
    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}