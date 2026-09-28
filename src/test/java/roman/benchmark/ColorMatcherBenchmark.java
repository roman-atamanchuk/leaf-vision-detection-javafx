package roman.benchmark;

import javafx.scene.paint.Color;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import roman.ColorMatcher;

public class ColorMatcherBenchmark {

    @State(Scope.Thread)
    public static class BenchmarkState {
        final Color pixel = Color.rgb(210, 145, 35);
        final Color[] references = {
                Color.rgb(200, 120, 20),
                Color.rgb(180, 160, 40),
                Color.rgb(230, 90, 30),
                null,
                null
        };
    }

    @Benchmark
    public boolean matchesReferenceColor(BenchmarkState state) {
        return ColorMatcher.matchesReferenceColor(state.pixel, state.references, 3, 40);
    }
}
