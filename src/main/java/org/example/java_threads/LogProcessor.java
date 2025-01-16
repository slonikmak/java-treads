package org.example.java_threads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Gatherers;

public class LogProcessor {

    public static void main(String[] args) throws Exception {
        String filePath = LogProcessor.class.getResource("apache_logs.txt").getPath();
        int chunkSize = 10; // Количество строк в одном чанке
        int threadCount = 8;  // Количество потоков

        long totalBytes = processFileThreadPool(filePath, chunkSize, threadCount);
        System.out.println("Общее количество байт: " + totalBytes);

        totalBytes = processFileByVirtualThread(filePath, chunkSize);
        System.out.println("Общее количество байт: " + totalBytes);

        totalBytes = processFileByMap(filePath);
        System.out.println("Общее количество байт: " + totalBytes);

        totalBytes = processFileByMapVirtualThreads(filePath);
        System.out.println("Общее количество байт: " + totalBytes);
    }

    public static long processFileSingleThread(String filePath) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            Parser parser = new Parser();
            long totalBytes = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                totalBytes += parser.parseBytes(line);
            }
            return totalBytes;
        }
    }

    public static long processFileThreadPool(String filePath, int chunkSize, int threadCount) throws Exception {

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount);
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            List<Future<Long>> results = new ArrayList<>();

            List<String> chunk = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                chunk.add(line);
                if (chunk.size() == chunkSize) {
                    results.add(executor.submit(new LogChunkProcessor(new ArrayList<>(chunk))));
                    chunk.clear();
                }
            }

            // Добавляем последний чанк, если остались строки
            if (!chunk.isEmpty()) {
                results.add(executor.submit(new LogChunkProcessor(chunk)));
            }
            // Завершаем работу потоков после обработки всех чанков
            executor.shutdown();

            // Суммируем байты из всех потоков
            long totalBytes = 0;
            for (Future<Long> result : results) {
                totalBytes += result.get();
            }

            return totalBytes;
        }
    }


    public static long processFileByVirtualThread(String filePath, int chunkSize) throws Exception {

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            List<Future<Long>> results = new ArrayList<>();

            List<String> chunk = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                chunk.add(line);
                if (chunk.size() == chunkSize) {
                    results.add(executor.submit(new LogChunkProcessor(new ArrayList<>(chunk))));
                    chunk.clear();
                }
            }

            // Добавляем последний чанк, если остались строки
            if (!chunk.isEmpty()) {
                results.add(executor.submit(new LogChunkProcessor(chunk)));
            }
            // Завершаем работу потоков после обработки всех чанков
            executor.shutdown();

            // Суммируем байты из всех потоков
            long totalBytes = 0;
            for (Future<Long> result : results) {
                totalBytes += result.get();
            }

            return totalBytes;
        }
    }

    //Пример использования parallelStream, но не очень эффективный в данном случае
    public static long processFileByMap(String filePath) throws Exception {
        try (var lines = Files.lines(new File(filePath).toPath())) {

            //Тут используется стандартный пул потоков, который создает потоки в зависимости от количества ядер - ForkJoinPool
            return lines
                    .parallel()
                    .map(l -> new Parser().parseBytes(l))
                    .reduce(Long::sum).orElse(0L);
        }
    }


    public static long processFileByMapVirtualThreads(String filePath) throws Exception {
        try (var lines = Files.lines(new File(filePath).toPath())) {

            //Тут используется стандартный пул потоков, который создает потоки в зависимости от количества ядер - ForkJoinPool
            return lines
                    .gather(Gatherers.mapConcurrent(100, l -> new Parser().parseBytes(l)))
                    .reduce(Long::sum).orElse(0L);
        }
    }

    public static long processFileByForkJoin(String filePath, int chunkSize) throws Exception {
        try (ForkJoinPool forkJoinPool = new ForkJoinPool()) {
            List<String> lines = Files.readAllLines(new File(filePath).toPath());
            ForkJoinTask<Long> task = new LogForkJoinTask(lines, chunkSize, 0, lines.size());
            return forkJoinPool.invoke(task);
        }
    }

}

class LogChunkProcessor implements Callable<Long> {
    private final List<String> lines;
    private final Parser parser = new Parser();

    public LogChunkProcessor(List<String> lines) {
        this.lines = lines;
    }

    @Override
    public Long call() {
        long totalBytes = 0;
        for (String line : lines) {
            totalBytes += parser.parseBytes(line);
        }
        return totalBytes;
    }


}

class Parser {

    public long parseBytes(String logLine) {
        // Регулярное выражение для CLF
        String logPattern = "^(\\S+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"([^\"]+)\" (\\d{3}) (\\S+) \"([^\"]*)\" \"([^\"]*)\"$";
        Pattern pattern = Pattern.compile(logPattern);
        Matcher matcher = pattern.matcher(logLine);

        if (matcher.matches()) {
            try {
                String bytesField = matcher.group(7); // 7-я группа соответствует числу байтов
                return bytesField.equals("-") ? 0 : Long.parseLong(bytesField);
            } catch (NumberFormatException e) {
                // Если поле байтов некорректное, игнорируем строку
                return 0;
            }
        }

        // Строка не соответствует формату
        return 0;
    }
}


class LogForkJoinTask extends RecursiveTask<Long> {
    private final int threshold;
    private final List<String> lines;
    private final int start;
    private final int end;
    private final Parser parser = new Parser();

    public LogForkJoinTask(List<String> lines, int threshold, int start, int end) {
        this.lines = lines;
        this.threshold = threshold;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        if (end - start <= threshold) {
            long totalBytes = 0;
            for (int i = start; i < end; i++) {
                totalBytes += parser.parseBytes(lines.get(i));
            }
            return totalBytes;
        } else {
            int mid = (start + end) / 2;
            LogForkJoinTask leftTask = new LogForkJoinTask(lines, threshold, start, mid);
            LogForkJoinTask rightTask = new LogForkJoinTask(lines, threshold, mid, end);
            leftTask.fork();
            long rightResult = rightTask.compute();
            long leftResult = leftTask.join();
            return leftResult + rightResult;
        }
    }
}
