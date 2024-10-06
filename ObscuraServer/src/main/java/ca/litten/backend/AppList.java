package ca.litten.backend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AppList {
    private static ArrayList<App> apps = new ArrayList<>();
    
    public static void loadAppDatabaseFile(File file) {
        try {
            FileReader reader = new FileReader(file);
            StringBuilder out = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while (reader.ready()) {
                read = reader.read(buf);
                for (int i = 0; i < read; i++)
                    out.append(buf[i]);
            }
            JSONArray appArray = new JSONArray(out.toString());
            for (Object appObject : appArray.toList()) {
                JSONObject appJSON = (JSONObject) appObject;
                App app = new App(appJSON.getString("name"), appJSON.getString("bundle"));
                for (Object versionObject : appJSON.getJSONArray("versions")) {
                    JSONObject versionJSON = (JSONObject) versionObject;
                    app.addAppVersionNoSort(versionJSON.getString("ver"),
                            (String[]) versionJSON.getJSONArray("urls").toList().toArray(),
                            versionJSON.getString("support"));
                }
                app.sortVersions();
                apps.add(app);
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found! Not importing anything.");
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    public static void saveAppDatabaseFile(File file) {
        JSONArray appArray = new JSONArray();
        for (App app : apps) {
            appArray.put(app.getAppJSON());
        }
        try {
            FileWriter writer = new FileWriter(file, false);
            writer.write(appArray.toString());
        } catch (IOException e) {
            System.err.println("Failed to write to file!");
        }
    }
    
    public static List<App> listAppsThatSupportVersion(String version) {
        return apps.stream().filter(app -> app.showAppForVersion(version)).collect(Collectors.toList());
    }
    
    public static App getAppByBundleID(String bundleID) {
        List<App> theApp = apps.stream().filter(app -> (app.getBundleID().equals(bundleID))).collect(Collectors.toList());
        if (theApp.isEmpty()) return null;
        return theApp.get(0);
    }
    
    public static List<App> searchApps(String query, String version) {
        return apps.stream()
                .filter(app -> (app.showAppForVersion(version) && app.getName().contains(query)))
                .sorted(Comparator.comparingInt(o -> o.getName().length())).collect(Collectors.toList());
    }
    
    public static List<App> searchApps(String query) {
        return apps.stream().filter(app -> app.getName().contains(query))
                .sorted(Comparator.comparingInt(o -> o.getName().length())).collect(Collectors.toList());
    }
}
