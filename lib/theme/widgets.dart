import 'package:flutter/material.dart';

import 'app_theme.dart';

/// AppBar nen sang (AppColors.surface) voi vien duoi xanh la + bong mem "duoi nang" - thay the
/// AppBar mac dinh o moi man hinh chinh (BlocklistScreen/CallHistoryScreen/OnboardingFlowScreen
/// Summary), dong bo voi `AppTheme.solarpunk.appBarTheme` (theme "Solarpunk Futuristic").
class GradientAppBar extends StatelessWidget implements PreferredSizeWidget {
  const GradientAppBar({super.key, required this.title, this.actions});

  final String title;
  final List<Widget>? actions;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        border: const Border(bottom: BorderSide(color: AppColors.primary, width: 1)),
        boxShadow: AppGradients.glow(opacity: 0.22, blurRadius: 14),
      ),
      child: AppBar(
        title: Text(title),
        actions: actions,
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
    );
  }
}

/// Nut CTA chinh - nen gradient xanh la->vang nang + bong mem, dac trung "Solarpunk Futuristic"
/// - thay ElevatedButton mac dinh o cac hanh dong chinh (Bat dau, Cap quyen, Them, Hoan tat).
class GradientButton extends StatelessWidget {
  const GradientButton({
    super.key,
    required this.onPressed,
    required this.child,
    this.minimumSize,
  });

  final VoidCallback? onPressed;
  final Widget child;
  final Size? minimumSize;

  @override
  Widget build(BuildContext context) {
    final disabled = onPressed == null;
    return Opacity(
      opacity: disabled ? 0.4 : 1,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          gradient: AppGradients.primary,
          boxShadow: disabled ? null : AppGradients.glow(),
        ),
        child: Material(
          color: Colors.transparent,
          borderRadius: BorderRadius.circular(16),
          child: InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: onPressed,
            child: Container(
              constraints: BoxConstraints(minHeight: minimumSize?.height ?? 48),
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
              alignment: Alignment.center,
              child: DefaultTextStyle(
                style: const TextStyle(
                  color: AppColors.onAccent,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 0.4,
                ),
                child: IconTheme(
                  data: const IconThemeData(color: AppColors.onAccent),
                  child: child,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
