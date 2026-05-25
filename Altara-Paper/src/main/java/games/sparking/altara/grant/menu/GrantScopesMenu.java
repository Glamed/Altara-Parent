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
                            .setDisplayName(CC.RED + CC.BOLD + "Confirm and grant")
                            .setLore(
                                    CC.MENU_BAR,
                                    CC.RED + "Please select at least one scope.",
                                    CC.MENU_BAR
                            ).build();
                }
                return new ItemBuilder(Material.DIAMOND_SWORD)
                        .setDisplayName(CC.GREEN + CC.BOLD + "Confirm and grant")
                        .setLore(
                                CC.MENU_BAR,
                                CC.format("&eClick to grant %s &ethe %s &erank",
                                        procedure.getTarget().getName(),
                                        procedure.getRank().getName()),
                                CC.YELLOW + (scopes.contains("GLOBAL") ? "This grant will be " + CC.RED + "Global"
                                        : "This grant will apply on: " + CC.RED + StringUtils.join(scopes, ", ")),
                                CC.format("&eReasoning: &c%s", procedure.getReason()),
                                CC.format("&eDuration: &c%s", Time.formatDetailed(procedure.getDuration())),
                                CC.MENU_BAR
                        ).build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                if (scopes.isEmpty()) {
                    return;
                }

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

                    /*Packet packet = new GrantAddPacket(target.getUuid(), grant.getRank().getUuid(),
                            grant.getDuration());*/
                    RequestResponse response = AltaraPaper.getPaperInstance().getBukkitProfileService().addGrant(target, grant);
                    if (response.couldNotConnect()) {
                        player.sendMessage(CC.format("&cCould not connect to API to create grant. " +
                                        "Adding grant to the queue. Error: %s (%d)",
                                response.getErrorMessage(), response.getCode()));
                    } else if (!response.wasSuccessful()) {
                        player.sendMessage(CC.format("&cCould not create grant: %s (%d)",
                                response.getErrorMessage(), response.getCode()));
                        return;
                    }

                    if (grant.getDuration() == -1)
                        player.sendMessage(CC.format(
                                "&aYou've &epermanently &agranted %s&a the %s&a rank.",
                                target.getName(),
                                procedure.getRank().getName()
                        ));
                    else
                        player.sendMessage(CC.format(
                                "&aYou've granted %s&a the %s&a rank for &e%s&a.",
                                target.getName(),
                                procedure.getRank().getName(),
                                Time.formatDetailed(grant.getDuration())
                        ));
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
            player.sendMessage(CC.RED + "You cancelled the grant procedure.");
        }
    }

    @Override
    public boolean isAutoUpdate() {
        return false;
    }

    @Override
    public boolean isClickUpdate() {
        return true;
    }

    @RequiredArgsConstructor
    public class ScopeButton extends Button {

        private final String server;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(scopes.contains(server) ? Material.LIME_WOOL :
                    Material.GRAY_WOOL)
                    .setDisplayName((scopes.contains(server) ? CC.GREEN : CC.GRAY) + CC.BOLD + WordUtils.capitalizeFully(server))
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!scopes.contains(server))
                scopes.add(server);
            else
                scopes.remove(server);

        }
    }
}
