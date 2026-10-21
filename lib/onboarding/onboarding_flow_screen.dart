import 'package:flutter/material.dart';

import '../permissions/permission_keys.dart';
import '../permissions/permissions_bridge.dart';
import 'onboarding_status.dart';
import 'permission_step_config.dart';

enum _FlowPhase { loading, error, welcome, rationale, settingsPending, deniedMandatory, summary }

/// Luong Onboarding/Permission gop (UIUX: "OnboardingScreen + PermissionScreen - gộp một
/// luồng"). Pham vi Phase 1 (1.2.1): Welcome, Rationale (lap lai theo [kOnboardingSteps]),
/// Denied-mandatory, Summary. Banner canh bao Danh ba (buoc 5 UIUX) la widget rieng hien thi
/// xuyen suot app (xem contacts_banner.dart), KHONG phai 1 buoc trong luong nay.
///
/// [startAtDeniedStepFor] : dung cho resume-check (AC4, do RootDecider o main.dart goi lai) -
/// vao thang Denied-mandatory step tuong ung, khong lap lai Welcome/cac buoc da qua.
class OnboardingFlowScreen extends StatefulWidget {
  const OnboardingFlowScreen({
    super.key,
    required this.onComplete,
    this.startAtDeniedStepFor,
  });

  final VoidCallback onComplete;
  final PermissionStepConfig? startAtDeniedStepFor;

  @override
  State<OnboardingFlowScreen> createState() => _OnboardingFlowScreenState();
}

class _OnboardingFlowScreenState extends State<OnboardingFlowScreen> with WidgetsBindingObserver {
  final PermissionsBridge _bridge = PermissionsBridge();

  _FlowPhase _phase = _FlowPhase.loading;
  int _currentStepIndex = 0;
  String? _errorMessage;
  bool _requestInFlight = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    final startAt = widget.startAtDeniedStepFor;
    if (startAt != null) {
      _currentStepIndex = kOnboardingSteps.indexOf(startAt);
      _phase = _FlowPhase.deniedMandatory;
    } else {
      _loadInitialStatus();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // UIUX "Settings redirect + Pending-return": SYSTEM_ALERT_WINDOW khong co callback dong bo -
    // phai tu kiem tra lai khi nguoi dung roi man hinh Settings quay ve app.
    if (state == AppLifecycleState.resumed && _phase == _FlowPhase.settingsPending) {
      _recheckAfterSettingsReturn();
    }
  }

  Future<void> _recheckAfterSettingsReturn() async {
    final status = await _bridge.checkPermissionStatus(_currentStep.permissionKeys.single);
    if (!mounted) return;
    if (status == PermissionStatus.granted) {
      _advanceToNextStepOrSummary();
    } else {
      setState(() => _phase = _FlowPhase.deniedMandatory);
    }
  }

