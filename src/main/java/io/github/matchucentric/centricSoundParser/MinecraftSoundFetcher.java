package io.github.matchucentric.centricSoundParser;

import io.github.matchucentric.centricSoundParser.model.SoundCatalogue;
import io.github.matchucentric.centricSoundParser.model.SoundData;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MinecraftSoundFetcher {
    private static final Logger logger = Logger.getLogger(MinecraftSoundFetcher.class.getName());
    private final File dataFolder;
    private final LengthParser lengthParser;

    public MinecraftSoundFetcher(File dataFolder) {
        this.dataFolder = dataFolder;
        this.lengthParser = new LengthParser();
    }

    public SoundCatalogue fetchMinecraftSounds() {
        SoundCatalogue catalogue = new SoundCatalogue();
        File minecraftSoundAssetsDir = new File(dataFolder, "minecraftSoundAssets");
        File indexesDir = new File(minecraftSoundAssetsDir, "indexes");
        File objectsDir = new File(minecraftSoundAssetsDir, "objects");

        if (!indexesDir.isDirectory() || !objectsDir.isDirectory()) {
            logger.warning("Cannot iterate sounds because one or both required subfolders are missing.");
            return catalogue;
        }

        iterateSoundIndexes(indexesDir.toPath(), objectsDir, catalogue);
        return catalogue;
    }

    private void iterateSoundIndexes(Path indexesDir, File objectsDir, SoundCatalogue catalogue) {
        List<Path> jsonFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(indexesDir)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                .forEach(jsonFiles::add);
        } catch (IOException e) {
            logger.warning("Failed to walk indexes directory: " + e.getMessage());
            return;
        }

        jsonFiles.sort(Comparator.comparing(Path::toString));

        Gson gson = new Gson();
        for (Path jsonFile : jsonFiles) {
            try (FileReader reader = new FileReader(jsonFile.toFile(), StandardCharsets.UTF_8)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("objects") || !root.get("objects").isJsonObject()) {
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

                    Double lengthSeconds = lengthParser.analyzeLength(hashFile);
                    String soundName = "minecraft:" + new File(unobfuscatedFilename).getName();
                    SoundData soundData = new SoundData(soundName, hashFile.getName(), lengthSeconds, "minecraft");
                    catalogue.addSoundData(soundData);
                }
            } catch (Exception e) {
                logger.warning("Failed to parse index json " + jsonFile + ": " + e.getMessage());
            }
        }
    }
}
