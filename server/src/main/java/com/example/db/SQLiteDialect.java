package com.example.db;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;

/**
 * Minimal SQLite dialect for Hibernate 6 (used by Spring Boot 3 / Spring Data JPA).
 *
 * <p>The upstream Hibernate distribution does not ship a SQLite dialect. This subclass
 * provides a valid {@link Dialect} so that Spring Boot can bootstrap against a local
 * SQLite database file. Column type mappings and the AUTOINCREMENT identity behaviour
 * fall back to Hibernate's defaults, which is sufficient for the versioned schema
 * defined in {@code schema.sql}.
 *
 * <p>Two things that the stock {@link Dialect} base class leaves unset are supplied
 * here. First, the base class returns {@code null} from
 * {@link #getSqlAstTranslatorFactory()}, which makes Hibernate's collection and model
 * mutation execution throw a {@link NullPointerException}. This subclass provides the
 * standard translator factory so that entity and collection inserts (for example the
 * NPC saving-throws collection) persist. Second, identity (auto-increment) values are
 * retrieved with SQLite's {@code last_insert_rowid()} function: the stock
 * {@link IdentityColumnSupportImpl} disables insert-select identity retrieval, which
 * makes Hibernate refuse to fetch the generated key for the {@code IDENTITY} strategy
 * and throw {@code HHH000515}. This subclass re-enables it.</p>
 */
public class SQLiteDialect extends Dialect {

    public SQLiteDialect() {
        super();
    }

    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return new StandardSqlAstTranslatorFactory();
    }

    @Override
    public IdentityColumnSupportImpl getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl() {
            @Override
            public boolean supportsIdentityColumns() {
                return true;
            }

            @Override
            public boolean supportsInsertSelectIdentity() {
                return true;
            }

            @Override
            public String getIdentitySelectString(String table, String columnName, int columnType) {
                return "select last_insert_rowid()";
            }
        };
    }
}
