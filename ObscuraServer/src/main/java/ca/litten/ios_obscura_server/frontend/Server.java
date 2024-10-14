package ca.litten.ios_obscura_server.frontend;

import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

public class Server {
    private HttpServer server;
    private static final HttpServerProvider provider = HttpServerProvider.provider();
    private static Random rand = new Random();
    private static byte[] searchIcon, favicon, mainicon, icon32, icon16;
    private static long lastReload = 0;
    public static boolean allowReload = false;
    private static String servername = "localhost";
    private static String donateURL = "";
    
    static {
        try {
            File file = new File("searchIcon.jpg");
            FileInputStream search = new FileInputStream(file);
            searchIcon = new byte[Math.toIntExact(file.length())];
            search.read(searchIcon);
            search.close();
            file = new File("favicon.ico");
            FileInputStream fav = new FileInputStream(file);
            favicon = new byte[Math.toIntExact(file.length())];
            fav.read(favicon);
            fav.close();
            file = new File("icon.png");
            FileInputStream icon = new FileInputStream(file);
            mainicon = new byte[Math.toIntExact(file.length())];
            icon.read(mainicon);
            icon.close();
            file = new File("icon16.png");
            FileInputStream icon16f = new FileInputStream(file);
            icon16 = new byte[Math.toIntExact(file.length())];
            icon16f.read(icon16);
            icon16f.close();
            file = new File("icon32.png");
            FileInputStream icon32f = new FileInputStream(file);
            icon32 = new byte[Math.toIntExact(file.length())];
            icon32f.read(icon32);
            icon32f.close();
            file = new File("host.txt");
            try {
                FileReader host = new FileReader(file);
                char[] buf = new char[Math.toIntExact(file.length())];
                host.read(buf);
                servername = String.valueOf(buf).trim();
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find host.txt");
            }
            file = new File("donate.txt");
            try {
                FileReader host = new FileReader(file);
                char[] buf = new char[Math.toIntExact(file.length())];
                host.read(buf);
                donateURL = String.valueOf(buf).trim();
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find donate.txt");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public Server(InetSocketAddress address) throws IOException {
        lastReload = System.currentTimeMillis();
        server = provider.createHttpServer(address, -1);
        server.createContext("/").setHandler(exchange -> {
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            if (!(exchange.getRequestURI().toString().equals("/") || exchange.getRequestURI().toString().isEmpty())) {
                byte[] bytes = ErrorPages.general404.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
                return;
            }
            StringBuilder out = new StringBuilder();
            out.append(Templates.generateBasicHeader("iOS Obscura Locator"))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div><center><strong>iPhoneOS Obscura Locator Homepage</strong></center></div></div><div><div><form action=\"searchPost\"><input type\"text\" name=\"search\" value=\"\" style=\"border-bottom:1px solid #999\" placeholder=\"Search\"><button style=\"float:right;background:none\" type=\"submit\"><img style=\"height:18px;border-radius:50%\" src=\"/searchIcon\"></button></form></div></div></fieldset><label>Some Apps</label><fieldset>");
            List<App> apps = AppList.listAppsThatSupportVersion(iOS_ver);
            App app;
            int random;
            int s = apps.size();
            for (int i = 0; i < Math.min(20, s); i++) {
                random = rand.nextInt(apps.size());
                app = apps.remove(random);
                out.append("<a style=\"height:77px\" href=\"getAppVersions/").append(app.getBundleID())
                        .append("\"><div><div style=\"height:77px;overflow:hidden\"><img loading=\"lazy\" style=\"float:left;height:57px;width:57px;border-radius:15.625%\" src=\"getAppIcon/")
                    .append(app.getBundleID()).append("\"><center style=\"line-height:57px\">").append(cutStringTo(app.getName(), 15))
                        .append("</center></div></div></a>");
            }
            out.append("</fieldset><fieldset><a href=\"https://github.com/CatsLover2006/iOSobscuraServer\"><div><div>Check out the Github!</div></div></a>");
            if (!donateURL.isEmpty())
                out.append("<a href=\"").append(donateURL).append("\"><div><div>Donate to this instance</div></div></a>");
            out.append("</fieldset></panel></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/getHeader").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            out.append("<!DOCTYPE html>\n<html><body><ol>");
            for (String key : incomingHeaders.keySet()) {
                out.append("<li>").append(key).append("<ol>");
                for (String val : incomingHeaders.get(key)) {
                    out.append("<li>").append(val).append("</li>");
                }
                out.append("</ol></li>");
            }
            out.append("</ol></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/getAppIcon/").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null || app.getArtworkURL().isEmpty() || !app.getArtworkURL().startsWith("http")) {
                outgoingHeaders.set("Location", "https://files.scottshar.es/Share%20Sheets/app-icons/Placeholder-Icon.png");
            } else {
                outgoingHeaders.set("Location", app.getArtworkURL());
            }
            outgoingHeaders.set("Cache-Control", "max-age=172800");
            exchange.sendResponseHeaders(308, 0);
            exchange.close();
        });
        server.createContext("/getAppVersions/").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                byte[] bytes = ErrorPages.app404.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
                return;
            }
            out.append(Templates.generateBasicHeader(app.getName()))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div style=\"height:57px;overflow:hidden\"><img loading=\"lazy\" style=\"float:left;height:57px;width:57px;border-radius:15.625%\" src=\"/getAppIcon/")
                    .append(app.getBundleID()).append("\"><strong style=\"padding:.5em 0;line-height:57px\"><center>").append(cutStringTo(app.getName(), 20))
                    .append("</center></strong></div></div><div><div>").append(app.getDeveloper())
                    .append("</div></div><a href=\"javascript:history.back()\"><div><div>Go Back</div></div></a></fieldset><label>Versions</label><fieldset>");
            for (String version : app.getSupportedAppVersions(iOS_ver)) {
                out.append("<a href=\"/getAppVersionLinks/").append(app.getBundleID()).append("/").append(version)
                        .append("\"><div><div>").append(version).append("</div></div></a>");
            }
            out.append("</fieldset></panel></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/generateInstallManifest/").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            StringBuilder out = new StringBuilder();
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                outgoingHeaders.set("Content-Type", "text/html");
                exchange.sendResponseHeaders(404, ErrorPages.app404.length());
                exchange.getResponseBody().write(ErrorPages.app404.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            outgoingHeaders.set("Content-Type", "text/xml");
            String[] versions = app.getUrlsForVersion(splitURI[3]);
            out.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n<plist version=\"1.0\">\n<dict><key>items</key><array><dict><key>assets</key><array><dict><key>kind</key><string>software-package</string><key>url</key><string>")
                    .append(versions[Integer.parseInt(splitURI[4])])
                    .append("</string></dict><dict><key>kind</key><string>display-image</string><key>needs-shine</key><false/><key>url</key><string>https://")
                    .append(servername).append("/getAppIcon/").append(app.getBundleID())
                    .append("</string></dict></array><key>metadata</key><dict><key>bundle-identifier</key><string>")
                    .append(splitURI[2]).append("</string><key>bundle-version</key><string>")
                    .append(splitURI[3]).append("</string><key>kind</key><string>software</string><key>title</key><string>")
                    .append(app.getName()).append("</string></dict></dict></array></dict></plist>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/getAppVersionLinks/").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                byte[] bytes = ErrorPages.app404.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
                return;
            }
            out.append(Templates.generateBasicHeader(app.getName()))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div style=\"height:57px;overflow:hidden\"><img loading=\"lazy\" style=\"float:left;height:57px;width:57px;border-radius:15.625%\" src=\"/getAppIcon/")
                    .append(app.getBundleID()).append("\"><strong style=\"padding:.5em 0;line-height:57px\"><center>").append(cutStringTo(app.getName(), 20))
                    .append("</center></strong></div></div><div><div>").append(app.getDeveloper())
                    .append("</div></div><div><div style=\"overflow:auto\">Version ").append(splitURI[3])
                    .append("<span style=\"float:right\">Requires iOS ").append(app.getCompatibleVersion(splitURI[3]))
                    .append("</span></div></div><a href=\"javascript:history.back()\"><div><div>Go Back</div></div></a></fieldset>");
            String[] versions = app.getUrlsForVersion(splitURI[3]);
            for (int i = 0; i < versions.length; i++) {
                out.append("<label>#").append(i + 1).append(", ").append(versions[i].split("//")[1].split("/")[0]);
                if (versions[i].split("//")[1].split("/")[0].contains("archive.org"))
                    out.append(", ").append(versions[i].split("//")[1].split("/")[2]);
                if (versions[i].startsWith("https"))
                    out.append(", SSL");
                out.append("</label><fieldset><a href=\"").append(versions[i])
                        .append("\"><div><div>Direct Download</div></div></a><a href=\"itms-services://?action=download-manifest&url=https://")
                        .append(servername).append("/generateInstallManifest/").append(splitURI[2]).append("/").append(splitURI[3]).append("/").append(i)
                        .append("\"><div><div>iOS Direct Install <small style=\"font-size:x-small\">Might Not Work</small></div></div></a></fieldset>");
            }
            out.append("</panel></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/sitemap").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            out.append("https://").append(servername).append("/\n");
            outgoingHeaders.set("Content-Type", "text/plain");
            for (App app : AppList.searchApps("")) {
                out.append("https://").append(servername).append("/getAppVersions/").append(app.getBundleID()).append("\n");
                for (String version : app.getSupportedAppVersions("99999999"))
                    out.append("https://").append(servername).append("/getAppVersionLinks/").append(app.getBundleID())
                            .append("/").append(version).append("\n");
            }
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/searchPost").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("\\?");
            outgoingHeaders.set("Location", "/search/" + splitURI[1].substring(7));
            outgoingHeaders.set("Cache-Control", "max-age=172800");
            exchange.sendResponseHeaders(308, 0);
            exchange.close();
        });
        server.createContext("/search").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            outgoingHeaders.set("Content-Type", "text/html; charset=utf-8");
            String[] splitURI = URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8.name()).split("/");
            String query;
            try {
                query = splitURI[2];
            } catch (IndexOutOfBoundsException e) {
                query = "";
            }
            out.append(Templates.generateBasicHeader("Search: " + query))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div><center><strong>Search iPhoneOS Obscura</strong></center></div></div>")
                    .append("<div><div><form action=\"/searchPost\"><input type\"text\" name=\"search\" value=\"").append(query)
                    .append("\" style=\"border-bottom:1px solid #999\" placeholder=\"Search\"><button style=\"float:right;background:none\" type=\"submit\"><img style=\"height:18px;border-radius:50%\" src=\"/searchIcon\"></button></form></div></div><a href=\"javascript:history.back()\"><div><div>Go Back</div></div></a></fieldset>");
            if (!query.isEmpty()) {
                out.append("<label>Search Results</label><fieldset>");
                List<App> apps = AppList.searchApps(query, iOS_ver);
                if (apps.isEmpty()) {
                    out.append("<div><div>Couldn't find anything!</div></div><div><div>Make sure you've typed everything correctly, or try shortening your query.</div></div>");
                } else {
                    App app;
                    int s = apps.size();
                    for (int i = 0; i < Math.min(20, s); i++) {
                        app = apps.remove(0);
                        out.append("<a style=\"height:77px\" href=\"/getAppVersions/").append(app.getBundleID())
                                .append("\"><div><div style=\"height:77px;overflow:hidden\"><img loading=\"lazy\" style=\"float:left;height:57px;width:57px;border-radius:15.625%\" src=\"/getAppIcon/")
                                .append(app.getBundleID()).append("\"><center style=\"line-height:57px\">").append(cutStringTo(app.getName(), 15))
                                .append("</center></div></div></a>");
                    }
                }
                out.append("</fieldset>");
            }
            out.append("</panel></body></html>");
            byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/searchIcon").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, searchIcon.length);
            exchange.getResponseBody().write(searchIcon);
            exchange.close();
        });
        server.createContext("/icon").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, mainicon.length);
            exchange.getResponseBody().write(mainicon);
            exchange.close();
        });
        server.createContext("/icon32").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, icon32.length);
            exchange.getResponseBody().write(icon32);
            exchange.close();
        });
        server.createContext("/icon16").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, icon16.length);
            exchange.getResponseBody().write(icon16);
            exchange.close();
        });
        server.createContext("/favicon.ico").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "image/vnd.microsoft.icon");
            exchange.sendResponseHeaders(200, favicon.length);
            exchange.getResponseBody().write(favicon);
            exchange.close();
        });
        server.createContext("/reload").setHandler(exchange -> {
            if (!allowReload || (lastReload + 1000 * 60 * 5) > System.currentTimeMillis()) {
                exchange.sendResponseHeaders(202, 0);
                exchange.close();
                return;
            }
            AppList.loadAppDatabaseFile(new File("db.json"));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
            lastReload = System.currentTimeMillis();
        });
    }
    
    public void startServer() {
        server.start();
    }
    
    private String cutStringTo(String str, int len) {
        str = str.trim();
        if (str.length() < len) {
            return str;
        }
        return (str.substring(0, len).trim()) + "...";
    }
}
