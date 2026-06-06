package games.sparking.altara.grant.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.grant.GrantProcedure;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class GrantScopesMenu extends Menu {

    private final Profile profile;
    private final List<String> scopes = new ArrayList<>();
    private boolean clicked = false;

    @Override
    public String getTitle(Player player) {
        return "Select scopes: " + profile.getGrantProcedure().getRank().getName();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<String> existingScopes = new ArrayList<>();
        int index = 0;
        for (ServerInfo server : ServerInfo.getServers()) {
            if (!existingScopes.contains(server.getGroup().toLowerCase())) {
                buttons.put(index++, new ScopeButton(server.getGroup().toLowerCase()));
                existingScopes.add(server.getGroup().toLowerCase());
            }
        }
        buttons.put(22, new ScopeButton("GLOBAL"));
        buttons.put(31, new Button() {
            @Override
            public ItemStack getItem(Player player) {
                GrantProcedure procedure = profile.getGrantProcedure();
                if (scopes.isEmpty()) {
                    return new ItemBuilder(Material.WOODEN_SWORD)
                            .setDisplayName(Component.text("Confirm and grant", CC.RED, TextDecoration.BOLD))
                            .setLore(
                                    CC.MENU_BAR,
                                    Component.text("Please select at least one scope.", CC.RED),
                                    CC.MENU_BAR
                            ).build();
                }
                return new ItemBuilder(Material.DIAMOND_SWORD)
                        .setDisplayName(Component.text("Confirm and grant", CC.GREEN, TextDecoration.BOLD))
                        .setLore(
                                CC.MENU_BAR,
                                CC.format("<yellow>Click to grant %s the %s rank",
                                        procedure.getTarget().getName(), procedure.getRank().getName()),
                                Component.text()
                                        .append(Component.text(scopes.contains("GLOBAL")
                                                ? "This grant will be " : "This grant will apply on: ", CC.YELLOW))
                                        .append(Component.text(scopes.contains("GLOBAL") ? "Global"
                                                : StringUtils.join(scopes, ", "), CC.RED))
                                        .build(),
                                CC.format("<yellow>Reasoning: <red>%s", procedure.getReason()),
                                CC.format("<yellow>Duration: <red>%s", Time.formatDetailed(procedure.getDuration())),
                                CC.MENU_BAR
                        ).build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                if (scopes.isEmpty()) return;

                clicked = true;
                player.closeInventory();

                Tasks.runAsync(() -> {
                    GrantProcedure procedure = profile.getGrantProcedure();
                    Profile target = procedure.getTarget();
                    Grant grant = new Grant(
                            procedure.getTarget().getUuid(),
                            procedure.getRank(),
                            procedure.getProfile().getUuid().toString(),
                            System.currentTimeMillis(),
                            procedure.getReason(),
                            procedure.getDuration(),
                            scopes
                    );

                    RequestResponse response = AltaraPaper.getPaperInstance().getBukkitProfileService().addGrant(target, grant);
                    if (response.couldNotConnect()) {
                        player.sendMessage(CC.format(
                                "<red>Could not connect to API. Adding to queue. Error: %s (%d)</red>",
                                response.getErrorMessage(), response.getCode()));
                    } else if (!response.wasSuccessful()) {
                        player.sendMessage(CC.format("<red>Could not create grant: %s (%d)</red>",
                                response.getErrorMessage(), response.getCode()));
                        return;
                    }

                    if (grant.getDuration() == -1)
                        player.sendMessage(CC.format(
                                "<green>You've <yellow>permanently</yellow> granted %s the %s rank.</green>",
                                target.getName(), procedure.getRank().getName()));
                    else
                        player.sendMessage(CC.format(
                                "<green>You've granted %s the %s rank for <yellow>%s</yellow>.</green>",
                                target.getName(), procedure.getRank().getName(),
                                Time.formatDetailed(grant.getDuration())));
                });
            }
        });
        return buttons;
    }

    @Override
    public void onClose(Player player) {
        if (!clicked) {
            Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(player);
            profile.setGrantProcedure(null);
            player.sendMessage(Component.text("You cancelled the grant procedure.", CC.RED));
        }
    }

    @Override public boolean isAutoUpdate() { return false; }
    @Override public boolean isClickUpdate() { return true; }

    @RequiredArgsConstructor
    public class ScopeButton extends Button {

        private final String server;

        @Override
        public ItemStack getItem(Player player) {
            boolean selected = scopes.contains(server);
            return new ItemBuilder(selected ? Material.LIME_WOOL : Material.GRAY_WOOL)
                    .setDisplayName(Component.text(
                            WordUtils.capitalizeFully(server),
                            selected ? CC.GREEN : CC.GRAY,
                            TextDecoration.BOLD))
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!scopes.contains(server)) scopes.add(server);
            else scopes.remove(server);
        }
    }
}
