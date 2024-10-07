package ca.litten.ios_obscura_server.frontend;

import ca.litten.ios_obscura_server.backend.App;
import ca.litten.ios_obscura_server.backend.AppList;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Server {
    private HttpServer server;
    private static final HttpServerProvider provider = HttpServerProvider.provider();
    private HttpContext rootContext;
    
    public Server(InetSocketAddress address) throws IOException {
        server = provider.createHttpServer(address, -1);
        server.createContext("/").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            int status = 404;
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            switch (exchange.getRequestURI().toString()) {
                default: {
                    System.out.println(exchange.getRequestURI());
                }
            }
            exchange.sendResponseHeaders(status, out.length());
            exchange.getResponseBody().write(out.toString().getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.createContext("/getHeader").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html");
            out.append("<!DOCTYPE html>\n<html><body><ol>");
            for (String key : incomingHeaders.keySet()) {
                out.append("<li>").append(key).append("<ol>");
                for (String val : incomingHeaders.get(key)) {
                    out.append("<li>").append(val).append("</li>");
                }
                out.append("</ol></li>");
            }
            out.append("</ol></body></html>");
            exchange.sendResponseHeaders(200, out.length());
            exchange.getResponseBody().write(out.toString().getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.createContext("/getAppIcon/").setHandler(exchange -> {
            Headers outgoingHeaders = exchange.getResponseHeaders();
            String[] splitURI = exchange.getRequestURI().toString().split("/");
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
            outgoingHeaders.set("Content-Type", "text/html");
            String userAgent = incomingHeaders.get("user-agent").get(0);
            boolean iOS_connection = userAgent.contains("iPhone OS") || userAgent.contains("iPad");
            String iOS_ver = "99999999";
            if (iOS_connection) {
                String[] split1 = userAgent.split("like Mac OS X");
                String[] split2 = split1[0].split(" ");
                iOS_ver = split2[split2.length - 1].replace("_", ".");
            }
            String[] splitURI = exchange.getRequestURI().toString().split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                exchange.sendResponseHeaders(404, ErrorPages.app404.length());
                exchange.getResponseBody().write(ErrorPages.app404.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            out.append(Templates.generateBasicHeader(app.getName()))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div style=\"height:57px\"><img style=\"float:left;height:57px;width:57px;border-radius:15.625%\" src=\"../getAppIcon/")
                    .append(app.getBundleID()).append("\"><strong style=\"padding:.5em 0;line-height:57px\"><center>").append(app.getName())
                    .append("</center></strong></div></div><a href=\"javascript:history.back()\"><div><div>Go Back</div></div></a></fieldset><label>Versions</label><fieldset>");
            for (String version : app.getSupportedAppVersions(iOS_ver)) {
                out.append("<a href=\"../../getAppVersionLinks/").append(app.getBundleID()).append("/").append(version)
                        .append("\"><div><div>").append(version).append("</div></div></a>");
            }
            out.append("</fieldset></panel></body></html>");
            exchange.sendResponseHeaders(200, out.length());
            exchange.getResponseBody().write(out.toString().getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.createContext("/generateInstallManifest/").setHandler(exchange -> {
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/xml");
            StringBuilder out = new StringBuilder();
            String[] splitURI = exchange.getRequestURI().toString().split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                exchange.sendResponseHeaders(404, ErrorPages.app404.length());
                exchange.getResponseBody().write(ErrorPages.app404.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            String[] versions = app.getUrlsForVersion(splitURI[3]);
            out.append("""
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
    <dict>
        <key>items</key>
        <array>
            <dict>
                <key>assets</key>
                <array>
                    <dict>
                        <key>kind</key>
                        <string>software-package</string>
                        <key>url</key>
                        <string>""").append(versions[Integer.parseInt(splitURI[4])]).append("""
</string>
                    </dict>
                    <dict>
                        <key>kind</key>
                        <string>display-image</string>
                        <key>needs-shine</key>
                        <false/>
                        <key>url</key>
                        <string>""").append(app.getArtworkURL()).append("""
</string>
                    </dict>
                </array>
                <key>metadata</key>
                <dict>
                    <key>bundle-identifier</key>
                    <string>""").append(splitURI[2]).append("""
</string>
                    <key>bundle-version</key>
                    <string>""").append(splitURI[3]).append("""
</string>
                    <key>kind</key>
                    <string>software</string>
                    <key>title</key>
                    <string>""").append(app.getName()).append("""
</string>
                </dict>
            </dict>
        </array>
    </dict>
</plist>""");
            exchange.sendResponseHeaders(200, out.length());
            exchange.getResponseBody().write(out.toString().getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.createContext("/getAppVersionLinks/").setHandler(exchange -> {
            StringBuilder out = new StringBuilder();
            Headers incomingHeaders = exchange.getRequestHeaders();
            Headers outgoingHeaders = exchange.getResponseHeaders();
            outgoingHeaders.set("Content-Type", "text/html");
            String[] splitURI = exchange.getRequestURI().toString().split("/");
            App app = AppList.getAppByBundleID(splitURI[2]);
            if (app == null) {
                exchange.sendResponseHeaders(404, ErrorPages.app404.length());
                exchange.getResponseBody().write(ErrorPages.app404.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            out.append(Templates.generateBasicHeader(app.getName()))
                    .append("<body class=\"pinstripe\"><panel><fieldset><div><div style=\"height:57px\"><img style=\"float:left;height:57px;width:57px;border-radius:15.625%\" src=\"../../getAppIcon/")
                    .append(app.getBundleID()).append("\"><strong style=\"padding:.5em 0;line-height:57px\"><center>").append(app.getName())
                    .append("</center></strong></div></div><div><div>Version ").append(splitURI[3])
                    .append("</div></div><a href=\"javascript:history.back()\"><div><div>Go Back</div></div></a></fieldset>");
            String[] versions = app.getUrlsForVersion(splitURI[3]);
            for (int i = 0; i < versions.length; i++) {
                out.append("<label>#").append(i + 1).append(", ").append(versions[i].split("//")[1].split("/")[0]);
                if (versions[i].startsWith("https"))
                    out.append(", SSL");
                out.append("</label><fieldset><a href=\"").append(versions[i])
                        .append("\"><div><div>Direct Download</div></div></a><a href=\"itms-services://?action=download-manifest&url=http://")
                        .append(incomingHeaders.get("host").get(0)).append("/generateInstallManifest/")
                        .append(splitURI[2]).append("/").append(splitURI[3]).append("/").append(i)
                        .append("\"><div><div>iOS Direct Install (Alpha)</div></div></a></fieldset>");
            }
            out.append("</panel></body></html>");
            exchange.sendResponseHeaders(200, out.length());
            exchange.getResponseBody().write(out.toString().getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
    }
    
    public void startServer() {
        server.start();
    }
}
