package walson.me.walsondev.warningsplugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WarningManager {

    private final WarningsPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public WarningManager(WarningsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warnings.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addWarning(UUID targetUuid, String targetName,
                                        UUID warnerUuid, String warnerName,
                                        String reason, long timestamp) {
        String base = "players." + targetUuid.toString();
        config.set(base + ".name", targetName);

        String listPath = base + ".warnings";
        List<Map<?, ?>> listRaw = config.getMapList(listPath);
        List<Map<String, Object>> list = new ArrayList<>();

        for (Map<?, ?> m : listRaw) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null) {
                    copy.put(e.getKey().toString(), e.getValue());
                }
            }
            list.add(copy);
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("reason", reason);
        entry.put("timestamp", timestamp);
        entry.put("warnerName", warnerName);
        if (warnerUuid != null) {
            entry.put("warnerUuid", warnerUuid.toString());
        }

        list.add(entry);
        config.set(listPath, list);
        save();
    }

    public synchronized List<Warning> getWarnings(UUID targetUuid) {
        List<Warning> result = new ArrayList<>();
        String base = "players." + targetUuid.toString();
        String name = config.getString(base + ".name", "Unknown");

        String listPath = base + ".warnings";
        List<Map<?, ?>> list = config.getMapList(listPath);
        for (Map<?, ?> m : list) {
            String reason = String.valueOf(m.getOrDefault("reason", "No reason"));
            long timestamp;
            try {
                Object tsObj = m.get("timestamp");
                if (tsObj instanceof Number) {
                    timestamp = ((Number) tsObj).longValue();
                } else {
                    timestamp = Long.parseLong(String.valueOf(tsObj));
                }
            } catch (Exception e) {
                timestamp = System.currentTimeMillis();
            }
            String warnerName = String.valueOf(m.getOrDefault("warnerName", "Unknown"));
            UUID warnerUuid = null;
            Object wu = m.get("warnerUuid");
            if (wu != null) {
                try {
                    warnerUuid = UUID.fromString(String.valueOf(wu));
                } catch (IllegalArgumentException ignored) {
                }
            }

            result.add(new Warning(targetUuid, name, reason, warnerName, warnerUuid, timestamp));
        }

        return result;
    }

    public synchronized String getLastKnownName(UUID uuid) {
        return config.getString("players." + uuid.toString() + ".name");
    }

    public synchronized UUID findUuidByName(String name) {
        ConfigurationSection playersSec = config.getConfigurationSection("players");
        if (playersSec == null) return null;

        for (String key : playersSec.getKeys(false)) {
            String storedName = playersSec.getString(key + ".name");
            if (storedName != null && storedName.equalsIgnoreCase(name)) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    public synchronized boolean clearWarnings(UUID uuid) {
        String base = "players." + uuid.toString();
        String listPath = base + ".warnings";
        if (!config.contains(listPath)) {
            return false;
        }

        // remove all warnings; optionally remove entire player section
        config.set(base, null);
        save();
        return true;
    }

    public synchronized boolean removeWarning(UUID uuid, int indexZeroBased) {
        String base = "players." + uuid.toString();
        String listPath = base + ".warnings";

        List<Map<?, ?>> listRaw = config.getMapList(listPath);
        if (indexZeroBased < 0 || indexZeroBased >= listRaw.size()) {
            return false;
        }
        listRaw.remove(indexZeroBased);

        if (listRaw.isEmpty()) {
            // no more warnings for this player
            config.set(base, null);
        } else {
            config.set(listPath, listRaw);
        }

        save();
        return true;
    }

    public synchronized List<Warning> getRecentWarnings(int limit) {
        List<Warning> all = new ArrayList<>();
        ConfigurationSection playersSec = config.getConfigurationSection("players");
        if (playersSec == null) {
            return Collections.emptyList();
        }

        for (String key : playersSec.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            all.addAll(getWarnings(uuid));
        }

        all.sort(Comparator.comparingLong(Warning::getTimestamp).reversed());

        if (all.size() > limit) {
            return new ArrayList<>(all.subList(0, limit));
        }
        return all;
    }

    public synchronized Map<UUID, Integer> getTopWarnCounts(int limit) {
        Map<UUID, Integer> map = new HashMap<>();

        ConfigurationSection playersSec = config.getConfigurationSection("players");
        if (playersSec == null) {
            return Collections.emptyMap();
        }

        for (String key : playersSec.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            int count = getWarnings(uuid).size();
            if (count > 0) {
                map.put(uuid, count);
            }
        }

        // sort
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        Map<UUID, Integer> result = new LinkedHashMap<>();
        int added = 0;
        for (Map.Entry<UUID, Integer> e : entries) {
            result.put(e.getKey(), e.getValue());
            added++;
            if (added >= limit) break;
        }

        return result;
    }
}