/*
 * Class: ConnectionTransactionRunner
 * Created by: Person 3
 * Description: Runs database work inside a transaction and guarantees commit or rollback.
 * Example: runner.run(conn -> doImportWork(conn))
 */
package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public class ConnectionTransactionRunner {
    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    private final Supplier<Connection> connectionSupplier;

    public ConnectionTransactionRunner(Supplier<Connection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    public <T> T run(TransactionWork<T> work) throws SQLException {
        // Guidance:
        // 1. Acquire connection.
        // 2. Disable auto-commit.
        // 3. Execute work.
        // 4. Commit on success.
        // 5. Roll back on SQLException/RuntimeException.
        // 6. Restore original auto-commit.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
