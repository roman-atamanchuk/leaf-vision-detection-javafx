package roman.algorithm;

import org.junit.jupiter.api.Test;
import roman.ClusterAnalyzer;
import roman.ClusterInfo;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClusterAnalyzerTest {

    private final ClusterAnalyzer clusterAnalyzer = new ClusterAnalyzer();

    @Test
    void findsSeparateClustersAndNumbersLargestFirst() {
        int[][] matrix = {
                {1, 1, 0, 0},
                {1, 0, 0, 1},
                {0, 0, 0, 1},
                {0, 1, 0, 0}
        };

        List<ClusterInfo> clusters = clusterAnalyzer.findClusters(matrix);

        assertEquals(3, clusters.size());
        assertEquals(3, clusters.get(0).pixelCount());
        assertEquals(2, clusters.get(1).pixelCount());
        assertEquals(1, clusters.get(2).pixelCount());
        assertEquals(1, clusters.get(0).number());
        assertEquals(2, clusters.get(1).number());
        assertEquals(3, clusters.get(2).number());
    }

    @Test
    void removesClustersBelowMinimumSize() {
        int[][] matrix = {
                {1, 1, 0, 0},
                {1, 0, 0, 1},
                {0, 0, 0, 1},
                {0, 1, 0, 0}
        };

        List<ClusterInfo> clusters = clusterAnalyzer.findClusters(matrix);
        clusterAnalyzer.removeSmallClusters(matrix, clusters, 2);

        int[][] expected = {
                {1, 1, 0, 0},
                {1, 0, 0, 1},
                {0, 0, 0, 1},
                {0, 0, 0, 0}
        };

        for (int y = 0; y < matrix.length; y++) {
            assertEquals(Arrays.toString(expected[y]), Arrays.toString(matrix[y]));
        }
    }

    @Test
    void removesClustersAboveMaximumSize() {
        int[][] matrix = {
                {1, 1, 0, 0},
                {1, 0, 0, 1},
                {0, 0, 0, 1},
                {0, 1, 0, 0}
        };

        List<ClusterInfo> clusters = clusterAnalyzer.findClusters(matrix);
        clusterAnalyzer.removeLargeClusters(matrix, clusters, 2);

        int[][] expected = {
                {0, 0, 0, 0},
                {0, 0, 0, 1},
                {0, 0, 0, 1},
                {0, 1, 0, 0}
        };

        for (int y = 0; y < matrix.length; y++) {
            assertEquals(Arrays.toString(expected[y]), Arrays.toString(matrix[y]));
        }
    }

    @Test
    void buildsNearestNeighbourRouteFromChosenStartCluster() {
        ClusterInfo first = new ClusterInfo(10, 1);
        first.addPixel(0, 0);
        first.addPixel(0, 1);

        ClusterInfo second = new ClusterInfo(20, 2);
        second.addPixel(0, 10);
        second.addPixel(0, 11);

        ClusterInfo third = new ClusterInfo(30, 3);
        third.addPixel(10, 10);
        third.addPixel(11, 10);

        List<Integer> route = clusterAnalyzer.buildNearestNeighbourRoute(List.of(first, second, third), 1);

        assertEquals(List.of(1, 2, 3), route);
    }

    @Test
    void doesNotTreatDiagonalPixelsAsSameCluster() {
        int[][] matrix = {
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        };

        List<ClusterInfo> clusters = clusterAnalyzer.findClusters(matrix);

        assertEquals(3, clusters.size());
        assertEquals(1, clusters.get(0).pixelCount());
        assertEquals(1, clusters.get(1).pixelCount());
        assertEquals(1, clusters.get(2).pixelCount());
    }

    @Test
    void keepsClusterWhenPixelCountMatchesMinimumSizeExactly() {
        int[][] matrix = {
                {1, 1, 0},
                {0, 0, 0},
                {0, 0, 0}
        };

        List<ClusterInfo> clusters = clusterAnalyzer.findClusters(matrix);
        clusterAnalyzer.removeSmallClusters(matrix, clusters, 2);

        assertEquals("[1, 1, 0]", Arrays.toString(matrix[0]));
    }

    @Test
    void routeContainsEachClusterExactlyOnce() {
        ClusterInfo first = new ClusterInfo(1, 1);
        first.addPixel(0, 0);

        ClusterInfo second = new ClusterInfo(2, 2);
        second.addPixel(0, 5);

        ClusterInfo third = new ClusterInfo(3, 3);
        third.addPixel(5, 5);

        ClusterInfo fourth = new ClusterInfo(4, 4);
        fourth.addPixel(9, 9);

        List<Integer> route = clusterAnalyzer.buildNearestNeighbourRoute(List.of(first, second, third, fourth), 3);

        assertEquals(4, route.size());
        assertEquals(List.of(1, 2, 3, 4).stream().sorted().toList(), route.stream().sorted().toList());
        assertEquals(3, route.getFirst());
    }
}
