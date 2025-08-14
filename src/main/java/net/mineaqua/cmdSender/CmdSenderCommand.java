package net.mineaqua.cmdSender;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CmdSenderCommand implements CommandExecutor, TabCompleter {

    private final CmdSenderPlugin plugin;

    public CmdSenderCommand(CmdSenderPlugin plugin) { this.plugin = plugin; }

    private void send(CommandSender s, String path, Map<String, String> vars) {
        s.sendMessage(plugin.messages().msg(path, vars));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("cmdsender.admin")) {
            send(sender, "no-permission", null);
            return true;
        }
        if (args.length < 2) {
            help(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String targetName = args[1];

        OfflinePlayer target = null;
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(targetName)) {
                target = p;
                break;
            }
        }
        if (target == null) {
            target = Bukkit.getOfflinePlayer(targetName); // Este sí existe en 1.8.8
        }


        switch (sub) {
            case "add" -> {
                if (args.length < 3) {
                    help(sender, label);
                    return true;
                }
                String rawCommand = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                // ¿está online ahora?
                Player online = Bukkit.getPlayer(target.getUniqueId());
                if (online != null && online.isOnline()) {
                    String cmdToRun = CmdSenderPlugin.replacePlaceholders(rawCommand, online.getName(), online.getUniqueId());
                    boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdToRun);
                    if (ok) {
                        send(sender, "add-online-executed", Map.of(
                                "target", online.getName(),
                                "command", cmdToRun
                        ));
                    } else {
                        plugin.getLogger().warning("Fallo al ejecutar (online): " + cmdToRun);
                        send(sender, "add-online-exec-failed", Map.of(
                                "target", online.getName(),
                                "command", cmdToRun
                        ));
                    }
                    return true; // no se encola porque ya se ejecutó
                }

                // offline → encolar como siempre
                plugin.addCommandForPlayer(target, rawCommand);
                send(sender, "added", Map.of(
                        "target", Objects.toString(target.getName(), targetName),
                        "command", rawCommand
                ));
            }

            case "list" -> {
                List<String> list = plugin.getCommands(target.getUniqueId());
                String tName = Objects.toString(target.getName(), targetName);
                if (list.isEmpty()) {
                    send(sender, "list-empty", Map.of("target", tName));
                } else {
                    send(sender, "list-header", Map.of("target", tName, "size", String.valueOf(list.size())));
                    int i = 1;
                    for (String c : list) {
                        sender.sendMessage(plugin.messages().msg("list-line", Map.of("index", String.valueOf(i++), "command", c)));
                    }
                }
            }
            case "clear" -> {
                plugin.clearCommands(target.getUniqueId());
                send(sender, "clear-done", Map.of("target", Objects.toString(target.getName(), targetName)));
            }
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender s, String label) {
        s.sendMessage(plugin.messages().msg("usage-header", null));
        s.sendMessage(plugin.messages().msg("usage-add", Map.of("label", label)));
        s.sendMessage(plugin.messages().msg("usage-list", Map.of("label", label)));
        s.sendMessage(plugin.messages().msg("usage-clear", Map.of("label", label)));
        s.sendMessage(plugin.messages().msg("placeholders-hint", null));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("cmdsender.admin")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("add", "list", "clear").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
