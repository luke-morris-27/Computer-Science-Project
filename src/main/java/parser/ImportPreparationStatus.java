/*
 * Class: ImportPreparationStatus
 * Created by: Omesh Sana
 * Description: Represents whether an import can proceed after file-hash deduplication checks.
 * Example: ImportPreparationStatus.READY
 */
package parser;

public enum ImportPreparationStatus {
    READY,
    DUPLICATE
}
