import 'package:flutter/material.dart';

import '../call_history/call_history_screen.dart';
import '../contacts/contacts_bridge.dart';
import '../onboarding/contacts_banner.dart';
import '../permissions/permissions_bridge.dart';
import '../theme/app_theme.dart';
import '../theme/widgets.dart';
import 'blocklist_export_import_models.dart';
import 'blocklist_export_service.dart';
import 'blocklist_import_service.dart';
import 'blocklist_models.dart';
import 'blocklist_service.dart';
import 'phone_number_normalizer.dart';

/// Man hinh quan ly blocklist thu cong (UIUX: "Man hinh: BlocklistScreen", LLD: `BlocklistScreen`).
/// Mo rong v1.3 (FR-9/7.5.1): bo chon loai entry EXACT/PREFIX; mo rong (FR-10/7.5.1): Export/Import.
class BlocklistScreen extends StatefulWidget {
  const BlocklistScreen({
    super.key,
    required this.permissionsBridge,
  });

  final PermissionsBridge permissionsBridge;

  @override
  State<BlocklistScreen> createState() => _BlocklistScreenState();
}

enum _LoadState { loading, error, loaded }

class _BlocklistScreenState extends State<BlocklistScreen> {
  final BlocklistService _service = BlocklistService();
  final ContactsBridge _contactsBridge = ContactsBridge();
  final PhoneNumberNormalizer _normalizer = PhoneNumberNormalizer();
  final BlocklistExportService _exportService = BlocklistExportService();
  final BlocklistImportService _importService = BlocklistImportService();
  final TextEditingController _inputController = TextEditingController();

  _LoadState _state = _LoadState.loading;
  List<BlocklistEntryDto> _entries = const [];
  String? _inlineError;
  bool _submitting = false;
  bool _exporting = false;
  bool _importing = false;
  EntryType _selectedEntryType = EntryType.exact;

  @override
  void initState() {
    super.initState();
    _loadList();
  }

  @override
  void dispose() {
    _inputController.dispose();
    super.dispose();
  }

