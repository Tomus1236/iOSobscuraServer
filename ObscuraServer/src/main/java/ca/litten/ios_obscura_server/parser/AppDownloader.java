package ca.litten.ios_obscura_server.parser;

import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
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
            String artwork = null;
            String developer = null;
            ZipInputStream zipExtractor = new ZipInputStream(connection.getInputStream());
            ZipEntry entry = zipExtractor.getNextEntry();
            boolean foundOther = false;
            while (entry != null) {
                if (entry.getName().endsWith(".app/Info.plist")) {
                    byte[] bytes;
                    if (entry.getSize() < 0) {
                        ArrayList<Byte> list = new ArrayList<>();
                        byte[] buf = new byte[1024];
                        int read, i;
                        while ((read = zipExtractor.read(buf)) != -1) {
                            for (i = 0; i < read; i++) {
                                list.add(buf[i]);
                            }
                        }
                        bytes = new byte[list.size()];
                        for (i = 0; i < list.size(); i++) {
                            bytes[i] = list.get(i);
                        }
                    } else {
                        bytes = new byte[Math.toIntExact(entry.getSize())];
                        zipExtractor.read(bytes);
                    }
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
                    byte[] bytes = new byte[Math.toIntExact(entry.getSize())];
                    zipExtractor.read(bytes);
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
                            case "softwareIcon57x57URL":{
                                String str = String.valueOf(parsedData.get("softwareIcon57x57URL"));
                                if (str.equals("null")) break;
                                artwork = str;
                            }
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
            app.updateArtwork(version, artwork);
            app.updateDeveloper(version, developer);
            app.addAppVersion(version, new String[]{url.toString()}, minimumVersion);
        } catch (Throwable e) {
            System.err.println(e);
        }
    }
}
