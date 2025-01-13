package org.example.java_threads;

public class DeadlockExample {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread 1: Holding lock1...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                synchronized (lock2) {
                    System.out.println("Thread 1: Acquired lock2!");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("Thread 2: Holding lock2...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                synchronized (lock1) {
                    System.out.println("Thread 2: Acquired lock1!");
                }
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("This line will never be reached!");
    }
}