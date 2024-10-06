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
            String artwork = "";
            String developer = null;
            ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
            ZipEntry entry = zipExtractor.getNextEntry();
            boolean foundOther = false;
            while (entry != null) {
                if (entry.getName().endsWith(".app/Info.plist")) {
                    byte[] bytes = zipExtractor.readAllBytes();
                    NSDictionary parsedData = (NSDictionary) PropertyListParser.parse(bytes);
                    for (String key : parsedData.allKeys()) {
                        switch (key) {
                            case "CFBundleDisplayName":
                                if (appName.isEmpty())
                                    appName = String.valueOf(parsedData.get("CFBundleDisplayName"));
                            case "CFBundleIdentifier": {
                                String str = String.valueOf(parsedData.get("CFBundleIdentifier"));
                                if (str.equals("null")) break;
                                bundleID = str;
                            }
                            case "CFBundleVersion": {
                                String str = String.valueOf(parsedData.get("CFBundleVersion"));
                                if (str.equals("null")) break;
                                version = str;
                            }
                            case "MinimumOSVersion":{
                                String str = String.valueOf(parsedData.get("MinimumOSVersion"));
                                if (str.equals("null")) break;
                                minimumVersion = str;
                            }
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
                            case "softwareVersionBundleId": {
                                String str = String.valueOf(parsedData.get("softwareVersionBundleId"));
                                if (str.equals("null")) break;
                                bundleID = str;
                            }
                            case "bundleShortVersionString": {
                                String str = String.valueOf(parsedData.get("bundleShortVersionString"));
                                if (str.equals("null")) break;
                                version = str;
                            }
                            case "itemName": {
                                String str = String.valueOf(parsedData.get("itemName"));
                                if (str.equals("null")) break;
                                appName = str;
                            }
                            case "softwareIcon57x57URL":
                                artwork = String.valueOf(parsedData.get("softwareIcon57x57URL"));
                            case "artistName":{
                                String str = String.valueOf(parsedData.get("artistName"));
                                if (str.equals("null")) break;
                                developer = str;
                            }
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
            app.updateArtwork(version, artwork);
            app.updateArtwork(version, developer);
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
