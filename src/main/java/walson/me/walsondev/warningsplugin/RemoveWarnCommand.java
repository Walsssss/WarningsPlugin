package walson.me.walsondev.warningsplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class RemoveWarnCommand implements CommandExecutor, TabCompleter {

    private final WarningsPlugin plugin;

    public RemoveWarnCommand(WarningsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command, String label, String[] args) {
        if (!sender.hasPermission("walsonwarn.removewarn")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("usage-removewarn"));
            return true;
        }

        String nameArg = args[0];
        String numArg = args[1];

        Player online = Bukkit.getPlayer(nameArg);
        UUID targetUuid = null;
        String targetName;

        if (online != null) {
            targetUuid = online.getUniqueId();
            targetName = online.getName();
        } else {
            targetUuid = plugin.getWarningManager().findUuidByName(nameArg);
            targetName = nameArg;
        }

        if (targetUuid == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", nameArg);
            sender.sendMessage(plugin.getMessage("no-warns", ph));
            return true;
        }

        if (numArg.equalsIgnoreCase("all")) {
            boolean cleared = plugin.getWarningManager().clearWarnings(targetUuid);
            if (!cleared) {
                Map<String, String> ph = new HashMap<>();
                ph.put("%player%", targetName);
                sender.sendMessage(plugin.getMessage("no-warns", ph));
                return true;
            }

            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", targetName);
            sender.sendMessage(plugin.getMessage("remove-warn-all-success", ph));
            return true;
        }

        int index;
        try {
            index = Integer.parseInt(numArg);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessage("invalid-warn-number"));
            return true;
        }

        if (index < 1) {
            sender.sendMessage(plugin.getMessage("invalid-warn-number"));
            return true;
        }

        boolean removed = plugin.getWarningManager().removeWarning(targetUuid, index - 1);
        if (!removed) {
            sender.sendMessage(plugin.getMessage("invalid-warn-number"));
            return true;
        }

        Map<String, String> ph = new HashMap<>();
        ph.put("%player%", targetName);
        ph.put("%number%", String.valueOf(index));
        sender.sendMessage(plugin.getMessage("remove-warn-success", ph));
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
        } else if (args.length == 2) {
            return Collections.singletonList("all");
        }
        return Collections.emptyList();
    }
}