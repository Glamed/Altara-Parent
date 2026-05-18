package games.sparking.altara.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoService.class);
    private final String connectionString;
    private final String databaseName;
    @Getter
    private MongoClient client;
    @Getter
    private MongoDatabase database;

    public MongoService(String connectionString, String databaseName) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
    }

    /**
     * Connects to MongoDB and verifies the connection by pinging the database.
     *
     * @return true if connection is successful, false otherwise
     */
    public MongoService connect() {
        try {
            // Setup POJO codec registry for automatic mapping
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(this.connectionString))
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .codecRegistry(pojoCodecRegistry)
                    .build();

            this.client = MongoClients.create(settings);
            this.database = client.getDatabase(this.databaseName);

            // Verify connection
            database.runCommand(new Document("ping", 1));
            LOGGER.info("Successfully connected to MongoDB database '{}'", databaseName);

            return this;
        } catch (Exception e) {
            LOGGER.error("Failed to connect to MongoDB database '{}'", databaseName, e);
            return this;
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            LOGGER.info("MongoDB connection closed for database '{}'", databaseName);
        }
    }
}
