package net.mineaqua.cmdSender;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CmdSenderPlugin extends JavaPlugin {

    private File queueFile;
    private FileConfiguration queueConfig;
    private final Map<UUID, List<String>> queue = new HashMap<>();
    private Messages messages;

    @Override
    public void onEnable() {
        // Crea config.yml si no existe
        saveDefaultConfig();
        messages = new Messages(this);

        // Asegurar carpeta + cargar cola
        getDataFolder().mkdirs();
        loadQueue();

        // Registrar comandos y listener
        getCommand("cmdsender").setExecutor(new CmdSenderCommand(this));
        getCommand("cmdsender").setTabCompleter(new CmdSenderCommand(this));
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        int total = queue.values().stream().mapToInt(List::size).sum();
        getLogger().info(ChatColor.stripColor(messages.msg("enabled", Map.of("total", String.valueOf(total)))));
    }

    @Override
    public void onDisable() {
        try {
            // Cancelar tareas programadas
            Bukkit.getScheduler().cancelTasks(this);

            // Desregistrar listeners
            HandlerList.unregisterAll(this);

            // Guardar datos pendientes
            saveQueue();

            getLogger().info("CmdSender enabled correctly.");
        } catch (Exception e) {
            getLogger().warning("Error while disabling: " + e.getMessage());
        }
    }

    public Messages messages() { return messages; }

    private void loadQueue() {
        try {
            queueFile = new File(getDataFolder(), "queue.yml");
            if (!queueFile.exists()) {
                queueFile.getParentFile().mkdirs();
                queueFile.createNewFile();
            }
            queueConfig = YamlConfiguration.loadConfiguration(queueFile);

            queue.clear();
            if (queueConfig.isConfigurationSection("queue")) {
                for (String uuidStr : Objects.requireNonNull(queueConfig.getConfigurationSection("queue")).getKeys(false)) {
                    UUID uuid = safeUUID(uuidStr);
                    if (uuid == null) continue;
                    List<String> cmds = queueConfig.getStringList("queue." + uuidStr);
                    if (!cmds.isEmpty()) queue.put(uuid, new ArrayList<>(cmds));
                }
            }
        } catch (IOException e) {
            getLogger().severe("Cannot create/load queue.yml: " + e.getMessage());
        }
    }

    public synchronized void saveQueue() {
        if (queueFile == null) return;
        if (queueConfig == null) queueConfig = new YamlConfiguration();
        queueConfig.set("queue", null);
        for (Map.Entry<UUID, List<String>> e : queue.entrySet()) {
            queueConfig.set("queue." + e.getKey(), e.getValue());
        }
        try { queueConfig.save(queueFile); } catch (IOException e) {
            getLogger().severe("Cannot save queue.yml: " + e.getMessage());
        }
    }

    public synchronized void addCommandForPlayer(OfflinePlayer target, String command) {
        UUID uuid = target.getUniqueId();
        queue.computeIfAbsent(uuid, k -> new ArrayList<>()).add(command);
        saveQueue();
    }

    public synchronized List<String> getCommands(UUID uuid) {
        return Collections.unmodifiableList(queue.getOrDefault(uuid, Collections.emptyList()));
    }

    public synchronized void clearCommands(UUID uuid) {
        queue.remove(uuid);
        saveQueue();
    }

    public synchronized List<String> popCommands(UUID uuid) {
        List<String> list = queue.remove(uuid);
        if (list == null) list = Collections.emptyList();
        saveQueue();
        return list;
    }

    public static UUID safeUUID(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    public static String replacePlaceholders(String raw, String playerName, UUID uuid) {
        return raw.replace("%player%", playerName).replace("%uuid%", uuid.toString());
    }
}
