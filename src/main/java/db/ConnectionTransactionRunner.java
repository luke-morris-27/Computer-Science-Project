/*
 * Class: ConnectionTransactionRunner
 * Created by: Luke Morris
 * Description: Runs database work inside a transaction and guarantees commit or rollback.
 * Example: runner.run(conn -> doImportWork(conn))
 */
package db;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionTransactionRunner {
    @FunctionalInterface
    public interface ConnectionFactory {
        Connection open() throws SQLException;
    }

    /*
     * Functional interface representing a block of work to execute
     * inside a transaction.
     *
     * Example usage:
     * runner.run(conn -> {
     *     // DB operations here
     *     return result;
     * });
     */
    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    // Supplies database connection when needed
    private final ConnectionFactory connectionFactory;

    public ConnectionTransactionRunner(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public <T> T run(TransactionWork<T> work) throws SQLException {
        // Acquire transaction, store original auto commit setting
        Connection conn = connectionFactory.open();
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
