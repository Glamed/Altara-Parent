package games.sparking.altara.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import games.sparking.altara.utils.json.adapter.UUIDAdapter;

import java.util.TimeZone;
import java.util.UUID;


public class Statics {

    public static final TimeZone TIME_ZONE = TimeZone.getTimeZone("America/New_York");
    public static final JsonParser JSON_PARSER = new JsonParser();
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeHierarchyAdapter(UUID.class, new UUIDAdapter())
            .disableHtmlEscaping()
            .create();
    public static final Gson PLAIN_GSON = new GsonBuilder().create();

}
