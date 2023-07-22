package info.kgeorgiy.ja.garipov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final ExecutorService downloadService;
    private final ExecutorService extractService;
    // :NOTE: Не потокобезопасный
    private final int perHost;

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        this.downloadService = Executors.newFixedThreadPool(downloaders);
        this.extractService = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    @Override
    public Result download(final String url, final int depth) {
        return launch(url, depth, null);
    }

    @Override
    public Result download(final String url, final int depth, final List<String> hosts) {
        // :NOTE: Не потокобезопасный
        final Set<String> avaliableHosts = new HashSet<>(hosts);
        return launch(url, depth, avaliableHosts);
    }

    private Result launch(final String url, final int depth, final Set<String> hosts) {
        ConcurrentLinkedQueue<String> urlsToDownloadQueue = new ConcurrentLinkedQueue<>();
        final Set<String> visited = ConcurrentHashMap.newKeySet();
        final ConcurrentHashMap<String, IOException> errorOccurred = new ConcurrentHashMap<>();
        final HashMap<String, HostDownloadersTaskQueue> hostsQueue = new HashMap<>();
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        urlsToDownloadQueue.add(url);
        final Phaser phaser = new Phaser(0);
        phaser.register();
        visited.add(url);
        // :NOTE: Дублирование
        boolean banned = false;
        if (hosts != null) {
            try {
                banned = !hosts.contains(URLUtils.getHost(url));
            } catch (final MalformedURLException e) {
                errorOccurred.put(url, e);
                banned = true;
            }
        }
        if (!banned) {
            for (int i = 1; i <= depth; i++) {
                urlsToDownloadQueue = downloadLayer(
                        urlsToDownloadQueue,
                        hostsQueue,
                        visited,
                        errorOccurred,
                        downloaded,
                        hosts,
                        phaser
                );
            }
        }
        return new Result(new ArrayList<>(downloaded), errorOccurred);
    }

    // :NOTE: Аргуенты россыпью
    private ConcurrentLinkedQueue<String> downloadLayer(final ConcurrentLinkedQueue<String> urlsToDownloadQueue,
                                                        final HashMap<String, HostDownloadersTaskQueue> hostsQueue,
                                                        final Set<String> visited,
                                                        final ConcurrentHashMap<String, IOException> errorOccurred,
                                                        final Set<String> downloaded,
                                                        final Set<String> hosts,
                                                        final Phaser phaser
    ) {
        final ConcurrentLinkedQueue<String> curUrlsQueue = new ConcurrentLinkedQueue<>();
        while (true) {
            final String url = urlsToDownloadQueue.poll();
            if (url == null) {
                break;
            }
            try {
                final HostDownloadersTaskQueue queue =
                        hostsQueue.computeIfAbsent(URLUtils.getHost(url), unnamed -> new HostDownloadersTaskQueue());
                final String urlHost = URLUtils.getHost(url);
                if (hosts != null && !hosts.contains(urlHost)) {
                    continue;
                }
                phaser.register();
                queue.add(() -> {
                    try {
                        final Document document = downloader.download(url);
                        downloaded.add(url);
                        phaser.register();
                        extractService.submit(() -> {
                            try {
                                document.extractLinks().forEach((curUrl) -> {
                                    if (!visited.contains(curUrl)) {
                                        visited.add(curUrl);
                                        curUrlsQueue.add(curUrl);
                                    }
                                });
                            } catch (final IOException e) {
                                errorOccurred.put(url, e);
                            } finally {
                                phaser.arriveAndDeregister();
                            }
                        });
                    } catch (final IOException e) {
                        errorOccurred.put(url, e);
                    } finally {
                        phaser.arriveAndDeregister();
                        queue.takeAndLaunch();
                    }
                });
            } catch (final MalformedURLException e) {
                errorOccurred.put(url, e);
            }
        }
        phaser.arriveAndAwaitAdvance();
        return curUrlsQueue;
    }

    @Override
    public void close() {
        downloadService.shutdown();
        extractService.shutdown();
    }

    public static void main(final String[] args) {
        int perHost = -1, extractors = 10, downloads = 10, depth = 1;
        final String url;
        final int len = args.length;
        switch (len) {
            case 5:
                perHost = Integer.parseInt(args[4]);
            case 4:
                extractors = Integer.parseInt(args[3]);
            case 3:
                downloads = Integer.parseInt(args[2]);
            case 2:
                depth = Integer.parseInt(args[1]);
            case 1:
                url = args[0];
                break;
            case 0:
                throw new IllegalArgumentException("URL required, format: " +
                        "WebCrawler url [depth [downloads [extractors [perHost]]]]");
            default:
                throw new IllegalArgumentException("At most five arguments required, format: " +
                        "WebCrawler url [depth [downloads [extractors [perHost]]]]");
        }
        if (perHost == -1) {
            perHost = downloads;
        }
        try (final WebCrawler crawler = new WebCrawler(new CachingDownloader(), downloads, extractors, perHost)) {
            final Result res = crawler.download(url, depth);
            System.out.println("Downloaded = " + Arrays.toString(res.getDownloaded().toArray()));
            System.out.println("Errors = " + res.getErrors().toString());
        } catch (final IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }


    private final class HostDownloadersTaskQueue {
        private int running = 0;
        private final Queue<Runnable> queue = new ArrayDeque<>();

        public synchronized void add(final Runnable task) {
            if (running < perHost) {
                running++;
                downloadService.submit(task);
            } else {
                queue.add(task);
            }
        }

        private synchronized void takeAndLaunch() {
            final Runnable task = queue.poll();
            if (task != null) {
                // running = running + 1 - 1
                extractService.submit(task);
            } else {
                running--;
            }
        }
    }
}
