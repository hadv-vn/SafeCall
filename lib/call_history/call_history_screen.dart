import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../theme/app_theme.dart';
import '../theme/widgets.dart';
import 'call_history_models.dart';
import 'call_history_service.dart';

/// Man hinh xem lai lich su cuoc goi, chi doc (UIUX: `CallHistoryScreen`, LLD: `CallHistoryService`).
///
/// Pham vi Phase 5 (5.2.1): 3 loai badge trang thai (Co/Khong/Binh thuong) - 2 badge
/// `BLOCKED_REALTIME_EXACT`/`BLOCKED_REALTIME_PREFIX` (Phase 7) va cac badge outcome
/// `RingingScamOverlay` (Phase 6) CHUA co du lieu de hien thi (`call_history` o giai doan nay
/// luon co `was_blocked=0` - xem ghi chu call_history_models.dart).
class CallHistoryScreen extends StatefulWidget {
  const CallHistoryScreen({super.key});

  @override
  State<CallHistoryScreen> createState() => _CallHistoryScreenState();
}

enum _LoadState { loading, error, loaded }

class _CallHistoryScreenState extends State<CallHistoryScreen> {
  static const int _pageSize = 20;

  final CallHistoryService _service = CallHistoryService();
  final ScrollController _scrollController = ScrollController();

  _LoadState _state = _LoadState.loading;
  final List<CallHistoryEntryDto> _entries = [];
  bool _hasMore = true;
  bool _loadingMore = false;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _loadFirstPage();
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_hasMore || _loadingMore || _state != _LoadState.loaded) return;
    if (_scrollController.position.pixels >= _scrollController.position.maxScrollExtent - 200) {
      _loadNextPage();
    }
  }

  Future<void> _loadFirstPage() async {
    setState(() => _state = _LoadState.loading);
    try {
      final page = await _service.getHistory(limit: _pageSize, offset: 0);
      if (!mounted) return;
      setState(() {
        _entries.clear();
        _entries.addAll(page);
        _hasMore = page.length == _pageSize;
        _state = _LoadState.loaded;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _state = _LoadState.error);
    }
  }

  Future<void> _loadNextPage() async {
    setState(() => _loadingMore = true);
    try {
      final page = await _service.getHistory(limit: _pageSize, offset: _entries.length);
      if (!mounted) return;
      setState(() {
        _entries.addAll(page);
        _hasMore = page.length == _pageSize;
      });
    } catch (_) {
      // UIUX khong dac ta loi rieng cho "tai them" - giu danh sach hien tai, chi dung tai them
      // (khong chuyen toan man hinh sang Error vi da co du lieu trang truoc).
      if (!mounted) return;
      setState(() => _hasMore = false);
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const GradientAppBar(title: 'Lịch sử cuộc gọi'),
      body: _buildBody(),
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
              const Text('Không tải được lịch sử cuộc gọi.'),
              TextButton(onPressed: _loadFirstPage, child: const Text('Thử lại')),
            ],
          ),
        );
      case _LoadState.loaded:
        if (_entries.isEmpty) {
          return const Center(child: Text('Chưa có cuộc gọi nào được ghi nhận'));
        }
        return ListView.builder(
          controller: _scrollController,
          padding: const EdgeInsets.only(top: 4, bottom: 8),
          itemCount: _entries.length + (_hasMore ? 1 : 0),
          itemBuilder: (context, index) {
            if (index >= _entries.length) {
              return const Padding(
                padding: EdgeInsets.all(16),
                child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
              );
            }
            return _CallHistoryTile(entry: _entries[index], service: _service);
          },
        );
    }
  }
}

class _CallHistoryTile extends StatelessWidget {
  const _CallHistoryTile({required this.entry, required this.service});

  final CallHistoryEntryDto entry;
  final CallHistoryService service;

  static final DateFormat _timeFormat = DateFormat('HH:mm dd/MM/yyyy');

  @override
  Widget build(BuildContext context) {
    final status = service.deriveDisplayStatus(entry);
    final (label, color) = switch (status) {
      CallHistoryDisplayStatus.blockedRealtimeExact => ('Đã chặn — Số cụ thể', AppColors.danger),
      CallHistoryDisplayStatus.blockedRealtimePrefix =>
        ('Đã chặn — Đầu số ${entry.matchedPrefixValue}', AppColors.danger),
      CallHistoryDisplayStatus.blockedAtRinging => ('Đã chặn & đã ngắt cuộc gọi', AppColors.success),
      CallHistoryDisplayStatus.confirmedYes => ('Đã hỏi xác nhận — Có', AppColors.danger),
      CallHistoryDisplayStatus.confirmedNo => ('Đã hỏi xác nhận — Không', AppColors.success),
      CallHistoryDisplayStatus.normal => ('Bình thường', AppColors.textSecondary),
    };
    // Badge PHU (UIUX "hiển thị SAU") - doc lap voi badge chinh o tren, ap dung cho CA 4 outcome
    // FR-8 (ke ca 3 outcome khong khop blockedAtRinging - xem ghi chu deriveDisplayStatus).
    final ringingBadge = service.ringingOutcomeBadgeLabel(entry);
    final semanticsLabel = '${entry.rawNumber ?? "Số không xác định"}, '
        '${_timeFormat.format(entry.endedAt)}, trạng thái: $label'
        '${ringingBadge != null ? ', $ringingBadge' : ''}';

    return Semantics(
      label: semanticsLabel,
      child: Card(
        child: ListTile(
          title: Text(entry.rawNumber ?? 'Số không xác định'),
          subtitle: Text(_timeFormat.format(entry.endedAt)),
          trailing: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Chip(
                label: Text(label, style: const TextStyle(fontSize: 12)),
                backgroundColor: color.withValues(alpha: 0.18),
                labelStyle: TextStyle(color: color, fontWeight: FontWeight.w700),
                side: BorderSide(color: color.withValues(alpha: 0.5)),
              ),
              if (ringingBadge != null)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    ringingBadge,
                    style: const TextStyle(fontSize: 11, color: AppColors.warning),
                    textAlign: TextAlign.end,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
