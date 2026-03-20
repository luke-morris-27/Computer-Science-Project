/*
 * Class: ImportHashLookup
 * Created by: Person 1
 * Description: Contract for checking whether a file hash already exists in persistent storage.
 * Example: hash -> importDao.existsByHash(hash)
 */
package parser;

import java.sql.SQLException;

@FunctionalInterface
public interface ImportHashLookup {
    boolean existsByHash(String fileHash) throws SQLException;
}
