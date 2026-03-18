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

        Connection conn = connectionSupplier.get();
        boolean originalAutoCommit = true;

        try {
            // 1. Save current auto-commit state
            originalAutoCommit = conn.getAutoCommit();

            // 2. Disable auto-commit (start transaction)
            conn.setAutoCommit(false);

            // 3. Execute work
            T result = work.execute(conn);

            // 4. Commit on success
            conn.commit();

            return result;

        } catch (SQLException | RuntimeException e) {
            // 5. Rollback on failure
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                // If rollback fails, include it in exception
                rollbackEx.addSuppressed(e);
                throw rollbackEx;
            }
            throw e;

        } finally {
            try {
                // 6. Restore auto-commit
                conn.setAutoCommit(originalAutoCommit);
                conn.close(); // close connection safely
            } catch (SQLException ignored) {
            }
        }
    }
}
