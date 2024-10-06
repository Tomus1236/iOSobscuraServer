package ca.litten.ios_obscura_server.parser;

import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AppDownloader {
    public static void downloadAndAddApp(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                System.err.println("Not found");
                return;
            }
            ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
            ZipEntry entry = zipExtractor.getNextEntry();
            while (entry != null) {
                if (entry.getName().endsWith("Info.plist")) {
                    String appName = "";
                    String bundleID = "";
                    String version = "0.0";
                    String minimumVersion = "0.0";
                    byte[] bytes = zipExtractor.readAllBytes();
                    NSDictionary parsedData = (NSDictionary) PropertyListParser.parse(bytes);
                    for (String key : parsedData.allKeys()) {
                        switch (key) {
                            case "CFBundleDisplayName":
                                appName = String.valueOf(parsedData.get("CFBundleDisplayName"));
                            case "CFBundleIdentifier":
                                bundleID = String.valueOf(parsedData.get("CFBundleIdentifier"));
                            case "CFBundleVersion":
                                version = String.valueOf(parsedData.get("CFBundleVersion"));
                            case "MinimumOSVersion":
                                minimumVersion = String.valueOf(parsedData.get("MinimumOSVersion"));
                        }
                    }
                    App app = AppList.getAppByBundleID(bundleID);
                    if (app == null) {
                        app = new App(appName, bundleID);
                        AppList.addApp(app);
                    }
                    app.addAppVersionNoSort(version, new String[]{url.toString()}, minimumVersion);
                    return;
                }
                entry = zipExtractor.getNextEntry();
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
