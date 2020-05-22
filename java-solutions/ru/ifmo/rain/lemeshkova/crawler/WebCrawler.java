package ru.ifmo.rain.lemeshkova.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;
    private final Map<String, HostHandler> hostDataMap;


    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        this.hostDataMap = new ConcurrentHashMap<>();
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> processUrls = new ConcurrentSkipListSet<>();
        processUrls.add(url);
        return bfs(depth, processUrls);
    }

    private Result bfs(int depth, Set<String> processUrls) {
        Set<String> result = new HashSet<>();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        for (int i = 0; i < depth; i++) {
            Phaser phaser = new Phaser(1);
            List<String> currentProcessingUrls = new ArrayList<>(processUrls);
            processUrls.clear();
            boolean extractable = (depth - i) != 1;
            currentProcessingUrls.stream().filter(x -> !result.contains(x)).forEach(url -> {
                result.add(url);
                phaser.register();
                try {
                    String host = URLUtils.getHost(url);
                    HostHandler hostHandler = hostDataMap.computeIfAbsent(host, s -> new HostHandler());
                    hostHandler.addToQueueOrSubmit(url, processUrls, errors, phaser, extractable);
                } catch (MalformedURLException e) {
                    errors.put(url, e);
                }
            });
            phaser.arriveAndAwaitAdvance();
        }
        result.removeAll(errors.keySet());
        return new Result(new ArrayList<>(result), errors);
    }

    private void downloadUrlAndSubmitExtraction(String url, Set<String> futureProcessingUrls, Map<String, IOException> errors,
                                                Phaser phaser, boolean isExtractable) {
        try {
            Document downloadedDocument = downloader.download(url);
            if (isExtractable) {
                phaser.register();
                extractors.submit(() -> {
                    try {
                        futureProcessingUrls.addAll(downloadedDocument.extractLinks());
                    } catch (IOException ignored) {
                        // No operations.
                    } finally {
                        phaser.arrive();
                    }
                });
            }
        } catch (IOException e) {
            errors.put(url, e);
        }
    }

    private void downloadThreadFunction(String currentUrl, HostHandler hostHandler, Set<String> futureProcessingUrls,
                                        Map<String, IOException> errors, Phaser phaser, boolean isExtractable) {
        while (currentUrl != null) {
            downloadUrlAndSubmitExtraction(currentUrl, futureProcessingUrls, errors, phaser, isExtractable);
            currentUrl = hostHandler.getUrlOrDecreaseHostDownloadingNumber();
            phaser.arrive();
        }
    }

    private class HostHandler {
        private int downloadingUrlCount;
        private final Queue<String> notProcessedHostUrls;

        public HostHandler() {
            this.downloadingUrlCount = 0;
            this.notProcessedHostUrls = new ArrayDeque<>();
        }

        private synchronized void addToQueueOrSubmit(String URL, Set<String> futureProcessingUrls, Map<String, IOException> errors,
                                                     Phaser phaser, boolean extractable) {
            if (downloadingUrlCount < perHost) {
                downloadingUrlCount++;
                downloaders.submit(() -> downloadThreadFunction(URL, this, futureProcessingUrls, errors, phaser, extractable));
            } else {
                notProcessedHostUrls.add(URL);
            }
        }

        private synchronized String getUrlOrDecreaseHostDownloadingNumber() {
            if (!notProcessedHostUrls.isEmpty()) {
                return notProcessedHostUrls.poll();
            }
            downloadingUrlCount--;
            return null;
        }

    }

    @Override
    public void close() {
        downloaders.shutdown();
        extractors.shutdown();
        final long timeout = Long.MAX_VALUE;

        try {
            downloaders.awaitTermination(5, TimeUnit.MILLISECONDS);
            extractors.awaitTermination(5, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

   /* private static int getArgumentOrDefault(String[] args, int index, int defaultValue) {
        return (args.length > index) ? Integer.parseInt(args[index]) : defaultValue;
    }

    private static void showResult(Result result) {
        System.out.println("Downloaded " + result.getDownloaded().size() + " :\n" +
                String.join("\n", result.getDownloaded()));
        System.out.println("URL with errors " + result.getErrors().size() + " :\n");
        result.getErrors().forEach((key, value) -> System.out.println(key + " - " + value.getMessage()));
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.format("Incorrect number of arguments.%nUsage:"
                    + " WebCrawler <URL> [depth [downloaders [extractors [perHost]]]]");
            return;
        }
        try {
            String URL = args[0];
            Downloader downloader = new CachingDownloader();
            int depth = getArgumentOrDefault(args, 1, 2);
            int downloaders = getArgumentOrDefault(args, 2, 10);
            int extractors = getArgumentOrDefault(args, 3, 10);
            int perHost = getArgumentOrDefault(args, 4, 2);
            showResult(new WebCrawler(downloader, downloaders, extractors, perHost).download(URL, depth));
        } catch (IOException e) {
            System.err.format("Incorrect usage.%nUsage: WebCrawler <URL> [depth [downloaders [extractors [perHost]]]]");
        }
    }*/
}





