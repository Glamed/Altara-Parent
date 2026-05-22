package games.sparking.altara.grant;

import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Data
public class GrantProcedure {

    private final Profile profile;
    private final Profile target;
    private Grant grant;

    private Rank rank;
    private String reason = "";
    private long duration = -1;
    private List<String> scopes = new ArrayList<>();

}
