package roman.model;

import org.junit.jupiter.api.Test;
import roman.ClusterInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClusterInfoTest {

    @Test
    void tracksBoundsAndCenterAsPixelsAreAdded() {
        ClusterInfo cluster = new ClusterInfo(42, 3);
        cluster.addPixel(4, 7);
        cluster.addPixel(6, 11);
        cluster.addPixel(5, 9);

        assertEquals(3, cluster.pixelCount());
        assertEquals(7, cluster.minX());
        assertEquals(11, cluster.maxX());
        assertEquals(4, cluster.minY());
        assertEquals(6, cluster.maxY());
        assertEquals(9.0, cluster.centerX());
        assertEquals(5.0, cluster.centerY());
    }

    @Test
    void preservesRootAndDisplayNumber() {
        ClusterInfo cluster = new ClusterInfo(99, 7);

        assertEquals(99, cluster.root());
        assertEquals(7, cluster.number());
    }
}
