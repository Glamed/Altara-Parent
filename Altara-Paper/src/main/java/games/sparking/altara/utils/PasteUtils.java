package games.sparking.altara.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/** Uploads text to Pastebin (unlisted) and returns the paste URL. Blocking. */
public final class PasteUtils {

    private static final String PASTE_URL = "https://pastebin.com/api/api_post.php";
    // TODO: move to configuration — API keys shouldn't live in source control.
    private static final String API_KEY = "MDAOJWg_yjIUBR5gqmgG-KqJvE_Kx4Ph";

    private PasteUtils() {}

    /**
     * @param raw return the {@code /raw/} URL instead of the normal page
     * @return the paste URL, or {@code null} if the upload failed
     */
    public static String paste(String content, boolean raw) {
        Map<String, String> arguments = new LinkedHashMap<>();
        arguments.put("api_dev_key", API_KEY);
        arguments.put("api_option", "paste");
        arguments.put("api_paste_code", content);
        arguments.put("api_paste_private", "1");

        StringJoiner body = new StringJoiner("&");
        arguments.forEach((key, value) -> body.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8)));

        HttpURLConnection http = null;
        try {
            http = (HttpURLConnection) URI.create(PASTE_URL).toURL().openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setConnectTimeout(5000);
            http.setReadTimeout(5000);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            try (OutputStream os = http.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            String response;
            try (InputStream is = http.getInputStream()) {
                response = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }

            // Pastebin reports errors ("Bad API request, ...") with a 200 status.
            if (!response.startsWith("http")) return null;
            return raw ? response.replace("pastebin.com/", "pastebin.com/raw/") : response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (http != null) http.disconnect();
        }
    }
}
