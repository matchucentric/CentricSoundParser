package io.github.matchucentric.centricSoundParser;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import io.github.matchucentric.centricSoundParser.model.SoundCatalogue;
import io.github.matchucentric.centricSoundParser.model.SoundData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("NullableProblems")
public class CspCommandExecutor implements CommandExecutor, TabCompleter {
    private final CentricSoundParser plugin;
    private final SoundFileIo soundFileIo;

    public CspCommandExecutor(CentricSoundParser plugin, SoundFileIo soundFileIo) {
        this.plugin = plugin;
        this.soundFileIo = soundFileIo;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("parse") && args[1].equalsIgnoreCase("minecraft")) {
            sender.sendMessage("[CSP] Minecraft parsing started asynchronously...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                SoundCatalogue minecraftCatalogue = soundFileIo.analyzeMinecraftSoundsOnStartup();
                SoundCatalogue nexoCatalogue = soundFileIo.readCatalogueFromFile("nexo");
                SoundCatalogue mergedCatalogue = SoundCatalogue.merge(List.of(minecraftCatalogue, nexoCatalogue));
                plugin.setLoadedCatalogue(mergedCatalogue);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("[CSP] Minecraft parsing complete.");
                    sender.sendMessage("[CSP] Returned entries: " + minecraftCatalogue.getSoundDataByName().size());
                    sender.sendMessage("[CSP] Loaded merged entries: " + mergedCatalogue.getSoundDataByName().size());
                });
            });
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("data")) {
            String name = args[1];
            SoundData soundData = plugin.getLoadedCatalogue().getSoundDataByName().get(name);
            if (soundData == null) {
                sender.sendMessage("[CSP] Sound not found: " + name);
                return true;
            }

            sender.sendMessage("[CSP] Sound Data");
            sender.sendMessage("[CSP] Name: " + soundData.getName());
            sender.sendMessage("[CSP] Filename: " + soundData.getFilename());
            sender.sendMessage("[CSP] Length: " + (soundData.getLength() == null ? "null" : soundData.getLength()));
            sender.sendMessage("[CSP] Source: " + soundData.getSource());
            return true;
        }

        sender.sendMessage("Usage: /" + label + " parse minecraft");
        sender.sendMessage("Usage: /" + label + " data <name>");
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return filterByPrefix(List.of("parse", "data"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("parse")) {
            return filterByPrefix(List.of("minecraft"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("data")) {
            List<String> loadedSoundNames = new ArrayList<>(plugin.getLoadedCatalogue().getSoundDataByName().keySet());
            loadedSoundNames.sort(Comparator.naturalOrder());
            return filterByPrefix(loadedSoundNames, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase().startsWith(normalizedPrefix)) {
                matches.add(value);
            }
        }
        return matches;
    }
}

