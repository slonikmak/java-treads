package org.example.java_threads;

import java.util.concurrent.atomic.AtomicInteger;

public class RaceConditionExample {

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Результат не соответствует ожидаемому 2000: " + counter.getCount());

        //Решение 1: использование synchronized методов
        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        Runnable synchronizedTask = () -> {
            for (int i = 0; i < 1000; i++) {
                synchronizedCounter.increment();
            }
        };

        t1 = new Thread(synchronizedTask);
        t2 = new Thread(synchronizedTask);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Результат соответствует ожидаемому 2000: " + synchronizedCounter.getCount());

        //Решение 2: использование класса AtomicCounter

        AtomicCounter atomicCounter = new AtomicCounter();
        Runnable atomicTask = () -> {
            for (int i = 0; i < 1000; i++) {
                atomicCounter.increment();
            }
        };

        t1 = new Thread(atomicTask);
        t2 = new Thread(atomicTask);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Результат соответствует ожидаемому 2000: " + atomicCounter.getCount());

    }
}

class Counter {
    private int count = 0;

    public void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}

class SynchronizedCounter {
    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}

class AtomicCounter {
    private AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet();
    }

    public int getCount() {
        return count.get();
    }
}