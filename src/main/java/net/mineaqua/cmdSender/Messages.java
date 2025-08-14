package net.mineaqua.cmdSender;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class Messages {
    private final JavaPlugin plugin;
    private final boolean hex;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hex = plugin.getConfig().getBoolean("color.enable-hex", true);
    }

    public String msg(String path, Map<String, String> vars) {
        String raw = plugin.getConfig().getString("messages." + path, "");
        String prefix = plugin.getConfig().getString("prefix", "");
        raw = raw.replace("{prefix}", prefix);

        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                raw = raw.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        return colorize(raw);
    }

    public String colorize(String s) {
        if (s == null) return "";
        boolean supportsHex = isAtLeast("1.16");
        if (hex && supportsHex) {
            Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
            s = pattern.matcher(s).replaceAll((MatchResult match) -> {
                String hexCode = match.group(1);
                StringBuilder out = new StringBuilder("§x");
                for (char c : hexCode.toCharArray()) {
                    out.append('§').append(c);
                }
                return out.toString();
            });
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private boolean isAtLeast(String ver) {
        String serverVersion = plugin.getServer().getBukkitVersion().split("-")[0];
        try {
            String[] current = serverVersion.split("\\.");
            String[] target = ver.split("\\.");
            for (int i = 0; i < Math.max(current.length, target.length); i++) {
                int c = (i < current.length) ? Integer.parseInt(current[i]) : 0;
                int t = (i < target.length) ? Integer.parseInt(target[i]) : 0;
                if (c < t) return false;
                if (c > t) return true;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
