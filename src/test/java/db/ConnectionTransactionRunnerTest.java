package db;

/*
 * Class: ConnectionTransactionRunnerTest
 * Created by: Archisha Sasson
 * Description: Verifies transaction runner commit, rollback, and exception handling behavior using a proxied connection.
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@Tag("task1-person3")
@DisplayName("Connection Transaction Runner Tests")
class ConnectionTransactionRunnerTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies commit/rollback transaction behavior.");
    }

    @Test
    @DisplayName("Successful work commits exactly once")
    void successfulRunCommits() throws Exception {
        ConnectionState state = new ConnectionState();
        ConnectionTransactionRunner runner = new ConnectionTransactionRunner(() -> fakeConnection(state));

        String result = runner.run(conn -> "ok");

        assertEquals("ok", result);
        assertEquals(1, state.commits.get());
        assertEquals(0, state.rollbacks.get());
    }

    @Test
    @DisplayName("SQLException triggers rollback")
    void sqlExceptionRollsBack() {
        ConnectionState state = new ConnectionState();
        ConnectionTransactionRunner runner = new ConnectionTransactionRunner(() -> fakeConnection(state));

        assertThrows(SQLException.class, () -> runner.run(conn -> {
            throw new SQLException("boom");
        }));

        assertEquals(0, state.commits.get());
        assertEquals(1, state.rollbacks.get());
    }

    private Connection fakeConnection(ConnectionState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "getAutoCommit" -> state.autoCommit;
                    case "setAutoCommit" -> {
                        state.autoCommit = (boolean) args[0];
                        yield null;
                    }
                    case "commit" -> {
                        state.commits.incrementAndGet();
                        yield null;
                    }
                    case "rollback" -> {
                        state.rollbacks.incrementAndGet();
                        yield null;
                    }
                    case "close" -> null;
                    default -> defaultValue(method.getReturnType());
                };
            }
        };

        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class[]{Connection.class},
            handler
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    private static final class ConnectionState {
        private boolean autoCommit = true;
        private final AtomicInteger commits = new AtomicInteger();
        private final AtomicInteger rollbacks = new AtomicInteger();
    }
}
