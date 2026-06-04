package io.github.matchucentric.centricSoundParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LengthParser {
    public Double analyzeLength(File oggFile) {
        try {
            byte[] data = Files.readAllBytes(oggFile.toPath());

            int sampleRate = findVorbisSampleRate(data);
            long totalSamples = findTotalSamplesFromOggPages(data);

            if (sampleRate <= 0 || totalSamples < 0) {
                return null;
            }

            double lengthSeconds = (double) totalSamples / sampleRate;
            return Math.round(lengthSeconds * 1000.0) / 1000.0;
        } catch (IOException e) {
            return null;
        }
    }

    private int findVorbisSampleRate(byte[] data) {
        // Vorbis identification header starts with 0x01 + "vorbis"
        for (int i = 0; i <= data.length - 16; i++) {
            if ((data[i] & 0xFF) == 0x01
                && data[i + 1] == 'v'
                && data[i + 2] == 'o'
                && data[i + 3] == 'r'
                && data[i + 4] == 'b'
                && data[i + 5] == 'i'
                && data[i + 6] == 's') {
                // packet_type(1) + "vorbis"(6) + version(4) + channels(1) = 12
                int offset = i + 12;
                return readIntLE(data, offset);
            }
        }

        return -1;
    }

    private long findTotalSamplesFromOggPages(byte[] data) {
        long maxGranule = -1;
        int i = 0;

        while (i <= data.length - 27) {
            if (data[i] == 'O' && data[i + 1] == 'g' && data[i + 2] == 'g' && data[i + 3] == 'S') {
                int segmentCount = data[i + 26] & 0xFF;
                if (i + 27 + segmentCount > data.length) {
                    break;
                }

                long granule = readLongLE(data, i + 6);
                if (granule > maxGranule) {
                    maxGranule = granule;
                }

                int payloadSize = 0;
                for (int s = 0; s < segmentCount; s++) {
                    payloadSize += data[i + 27 + s] & 0xFF;
                }

                i += 27 + segmentCount + payloadSize;
            } else {
                i++;
            }
        }

        return maxGranule;
    }

    private int readIntLE(byte[] data, int offset) {
        if (offset + 4 > data.length) {
            return -1;
        }

        return (data[offset] & 0xFF)
            | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16)
            | ((data[offset + 3] & 0xFF) << 24);
    }

    private long readLongLE(byte[] data, int offset) {
        if (offset + 8 > data.length) {
            return -1;
        }

        return ((long) (data[offset] & 0xFF))
            | ((long) (data[offset + 1] & 0xFF) << 8)
            | ((long) (data[offset + 2] & 0xFF) << 16)
            | ((long) (data[offset + 3] & 0xFF) << 24)
            | ((long) (data[offset + 4] & 0xFF) << 32)
            | ((long) (data[offset + 5] & 0xFF) << 40)
            | ((long) (data[offset + 6] & 0xFF) << 48)
            | ((long) (data[offset + 7] & 0xFF) << 56);
    }
}





