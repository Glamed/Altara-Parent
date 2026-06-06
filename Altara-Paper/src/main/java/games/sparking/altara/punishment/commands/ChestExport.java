package games.sparking.altara.punishment.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static games.sparking.altara.utils.Statics.GSON;

public class ChestExport {

    @Command(names = "chestexport", description = "Export chest data to a file", permission = "altara.chestexport")
    public boolean cmd(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null || !(target.getState() instanceof Chest chest)) {
            player.sendMessage("§cYou must be looking at a chest.");
            return true;
        }

        InventoryHolder holder = chest.getInventory().getHolder();
        Inventory inventory;
        String chestLabel;

        if (holder instanceof DoubleChest doubleChest) {
            inventory = doubleChest.getInventory();
            chestLabel = "double_chest";
        } else {
            inventory = chest.getInventory();
            chestLabel = "chest";
        }

        JsonObject root = new JsonObject();
        root.addProperty("type", chestLabel);
        root.addProperty("world", target.getWorld().getName());
        root.addProperty("x", target.getX());
        root.addProperty("y", target.getY());
        root.addProperty("z", target.getZ());
        root.addProperty("exported_by", player.getName());
        root.addProperty("size", inventory.getSize());

        JsonArray items = new JsonArray();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null) continue;

            JsonObject entry = new JsonObject();
            entry.addProperty("slot", i);
            entry.addProperty("type", item.getType().name());
            entry.addProperty("amount", item.getAmount());

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName()) {
                    entry.addProperty("display_name", meta.getDisplayName());
                }
                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        JsonArray loreArray = new JsonArray();
                        lore.forEach(loreArray::add);
                        entry.add("lore", loreArray);
                    }
                }
                if (meta.hasCustomModelData()) {
                    entry.addProperty("custom_model_data", meta.getCustomModelData());
                }
                if (meta.isUnbreakable()) {
                    entry.addProperty("unbreakable", true);
                }
            }

            items.add(entry);
        }

        root.add("items", items);
        root.addProperty("item_count", items.size());

        Altara.getSharedInstance().getLogger().info(
                "Chest export at " + target.getX() + " " + target.getY() + " " + target.getZ() + ":\n" + GSON.toJson(root)
        );
        player.sendMessage("§aExported §d" + items.size() + " §aitem(s) to console.");
        return true;
    }
}
