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
            String appName = "";
            String bundleID = "";
            String version = "0.0";
            String minimumVersion = "0.0";
            ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
            ZipEntry entry = zipExtractor.getNextEntry();
            boolean foundOther = false;
            while (entry != null) {
                if (entry.getName().endsWith(".app/Info.plist")) {
                    byte[] bytes = zipExtractor.readAllBytes();
                    NSDictionary parsedData = (NSDictionary) PropertyListParser.parse(bytes);
                    for (String key : parsedData.allKeys()) {
                        switch (key) {
                            case "CFBundleIdentifier":
                                bundleID = String.valueOf(parsedData.get("CFBundleIdentifier"));
                            case "CFBundleVersion":
                                version = String.valueOf(parsedData.get("CFBundleVersion"));
                            case "MinimumOSVersion":
                                minimumVersion = String.valueOf(parsedData.get("MinimumOSVersion"));
                        }
                    }
                    if (foundOther) {
                        break;
                    }
                    foundOther = true;
                }
                if (entry.getName().endsWith("iTunesMetadata.plist")) {
                    byte[] bytes = zipExtractor.readAllBytes();
                    NSDictionary parsedData = (NSDictionary) PropertyListParser.parse(bytes);
                    for (String key : parsedData.allKeys()) {
                        switch (key) {
                            case "softwareVersionBundleId":
                                bundleID = String.valueOf(parsedData.get("softwareVersionBundleId"));
                            case "bundleVersion":
                                version = String.valueOf(parsedData.get("bundleVersion"));
                            case "itemName":
                                appName = String.valueOf(parsedData.get("itemName"));
                        }
                    }
                    if (foundOther) {
                        break;
                    }
                    foundOther = true;
                }
                entry = zipExtractor.getNextEntry();
            }
            App app = AppList.getAppByBundleID(bundleID);
            if (app == null) {
                app = new App(appName, bundleID);
                AppList.addApp(app);
            }
            app.addAppVersionNoSort(version, new String[]{url.toString()}, minimumVersion);
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
