package ca.litten.ios_obscura_server;

import ca.litten.ios_obscura_server.backend.AppList;
import ca.litten.ios_obscura_server.parser.AppDownloader;
import ca.litten.ios_obscura_server.parser.ArchiveListDecoder;
import com.google.common.escape.Escaper;

import java.net.MalformedURLException;
import java.net.URL;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

public class Main {
    private static URL archive_url;
    
    static {
        try {
            archive_url = new URL("https://archive.org/download/iOSObscura/iOSObscura_files.xml");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void main(String[] args) {
        Thread archiveParser = new Thread(() -> {
            Escaper escaper = urlPathSegmentEscaper();
            String[] urlist = ArchiveListDecoder.getUrlListFromArchiveOrgListing(archive_url);
            Thread task1, task2, task3, task4, task;
            task1 = new Thread(() -> {});
            task2 = task1;
            task3 = task1;
            task4 = task1;
            task1.start();
            for (String url : urlist) {
                if (url.contains("PossiblyBroken"))
                    continue;
                task = new Thread(() -> {
                    try {
                        String[] urlfrags = url.split("/");
                        String fin = "";
                        for (String frag : urlfrags) {
                            fin += escaper.escape(frag) + "/";
                        }
                        fin = fin.substring(0, fin.length() - 1);
                        AppDownloader.downloadAndAddApp(new URL(fin));
                        System.out.println("Parsed: " + fin);
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                });
                while (task1.isAlive() && task2.isAlive() && task3.isAlive() && task4.isAlive()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                }
                if (!task1.isAlive())
                    task1 = task;
                else if (!task2.isAlive())
                    task2 = task;
                else if (!task3.isAlive())
                    task3 = task;
                else
                    task4 = task;
                task.start();
            }
        });
        archiveParser.start();
        while (true) {
            System.out.println(AppList.searchApps("").size());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
