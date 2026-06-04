package io.github.matchucentric.centricSoundParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.matchucentric.centricSoundParser.model.SoundCatalogue;
import io.github.matchucentric.centricSoundParser.model.SoundData;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SoundFileIo {
    private static final Logger logger = Logger.getLogger(SoundFileIo.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFolder;
    private final MinecraftSoundFetcher minecraftSoundFetcher;
    private final NexoSoundFetcher nexoSoundFetcher;

    public SoundFileIo(java.io.File dataFolder, java.io.File worldContainer) {
        this.dataFolder = dataFolder;
        this.minecraftSoundFetcher = new MinecraftSoundFetcher(dataFolder);
        this.nexoSoundFetcher = new NexoSoundFetcher(dataFolder, worldContainer);
    }

    public SoundCatalogue analyzeMinecraftSoundsOnStartup() {
        try {
            SoundCatalogue catalogue = minecraftSoundFetcher.fetchMinecraftSounds();
            writeCatalogueToFile(catalogue, "minecraft");
            return catalogue;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error analyzing Minecraft sounds on startup", e);
            return new SoundCatalogue();
        }
    }

    public SoundCatalogue analyzeNexoSoundsOnStartup() {
        try {
            SoundCatalogue catalogue = nexoSoundFetcher.fetchNexoSounds();
            writeCatalogueToFile(catalogue, "nexo");
            return catalogue;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error analyzing Nexo sounds on startup", e);
            return new SoundCatalogue();
        }
    }

    public SoundCatalogue loadMinecraftCatalogueOnStartup() {
        try {
            return readCatalogueFromFile("minecraft");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error loading Minecraft catalogue from file", e);
            return new SoundCatalogue();
        }
    }

    public void writeCatalogueToFile(SoundCatalogue catalogue, String source) {
        String normalizedSource = source == null ? "" : source.trim().toLowerCase();
        String fileName;
        switch (normalizedSource) {
            case "nexo" -> fileName = "nexo.json";
            case "minecraft" -> fileName = "minecraft.json";
            default -> {
                logger.warning("Unsupported catalogue source: " + source);
                return;
            }
        }

        File cataloguesDir = new File(dataFolder, "catalogues");
        if (!cataloguesDir.exists() && !cataloguesDir.mkdirs()) {
            logger.warning("Could not create catalogues directory: " + cataloguesDir.getAbsolutePath());
            return;
        }

        File outputFile = new File(cataloguesDir, fileName);
         try {
             String json = GSON.toJson(new TreeMap<>(catalogue.getSoundDataByName()));
             Files.writeString(outputFile.toPath(), json, StandardCharsets.UTF_8);
         } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to write catalogue file: " + outputFile.getAbsolutePath(), e);
        }
    }

    public SoundCatalogue readCatalogueFromFile(String source) {
        String normalizedSource = source == null ? "" : source.trim().toLowerCase();
        String fileName;
        switch (normalizedSource) {
            case "nexo" -> fileName = "nexo.json";
            case "minecraft" -> fileName = "minecraft.json";
            default -> {
                logger.warning("Unsupported catalogue source: " + source);
                return new SoundCatalogue();
            }
        }

        File inputFile = new File(new File(dataFolder, "catalogues"), fileName);
        if (!inputFile.exists() || !inputFile.isFile()) {
            logger.warning("Catalogue file not found: " + inputFile.getAbsolutePath());
            return new SoundCatalogue();
        }

        try {
            String json = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
            Type mapType = new TypeToken<Map<String, SoundData>>() {}.getType();
            Map<String, SoundData> parsed = GSON.fromJson(json, mapType);

            SoundCatalogue catalogue = new SoundCatalogue();
            if (parsed != null) {
                for (SoundData soundData : parsed.values()) {
                    if (soundData != null && soundData.getName() != null) {
                        catalogue.addSoundData(soundData);
                    }
                }
            }

            return catalogue;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read catalogue file: " + inputFile.getAbsolutePath(), e);
            return new SoundCatalogue();
        }
    }
}
