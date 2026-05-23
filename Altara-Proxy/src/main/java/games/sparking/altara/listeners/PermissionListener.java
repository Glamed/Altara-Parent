package games.sparking.altara.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;

public class PermissionListener {

    @Subscribe
    public void onPermissionsSetup(PermissionsSetupEvent event) {

        PermissionProvider provider = _ -> (PermissionFunction) permission -> {

            if (permission.equalsIgnoreCase("velocity.command.server")) {
                return Tristate.FALSE;
            }

            return Tristate.UNDEFINED;
        };

        event.setProvider(provider);
    }
}
