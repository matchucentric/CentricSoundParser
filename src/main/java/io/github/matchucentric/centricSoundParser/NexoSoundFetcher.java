package io.github.matchucentric.centricSoundParser;

import io.github.matchucentric.centricSoundParser.model.SoundCatalogue;
import io.github.matchucentric.centricSoundParser.model.SoundData;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class NexoSoundFetcher {
    private static final Logger logger = Logger.getLogger(NexoSoundFetcher.class.getName());

    private final File dataFolder;
    private final File worldContainer;
    private final LengthParser lengthParser;

    public NexoSoundFetcher(File dataFolder, File worldContainer) {
        this.dataFolder = dataFolder;
        this.worldContainer = worldContainer;
        this.lengthParser = new LengthParser();
    }

    public SoundCatalogue fetchNexoSounds() {
        SoundCatalogue catalogue = new SoundCatalogue();

        Plugin nexo = Bukkit.getPluginManager().getPlugin("Nexo");
        if (nexo == null || !nexo.isEnabled()) {
            // logger.info("Nexo plugin not enabled, skipping Nexo sounds");
            return catalogue;
        }

        File pluginsDir = resolvePluginsDirectory();
        if (pluginsDir == null) {
            logger.warning("Unable to locate plugins directory");
            return catalogue;
        }

        File[] assetRoots = getExistingNexoAssetRoots(pluginsDir);
        if (assetRoots.length == 0) {
            logger.warning("Nexo assets directory not found under plugins folder");
            return catalogue;
        }

        // for (File assetRoot : assetRoots) {
        //     logger.info("Using Nexo assets from: " + assetRoot.getAbsolutePath());
        // }


        for (File ymlFile : getNexoSoundYamlCandidates(pluginsDir)) {
            if (!ymlFile.exists() || !ymlFile.isFile()) {
                // logger.info("Nexo sound mapping file not found, skipping: " + ymlFile.getAbsolutePath());
                continue;
            }

            // logger.info("Loading Nexo sounds from: " + ymlFile.getAbsolutePath());
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(ymlFile);
            parseNexoSoundsList(configuration, assetRoots, catalogue, ymlFile.getName());
        }

        return catalogue;
    }

    private File resolvePluginsDirectory() {
        File pluginsDir = dataFolder.getParentFile();
        if (pluginsDir == null && worldContainer != null) {
            pluginsDir = worldContainer.getParentFile();
        }
        return pluginsDir;
    }

    private File[] getExistingNexoAssetRoots(File pluginsDir) {
        File[] candidates = {
            new File(pluginsDir, "Nexo-pack-assets/assets"),
            new File(pluginsDir, "Nexo/pack/assets")
        };

        return java.util.Arrays.stream(candidates)
            .filter(File::exists)
            .filter(File::isDirectory)
            .toArray(File[]::new);
    }

    private File[] getNexoSoundYamlCandidates(File pluginsDir) {
        return new File[] {
            new File(pluginsDir, "Nexo/sounds.yml")
        };
    }

    private void parseNexoSoundsList(YamlConfiguration configuration, File[] assetRoots, SoundCatalogue catalogue, String sourceName) {
        List<Map<?, ?>> soundsList = configuration.getMapList("sounds");
        if (soundsList.isEmpty()) {
            // logger.info("No 'sounds' list found in " + sourceName);
            return;
        }

        // logger.info("Processing " + soundsList.size() + " sound entries from " + sourceName);
        for (Map<?, ?> soundEntry : soundsList) {
            Object idValue = soundEntry.get("id");
            Object soundValue = soundEntry.get("sound");

            if (!(idValue instanceof String inGameId) || !(soundValue instanceof String soundRef)) {
                logger.warning("Invalid sounds.yml entry (missing string id/sound) in " + sourceName + ": " + soundEntry);
                continue;
            }

            File resolvedFile = resolveNexoSoundFile(soundRef, assetRoots);
            if (resolvedFile == null) {
                logger.warning("Could not resolve Nexo sound file for " + inGameId + " from reference: " + soundRef);
                continue;
            }

             Double lengthSeconds = lengthParser.analyzeLength(resolvedFile);
             String filename = resolvedFile.getName();
             SoundData soundData = new SoundData(inGameId, filename, lengthSeconds, "nexo");
             catalogue.addSoundData(soundData);
        }
    }

    private File resolveNexoSoundFile(String soundRef, File[] assetRoots) {
        String resolvedRelativePath = convertNexoSoundRefToFilePath(soundRef);
        if (resolvedRelativePath == null) {
            return null;
        }

        for (File assetRoot : assetRoots) {
            File resolvedFile = new File(assetRoot, resolvedRelativePath);
            if (resolvedFile.exists() && resolvedFile.isFile()) {
                return resolvedFile;
            }
        }

        return null;
    }

    private String convertNexoSoundRefToFilePath(String soundRef) {
        if (soundRef == null || soundRef.isBlank()) {
            return null;
        }

        String normalizedRef = soundRef.trim().replace('\\', '/');
        int namespaceSeparator = normalizedRef.indexOf(':');
        if (namespaceSeparator <= 0 || namespaceSeparator == normalizedRef.length() - 1) {
            return null;
        }

        String namespace = normalizedRef.substring(0, namespaceSeparator);
        String path = normalizedRef.substring(namespaceSeparator + 1);
        String combinedPath = namespace + "/sounds/" + path;
        return normalizeSoundPath(combinedPath);
    }

    private String normalizeSoundPath(String rawPath) {
        if (rawPath == null) {
            return null;
        }

        String normalized = rawPath.trim().replace('\\', '/');
        if (normalized.isEmpty()) {
            return null;
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.startsWith("assets/")) {
            normalized = normalized.substring("assets/".length());
        }

        if (!normalized.endsWith(".ogg")) {
            normalized = normalized + ".ogg";
        }

        return normalized;
    }
}

