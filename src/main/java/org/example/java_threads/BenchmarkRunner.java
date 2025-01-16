package org.example.java_threads;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class BenchmarkRunner {
    String filePath;
    @Param({"10", "100", "1000"})
    int chunkSize;

    @Setup
    public void setup() {
        filePath = LogProcessor.class.getResource("apache_logs.txt").getPath();
        //chunkSize = 100;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(value = 2, warmups = 1)
    @Threads(4)
    public void benchmarkProcessFileSingleThread() throws Exception {
        LogProcessor.processFileSingleThread(filePath);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(value = 2, warmups = 1)
    @Threads(4)
    public void benchmarkProcessFileThreadPool() throws Exception {
        LogProcessor.processFileThreadPool(filePath, chunkSize, 8);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(value = 2, warmups = 1)
    @Threads(4)
    public void benchmarkProcessFileByVirtualThread() throws Exception {
        LogProcessor.processFileByVirtualThread(filePath, chunkSize);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(value = 2, warmups = 1)
    @Threads(4)
    public void benchmarkProcessFileByParallelMap() throws Exception {
        LogProcessor.processFileByMap(filePath);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(value = 2, warmups = 1)
    @Threads(4)
    public void benchmarkProcessFileByMapVirtualThreads() throws Exception {
        LogProcessor.processFileByMapVirtualThreads(filePath);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(value = 2, warmups = 1)
    @Threads(4)
    public void benchmarkProcessFileByForkJoin() throws Exception {
        LogProcessor.processFileByForkJoin(filePath, chunkSize);
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
