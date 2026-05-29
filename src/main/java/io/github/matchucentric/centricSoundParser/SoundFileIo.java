package io.github.matchucentric.centricSoundParser;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SoundFileIo {
    private static final Logger logger = Logger.getLogger(SoundFileIo.class.getName());
    private final MinecraftSoundFetcher minecraftSoundFetcher;
    private final NexoSoundFetcher nexoSoundFetcher;

    public SoundFileIo(java.io.File dataFolder, java.io.File worldContainer) {
        this.minecraftSoundFetcher = new MinecraftSoundFetcher(dataFolder, worldContainer);
        this.nexoSoundFetcher = new NexoSoundFetcher(dataFolder, worldContainer);
    }

    public Set<String> parseMinecraftSounds() {
        Set<String> soundFiles = new HashSet<>();
        try {
            minecraftSoundFetcher.fetchMinecraftSounds(soundFiles);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing Minecraft sounds", e);
        }
        return soundFiles;
    }

    public Set<String> analyzeNexoSoundsOnStartup() {
        Set<String> soundFiles = new HashSet<>();
        try {
            nexoSoundFetcher.fetchNexoSounds(soundFiles);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error analyzing Nexo sounds on startup", e);
        }
        return soundFiles;
    }
}
