package ca.litten.ios_obscura_server.frontend;

public class Templates {
    public static String generateBasicHeader(String title) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "    <head>\n" +
                "        <title>" + title + "</title>\n" +
                "        <style>\n" +
                "        @import url(https://cydia.saurik.com/cytyle/style-3163da6b7950852a03d31ea77735f4e1d2ba6699.css);\n" +
                "        @import url(http://cydia.saurik.com/cytyle/style-3163da6b7950852a03d31ea77735f4e1d2ba6699.css);\n" +
                "        </style>\n" +
                "    </head>";
    }
}
