package io.github.matchucentric.centricSoundParser.model;

public class SoundData {
    private final String name;
    private final String filename;
    private final Double length;
    private final String source;

    public SoundData(String name, String filename, Double length, String source) {
        this.name = name;
        this.filename = filename;
        this.length = length;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public Double getLength() {
        return length;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        String lengthText = length != null ? length + "s" : "unknown length";
        return "SoundData{name='" + name + "', filename='" + filename + "', length=" + lengthText + ", source='" + source + "'}";
    }
}

