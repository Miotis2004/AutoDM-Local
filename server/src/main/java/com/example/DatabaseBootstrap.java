package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Safe local database bootstrap.
 *
 * <p>The back-end stores all campaign state in a single local SQLite file whose location is
 * fully configurable (see {@code autodm.database.path} in
 * {@code server/src/main/resources/application.properties}). This bootstrap runs before any
 * connection is opened so that a brand-new database can be created safely, while never
 * disturbing an existing database:
 *
 * <ul>
 *   <li><b>Creates the database when none exists.</b> SQLite creates the database file lazily
 *       the first time a connection is opened. If the file's parent directory does not exist
 *       that creation fails with an IO error, so this initializer ensures the parent directory
 *       exists first. A relative path such as {@code ./data/autodm.db} therefore works instead
 *       of failing because {@code ./data} was missing.</li>
 *   <li><b>Never destroys existing data.</b> Creating a directory or a new file is a no-op when
 *       the directory or file already exists. This component performs no schema changes, no
 *       {@code DROP}, and no data writes. The schema itself is versioned in
 *       {@code src/main/resources/schema.sql} and applied idempotently by Spring Boot's SQL
 *       init (only {@code CREATE TABLE IF NOT EXISTS statements} are run, on every startup), so
 *       existing campaign data is preserved across restarts.</li>
 *   <li><b>Idempotent.</b> Running it any number of times has the same effect as running it once:
 *       the parent directory exists and, if the database file was absent, it is created empty.</li>
 * </ul>
 */
public class DatabaseBootstrap
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrap.class);

    private static final String DATABASE_PATH_PROPERTY = "autodm.database.path";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Assert.notNull(context, "context must not be null");
        final Environment environment = context.getEnvironment();
        Assert.isTrue(environment.containsProperty(DATABASE_PATH_PROPERTY),
                () -> "Required property '" + DATABASE_PATH_PROPERTY + "' is not configured");

        final String path = environment.getProperty(DATABASE_PATH_PROPERTY);
        if (path == null || path.isBlank()) {
            log.warn("'{}' is empty; SQLite will create the database file in the working directory",
                    DATABASE_PATH_PROPERTY);
            return;
        }

        final Path databasePath = Paths.get(path);
        final Path parent = databasePath.getParent();
        if (parent == null) {
            // A bare file name (e.g. "autodm.db") lives in the working directory; nothing to create.
            log.info("Autodm database file will be created in the working directory: {}", path);
            return;
        }

        ensureDirectory(parent);
    }

    private void ensureDirectory(Path directory) {
        if (Files.isDirectory(directory)) {
            return;
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to ensure database directory exists for " + directory, e);
        }
        log.info("Ensured database directory exists: {}", directory.toAbsolutePath());
    }
}
