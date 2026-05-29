package io.github.matchucentric.centricSoundParser;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class NexoSoundFetcher {
    private static final Logger logger = Logger.getLogger(NexoSoundFetcher.class.getName());

    private final File dataFolder;
    private final File worldContainer;

    public NexoSoundFetcher(File dataFolder, File worldContainer) {
        this.dataFolder = dataFolder;
        this.worldContainer = worldContainer;
    }

    public void fetchNexoSounds(Set<String> soundFiles) {
        Plugin nexo = Bukkit.getPluginManager().getPlugin("Nexo");
        if (nexo == null || !nexo.isEnabled()) {
            logger.info("Nexo plugin not enabled, skipping Nexo sounds");
            return;
        }

        File pluginsDir = resolvePluginsDirectory();
        if (pluginsDir == null) {
            logger.warning("Unable to locate plugins directory");
            return;
        }

        Map<String, String> soundNameLookup = loadNexoSoundNameLookup(pluginsDir);

        boolean scannedAnyAssets = false;
        for (File assetRoot : getNexoAssetRoots(pluginsDir)) {
            if (assetRoot.exists() && assetRoot.isDirectory()) {
                scannedAnyAssets = true;
                logger.info("Scanning Nexo assets from: " + assetRoot.getAbsolutePath());
                scanDirectoryForSounds(assetRoot, pluginsDir, soundFiles, soundNameLookup);
            }
        }

        if (!scannedAnyAssets) {
            logger.warning("Nexo assets directory not found under plugins folder");
        }
    }

    private File resolvePluginsDirectory() {
        File pluginsDir = dataFolder.getParentFile();
        if (pluginsDir == null && worldContainer != null) {
            pluginsDir = worldContainer.getParentFile();
        }
        return pluginsDir;
    }

    private File[] getNexoAssetRoots(File pluginsDir) {
        return new File[] {
            new File(pluginsDir, "Nexo-pack-assets/assets"),
            new File(pluginsDir, "Nexo/pack/assets")
        };
    }

    private Map<String, String> loadNexoSoundNameLookup(File pluginsDir) {
        Map<String, String> soundNameLookup = new HashMap<>();
        int loadedFiles = 0;

        for (File ymlFile : getNexoSoundYamlCandidates(pluginsDir)) {
            if (!ymlFile.exists() || !ymlFile.isFile()) {
                logger.info("Nexo sound mapping file not found, skipping: " + ymlFile.getAbsolutePath());
                continue;
            }

            logger.info("Loading Nexo sound mappings from: " + ymlFile.getAbsolutePath());
            int before = soundNameLookup.size();
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(ymlFile);
            YamlScanStats stats = new YamlScanStats(ymlFile.getName());
            parseNexoSoundsList(configuration, soundNameLookup, stats);
            collectNexoSoundMappings(configuration, null, soundNameLookup, stats);
            loadedFiles++;
            logger.info("Finished " + ymlFile.getName()
                + ": sections=" + stats.sectionsVisited
                + ", mappedPaths=" + stats.soundPathsMapped
                + ", malformedEntries=" + stats.malformedEntries
                + ", uniqueNamesAdded=" + (soundNameLookup.size() - before));
        }

        logger.info("Nexo sound mapping load complete: filesLoaded=" + loadedFiles
            + ", totalUniqueMappings=" + soundNameLookup.size());

        return soundNameLookup;
    }

    private File[] getNexoSoundYamlCandidates(File pluginsDir) {
        return new File[] {
            new File(pluginsDir, "Nexo/sounds.yml")
        };
    }

    private void parseNexoSoundsList(YamlConfiguration configuration, Map<String, String> soundNameLookup, YamlScanStats stats) {
        List<Map<?, ?>> soundsList = configuration.getMapList("sounds");
        if (soundsList.isEmpty()) {
            logger.info("No 'sounds' list found in " + stats.fileName);
            return;
        }

        logger.info("Processing " + soundsList.size() + " sound entries from " + stats.fileName);
        for (Map<?, ?> soundEntry : soundsList) {
            Object idValue = soundEntry.get("id");
            Object soundValue = soundEntry.get("sound");

            if (!(idValue instanceof String inGameId) || !(soundValue instanceof String soundRef)) {
                stats.malformedEntries++;
                if (stats.malformedEntries <= 5) {
                    logger.warning("Invalid sounds.yml entry (missing string id/sound) in " + stats.fileName + ": " + soundEntry);
                }
                continue;
            }

            String resolvedFilePath = convertNexoSoundRefToFilePath(soundRef);
            if (resolvedFilePath == null) {
                stats.malformedEntries++;
                if (stats.malformedEntries <= 5) {
                    logger.warning("Invalid sound reference format in " + stats.fileName + ": " + soundRef);
                }
                continue;
            }

            addSoundMapping(soundNameLookup, inGameId, resolvedFilePath, stats);
            logger.info("Mapped Nexo sound id '" + inGameId + "' to file '" + resolvedFilePath + "'");
        }
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

    private void collectNexoSoundMappings(
        ConfigurationSection section,
        String currentInGameName,
        Map<String, String> soundNameLookup,
        YamlScanStats stats
    ) {
        if (section == null) {
            return;
        }

        stats.sectionsVisited++;

        String resolvedName = currentInGameName;
        String directName = firstNonBlank(
            section.getString("name"),
            section.getString("sound"),
            section.getString("id"),
            section.getString("soundName")
        );
        if (directName != null) {
            resolvedName = directName;
        }

        for (String key : section.getKeys(false)) {
            Object value = section.get(key);

            if (value instanceof ConfigurationSection childSection) {
                collectNexoSoundMappings(childSection, resolvedName, soundNameLookup, stats);
                continue;
            }

            if (value instanceof List<?> values) {
                for (Object item : values) {
                    if (item instanceof String path) {
                        addSoundMapping(soundNameLookup, resolvedName != null ? resolvedName : key, path, stats);
                    } else {
                        stats.malformedEntries++;
                        if (stats.malformedEntries <= 5) {
                            logger.warning("Non-string sound path entry in " + stats.fileName + " at key " + key);
                        }
                    }
                }
                continue;
            }

            if (value instanceof String path) {
                if (looksLikeSoundPath(path)) {
                    addSoundMapping(soundNameLookup, resolvedName != null ? resolvedName : key, path, stats);
                } else if (resolvedName == null) {
                    resolvedName = path;
                }
            } else if (value != null) {
                stats.malformedEntries++;
            }
        }
    }

    private void addSoundMapping(Map<String, String> soundNameLookup, String inGameName, String rawPath, YamlScanStats stats) {
        if (rawPath == null || rawPath.isBlank()) {
            stats.malformedEntries++;
            return;
        }

        String normalizedPath = normalizeSoundPath(rawPath);
        if (normalizedPath == null) {
            stats.malformedEntries++;
            return;
        }

        String displayName = inGameName == null || inGameName.isBlank() ? "unknown" : inGameName;
        stats.soundPathsMapped++;
        soundNameLookup.putIfAbsent(normalizedPath, displayName);
        soundNameLookup.putIfAbsent(stripLeadingMinecraftPrefix(normalizedPath), displayName);
        soundNameLookup.putIfAbsent(stripToFilename(normalizedPath), displayName);
    }

    private void scanDirectoryForSounds(File directory, File pluginsDir, Set<String> soundFiles, Map<String, String> soundNameLookup) {
        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".ogg"))
                .forEach(path -> {
                    String pluginsRelativePath = pluginsDir.toPath().relativize(path).toString().replace(File.separatorChar, '/');
                    String assetRelativePath = directory.toPath().relativize(path).toString().replace(File.separatorChar, '/');
                    String displayName = resolveInGameName(soundNameLookup, assetRelativePath, pluginsRelativePath);
                    soundFiles.add(pluginsRelativePath + " -> " + displayName);
                });
        } catch (IOException e) {
            logger.log(java.util.logging.Level.WARNING, "Error scanning directory: " + directory.getAbsolutePath(), e);
        }
    }

    private String resolveInGameName(Map<String, String> soundNameLookup, String assetRelativePath, String pluginsRelativePath) {
        String normalizedAssetPath = normalizeSoundPath(assetRelativePath);
        String normalizedPluginsPath = normalizeSoundPath(pluginsRelativePath);

        String resolved = soundNameLookup.get(normalizedAssetPath);
        if (resolved == null) {
            resolved = soundNameLookup.get(stripLeadingMinecraftPrefix(normalizedAssetPath));
        }
        if (resolved == null) {
            resolved = soundNameLookup.get(normalizedPluginsPath);
        }
        if (resolved == null) {
            resolved = soundNameLookup.get(stripToFilename(normalizedAssetPath));
        }

        return resolved == null ? "unknown" : resolved;
    }

    private boolean looksLikeSoundPath(String value) {
        String normalized = normalizeSoundPath(value);
        return normalized != null && (normalized.endsWith(".ogg") || normalized.contains("/") || normalized.contains("\\"));
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

    private String stripLeadingMinecraftPrefix(String normalizedPath) {
        if (normalizedPath.startsWith("minecraft/")) {
            return normalizedPath.substring("minecraft/".length());
        }
        return normalizedPath;
    }

    private String stripToFilename(String normalizedPath) {
        int lastSlash = normalizedPath.lastIndexOf('/');
        return lastSlash >= 0 ? normalizedPath.substring(lastSlash + 1) : normalizedPath;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static final class YamlScanStats {
        private final String fileName;
        private int sectionsVisited;
        private int soundPathsMapped;
        private int malformedEntries;

        private YamlScanStats(String fileName) {
            this.fileName = fileName;
        }
    }
}

