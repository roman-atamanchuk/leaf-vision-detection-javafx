package roman.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public final class BenchmarkRunner {
    private BenchmarkRunner() {
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include("roman\\.benchmark\\..*")
                .warmupIterations(2)
                .measurementIterations(3)
                .warmupTime(TimeValue.seconds(1))
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .shouldFailOnError(true)
                .build();

        new Runner(options).run();
    }
}
