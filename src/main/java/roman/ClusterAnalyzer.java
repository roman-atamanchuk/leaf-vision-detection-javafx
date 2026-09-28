package roman;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClusterAnalyzer {
    public List<ClusterInfo> findClusters(int[][] matrix) {
        int height = matrix.length;
        int width = matrix[0].length;
        UnionFind unionFind = new UnionFind(width * height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[y][x] != 1) {
                    continue;
                }

                if (x + 1 < width && matrix[y][x + 1] == 1) {
                    unionFind.union(index(y, x, width), index(y, x + 1, width));
                }
                if (y + 1 < height && matrix[y + 1][x] == 1) {
                    unionFind.union(index(y, x, width), index(y + 1, x, width));
                }
            }
        }

        Map<Integer, ClusterInfo> clusterMap = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[y][x] != 1) {
                    continue;
                }

                int root = unionFind.find(index(y, x, width));
                ClusterInfo cluster = clusterMap.computeIfAbsent(root, ignored -> new ClusterInfo(root, 0));
                cluster.addPixel(y, x);
            }
        }

        List<ClusterInfo> rawClusters = clusterMap.values().stream()
                .sorted(Comparator.comparingInt(ClusterInfo::pixelCount).reversed())
                .toList();

        List<ClusterInfo> numberedClusters = new ArrayList<>();
        for (int i = 0; i < rawClusters.size(); i++) {
            ClusterInfo source = rawClusters.get(i);
            ClusterInfo numbered = new ClusterInfo(source.root(), i + 1);
            for (int[] pixel : source.pixels()) {
                numbered.addPixel(pixel[0], pixel[1]);
            }
            numberedClusters.add(numbered);
        }
        return numberedClusters;
    }

    public void removeSmallClusters(int[][] matrix, List<ClusterInfo> clusters, int minClusterSize) {
        for (ClusterInfo cluster : clusters) {
            if (cluster.pixelCount() >= minClusterSize) {
                continue;
            }

            for (int[] pixel : cluster.pixels()) {
                matrix[pixel[0]][pixel[1]] = 0;
            }
        }
    }

    public void removeLargeClusters(int[][] matrix, List<ClusterInfo> clusters, int maxClusterSize) {
        for (ClusterInfo cluster : clusters) {
            if (cluster.pixelCount() <= maxClusterSize) {
                continue;
            }

            for (int[] pixel : cluster.pixels()) {
                matrix[pixel[0]][pixel[1]] = 0;
            }
        }
    }

    public List<Integer> buildNearestNeighbourRoute(List<ClusterInfo> clusters, int startNumber) {
        List<Integer> remaining = new ArrayList<>();
        for (int i = 1; i <= clusters.size(); i++) {
            if (i != startNumber) {
                remaining.add(i);
            }
        }

        List<Integer> route = new ArrayList<>();
        route.add(startNumber);
        int current = startNumber;

        while (!remaining.isEmpty()) {
            int previous = current;
            current = remaining.stream()
                    .min(Comparator.comparingDouble(candidate -> distanceBetween(clusters, previous, candidate)))
                    .orElseThrow();
            route.add(current);
            remaining.remove(Integer.valueOf(current));
        }

        return route;
    }

    private double distanceBetween(List<ClusterInfo> clusters, int firstClusterNumber, int secondClusterNumber) {
        ClusterInfo first = clusters.get(firstClusterNumber - 1);
        ClusterInfo second = clusters.get(secondClusterNumber - 1);
        double dx = first.centerX() - second.centerX();
        double dy = first.centerY() - second.centerY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private int index(int y, int x, int width) {
        return y * width + x;
    }
}
