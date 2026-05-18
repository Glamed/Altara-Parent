package games.sparking.altara.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomUtil {

    private static final int TOKEN_CHARS = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateToken() {
        // 6 bits per character, round to nearest byte
        int byteAmount = (int) Math.ceil((TOKEN_CHARS * 6) / 8.0);
        byte[] bytes = new byte[byteAmount];
        SECURE_RANDOM.nextBytes(bytes);

        String token = Base64.getUrlEncoder().encodeToString(bytes);
        token = replaceDashes(token);

        return token;
    }

    private static String replaceDashes(String token) {
        for (int i = 0; i < token.length(); i++) {
            char originalChar = token.charAt(i);
            char newChar = originalChar;

            while (newChar == '-') {
                byte[] replacementBytes = new byte[1];
                SECURE_RANDOM.nextBytes(replacementBytes);
                newChar = Base64.getUrlEncoder().encodeToString(replacementBytes).charAt(0);
            }

            token = token.replaceFirst(String.valueOf(originalChar), String.valueOf(newChar));
        }

        return token;
    }

}
