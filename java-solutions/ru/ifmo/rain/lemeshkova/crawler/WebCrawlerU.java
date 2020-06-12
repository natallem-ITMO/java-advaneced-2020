package ru.ifmo.rain.lemeshkova.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Implementation for interface {@link Crawler}
 */
public class WebCrawlerU implements Crawler {
    private class Task {
        final String url;
        final int curDepth;
        final int depth;

        Task(final String url, final int curDepth, final int depth) {
            this.url = url;
            this.curDepth = curDepth;
            this.depth = depth;
        }
    }

    private final Downloader downloader;
    private final int perHost;
    private ExecutorService downloaderThreadPool;
    private ExecutorService extractorThreadPool;
    private Map<String, MyQueue> hostQueue;


    private Set<String> downloaded;
    //Map
    private ConcurrentMap<String, IOException> errors;
    private Set<String> nextQueue;
    private Set<String> visited;
    private Phaser phaser;

    /**
     * Creates new instance of {@link WebCrawler}
     *
     * @param downloader  allows to download pages and extract links from them
     * @param downloaders maximum number of simultaneously loaded pages
     * @param extractors  maximum number of pages from which links are extracted
     * @param perHost     maximum number of pages simultaneously loaded from one host
     */
    public WebCrawlerU(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        this.downloaderThreadPool = Executors.newFixedThreadPool(downloaders);
        this.extractorThreadPool = Executors.newFixedThreadPool(extractors);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result download(final String url, final int depth) {
        downloaded = ConcurrentHashMap.newKeySet();
        errors = new ConcurrentHashMap<>();
        visited = ConcurrentHashMap.newKeySet();
        hostQueue = new HashMap<>();
        nextQueue = ConcurrentHashMap.newKeySet();
        nextQueue.add(url);
        bfs(depth);
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private void bfs(final int depth) {
        for (int i = 0; i < depth; i++) {
            phaser = new Phaser(1);
            final ArrayDeque<String> temp = new ArrayDeque<>(nextQueue);
            nextQueue.clear();
            int finalI = i;
            for (String s : temp) {
                if (visited.add(s)) download(s, finalI, depth);
            }
            phaser.arriveAndAwaitAdvance();

        }
    }

    private void download(final String url, final int curDepth, final int depth) {
        try {
            final String host = URLUtils.getHost(url);
            hostQueue.putIfAbsent(host, new MyQueue());
            phaser.register();
            hostQueue.get(host).add(new Task(url, curDepth, depth));
        } catch (final MalformedURLException e) {
            errors.put(url, e);
        }
    }

    private class MyQueue {
        ///Deque
        private final ArrayDeque<Task> queue;
        private int count;

        MyQueue() {
            queue = new ArrayDeque<>();
            count = 0;
        }

        synchronized private void finished() {
            count--;
        }

        synchronized void run() {
            if (count < perHost && queue.size() > 0) {
                count++;
                downloaderThreadPool.submit(new DownloaderThread(queue.removeFirst(), this));
            }
        }

        synchronized void add(final Task task) {
            queue.addLast(task);
            run();
        }

    }

    class DownloaderThread implements Runnable {
        final Task task;
        final MyQueue host;

        DownloaderThread(final Task task, final MyQueue host) {
            this.task = task;
            this.host = host;
        }

        @Override
        public void run() {
            downloadImpl(task);
            host.finished();
            host.run();
            phaser.arrive();
        }
    }

    private void downloadImpl(final Task task) {
        try {
            final Document document = downloader.download(task.url);
            downloaded.add(task.url);
            if (task.depth - task.curDepth > 1) {
                extract(document);
            }
        } catch (final IOException e) {
            errors.put(task.url, e);
        }
    }

    private void extract(final Document document) {
        phaser.register();
        extractorThreadPool.submit(new ExtractorThread(document));
    }

    class ExtractorThread implements Runnable {
        final Document document;

        ExtractorThread(final Document document) {
            this.document = document;
        }

        @Override
        public void run() {
            try {
                extractImpl(document);
            } catch (final IOException ignored) {
            }
            phaser.arrive();
        }
    }

    private void extractImpl(final Document document) throws IOException {
        final Set<String> next = (document.extractLinks().stream().filter(s -> !visited.contains(s)).collect(Collectors.toSet()));
        nextQueue.addAll(next);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.downloaderThreadPool.shutdown();
        this.extractorThreadPool.shutdown();
        try {
            this.downloaderThreadPool.awaitTermination(2000, TimeUnit.MILLISECONDS);
            this.extractorThreadPool.awaitTermination(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
