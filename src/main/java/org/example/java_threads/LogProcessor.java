package org.example.java_threads;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogProcessor {

    public static void main(String[] args) throws Exception {
        String filePath = LogProcessor.class.getResource("apache_logs.txt").getPath();
        int chunkSize = 10; // Количество строк в одном чанке
        int threadCount = 4;  // Количество потоков

        long totalBytes = processFile(filePath, chunkSize, threadCount);
        System.out.println("Общее количество байт: " + totalBytes);
    }

    public static long processFile(String filePath, int chunkSize, int threadCount) throws Exception {

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
}

class LogChunkProcessor implements Callable<Long> {
    private final List<String> lines;

    public LogChunkProcessor(List<String> lines) {
        this.lines = lines;
    }

    @Override
    public Long call() {
        //Чтобы увидеть какие потоки выполняются
        System.out.println(Thread.currentThread().getName());
        long totalBytes = 0;
        for (String line : lines) {
            totalBytes += parseBytes(line);
        }
        return totalBytes;
    }

    private long parseBytes(String logLine) {
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
