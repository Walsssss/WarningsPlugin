package walson.me.walsondev.warningsplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class WalsonWarnCommand implements CommandExecutor, TabCompleter {

    private final WarningsPlugin plugin;

    public WalsonWarnCommand(WarningsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command, String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload":
                if (!sender.hasPermission("walsonwarn.reload")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(plugin.getMessage("reload-success"));
                return true;

            case "panel":
                if (!sender.hasPermission("walsonwarn.panel")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessage("player-only"));
                    return true;
                }
                Player p = (Player) sender;
                plugin.openPanelGui(p);
                return true;

            case "help":
                // Help is allowed for everyone
                sendHelp(sender);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        YamlConfiguration msg = plugin.getMessagesConfig();
        List<String> lines = msg.getStringList("walsonwarn-help");
        if (lines == null || lines.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "WarningsPlugin commands:");
            sender.sendMessage(ChatColor.YELLOW + "/warn <player> <reason>");
            sender.sendMessage(ChatColor.YELLOW + "/warns <player>");
            sender.sendMessage(ChatColor.YELLOW + "/removewarn <player> <n|all>");
            sender.sendMessage(ChatColor.YELLOW + "/walsonwarn panel");
            sender.sendMessage(ChatColor.YELLOW + "/walsonwarn reload");
            sender.sendMessage(ChatColor.YELLOW + "/walsonwarn help");
            return;
        }

        String prefix = msg.getString("prefix", "");
        for (String line : lines) {
            line = line.replace("%prefix%", prefix);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            String input = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(input)) subs.add("reload");
            if ("panel".startsWith(input)) subs.add("panel");
            if ("help".startsWith(input)) subs.add("help");
            return subs;
        }
        return Collections.emptyList();
    }
}