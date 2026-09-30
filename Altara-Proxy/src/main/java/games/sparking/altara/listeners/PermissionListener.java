package games.sparking.altara.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.Tristate;

import java.util.Set;

/**
 * Denies Velocity's built-in commands that bypass the queue, and otherwise defers to
 * whichever provider was installed before us (so a permissions plugin keeps working).
 */
public class PermissionListener {

    private static final Set<String> DENIED = Set.of("velocity.command.server");

    @Subscribe
    public void onPermissionsSetup(PermissionsSetupEvent event) {
        PermissionProvider original = event.getProvider();
        event.setProvider(subject -> {
            PermissionFunction delegate = original.createFunction(subject);
            return permission -> DENIED.contains(permission.toLowerCase())
                    ? Tristate.FALSE
                    : delegate.getPermissionValue(permission);
        });
    }
}
