package walson.me.walsondev.warningsplugin;

import org.bukkit.ChatColor;
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

        if (!isWarnsGui && !isPanelGui) return;

        // prevent taking items / moving
        event.setCancelled(true);

        if (!isPanelGui) {
            // warns-gui is view-only
            return;
        }

        // panel GUI actions
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