  Future<void> _loadList() async {
    setState(() => _state = _LoadState.loading);
    try {
      final entries = await _service.getAll();
      if (!mounted) return;
      setState(() {
        _entries = entries;
        _state = _LoadState.loaded;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _state = _LoadState.error);
    }
  }

  Future<void> _onAddPressed() async {
    final raw = _inputController.text;
    final entryType = _selectedEntryType;
    setState(() => _inlineError = null);

    // Buoc 1 - validate dinh dang tai client (UIUX: "validate inline, ngay khi submit, tai client").
    String normalized;
    try {
      normalized = _normalizer.validateAndNormalize(raw, entryType: entryType);
    } on InvalidPhoneNumberFormatException catch (e) {
      setState(() => _inlineError = e.message);
      return;
    }

    // Buoc 2 - canh bao contact, chi khi co quyen Danh ba. EXACT: FR-5/AC8 (1 contact cu the).
    // PREFIX: FR-9/AC5 (dem N contact khop theo quy tac prefix).
    if (entryType == EntryType.prefix) {
      final count = await _contactsBridge.countContactsMatchingPrefix(normalized);
      if (count > 0) {
        final confirmed = await _showPrefixContactWarningDialog(count);
        if (confirmed != true) return;
      }
    } else {
      final isContact = await _contactsBridge.isNumberInContacts(normalized);
      if (isContact) {
        final confirmed = await _showContactWarningDialog();
        if (confirmed != true) return;
      }
    }

    // Buoc 3 - goi addNumber() thuc, khong pre-check trung tai UI (tranh race - LLD).
    setState(() => _submitting = true);
    try {
      final result = await _service.addNumber(raw, entryType: entryType);
      if (!mounted) return;
      switch (result.outcome) {
        case AddNumberOutcome.success:
          _inputController.clear();
          await _loadList();
          break;
        case AddNumberOutcome.invalidFormat:
        case AddNumberOutcome.alreadyExists:
          setState(() => _inlineError = result.errorMessage);
          break;
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<bool?> _showContactWarningDialog() {
    return showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Số này có trong danh bạ của bạn'),
        content: const Text('Bạn có chắc muốn chặn?'),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Hủy')),
          TextButton(onPressed: () => Navigator.of(context).pop(true), child: const Text('Vẫn chặn')),
        ],
      ),
    );
  }

  Future<bool?> _showPrefixContactWarningDialog(int count) {
    return showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Đầu số này khớp với liên hệ trong danh bạ'),
        content: Text(
          'Đầu số này khớp với $count liên hệ trong danh bạ của bạn, các cuộc gọi từ họ cũng sẽ '
          'bị chặn — bạn có chắc muốn tiếp tục?',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Hủy')),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Vẫn tiếp tục'),
          ),
        ],
      ),
    );
  }

  Future<void> _onDeletePressed(BlocklistEntryDto entry) async {
    final isPrefix = entry.entryType == EntryType.prefix;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(isPrefix ? 'Xóa đầu số khỏi danh sách chặn?' : 'Xóa số khỏi danh sách chặn?'),
        content: Text(
          isPrefix
              ? 'Xóa đầu số ${entry.normalizedNumber} khỏi danh sách chặn? Các số thuộc dải này '
                  'sẽ không còn bị chặn tự động nữa (trừ khi vẫn khớp số cụ thể khác đang bị chặn).'
              : 'Xóa số ${entry.normalizedNumber} khỏi danh sách chặn? '
                  'Số này sẽ không còn bị chặn tự động nữa.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Hủy')),
          TextButton(onPressed: () => Navigator.of(context).pop(true), child: const Text('Xóa')),
        ],
      ),
    );
    if (confirmed != true) return;

    await _service.removeNumber(entry.id);
    if (!mounted) return;
    await _loadList();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Đã xóa ${entry.normalizedNumber}')),
    );
  }

  Future<void> _onExportPressed() async {
    setState(() => _exporting = true);
    try {
      final result = await _exportService.exportBlocklist();
      if (!mounted) return;
      if (result.outcome == ExportOutcome.failed) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text('Không tạo được file xuất, vui lòng thử lại'),
            action: SnackBarAction(label: 'Thử lại', onPressed: _onExportPressed),
          ),
        );
      }
      // Thanh cong (hoac nguoi dung huy trong Share Sheet) - khong hien thong bao gi them (UIUX AC3).
    } finally {
      if (mounted) setState(() => _exporting = false);
    }
  }

  Future<void> _onImportPressed() async {
    setState(() => _importing = true);
    try {
      final result = await _importService.importBlocklist();
      if (!mounted) return;
      if (result.outcome == ImportOutcome.cancelled) return;
      await _showImportResultDialog(result);
      await _loadList();
    } finally {
      if (mounted) setState(() => _importing = false);
    }
  }

  Future<void> _showImportResultDialog(ImportResult result) {
    final message = switch (result.outcome) {
      ImportOutcome.success =>
        'Đã thêm ${result.added} số mới, bỏ qua ${result.skippedDuplicate} số đã có'
            '${result.skippedInvalid > 0 ? ", ${result.skippedInvalid} dòng không hợp lệ" : ""}.',
      ImportOutcome.invalidFile => 'File không đúng định dạng của SafeCall.',
      ImportOutcome.unsupportedNewerVersion =>
        'File này được xuất từ một phiên bản SafeCall mới hơn, chưa thể nhập ở phiên bản hiện tại.',
      ImportOutcome.writeError => 'Không ghi được dữ liệu, vui lòng thử lại.',
      ImportOutcome.cancelled => '',
    };
    return showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Kết quả nhập blocklist'),
        content: Text(message),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Đóng')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final canExport = _entries.isNotEmpty;
    return Scaffold(
      appBar: GradientAppBar(
        title: 'Danh sách chặn',
        actions: [
          IconButton(
            icon: _exporting
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.upload_outlined),
            tooltip: canExport ? 'Xuất' : 'Chưa có số nào để xuất',
            onPressed: (canExport && !_exporting) ? _onExportPressed : null,
          ),
          IconButton(
            icon: _importing
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.download_outlined),
            tooltip: 'Nhập',
            onPressed: _importing ? null : _onImportPressed,
          ),
          IconButton(
            icon: const Icon(Icons.history),
            tooltip: 'Lịch sử cuộc gọi',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (context) => const CallHistoryScreen()),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          ContactsDeniedBanner(bridge: widget.permissionsBridge),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: SegmentedButton<EntryType>(
              segments: const [
                ButtonSegment(value: EntryType.exact, label: Text('Số cụ thể')),
                ButtonSegment(value: EntryType.prefix, label: Text('Đầu số')),
              ],
              selected: {_selectedEntryType},
              onSelectionChanged: (selection) => setState(() {
                _selectedEntryType = selection.first;
                _inlineError = null;
              }),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _inputController,
                    keyboardType: TextInputType.phone,
                    decoration: InputDecoration(
                      hintText: _selectedEntryType == EntryType.prefix
                          ? 'Nhập đầu số, ví dụ: 0901 hoặc +84901'
                          : 'Nhập số điện thoại, ví dụ: 0901234567',
                      errorText: _inlineError,
                      border: const OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                GradientButton(
                  onPressed: _submitting ? null : _onAddPressed,
                  child: const Text('Thêm'),
                ),
              ],
            ),
          ),
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildBody() {
    switch (_state) {
      case _LoadState.loading:
        return const Center(child: CircularProgressIndicator());
      case _LoadState.error:
        return Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Không tải được danh sách chặn.'),
              TextButton(onPressed: _loadList, child: const Text('Thử lại')),
            ],
          ),
        );
      case _LoadState.loaded:
        if (_entries.isEmpty) {
          return const Center(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.shield_outlined, size: 48, color: AppColors.primary),
                  SizedBox(height: 12),
                  Text('Chưa có số nào bị chặn'),
                  SizedBox(height: 4),
                  Text(
                    'Nhập số điện thoại hoặc đầu số bạn muốn chặn ở ô phía trên',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: AppColors.textSecondary),
                  ),
                ],
              ),
            ),
          );
        }
        return ListView.builder(
          padding: const EdgeInsets.only(top: 4, bottom: 8),
          itemCount: _entries.length,
          itemBuilder: (context, index) {
            final entry = _entries[index];
            final isPrefix = entry.entryType == EntryType.prefix;
            return Card(
              child: ListTile(
                leading: Icon(isPrefix ? Icons.call_split : Icons.phone),
                title: Row(
                  children: [
                    Text(isPrefix ? '${entry.normalizedNumber}*' : entry.normalizedNumber),
                    const SizedBox(width: 8),
                    Chip(
                      label: Text(isPrefix ? 'Đầu số' : 'Số cụ thể', style: const TextStyle(fontSize: 11)),
                      visualDensity: VisualDensity.compact,
                      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                  ],
                ),
                subtitle: Text(_sourceLabel(entry.source)),
                trailing: IconButton(
                  icon: const Icon(Icons.delete_outline),
                  onPressed: () => _onDeletePressed(entry),
                ),
              ),
            );
          },
        );
    }
  }

  String _sourceLabel(BlocklistSource source) => switch (source) {
        BlocklistSource.manual => 'Thủ công',
        BlocklistSource.autoConfirmed => 'Tự động sau xác nhận',
        BlocklistSource.imported => 'Nhập từ file',
      };
}
