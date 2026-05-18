package games.sparking.altara.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import games.sparking.altara.Altara;
/*import games.sparking.altara.punish.Punishment;
import games.sparking.altara.reports.Report;*/
import lombok.Data;
import lombok.Getter;

@Data
public class MongoCollections {

    @Getter
    private static MongoDatabase database;

/*    @Getter
    private static MongoCollection<Report> reports;

    @Getter
    private static MongoCollection<Punishment> punishments;*/

    public static void init() {
/*        database = Altara.getMongoService().getClient().getDatabase("Altara");

        reports = database.getCollection("reports", Report.class);
        punishments = database.getCollection("punishments", Punishment.class);*/
    }
}
