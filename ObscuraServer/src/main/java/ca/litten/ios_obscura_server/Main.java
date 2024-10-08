package ca.litten.ios_obscura_server;

import ca.litten.ios_obscura_server.backend.AppList;
import ca.litten.ios_obscura_server.frontend.Server;
import ca.litten.ios_obscura_server.parser.AppDownloader;
import ca.litten.ios_obscura_server.parser.ArchiveListDecoder;
import com.google.common.escape.Escaper;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

public class Main {
    private static URL[] archive_urls;
    
    static {
        try {
            archive_urls = new URL[]{
                    new URL("https://archive.org/download/iOSObscura/iOSObscura_files.xml"),
                    new URL("https://archive.org/download/jos-ipa-archive/jos-ipa-archive_files.xml")
            };
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static class ArchiveParser extends Thread {
        @Override
        public void run() {
            Escaper escaper = urlPathSegmentEscaper();
            LinkedList<String> urlist = new LinkedList<>();
            for (URL url : archive_urls) {
                urlist.addAll(List.of(ArchiveListDecoder.getUrlListFromArchiveOrgListing(url)));
            }
            Thread task;
            Thread[] tasks = new Thread[Runtime.getRuntime().availableProcessors()];
            task = new Thread(() -> {});
            Arrays.fill(tasks, task);
            for (String temp : urlist) {
                String[] urlfrags = temp.split("/");
                String url = "";
                for (String frag : urlfrags) {
                    url += escaper.escape(frag) + "/";
                }
                url = url.substring(0, url.length() - 1);
                if (url.contains("PossiblyBroken") || url.contains("Homebrew%20IPAs"))
                    continue;
                boolean good;
                while (true) {
                    try {
                        good = AppList.appUrlAlreadyExists(url);
                        break;
                    } catch (ConcurrentModificationException e) {
                        // Do nothing
                    }
                }
                if (good)
                    continue;
                String finalUrl = url;
                task = new Thread(() -> {
                    try {
                        AppDownloader.downloadAndAddApp(new URL(finalUrl));
                        System.out.println("Parsed: " + finalUrl);
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                });
                while (!task.isAlive()) {
                    for (int i = 0; i < tasks.length; i++) {
                        if (!tasks[i].isAlive()) {
                            tasks[i] = task;
                            task.start();
                            break;
                        }
                    }
                }
            }
            for (Thread thread : tasks) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // LOL
                }
            }
        }
    }
    
    private static Server server;
    
    public static void main(String[] args) {
        AppList.loadAppDatabaseFile(new File("db.json"));
        {
            int port;
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception e) {
                port = 12345;
            }
            try {
                server = new Server(new InetSocketAddress(port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        server.startServer();
        if (Arrays.stream(args).noneMatch(a -> a.equals("--noParse"))) {
            ArchiveParser archiveParser = new ArchiveParser();
            archiveParser.start();
            while (archiveParser.isAlive()) {
                try {
                    Thread.sleep(1000 * 60 * 2);
                } catch (InterruptedException e) {
                    System.out.println("Saving database...");
                    AppList.saveAppDatabaseFile(new File("db.json"));
                    break;
                }
                System.out.println("Saving database...");
                AppList.saveAppDatabaseFile(new File("db.json"));
            }
            System.out.println("Finished parsing!");
        }
        Server.allowReload = true;
    }
}
