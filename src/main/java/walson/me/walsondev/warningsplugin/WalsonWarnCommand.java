package walson.me.walsondev.warningsplugin;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

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
            sender.sendMessage("/walsonwarn reload");
            sender.sendMessage("/walsonwarn panel");
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

            default:
                sender.sendMessage("/walsonwarn reload");
                sender.sendMessage("/walsonwarn panel");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                subs.add("reload");
            }
            if ("panel".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                subs.add("panel");
            }
            return subs;
        }
        return Collections.emptyList();
    }
}