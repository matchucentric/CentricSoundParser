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
        Set<String> soundFiles = soundFileIo.scanAllSounds();

        getLogger().info("Found " + soundFiles.size() + " sound files:");
        soundFiles.stream()
            .sorted()
            .forEach(sound -> getLogger().info("  - " + sound));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
