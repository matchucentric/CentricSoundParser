package io.github.matchucentric.centricSoundParser;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Set;

public class CspCommandExecutor implements CommandExecutor {
    private final SoundFileIo soundFileIo;

    public CspCommandExecutor(SoundFileIo soundFileIo) {
        this.soundFileIo = soundFileIo;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("parse") && args[1].equalsIgnoreCase("minecraft")) {
            Set<String> soundFiles = soundFileIo.parseMinecraftSounds();
            sender.sendMessage("[CSP] Minecraft parsing invoked.");
            sender.sendMessage("[CSP] Returned entries: " + soundFiles.size());
            return true;
        }

        sender.sendMessage("Usage: /" + label + " parse minecraft");
        return true;
    }
}

