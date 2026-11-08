import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import '../theme/widgets.dart';
import 'settings_models.dart';
import 'settings_service.dart';

/// Man hinh Cai dat (UIUX: "Man hinh: SettingsScreen", LLD: `SettingsScreen`, MOI v1.4/v1.7,
/// FR-11/CONTACTS-PRIORITY + FR-12/BLOCK-TOGGLE). Vao tu icon "Cai dat" tren AppBar cua
/// `BlocklistScreen`. Doi co nao ap dung ngay, khong co nut "Luu" rieng.
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

enum _LoadState { loading, error, loaded }

class _SettingsScreenState extends State<SettingsScreen> {
  final SettingsService _service = SettingsService();

  _LoadState _state = _LoadState.loading;
  SettingsFlags _flags = const SettingsFlags.defaults();

  @override
  void initState() {
    super.initState();
    _loadFlags();
  }

  Future<void> _loadFlags() async {
    setState(() => _state = _LoadState.loading);
    try {
      final flags = await _service.getFlags();
      if (!mounted) return;
      setState(() {
        _flags = flags;
        _state = _LoadState.loaded;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _state = _LoadState.error);
    }
  }

  /// UIUX muc "Hanh vi ghi co": doi UI ngay (optimistic), ghi qua `SettingsService.setFlag`;
  /// neu loi - revert lai gia tri truoc do + SnackBar loi kem "Thu lai".
  Future<void> _onToggleChanged(SettingsFlag flag, bool value) async {
    final previous = _flags;
    setState(() => _flags = _flags.withValue(flag, value));
    try {
      await _service.setFlag(flag, value);
    } catch (_) {
      if (!mounted) return;
      setState(() => _flags = previous);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('Không lưu được cài đặt, vui lòng thử lại.'),
          action: SnackBarAction(
            label: 'Thử lại',
            onPressed: () => _onToggleChanged(flag, value),
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const GradientAppBar(title: 'Cài đặt'),
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
              const Text('Không tải được cài đặt.'),
              TextButton(onPressed: _loadFlags, child: const Text('Thử lại')),
            ],
          ),
        );
      case _LoadState.loaded:
        return ListView(
          padding: const EdgeInsets.symmetric(vertical: 8),
          children: [
            const _SectionHeader(title: 'Ưu tiên liên hệ'),
            _SwitchTile(
              title: 'Ưu tiên Danh bạ',
              description: 'Không chặn người quen trong danh bạ, dù số của họ vô tình khớp một '
                  'đầu số bạn đã chặn. Không ảnh hưởng tới số bạn đã chặn trực tiếp (số cụ thể) — '
                  'số đó vẫn luôn bị chặn.',
              value: _flags.contactsPriorityEnabled,
              onChanged: (value) => _onToggleChanged(SettingsFlag.contactsPriority, value),
            ),
            const Divider(height: 24),
            const _SectionHeader(
              title: 'Chặn nâng cao',
              subtitle: 'Chặn thẳng theo điều kiện, không cần thêm từng số vào danh sách chặn. '
                  'Cả 3 mặc định tắt.',
            ),
            _SwitchTile(
              title: 'Chặn tất cả số lạ',
              description: 'Chặn mọi cuộc gọi từ số KHÔNG có trong danh bạ của bạn. Số đã lưu '
                  'trong danh bạ luôn được gọi bình thường.',
              warning: 'Lưu ý: có thể chặn cả cuộc gọi quan trọng từ số bạn chưa lưu (ví dụ ngân '
                  'hàng, giao hàng, bệnh viện).',
              value: _flags.blockUnknownEnabled,
              onChanged: (value) => _onToggleChanged(SettingsFlag.blockUnknown, value),
            ),
            _SwitchTile(
              title: 'Chặn số ẩn danh',
              description: 'Chặn các cuộc gọi không hiển thị số (số ẩn danh/riêng tư). Không áp '
                  'dụng miễn trừ danh bạ cho công tắc này — số ẩn danh không có số để xác định có '
                  'phải người quen hay không.',
              value: _flags.blockAnonymousEnabled,
              onChanged: (value) => _onToggleChanged(SettingsFlag.blockAnonymous, value),
            ),
            _SwitchTile(
              title: 'Chặn số quốc tế',
              description: 'Chặn cuộc gọi từ số nước ngoài (khác mã vùng Việt Nam +84). Số đã lưu '
                  'trong danh bạ luôn được gọi bình thường. Quốc gia tham chiếu cố định +84, Phase '
                  '1 chưa hỗ trợ đổi.',
              value: _flags.blockInternationalEnabled,
              onChanged: (value) => _onToggleChanged(SettingsFlag.blockInternational, value),
            ),
          ],
        );
    }
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.title, this.subtitle});

  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              color: AppColors.primary,
              fontWeight: FontWeight.w700,
              fontSize: 15,
            ),
          ),
          if (subtitle != null) ...[
            const SizedBox(height: 4),
            Text(subtitle!, style: const TextStyle(color: AppColors.textSecondary, fontSize: 12)),
          ],
        ],
      ),
    );
  }
}

class _SwitchTile extends StatelessWidget {
  const _SwitchTile({
    required this.title,
    required this.description,
    required this.value,
    required this.onChanged,
    this.warning,
  });

  final String title;
  final String description;
  final String? warning;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
                subtitle: Text(description),
                value: value,
                onChanged: onChanged,
              ),
              if (warning != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(0, 0, 0, 12),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.warning_amber_rounded, color: AppColors.warning, size: 18),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          warning!,
                          style: const TextStyle(color: AppColors.warning, fontSize: 12),
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
