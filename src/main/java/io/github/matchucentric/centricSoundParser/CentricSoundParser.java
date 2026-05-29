package io.github.matchucentric.centricSoundParser;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class CentricSoundParser extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("=== CentricSoundParser Enabled ===");

        SoundFileIo soundFileIo = new SoundFileIo(
            getDataFolder(),
            Bukkit.getServer().getWorldContainer()
        );

        Set<String> nexoSoundFiles = soundFileIo.analyzeNexoSoundsOnStartup();
        getLogger().info("Nexo analyzer startup run complete. Entries found: " + nexoSoundFiles.size());

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
