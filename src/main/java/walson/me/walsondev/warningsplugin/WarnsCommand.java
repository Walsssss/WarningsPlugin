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

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessage("usage-warns"));
            return true;
        }

        Player viewer = (Player) sender;
        String nameArg = args[0];

        Player online = Bukkit.getPlayer(nameArg);
        UUID targetUuid = null;
        String targetName;

        if (online != null) {
            targetUuid = online.getUniqueId();
            targetName = online.getName();
        } else {
            // try from stored data
            targetUuid = plugin.getWarningManager().findUuidByName(nameArg);
            targetName = nameArg;
        }

        if (targetUuid == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", nameArg);
            viewer.sendMessage(plugin.getMessage("no-warns", ph));
            return true;
        }

        plugin.openWarnsGui(viewer, targetUuid);
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