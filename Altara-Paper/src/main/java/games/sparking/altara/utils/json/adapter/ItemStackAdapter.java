package games.sparking.altara.utils.json.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Base64;

/**
 * Gson adapter that serialises a Bukkit {@link ItemStack} as a Base64-encoded
 * string produced by {@link ItemStack#serializeAsBytes()} and deserialises it
 * back via {@link ItemStack#deserializeBytes(byte[])}.
 *
 * <p>This avoids Gson's raw reflection over CraftBukkit/NMS internals, which
 * produced malformed JSON that could not be read back.</p>
 */
public class ItemStackAdapter extends TypeAdapter<ItemStack> {

    @Override
    public void write(JsonWriter out, ItemStack value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(Base64.getEncoder().encodeToString(value.serializeAsBytes()));
    }

    @Override
    public ItemStack read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String base64 = in.nextString();
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(base64));
    }
}

