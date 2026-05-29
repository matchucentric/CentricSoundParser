package io.github.matchucentric.centricSoundParser;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CentricSoundParser extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("=== CentricSoundParser Enabled ===");

        SoundFileIo soundFileIo = new SoundFileIo(
            getDataFolder(),
            Bukkit.getServer().getWorldContainer()
        );

        var cspCommand = getCommand("csp");
        if (cspCommand != null) {
            cspCommand.setExecutor(new CspCommandExecutor(soundFileIo));
        } else {
            getLogger().warning("Command 'csp' is not defined in plugin.yml");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
