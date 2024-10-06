package ca.litten.ios_obscura_server;

import ca.litten.ios_obscura_server.backend.AppList;
import ca.litten.ios_obscura_server.parser.AppDownloader;
import ca.litten.ios_obscura_server.parser.ArchiveListDecoder;
import com.google.common.escape.Escaper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

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
        AppList.loadAppDatabaseFile(new File("db.json"));
        Thread archiveParser = new Thread(() -> {
            Escaper escaper = urlPathSegmentEscaper();
            String[] urlist = ArchiveListDecoder.getUrlListFromArchiveOrgListing(archive_url);
            Thread task1, task2, task3, task4, task;
            task1 = new Thread(() -> {});
            task2 = task1;
            task3 = task1;
            task4 = task1;
            task1.start();
            for (String temp : urlist) {
                String[] urlfrags = temp.split("/");
                String url = "";
                for (String frag : urlfrags) {
                    url += escaper.escape(frag) + "/";
                }
                url = url.substring(0, url.length() - 1);
                if (url.contains("PossiblyBroken") || url.contains("Homebrew%20IPAs"))
                    continue;
                if (AppList.appUrlAlreadyExists(url)) {
                    System.out.println("Skipped: " + url);
                    continue;
                }
                String finalUrl = url;
                task = new Thread(() -> {
                    try {
                        AppDownloader.downloadAndAddApp(new URL(finalUrl));
                        System.out.println("Parsed: " + finalUrl);
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
        Scanner scanner = new Scanner(System.in);
        while (archiveParser.isAlive()) {
            scanner.nextLine();
            System.out.println("Saving database...");
            AppList.saveAppDatabaseFile(new File("db.json"));
        }
    }
}
