import 'package:flutter/material.dart';

import 'blocklist/blocklist_screen.dart';
import 'call_events/scam_confirmation_dialog.dart';
import 'onboarding/onboarding_flow_screen.dart';
import 'onboarding/onboarding_status.dart';
import 'onboarding/permission_step_config.dart';
import 'permissions/permissions_bridge.dart';

void main() {
  runApp(const SafeCallApp());
}

class SafeCallApp extends StatelessWidget {
  const SafeCallApp({super.key});

  static final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SafeCall',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.indigo),
      navigatorKey: navigatorKey,
      home: const RootDecider(),
      // ScamPromptListener dung navigatorKey.currentContext (KHONG phai context cua chinh no) de
      // hien dialog - context cua builder() nam PHIA TREN Navigator ma MaterialApp tu tao, goi
      // showDialog truc tiep voi context do se khong tim thay Navigator.
      builder: (context, child) => ScamPromptListener(
        navigatorKey: navigatorKey,
        child: child ?? const SizedBox.shrink(),
      ),
    );
  }
}

/// Quyet dinh hien Onboarding hay Home, va thuc hien resume-check (AC4): moi lan app vao
/// foreground TRONG LUC dang o Home, kiem tra lai quyen bat buoc - neu bi thu hoi, dua ve dung
/// Denied-mandatory step tuong ung (khong lap lai toan bo onboarding).
class RootDecider extends StatefulWidget {
  const RootDecider({super.key});

  @override
  State<RootDecider> createState() => _RootDeciderState();
}

class _RootDeciderState extends State<RootDecider> with WidgetsBindingObserver {
  final PermissionsBridge _bridge = PermissionsBridge();
  bool _onHome = false;
  PermissionStepConfig? _resumeDeniedStep;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _onHome) {
      _runResumeCheck();
    }
  }

  Future<void> _runResumeCheck() async {
    final snapshot = await OnboardingStatusSnapshot.load(_bridge);
    final revoked = snapshot.firstRevokedMandatoryStep();
    if (!mounted) return;
    if (revoked != null) {
      setState(() {
        _onHome = false;
        _resumeDeniedStep = revoked;
      });
    }
  }

  void _goHome() {
    setState(() {
      _onHome = true;
      _resumeDeniedStep = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_onHome) {
      // AC3: da cap du quyen bat buoc -> vao thang BlocklistScreen (man hinh chinh, Phase 2).
      return BlocklistScreen(permissionsBridge: _bridge);
    }
    return OnboardingFlowScreen(
      onComplete: _goHome,
      startAtDeniedStepFor: _resumeDeniedStep,
    );
  }
}
