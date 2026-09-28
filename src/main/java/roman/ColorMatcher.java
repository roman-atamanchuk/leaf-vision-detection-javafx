package roman;

import javafx.scene.paint.Color;

public final class ColorMatcher {
    private ColorMatcher() {
    }

    public static boolean matchesReferenceColor(Color pixelColor, Color[] referenceColors, int referenceColorCount, int differenceThreshold) {
        if (referenceColorCount == 0) {
            return true;
        }

        int[] pixelChannels = toRgb(pixelColor);
        int pixelDominantIndex = dominantChannelIndex(pixelChannels);
        int pixelDominantValue = pixelChannels[pixelDominantIndex];

        for (int i = 0; i < referenceColorCount; i++) {
            Color referenceColor = referenceColors[i];
            if (referenceColor == null) {
                continue;
            }

            int[] referenceChannels = toRgb(referenceColor);
            int referenceDominantIndex = dominantChannelIndex(referenceChannels);
            int referenceDominantValue = referenceChannels[referenceDominantIndex];

            if (pixelDominantIndex == referenceDominantIndex
                    && Math.abs(pixelDominantValue - referenceDominantValue) <= differenceThreshold) {
                return true;
            }
        }

        return false;
    }

    public static int[] toRgb(Color color) {
        return new int[]{
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255)
        };
    }

    public static int dominantChannelIndex(int[] rgb) {
        if (rgb[0] >= rgb[1] && rgb[0] >= rgb[2]) {
            return 0;
        }
        if (rgb[1] >= rgb[0] && rgb[1] >= rgb[2]) {
            return 1;
        }
        return 2;
    }
}
