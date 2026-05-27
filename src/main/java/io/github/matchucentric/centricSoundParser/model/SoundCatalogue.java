package io.github.matchucentric.centricSoundParser.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SoundCatalogue {
    private final Map<String, SoundData> soundDataByName = new HashMap<>();

    public void addSoundData(SoundData soundData) {
        soundDataByName.put(soundData.getName(), soundData);
    }

    public Map<String, SoundData> getSoundDataByName() {
        return Collections.unmodifiableMap(soundDataByName);
    }
}

