package io.github.matchucentric.centricSoundParser.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundCatalogue {
    private static final List<String> SOURCE_PRIORITY_ORDER = List.of("minecraft", "nexo");
    private final Map<String, SoundData> soundDataByName = new HashMap<>();

    public void addSoundData(SoundData soundData) {
        soundDataByName.put(soundData.getName(), soundData);
    }

    public Map<String, SoundData> getSoundDataByName() {
        return Collections.unmodifiableMap(soundDataByName);
    }

    public static SoundCatalogue merge(List<SoundCatalogue> catalogues) {
        SoundCatalogue merged = new SoundCatalogue();
        if (catalogues == null) {
            return merged;
        }

        for (SoundCatalogue catalogue : catalogues) {
            if (catalogue == null) {
                continue;
            }

            for (SoundData incoming : catalogue.soundDataByName.values()) {
                if (incoming == null || incoming.getName() == null) {
                    continue;
                }

                SoundData existing = merged.soundDataByName.get(incoming.getName());
                if (existing == null || getSourcePriority(incoming.getSource()) > getSourcePriority(existing.getSource())) {
                    merged.soundDataByName.put(incoming.getName(), incoming);
                }
            }
        }

        return merged;
    }

    private static int getSourcePriority(String source) {
        if (source == null) {
            return -1;
        }

        int index = SOURCE_PRIORITY_ORDER.indexOf(source.trim().toLowerCase());
        return index >= 0 ? index : -1;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SoundCatalogue{count=")
            .append(soundDataByName.size())
            .append(", entries=[");

        boolean first = true;
        for (SoundData soundData : soundDataByName.values()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(soundData);
            first = false;
        }

        builder.append("]}");
        return builder.toString();
    }
}

