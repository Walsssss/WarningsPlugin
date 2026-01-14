package walson.me.walsondev.warningsplugin;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class WarnCommand implements CommandExecutor, TabCompleter {

    private final WarningsPlugin plugin;

    public WarnCommand(WarningsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("walsonwarn.warn")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("usage-warn"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("%player%", args[0]);
            sender.sendMessage(plugin.getMessage("player-not-found", ph));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String staffName = (sender instanceof Player) ? ((Player) sender).getName() : "Console";
        UUID staffUuid = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;

        long timestamp = System.currentTimeMillis();

        plugin.getWarningManager().addWarning(
                target.getUniqueId(),
                target.getName(),
                staffUuid,
                staffName,
                reason,
                timestamp
        );

        Map<String, String> ph = new HashMap<>();
        ph.put("%player%", target.getName());
        ph.put("%reason%", reason);
        ph.put("%staff%", staffName);

        sender.sendMessage(plugin.getMessage("warn-success", ph));

        target.sendMessage(plugin.getMessage("warn-received", ph));
        sendTitleAndSound(target, reason);

        // broadcast to staff with permission walsonwarn.see and console
        String notify = plugin.getMessage("warn-notify", ph);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("walsonwarn.see")) {
                p.sendMessage(notify);
            }
        }
        Bukkit.getConsoleSender().sendMessage(notify);

        return true;
    }

    private void sendTitleAndSound(Player target, String reason) {
        FileConfiguration cfg = plugin.getConfig();

        // Title
        if (cfg.getBoolean("warn.title.enabled", true)) {
            String main = cfg.getString("warn.title.main", "&cYou have been warned!");
            String sub = cfg.getString("warn.title.subtitle", "&7Reason: &f%reason%");
            main = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    main.replace("%reason%", reason));
            sub = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    sub.replace("%reason%", reason));

            int fadeIn = cfg.getInt("warn.title.fade-in", 10);
            int stay = cfg.getInt("warn.title.stay", 70);
            int fadeOut = cfg.getInt("warn.title.fade-out", 20);

            target.sendTitle(main, sub, fadeIn, stay, fadeOut);
        }

        // Sound
        if (cfg.getBoolean("warn.sound.enabled", true)) {
            String soundName = cfg.getString("warn.sound.name", "ENTITY_VILLAGER_NO");
            float volume = (float) cfg.getDouble("warn.sound.volume", 1.0);
            float pitch = (float) cfg.getDouble("warn.sound.pitch", 1.0);

            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                target.playSound(target.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException ex) {
                // invalid sound, ignore
            }
        }
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