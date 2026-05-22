package games.sparking.altara.connection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.Request;

@RequiredArgsConstructor
public abstract class BackLogEntry {

    @Getter
    private final Request.Builder builder;

    public abstract void onSend(RequestResponse response);

}
