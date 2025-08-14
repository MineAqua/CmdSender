package net.mineaqua.cmdSender;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JoinListener implements Listener {

    private final CmdSenderPlugin plugin;

    public JoinListener(CmdSenderPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Ejecutar en el siguiente tick para asegurar que el jugador esté totalmente cargado
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<String> toRun = plugin.popCommands(uuid);
            if (toRun.isEmpty()) return;

            for (String raw : toRun) {
                String cmd = CmdSenderPlugin.replacePlaceholders(raw, event.getPlayer().getName(), uuid);
                // Ejecuta como consola
                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                if (!ok) {
                    plugin.getLogger().warning(ChatColor.stripColor(
                            plugin.messages().msg("exec-failed", Map.of("command", cmd))
                    ));
                }

            }
        });
    }
}
