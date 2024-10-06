package ca.litten.backend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class App {
    
    private class Version {
        private final String version;
        private final String[] urls;
        private final String supportedVersion;
        
        public Version(String version, String[] urls, String supportedVersion) {
            this.version = version;
            this.urls = urls;
            this.supportedVersion = supportedVersion;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(version, Arrays.hashCode(urls), supportedVersion);
        }
    }
    
    private String name;
    private String bundleID;
    private String earliestSupportedVersion = "99999999";
    
    private ArrayList<Version> versions;
    
    public boolean showAppForVersion(String version) {
        return isVersionLater(earliestSupportedVersion, version);
    }
    
    public String getName() {
        return name;
    }
    
    public String getBundleID() {
        return bundleID;
    }
    
    public String getEarliestSupportedVersion() {
        return earliestSupportedVersion;
    }
    
    public App(String name, String bundleID) {
        this.name = name;
        this.bundleID = bundleID;
        versions = new ArrayList<>();
    }
    
    public void addAppVersionNoSort(String version, String[] urls, String supportedVersion) {
        Version appVersion = new Version(version, urls, supportedVersion);
        for (Version otherVersion : versions) {
            if (otherVersion.equals(appVersion)) return;
        }
        versions.add(appVersion);
        if (isVersionLater(supportedVersion, earliestSupportedVersion))
            earliestSupportedVersion = supportedVersion;
    }
    
    public void sortVersions() {
        versions.sort((o1, o2) -> {
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            if (o1.version.equals(o2.version)) return 0;
            return (isVersionLater(o1.version, o2.version)) ? -1 : 1;
        });
    }
    
    public void addAppVersion(String version, String[] urls, String supportedVersion) {
        addAppVersionNoSort(version, urls, supportedVersion);
        sortVersions();
    }
    
    public static boolean isVersionLater(String lateVersion, String checkVersion) {
        String[] support = lateVersion.split("\\.");
        String[] check = checkVersion.split("\\.");
        int checkLen = Math.max(check.length, support.length);
        int checkVer;
        int supportVer;
        for (int i = 0; i < checkLen; i++) {
            try {
                supportVer = Integer.parseInt(support[i]);
            } catch (IndexOutOfBoundsException e) {
                supportVer = 0;
            }
            try {
                checkVer = Integer.parseInt(check[i]);
            } catch (IndexOutOfBoundsException e) {
                checkVer = 0;
            }
            if (supportVer > checkVer) return false;
            if (supportVer < checkVer) return true;
        }
        return true; // Earliest supported version
    }
    
    public SortedMap<String, String[]> getSupportedAppVersions(String version) {
        SortedMap<String, String[]> map = new TreeMap<>();
        for (Version appVer : versions) {
            if (isVersionLater(appVer.supportedVersion, version)) map.put(appVer.version, appVer.urls);
        }
        return map;
    }
    
    public JSONObject getAppJSON() {
        JSONObject appJSON = new JSONObject();
        appJSON.put("name", name);
        appJSON.put("bundle", bundleID);
        JSONArray versionArray = new JSONArray();
        for (Version version : versions) {
            JSONObject versionJSON = new JSONObject();
            versionJSON.put("ver", version.version);
            versionJSON.put("support", version.supportedVersion);
            JSONArray urls = new JSONArray();
            for (String url : version.urls) {
                urls.put(url);
            }
            versionJSON.put("urls", urls);
            versionArray.put(versionJSON);
        }
        appJSON.put("versions", versionArray);
        return appJSON;
    }
}
