package ca.litten.ios_obscura_server.frontend;

public class ErrorPages {
    
    public static final String app404 = """
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>Nonexistent App</title>
        <style>
        @import url(https://cydia.saurik.com/cytyle/style-3163da6b7950852a03d31ea77735f4e1d2ba6699.css);
        @import url(http://cydia.saurik.com/cytyle/style-3163da6b7950852a03d31ea77735f4e1d2ba6699.css);
        </style>
    </head>
    <body class="pinstripe">
        <panel>
            <fieldset>
                <div>
                    <div>
                        <strong><center>Error 404</center></strong>
                    </div>
                </div>
                <div>
                    <div>
                        That app doesn't exist. At least, it isn't part of iOS Obscura.
                    </div>
                </div>
                <a href="https://discord.gg/rTJ9zxjMu3">
                    <div>
                        <div>
                            Have the app?<br>
                            Join the iOS Obscura discord!
                        </div>
                    </div>
                </a>
                <div>
                    <div>
                        <form action=\"/searchPost\">
                            <input type\"text\" name=\"search\" value=\"\" style=\"border-bottom:1px solid #999\" placeholder=\"Search\">
                            <button style=\"float:right;background:none\" type=\"submit\">
                                <img style=\"height:18px;border-radius:50%\" src=\"/searchIcon\">
                            </button>
                        </form>
                    </div>
                </div>
                <a href="javascript:history.back()">
                    <div>
                        <div>
                            Go Back
                        </div>
                    </div>
                <a>
            </fieldset>
        </panel>
    </body>
</html>""";
    
    public static final String general404 = """
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>Nonexistent App</title>
        <style>
        @import url(https://cydia.saurik.com/cytyle/style-3163da6b7950852a03d31ea77735f4e1d2ba6699.css);
        @import url(http://cydia.saurik.com/cytyle/style-3163da6b7950852a03d31ea77735f4e1d2ba6699.css);
        </style>
    </head>
    <body class="pinstripe">
        <panel>
            <fieldset>
                <div>
                    <div>
                        <strong><center>Error 404</center></strong>
                    </div>
                </div>
                <div>
                    <div>
                        That location doesn't exist.
                    </div>
                </div>
                <a href="https://discord.gg/rTJ9zxjMu3">
                    <div>
                        <div>
                            Have any legacy iOS apps?<br>
                            Join the iOS Obscura discord!
                        </div>
                    </div>
                </a>
                <div>
                    <div>
                        <form action=\"/searchPost\">
                            <input type\"text\" name=\"search\" value=\"\" style=\"border-bottom:1px solid #999\" placeholder=\"Search\">
                            <button style=\"float:right;background:none\" type=\"submit\">
                                <img style=\"height:18px;border-radius:50%\" src=\"/searchIcon\">
                            </button>
                        </form>
                    </div>
                </div>
                <a href="javascript:history.back()">
                    <div>
                        <div>
                            Go Back
                        </div>
                    </div>
                <a>
            </fieldset>
        </panel>
    </body>
</html>""";
}