  Future<void> _loadInitialStatus() async {
    setState(() => _phase = _FlowPhase.loading);
    try {
      final snapshot = await OnboardingStatusSnapshot.load(_bridge);
      if (!mounted) return;
      if (snapshot.hasAllMandatoryGranted) {
        widget.onComplete();
        return;
      }
      setState(() => _phase = _FlowPhase.welcome);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _phase = _FlowPhase.error;
        _errorMessage = 'Không kiểm tra được trạng thái quyền.';
      });
    }
  }

  PermissionStepConfig get _currentStep => kOnboardingSteps[_currentStepIndex];

  bool get _isLastStep => _currentStepIndex == kOnboardingSteps.length - 1;

  void _advanceToNextStepOrSummary() {
    if (_isLastStep) {
      setState(() => _phase = _FlowPhase.summary);
    } else {
      setState(() {
        _currentStepIndex += 1;
        _phase = _FlowPhase.rationale;
      });
    }
  }

  Future<bool> _requestCurrentStep() async {
    final step = _currentStep;
    if (step.isRole) {
      return _bridge.requestRole();
    }
    for (final key in step.permissionKeys) {
      final status = await _bridge.requestPermission(key);
      if (status != PermissionStatus.granted) return false;
    }
    return true;
  }

  Future<void> _onGrantPressed() async {
    if (_requestInFlight) return;
    final step = _currentStep;

    if (step.isSettingsRedirect) {
      // Khong co ket qua dong bo - chi mo Settings roi cho resume-check xu ly tiep
      // (_recheckAfterSettingsReturn), KHONG quyet dinh granted/denied ngay tai day.
      setState(() => _requestInFlight = true);
      try {
        await _bridge.requestPermission(step.permissionKeys.single);
      } finally {
        if (mounted) setState(() => _requestInFlight = false);
      }
      if (!mounted) return;
      setState(() => _phase = _FlowPhase.settingsPending);
      return;
    }

    setState(() => _requestInFlight = true);
    try {
      final granted = await _requestCurrentStep();
      if (!mounted) return;
      if (granted) {
        _advanceToNextStepOrSummary();
      } else if (step.mandatory) {
        setState(() => _phase = _FlowPhase.deniedMandatory);
      } else {
        _advanceToNextStepOrSummary();
      }
    } finally {
      if (mounted) setState(() => _requestInFlight = false);
    }
  }

  void _onSkipOptionalPressed() {
    _advanceToNextStepOrSummary();
  }

  void _onContinueAnywayPressed() {
    // AC2: "Tiếp tục, cấp sau" - vao app o trang thai suy giam, khong ep dong app.
    final startedFromResume = widget.startAtDeniedStepFor != null;
    if (startedFromResume) {
      widget.onComplete();
    } else {
      _advanceToNextStepOrSummary();
    }
  }

  @override
  Widget build(BuildContext context) {
    switch (_phase) {
      case _FlowPhase.loading:
        return const _LoadingView();
      case _FlowPhase.error:
        return _ErrorView(
          message: _errorMessage ?? 'Đã xảy ra lỗi.',
          onRetry: _loadInitialStatus,
          onIgnore: () => setState(() => _phase = _FlowPhase.welcome),
        );
      case _FlowPhase.welcome:
        return _WelcomeView(
          onStart: () => setState(() {
            _currentStepIndex = 0;
            _phase = _FlowPhase.rationale;
          }),
        );
      case _FlowPhase.rationale:
        return _RationaleView(
          step: _currentStep,
          requestInFlight: _requestInFlight,
          onGrant: _onGrantPressed,
          onSkip: _currentStep.mandatory ? null : _onSkipOptionalPressed,
        );
      case _FlowPhase.settingsPending:
        return const _SettingsPendingView();
      case _FlowPhase.deniedMandatory:
        return _DeniedMandatoryView(
          step: _currentStep,
          onRetry: _onGrantPressed,
          onContinueAnyway: _onContinueAnywayPressed,
        );
      case _FlowPhase.summary:
        return _SummaryView(bridge: _bridge, onFinish: widget.onComplete);
    }
  }
}

class _LoadingView extends StatelessWidget {
  const _LoadingView();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(child: CircularProgressIndicator()),
    );
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry, required this.onIgnore});

  final String message;
  final VoidCallback onRetry;
  final VoidCallback onIgnore;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(message, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              ElevatedButton(onPressed: onRetry, child: const Text('Thử lại')),
              TextButton(onPressed: onIgnore, child: const Text('Bỏ qua')),
            ],
          ),
        ),
      ),
    );
  }
}

class _WelcomeView extends StatelessWidget {
  const _WelcomeView({required this.onStart});

  final VoidCallback onStart;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'Chào mừng đến SafeCall',
                style: Theme.of(context).textTheme.headlineSmall,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              const Text(
                'SafeCall giúp bạn tự động chặn các cuộc gọi lừa đảo và hỏi xác nhận khi có '
                'số lạ gọi đến.',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 32),
              ElevatedButton(onPressed: onStart, child: const Text('Bắt đầu')),
            ],
          ),
        ),
      ),
    );
  }
}

