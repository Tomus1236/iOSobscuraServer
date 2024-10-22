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

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

public class Main {
    private static URL[] archive_urls;
    private static String[] random_ipa_urls;
    
    /* Sources:
     *  - https://github.com/relikd/ipa-archive/blob/main/data/urls.json
     */
    static {
        try {
            archive_urls = new URL[]{
                    new URL("https://archive.org/download/mactracker-ipa-collection/mactracker-ipa-collection_files.xml"),
                    new URL("https://archive.org/download/apps-ios/apps-ios_files.xml"),
                    new URL("https://archive.org/download/virtually-extinct-ipas/virtually-extinct-ipas_files.xml"),
                    new URL("https://archive.org/download/ios_2_ipa/ios_2_ipa_files.xml"),
                    new URL("https://archive.org/download/ios_2_ipa_p2/ios_2_ipa_p2_files.xml"),
                    new URL("https://archive.org/download/ios_3_2_ipa/ios_3_2_ipa_files.xml"),
                    new URL("https://archive.org/download/ios_3_ipa/ios_3_ipa_files.xml"),
                    new URL("https://archive.org/download/ios_40_42_ipa/ios_40_42_ipa_files.xml"),
                    new URL("https://archive.org/download/iOSObscura/iOSObscura_files.xml"),
                    new URL("https://archive.org/download/jos-ipa-archive/jos-ipa-archive_files.xml"),
                    new URL("https://archive.org/download/apple-ios-logo-png-open-2000/apple-ios-logo-png-open-2000_files.xml"),
                    new URL("https://archive.org/download/alyssas-ios-ipa-archive/alyssas-ios-ipa-archive_files.xml"),
                    new URL("https://archive.org/download/iphone-IPA-1/iphone-IPA-1_files.xml"),
                    new URL("https://archive.org/download/ios-ipa-collection/ios-ipa-collection_files.xml"),
                    new URL("https://archive.org/download/hot-donut-hd-v-1.3/hot-donut-hd-v-1.3_files.xml")
            };
            random_ipa_urls = new String[]{
                // https://mtmdev.org/forum/index.php?threads/all-geometry-dash-ipas-in-existance-probably-idk.4290/
                "https://files.catbox.moe/vpvw7b.ipa", // GD 1.0
                "https://files.catbox.moe/e3ztj4.ipa", // GD 1.4
                "https://files.catbox.moe/4t7jix.ipa", // GD 1.7
                "https://files.catbox.moe/e5973l.ipa", // GD 1.8
                "https://files.catbox.moe/cmkpko.ipa", // GD 1.9
                "https://files.catbox.moe/bk69e2.ipa", // GD 2.0
                "https://files.catbox.moe/8cburs.ipa", // GD 2.11
                "https://files.catbox.moe/tglcyg.ipa"  // GD 2.10
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
            urlist.addAll(Arrays.asList(random_ipa_urls));
            for (URL url : archive_urls) {
                urlist.addAll(Arrays.asList(ArchiveListDecoder.getUrlListFromArchiveOrgListing(url)));
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
                if (url.contains("iOSObscura") && (url.contains("PossiblyBroken") || url.contains("Homebrew%20IPAs")))
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
