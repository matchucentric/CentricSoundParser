package io.github.matchucentric.centricSoundParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MinecraftSoundFetcher {
    private static final Logger logger = Logger.getLogger(MinecraftSoundFetcher.class.getName());
    private final File dataFolder;
    private final File worldContainer;

    public MinecraftSoundFetcher(File dataFolder, File worldContainer) {
        this.dataFolder = dataFolder;
        this.worldContainer = worldContainer;
    }

    public void fetchMinecraftSounds(Set<String> soundFiles) {
        logger.info("MinecraftSoundFetcher.fetchMinecraftSounds was called.");
        logger.info("Data folder: " + dataFolder.getAbsolutePath());

        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                logger.warning("Failed to create plugin data folder");
                return;
            }
        }

        File minecraftSoundAssetsDir = new File(dataFolder, "minecraftSoundAssets");
        if (!minecraftSoundAssetsDir.exists()) {
            if (minecraftSoundAssetsDir.mkdirs()) {
                logger.info("Created minecraftSoundAssets folder");
            } else {
                logger.warning("Failed to create minecraftSoundAssets folder");
            }
        }

        File indexesDir = new File(minecraftSoundAssetsDir, "indexes");
        File objectsDir = new File(minecraftSoundAssetsDir, "objects");

        boolean indexesExists = indexesDir.exists() && indexesDir.isDirectory();
        boolean objectsExists = objectsDir.exists() && objectsDir.isDirectory();

        logger.info("minecraftSoundAssets folder: " + minecraftSoundAssetsDir.getAbsolutePath());
        logger.info("  - indexes subfolder exists: " + indexesExists);
        logger.info("  - objects subfolder exists: " + objectsExists);

        soundFiles.add("minecraft fetcher called @ " + worldContainer.getAbsolutePath());

        if (!indexesExists || !objectsExists) {
            logger.warning("Cannot iterate sounds because one or both required subfolders are missing.");
            return;
        }

        iterateSoundIndexes(indexesDir.toPath(), objectsDir, soundFiles);
    }

    private void iterateSoundIndexes(Path indexesDir, File objectsDir, Set<String> soundFiles) {
        List<Path> jsonFiles = new ArrayList<>();
        try {
            Files.walk(indexesDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                .forEach(jsonFiles::add);
        } catch (IOException e) {
            logger.warning("Failed to walk indexes directory: " + e.getMessage());
            return;
        }

        jsonFiles.sort(Comparator.comparing(Path::toString));
        logger.info("Found " + jsonFiles.size() + " index json files to inspect.");

        Gson gson = new Gson();
        for (Path jsonFile : jsonFiles) {
            logger.info("Scanning index json: " + jsonFile);
            try (FileReader reader = new FileReader(jsonFile.toFile(), StandardCharsets.UTF_8)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("objects") || !root.get("objects").isJsonObject()) {
                    logger.warning("Skipping " + jsonFile + " because it has no top-level objects map.");
                    continue;
                }

                JsonObject objects = root.getAsJsonObject("objects");
                for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
                    String unobfuscatedFilename = entry.getKey();
                    if (!unobfuscatedFilename.toLowerCase().endsWith(".ogg")) {
                        continue;
                    }

                    JsonElement value = entry.getValue();
                    if (value == null || !value.isJsonObject()) {
                        logger.warning("Skipping malformed entry for " + unobfuscatedFilename + " in " + jsonFile);
                        continue;
                    }

                    JsonObject valueObject = value.getAsJsonObject();
                    JsonElement hashElement = valueObject.get("hash");
                    if (hashElement == null || hashElement.isJsonNull()) {
                        logger.warning("Skipping " + unobfuscatedFilename + " because no hash was found in " + jsonFile);
                        continue;
                    }

                    String hash = hashElement.getAsString();
                    if (hash == null || hash.length() < 2) {
                        logger.warning("Skipping " + unobfuscatedFilename + " because hash was invalid: " + hash);
                        continue;
                    }

                    File hashFile = new File(new File(objectsDir, hash.substring(0, 2)), hash);
                    if (!hashFile.exists() || !hashFile.isFile()) {
                        logger.warning("Missing hashed object file for " + unobfuscatedFilename + " at " + hashFile.getPath());
                        continue;
                    }

                    String output = dataFolder.toPath().relativize(hashFile.toPath()).toString().replace(File.separatorChar, '/')
                        + " -> " + unobfuscatedFilename;
                    logger.info(output);
                    soundFiles.add(output);
                }
            } catch (Exception e) {
                logger.warning("Failed to parse index json " + jsonFile + ": " + e.getMessage());
            }
        }
    }
}
