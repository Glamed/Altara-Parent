package games.sparking.altara.disguise;

import games.sparking.altara.Altara;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;

import java.util.UUID;

public class DisguiseService {


    public DisguiseData getDisguiseData(UUID uuid) {
        RequestResponse response = RequestHandler.get("disguise/%s", uuid.toString());
        if (response.wasSuccessful())
            return new DisguiseData(response.asObject());

        if (response.getCode() == 404) {
            return new DisguiseData(uuid);
        }

        Altara.getSharedInstance().getLogger().warning(String.format(
                "Could not load disguise data of %s: %s (%d)",
                uuid,
                response.getErrorMessage(),
                response.getCode()
        ));
        return null;
    }

}
