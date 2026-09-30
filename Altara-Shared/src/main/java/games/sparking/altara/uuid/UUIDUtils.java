package games.sparking.altara.uuid;

import java.util.regex.Pattern;

public class UUIDUtils {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public static boolean isUUID(String string) {
        return string != null && UUID_PATTERN.matcher(string).matches();
    }
}
