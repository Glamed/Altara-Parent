package games.sparking.altara.configuration;

import com.google.gson.Gson;
import games.sparking.altara.Altara;
import games.sparking.altara.utils.Statics;
import lombok.Setter;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JsonConfigurationService implements ConfigurationService {

    @Setter public static Gson gson = Statics.GSON;

    @Override
    public void saveConfiguration(StaticConfiguration configuration, File file) {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {

            gson.toJson(configuration, writer);

        } catch (IOException e) {
            Altara.getSharedInstance().getLogger().warn(
                    "Failed to save configuration " + configuration.getClass().getName() +
                            " to file " + file.getName() + ": " + e.getMessage()
            );
        }
    }

    @Override
    public <T extends StaticConfiguration> T loadConfiguration(Class<? extends T> clazz, File file) {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            Altara.getSharedInstance().getLogger().warn(
                    "Failed to create parent folder for " + file.getName()
            );
            return null;
        }

        try {
            if (!file.exists()) {
                T config = clazz.getDeclaredConstructor().newInstance();

                if (!file.createNewFile()) {
                    Altara.getSharedInstance().getLogger().warn(
                            "Failed to create file for " + file.getName()
                    );
                    return null;
                }

                saveConfiguration(config, file);
                return config;
            }
        } catch (Exception e) {
            Altara.getSharedInstance().getLogger().error(
                    "Failed to initialize configuration " + clazz.getName() +
                            ": " + e.getMessage()
            );
            return null;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            T config = gson.fromJson(reader, clazz);

            if (config == null) {
                config = clazz.getDeclaredConstructor().newInstance();
            }

            saveConfiguration(config, file);
            return config;

        } catch (Exception e) {
            Altara.getSharedInstance().getLogger().error(
                    "Failed to load configuration " + clazz.getName() +
                            " from file " + file.getName() + ": " + e.getMessage()
            );
            return null;
        }
    }
}