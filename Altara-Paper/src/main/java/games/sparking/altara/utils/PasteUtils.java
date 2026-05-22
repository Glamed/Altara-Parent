package games.sparking.altara.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class PasteUtils {

    private static final String PASTE_URL = "https://pastebin.com/api/api_post.php";
    private static final String RAW_PASTE_URL = "https://pastebin.com/api/api_raw.php";

//    public static String paste(String content, boolean raw) {
//        HttpURLConnection connection = null;
//        try {
//            URL url = new URL(PASTE_ENDPOINT);
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setConnectTimeout(5000);
//            connection.setReadTimeout(5000);
//            connection.setDoOutput(true);
//            connection.setInstanceFollowRedirects(false);
//            connection.setRequestMethod("POST");
//            connection.setRequestProperty("User-Agent", "ILib Hastebin API");
//            connection.setRequestProperty("Content-Length", String.valueOf(content.getBytes().length));
//            connection.setUseCaches(false);
//
//            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
//            outputStream.write(content.getBytes());
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder json = new StringBuilder();
//            reader.lines().forEach(json::append);
//
//            JsonObject object = Statics.JSON_PARSER.parse(json.toString()).getAsJsonObject();
//            return (raw ? RAW_PASTE_URL : PASTE_URL) + object.get("key").getAsString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        } finally {
//            if (connection != null)
//                connection.disconnect();
//        }
//    }

    public static String paste(String content, boolean raw) {
        try {
            URL url = new URL(raw ? RAW_PASTE_URL : PASTE_URL);
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setDoInput(true);
            Map<String, String> arguments = new HashMap<>();

            arguments.put("api_dev_key", "MDAOJWg_yjIUBR5gqmgG-KqJvE_Kx4Ph");
            arguments.put("api_option", "paste");
            arguments.put("api_paste_code", content);
            arguments.put("api_paste_private", "1");

            StringJoiner sj = new StringJoiner("&");
            for (Map.Entry<String, String> entry : arguments.entrySet())
                sj.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
//            System.out.println(sj);
//            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            http.connect();
            OutputStream os = http.getOutputStream();
            os.write(out);
            InputStream is = http.getInputStream();
            String text = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
//            System.out.println(text);
            return text;
        } catch (IOException urlException) {
            urlException.printStackTrace();
        }

        return null;
    }
}
