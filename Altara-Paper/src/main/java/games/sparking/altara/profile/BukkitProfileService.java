package games.sparking.altara.profile;

import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.utils.json.JsonBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class BukkitProfileService {

    public RequestResponse addGrant(Profile target, Grant grant) {
        JsonBuilder builder = new JsonBuilder();
        builder.add("id", grant.getId());
        builder.add("rank", grant.asRank().getUuid());
        builder.add("grantedBy", grant.getGrantedBy());
        builder.add("grantedAt", grant.getGrantedAt());
        builder.add("grantedReason", grant.getGrantedReason());
        builder.add("scopes", StringUtils.join(grant.getScopes(), ","));
        builder.add("duration", grant.getDuration());
        builder.add("end", grant.getEnd());

        RequestResponse response = RequestHandler.post("api/profile/%s/grants", builder.build(),
                target.getUuid().toString());
        if (response.couldNotConnect())
            RequestHandler.addToBackLog(new GrantBackLogEntry(grant, target.getUuid(),
                    response.getRequestBuilder()));
        return response;
    }

    public RequestResponse removeGrant(Profile target, Grant grant) {
        JsonBuilder builder = new JsonBuilder();
        builder.add("removedBy", grant.getRemovedBy());
        builder.add("removedAt", grant.getRemovedAt());
        builder.add("removedReason", grant.getRemovedReason());
        builder.add("removed", grant.isRemoved());

        RequestResponse response = RequestHandler.put("api/profile/%s/grants/%s", builder.build(),
                target.getUuid().toString(), grant.getId().toString());
        if (response.couldNotConnect())
            RequestHandler.addToBackLog(new GrantBackLogEntry(grant, target.getUuid(),
                    response.getRequestBuilder()));
        else if (!response.wasSuccessful()) {
            grant.setRemovedBy("N/A");
            grant.setRemovedAt(-1);
            grant.setRemovedReason("N/A");
            grant.setRemoved(false);
        }
        return response;
    }

//    public RequestResponse addNote(Profile target, Note note) {
//        RequestResponse response = RequestHandler.post("profile/%s/notes", note.toJson(), target.getUuid());
//        if (response.couldNotConnect())
//            RequestHandler.addToBackLog(new NoteBackLogEntry(note, target.getUuid(), response.getRequestBuilder()));
//        return response;
//    }
//
//    public RequestResponse removeNote(Profile target, Note note) {
//        return RequestHandler.delete("profile/%s/notes/%s", target.getUuid().toString(), note.getId().toString());
//    }

}