package ca.litten.ios_obscura_server;

import ca.litten.ios_obscura_server.backend.AppList;
import ca.litten.ios_obscura_server.frontend.Server;
import ca.litten.ios_obscura_server.parser.AppDownloader;
import ca.litten.ios_obscura_server.parser.ArchiveListDecoder;
import com.google.common.escape.Escaper;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

public class Main {
    
    private static class ArchiveParser extends Thread {
        @Override
        public void run() {
            Escaper escaper = urlPathSegmentEscaper();
            LinkedList<String> urlist = new LinkedList<>();
            FileReader reader = null;
            try {
                reader = new FileReader("config.json");
                StringBuilder out = new StringBuilder();
                char[] buf = new char[4096];
                int read;
                while (reader.ready()) {
                    read = reader.read(buf);
                    for (int i = 0; i < read; i++)
                        out.append(buf[i]);
                }
                JSONObject object = new JSONObject(out.toString());
                for (Object o : object.getJSONArray("network_files")) {
                    urlist.add(o.toString());
                }
                for (Object o : object.getJSONArray("archive_org_archives")) {
                    urlist.addAll(Arrays.asList(ArchiveListDecoder
                            .getUrlListFromArchiveOrgListing(new URL("https://archive.org/download/"
                                    + o.toString() + "/" + o.toString() + "_files.xml"))));
                }
            } catch (Exception e) {
                return;
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
        try {
            server = new Server();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.startServer();
        while (true) {
            Server.allowReload = false;
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
            try {
                Thread.sleep(1000 * 60 * 60 * 24);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
