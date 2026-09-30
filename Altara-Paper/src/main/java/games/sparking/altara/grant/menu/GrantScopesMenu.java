package games.sparking.altara.grant.menu;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.grant.GrantProcedure;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/** Step 3 of {@code /grant}: choose where the grant applies, then confirm. */
public class GrantScopesMenu extends Menu {

    private static final String GLOBAL = "GLOBAL";
    private static final int[] SCOPE_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private final Profile profile;
    private final List<String> scopes = new ArrayList<>();
    private boolean continued = false;

    public GrantScopesMenu(Profile profile) {
        this.profile = profile;
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Grant", "Scopes");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.ALTARA;
    }

    @Override
    public boolean isAutoUpdate() {
        return false;
    }

    @Override
    public boolean isClickUpdate() {
        return true;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        GrantProcedure procedure = profile.getGrantProcedure();
        if (procedure == null) return buttons;

        buttons.put(4, Button.createPlaceholder(summary(procedure)));
        buttons.put(13, new ScopeButton(GLOBAL));

        // One toggle per server group currently on the network (proxies can't hold ranks).
        Set<String> groups = new TreeSet<>();
        for (ServerInfo server : ServerInfo.getServers()) {
            if (!server.isProxy() && !server.getGroup().isBlank()) groups.add(server.getGroup().toLowerCase());
        }
        int index = 0;
        for (String group : groups) {
            if (index >= SCOPE_SLOTS.length) break;
            buttons.put(SCOPE_SLOTS[index++], new ScopeButton(group));
        }

        buttons.put(49, new ConfirmButton());
        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());
        return buttons;
    }

    private ItemStack summary(GrantProcedure procedure) {
        return new ItemBuilder(Material.PAPER)
                .setDisplayName(Gui.name("Grant", "Summary"))
                .setLore(Gui.lore()
                        .value("Player", procedure.getTarget().getName())
                        .value("Rank", CC.format(procedure.getRank().getDisplayName()))
                        .value("Duration", procedure.getDuration() == -1 ? "Permanent" : Time.formatDetailed(procedure.getDuration()))
                        .value("Reason", procedure.getReason())
                        .build())
                .build();
    }

    @Override
    public void onClose(Player player) {
        if (!continued) {
            profile.setGrantProcedure(null);
            player.sendMessage(CC.info("Grant cancelled."));
        }
    }

    private class ScopeButton extends Button {

        private final String scope;

        ScopeButton(String scope) {
            this.scope = scope;
        }

        @Override
        public ItemStack getItem(Player player) {
            boolean selected = scopes.contains(scope);
            boolean global = scope.equals(GLOBAL);
            return new ItemBuilder(global ? (selected ? Material.ENDER_EYE : Material.ENDER_PEARL) : Gui.settingMaterial(selected))
                    .setDisplayName(Gui.settingName(global ? "Every realm" : StringUtils.capitalize(scope), selected))
                    .setLore(Gui.lore()
                            .text(global ? "The rank applies everywhere." : "The rank applies on " + scope + " realms.")
                            .cta(selected ? "remove this scope" : "add this scope")
                            .build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!scopes.remove(scope)) scopes.add(scope);
        }
    }

    private class ConfirmButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            if (scopes.isEmpty()) {
                return new ItemBuilder(Material.GRAY_WOOL)
                        .setDisplayName(Component.text(Theme.CROSS + " ", Theme.TEXT)
                                .append(Component.text("Confirm grant", Theme.ERROR)))
                        .setLore(Gui.lore().text("Select at least one scope first.").build())
                        .build();
            }

            String where = scopes.contains(GLOBAL) ? "every realm" : String.join(", ", scopes);
            return Gui.acceptItem("Confirm grant", Gui.lore()
                    .value("Applies on", where)
                    .cta("issue this grant")
                    .build());
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            GrantProcedure procedure = profile.getGrantProcedure();
            if (scopes.isEmpty() || procedure == null) return;

            continued = true;
            profile.setGrantProcedure(null);
            player.closeInventory();

            List<String> grantScopes = scopes.contains(GLOBAL) ? List.of(GLOBAL) : new ArrayList<>(scopes);
            Tasks.runAsync(() -> issue(player, procedure, grantScopes));
        }
    }

    private static void issue(Player player, GrantProcedure procedure, List<String> scopes) {
        Profile target = procedure.getTarget();
        Grant grant = new Grant(target.getUuid(), procedure.getRank(), procedure.getProfile().getUuid().toString(),
                System.currentTimeMillis(), procedure.getReason(), procedure.getDuration(), scopes);

        RequestResponse response = AltaraPaper.getPaperInstance().getBukkitProfileService().addGrant(target, grant);
        if (response.couldNotConnect()) {
            player.sendMessage(CC.notice("Grant queued.", "The API is unreachable, so the grant will be sent when it's back."));
            return;
        }
        if (!response.wasSuccessful()) {
            player.sendMessage(CC.error("Unable to grant rank.", response.getErrorMessage() + " (" + response.getCode() + ")"));
            return;
        }

        player.sendMessage(CC.success("Rank granted.", "*" + target.getName() + "* now has *" + procedure.getRank().getName() + "* "
                + (grant.getDuration() == -1 ? "permanently" : "for *" + Time.formatDetailed(grant.getDuration()) + "*") + "."));
    }
}
