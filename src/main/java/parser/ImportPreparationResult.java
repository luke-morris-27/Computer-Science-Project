/*
 * Class: ImportPreparationResult
 * Created by: Person 1
 * Description: Captures the outcome of pre-import validation and deduplication checks.
 * Example: new ImportPreparationResult(ImportPreparationStatus.READY, hash, "Ready to import")
 */
package parser;

public record ImportPreparationResult(ImportPreparationStatus status, String fileHash, String message) {

    public boolean readyToImport() {
        // Return true only when status is READY.
        return status == ImportPreparationStatus.READY;
    }
}
