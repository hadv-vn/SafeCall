import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import 'blocklist_export_import_models.dart';
import 'blocklist_models.dart';
import 'blocklist_service.dart';

/// Export toan bo blocklist ra file JSON, chia se qua Android Share Sheet (LLD:
/// `BlocklistExportService`, FR-10/AC1-AC3). Chi doc (`BlocklistService.getAll()`), KHONG bao
/// gio ghi de blocklist - huy Share Sheet khong lam thay doi gi (AC3).
class BlocklistExportService {
  BlocklistExportService({BlocklistService? blocklistService})
      : _blocklistService = blocklistService ?? BlocklistService();

  final BlocklistService _blocklistService;

  Future<ExportResult> exportBlocklist() async {
    try {
      final entries = await _blocklistService.getAll();
      final payload = _buildExportPayload(entries);
      final filePath = await _writeTempFile(payload);
      await Share.shareXFiles([XFile(filePath, mimeType: 'application/json')]);
      return const ExportResult(ExportOutcome.success);
    } catch (_) {
      return const ExportResult(ExportOutcome.failed);
    }
  }

  Map<String, dynamic> _buildExportPayload(List<BlocklistEntryDto> entries) {
    return {
      'schema_version': kBlocklistSchemaVersion,
      'exported_at': DateTime.now().toUtc().toIso8601String(),
      'entries': entries
          .map(
            (e) => {
              'entry_type': e.entryType.wireValue,
              'value': e.normalizedNumber,
              'source': e.source.wireValue,
            },
          )
          .toList(),
    };
  }

  Future<String> _writeTempFile(Map<String, dynamic> payload) async {
    final dir = await getTemporaryDirectory();
    final filePath = p.join(dir.path, 'safecall_blocklist_export.json');
    final file = File(filePath);
    await file.writeAsString(jsonEncode(payload));
    return filePath;
  }
}
