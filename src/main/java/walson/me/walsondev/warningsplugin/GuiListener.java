package walson.me.walsondev.warningsplugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class GuiListener implements Listener {

    private final WarningsPlugin plugin;

    public GuiListener(WarningsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());

        boolean isWarnsGui = title.startsWith(plugin.getWarnsTitlePrefixStripped());
        boolean isPanelGui = title.equalsIgnoreCase(plugin.getPanelTitleStripped());
        boolean isRecentGui = title.equalsIgnoreCase(plugin.getRecentTitleStripped());
        boolean isPlayersGui = title.equalsIgnoreCase(plugin.getPlayersTitleStripped());
        boolean isTopGui = title.equalsIgnoreCase(plugin.getTopTitleStripped());

        if (!isWarnsGui && !isPanelGui && !isRecentGui && !isPlayersGui && !isTopGui) return;

        event.setCancelled(true);

        if (isWarnsGui) {
            // Read-only GUI
            return;
        }

        if (isPanelGui) {
            if (event.getClickedInventory() == null) return;
            int rawSlot = event.getRawSlot();
            if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;

            FileConfiguration cfg = plugin.getConfig();
            int recentSlot = cfg.getInt("gui.panel.buttons.recent.slot", 11);
            int playersSlot = cfg.getInt("gui.panel.buttons.players.slot", 13);
            int topSlot = cfg.getInt("gui.panel.buttons.top.slot", 15);

            if (rawSlot == recentSlot) {
                plugin.openRecentWarnsGui(player);
            } else if (rawSlot == playersSlot) {
                plugin.openPlayersGui(player);
            } else if (rawSlot == topSlot) {
                plugin.openTopPlayersGui(player);
            }
            return;
        }

        // Recent / Players / Top GUIs: clicking an item opens that player's warns GUI
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(plugin.getPanelTargetKey(), PersistentDataType.STRING)) return;

        String uuidStr = pdc.get(plugin.getPanelTargetKey(), PersistentDataType.STRING);
        if (uuidStr == null) return;

        try {
            UUID targetUuid = UUID.fromString(uuidStr);
            plugin.openWarnsGui(player, targetUuid);
        } catch (IllegalArgumentException ignored) {
        }
    }
}