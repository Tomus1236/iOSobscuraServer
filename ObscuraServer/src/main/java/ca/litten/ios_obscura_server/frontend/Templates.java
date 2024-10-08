package ca.litten.ios_obscura_server.frontend;

public class Templates {
    public static String generateBasicHeader(String title) {
        return "<!DOCTYPE html>\n" +
                "<html><head><meta charset=\"utf-8\"><title>" + title + "</title><style>@import url(https://cydia.saurik.com/cytyle/style-3163da6b7950852a03d31ea77735f4e1d2ba6699.css);@import url(http://cydia.saurik.com/cytyle/style-3163da6b7950852a03d31ea77735f4e1d2ba6699.css);</style></head>";
    }
}
