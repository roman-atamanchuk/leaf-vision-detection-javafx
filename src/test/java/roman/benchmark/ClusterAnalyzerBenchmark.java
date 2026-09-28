package roman.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import roman.ClusterAnalyzer;

public class ClusterAnalyzerBenchmark {

    @State(Scope.Thread)
    public static class BenchmarkState {
        final ClusterAnalyzer clusterAnalyzer = new ClusterAnalyzer();
        final int[][] matrix = createMatrix();

        private static int[][] createMatrix() {
            int size = 200;
            int[][] matrix = new int[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    matrix[y][x] = ((x * 7 + y * 11) % 19) < 6 ? 1 : 0;
                }
            }
            return matrix;
        }
    }

    @Benchmark
    public Object findClusters(BenchmarkState state) {
        return state.clusterAnalyzer.findClusters(state.matrix);
    }
}
