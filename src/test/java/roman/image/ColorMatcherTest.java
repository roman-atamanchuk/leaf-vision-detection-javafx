package roman.image;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import roman.ColorMatcher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorMatcherTest {

    @Test
    void matchesWhenDominantChannelAndDifferenceFitThreshold() {
        Color[] references = {Color.rgb(150, 255, 100), null, null, null, null};

        boolean matches = ColorMatcher.matchesReferenceColor(
                Color.rgb(100, 240, 30),
                references,
                1,
                20
        );

        assertTrue(matches);
    }

    @Test
    void rejectsWhenDominantChannelIsDifferent() {
        Color[] references = {Color.rgb(200, 50, 20), null, null, null, null};

        boolean matches = ColorMatcher.matchesReferenceColor(
                Color.rgb(100, 250, 0),
                references,
                1,
                255
        );

        assertFalse(matches);
    }

    @Test
    void matchesAutomaticallyWhenNoReferenceColorsExist() {
        boolean matches = ColorMatcher.matchesReferenceColor(
                Color.rgb(10, 20, 30),
                new Color[5],
                0,
                5
        );

        assertTrue(matches);
    }

    @Test
    void convertsColorToRgbChannels() {
        assertArrayEquals(new int[]{255, 128, 0}, ColorMatcher.toRgb(Color.rgb(255, 128, 0)));
    }

    @Test
    void detectsDominantChannelIndex() {
        assertEquals(0, ColorMatcher.dominantChannelIndex(new int[]{200, 50, 10}));
        assertEquals(1, ColorMatcher.dominantChannelIndex(new int[]{90, 180, 80}));
        assertEquals(2, ColorMatcher.dominantChannelIndex(new int[]{30, 60, 220}));
    }

    @Test
    void rejectsWhenDifferenceExceedsThreshold() {
        Color[] references = {Color.rgb(150, 255, 100), null, null, null, null};

        boolean matches = ColorMatcher.matchesReferenceColor(
                Color.rgb(100, 210, 30),
                references,
                1,
                20
        );

        assertFalse(matches);
    }

    @Test
    void ignoresNullEntriesInsideReferenceArray() {
        Color[] references = {null, Color.rgb(20, 40, 240), null, null, null};

        boolean matches = ColorMatcher.matchesReferenceColor(
                Color.rgb(30, 50, 230),
                references,
                5,
                20
        );

        assertTrue(matches);
    }
}
