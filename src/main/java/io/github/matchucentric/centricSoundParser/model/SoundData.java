package io.github.matchucentric.centricSoundParser.model;

public class SoundData {
    private final String name;
    private final String filename;
    private final long length;

    public SoundData(String name, String filename, long length) {
        this.name = name;
        this.filename = filename;
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public long getLength() {
        return length;
    }
}

