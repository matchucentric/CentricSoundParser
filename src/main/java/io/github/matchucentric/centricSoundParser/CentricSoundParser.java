package io.github.matchucentric.centricSoundParser;

import io.github.matchucentric.centricSoundParser.model.SoundCatalogue;
import io.github.matchucentric.centricSoundParser.model.SoundData;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class CentricSoundParser extends JavaPlugin {
    private static CentricSoundParser instance;
    private SoundCatalogue loadedCatalogue = new SoundCatalogue();

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("=== CentricSoundParser Enabled ===");

        SoundFileIo soundFileIo = new SoundFileIo(
            getDataFolder(),
            Bukkit.getServer().getWorldContainer()
        );

        SoundCatalogue minecraftCatalogue = soundFileIo.loadMinecraftCatalogueOnStartup();
        SoundCatalogue nexoCatalogue = soundFileIo.analyzeNexoSoundsOnStartup();
        SoundCatalogue mergedCatalogue = SoundCatalogue.merge(List.of(minecraftCatalogue, nexoCatalogue));
        loadedCatalogue = mergedCatalogue;
        getLogger().info("Catalogues ready. Minecraft=" + minecraftCatalogue.getSoundDataByName().size()
            + ", Nexo=" + nexoCatalogue.getSoundDataByName().size()
            + ", Merged=" + mergedCatalogue.getSoundDataByName().size());

        var cspCommand = getCommand("csp");
        if (cspCommand != null) {
            CspCommandExecutor cspCommandExecutor = new CspCommandExecutor(this, soundFileIo);
            cspCommand.setExecutor(cspCommandExecutor);
            cspCommand.setTabCompleter(cspCommandExecutor);
        } else {
            getLogger().warning("Command 'csp' is not defined in plugin.yml");
        }
    }

    public static CentricSoundParser getInstance() {
        return instance;
    }

    public SoundCatalogue getLoadedCatalogue() {
        return loadedCatalogue;
    }

    public void setLoadedCatalogue(SoundCatalogue loadedCatalogue) {
        this.loadedCatalogue = loadedCatalogue == null ? new SoundCatalogue() : loadedCatalogue;
    }

    public Double getSoundLength(String soundName) {
        if (soundName == null) {
            return null;
        }

        StringBuilder nexoCatalogue = new StringBuilder("[");
        boolean first = true;
        for (SoundData entry : loadedCatalogue.getSoundDataByName().values()) {
            if (entry == null || entry.getSource() == null
                || !"nexo".equalsIgnoreCase(entry.getSource().trim())) {
                continue;
            }
            if (!first) {
                nexoCatalogue.append(", ");
            }
            nexoCatalogue.append(entry);
            first = false;
        }
        nexoCatalogue.append("]");
//        getLogger().info("getSoundLength requested sound='" + soundName
//            + "', nexo catalogue=" + nexoCatalogue);

        SoundData soundData = loadedCatalogue.getSoundDataByName().get(soundName);
        return soundData == null ? null : soundData.getLength();
    }

    @Override
    public void onDisable() {
        instance = null;
        // Plugin shutdown logic
    }
}
