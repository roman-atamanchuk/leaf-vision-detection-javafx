package roman;

import java.util.ArrayList;
import java.util.List;

public final class ClusterInfo {
    private final int root;
    private final int number;
    private final List<int[]> pixels = new ArrayList<>();
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;

    public ClusterInfo(int root, int number) {
        this.root = root;
        this.number = number;
    }

    public void addPixel(int y, int x) {
        pixels.add(new int[]{y, x});
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
    }

    public int root() {
        return root;
    }

    public int number() {
        return number;
    }

    public List<int[]> pixels() {
        return pixels;
    }

    public int pixelCount() {
        return pixels.size();
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public double centerX() {
        return (minX + maxX) / 2.0;
    }

    public double centerY() {
        return (minY + maxY) / 2.0;
    }
}
