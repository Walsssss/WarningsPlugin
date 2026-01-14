package walson.me.walsondev.warningsplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class WarnsCommand implements CommandExecutor, TabCompleter {

    private final WarningsPlugin plugin;

    public WarnsCommand(WarningsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command, String label, String[] args) {
        if (!sender.hasPermission("walsonwarn.warns")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessage("usage-warns"));
            return true;
        }

        String nameArg = args[0];

        Player online = Bukkit.getPlayer(nameArg);
        UUID targetUuid = null;
        String targetName = nameArg;

        if (online != null) {
            targetUuid = online.getUniqueId();
            targetName = online.getName();
        } else {
            targetUuid = plugin.getWarningManager().findUuidByName(nameArg);
            if (targetUuid != null) {
                String stored = plugin.getWarningManager().getLastKnownName(targetUuid);
                if (stored != null) targetName = stored;
            }
        }

        if (targetUuid == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", nameArg);
            sender.sendMessage(plugin.getMessage("no-warns", ph));
            return true;
        }

        List<Warning> warnings = plugin.getWarningManager().getWarnings(targetUuid);
        if (warnings.isEmpty()) {
            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", targetName);
            sender.sendMessage(plugin.getMessage("no-warns", ph));
            return true;
        }

        if (sender instanceof Player) {
            Player viewer = (Player) sender;
            plugin.openWarnsGui(viewer, targetUuid);
            return true;
        }

        // Console or command block: text output
        Map<String, String> headerPh = new HashMap<>();
        headerPh.put("%player%", targetName);
        headerPh.put("%count%", String.valueOf(warnings.size()));
        sender.sendMessage(plugin.getMessage("warns-header", headerPh));

        for (int i = 0; i < warnings.size(); i++) {
            Warning w = warnings.get(i);
            Map<String, String> ph = new HashMap<>();
            ph.put("%number%", String.valueOf(i + 1));
            ph.put("%player%", w.getTargetName());
            ph.put("%reason%", w.getReason());
            ph.put("%staff%", w.getWarnerName());
            ph.put("%date%", plugin.formatTimestamp(w.getTimestamp()));
            sender.sendMessage(plugin.getMessage("warns-entry", ph));
        }

        String footerRaw = plugin.getMessagesConfig().getString("warns-footer", "");
        if (footerRaw != null && !footerRaw.isEmpty()) {
            String prefix = plugin.getMessagesConfig().getString("prefix", "");
            footerRaw = footerRaw.replace("%prefix%", prefix);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', footerRaw));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT)
                        .startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}