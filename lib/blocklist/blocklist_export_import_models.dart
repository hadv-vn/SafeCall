/// Phien ban schema JSON export/import hien tai (LLD muc "JSON schema").
const int kBlocklistSchemaVersion = 1;

enum ExportOutcome { success, failed }

class ExportResult {
  const ExportResult(this.outcome);

  final ExportOutcome outcome;
}

/// 5 outcome (LLD/UIUX FR-10/AC4-AC9): SUCCESS/CANCELLED/INVALID_FILE/UNSUPPORTED_NEWER_VERSION/WRITE_ERROR.
enum ImportOutcome { success, cancelled, invalidFile, unsupportedNewerVersion, writeError }

class ImportResult {
  const ImportResult(
    this.outcome, {
    this.added = 0,
    this.skippedDuplicate = 0,
    this.skippedInvalid = 0,
  });

  final ImportOutcome outcome;
  final int added;
  final int skippedDuplicate;
  final int skippedInvalid;
}