class _RationaleView extends StatelessWidget {
  const _RationaleView({
    required this.step,
    required this.requestInFlight,
    required this.onGrant,
    required this.onSkip,
  });

  final PermissionStepConfig step;
  final bool requestInFlight;
  final VoidCallback onGrant;
  final VoidCallback? onSkip;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Semantics(
                header: true,
                child: Text(
                  step.title,
                  style: Theme.of(context).textTheme.titleLarge,
                  textAlign: TextAlign.center,
                ),
              ),
              const SizedBox(height: 12),
              Text(step.rationale, textAlign: TextAlign.center),
              const SizedBox(height: 32),
              ElevatedButton(
                onPressed: requestInFlight ? null : onGrant,
                child: requestInFlight
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : Text(step.isSettingsRedirect ? 'Mở Cài đặt' : 'Cấp quyền'),
              ),
              if (onSkip != null)
                TextButton(onPressed: requestInFlight ? null : onSkip, child: const Text('Để sau')),
            ],
          ),
        ),
      ),
    );
  }
}

class _DeniedMandatoryView extends StatelessWidget {
  const _DeniedMandatoryView({
    required this.step,
    required this.onRetry,
    required this.onContinueAnyway,
  });

  final PermissionStepConfig step;
  final VoidCallback onRetry;
  final VoidCallback onContinueAnyway;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '${step.title} chưa được cấp',
                style: Theme.of(context).textTheme.titleLarge,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              Text(
                'SafeCall sẽ KHÔNG hoạt động đầy đủ cho tới khi bạn cấp quyền này.',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 32),
              ElevatedButton(
                onPressed: onRetry,
                child: Text(step.isSettingsRedirect ? 'Mở Cài đặt lại' : 'Thử lại'),
              ),
              TextButton(onPressed: onContinueAnyway, child: const Text('Tiếp tục, cấp sau')),
            ],
          ),
        ),
      ),
    );
  }
}

class _SettingsPendingView extends StatelessWidget {
  const _SettingsPendingView();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const CircularProgressIndicator(),
                const SizedBox(height: 16),
                Semantics(
                  liveRegion: true,
                  child: const Text(
                    'Đang chờ bạn bật quyền trong Cài đặt — quay lại SafeCall sau khi bật xong.',
                    textAlign: TextAlign.center,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SummaryView extends StatelessWidget {
  const _SummaryView({required this.bridge, required this.onFinish});

  final PermissionsBridge bridge;
  final VoidCallback onFinish;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tổng kết quyền')),
      body: FutureBuilder<OnboardingStatusSnapshot>(
        future: OnboardingStatusSnapshot.load(bridge),
        builder: (context, snapshot) {
          if (!snapshot.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final data = snapshot.data!;
          return SafeArea(
            child: Column(
              children: [
                Expanded(
                  child: ListView(
                    children: [
                      _SummaryRow(
                        label: kRoleStep.title,
                        granted: data.roleGranted,
                      ),
                      for (final step in kOnboardingSteps)
                        if (!step.isRole)
                          _SummaryRow(
                            label: step.title,
                            granted: step.permissionKeys.every(data.isGranted),
                          ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: ElevatedButton(onPressed: onFinish, child: const Text('Hoàn tất')),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({required this.label, required this.granted});

  final String label;
  final bool granted;

  @override
  Widget build(BuildContext context) {
    final statusText = granted ? 'Đã cấp' : 'Chưa cấp';
    return ListTile(
      leading: Icon(granted ? Icons.check_circle : Icons.cancel,
          color: granted ? Colors.green : Colors.red),
      title: Text(label),
      trailing: Semantics(label: statusText, child: Text(statusText)),
    );
  }
}
