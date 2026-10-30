import 'package:flutter/material.dart';

/// Theme "Solarpunk Futuristic" — đổi từ "Cyberpunk Futuristic" theo yêu cầu 2026-10-28 (xem
/// `docs/design/UIUX-SafeCall-Phase1.md` v1.6). Nền sáng ấm (giấy/nắng), accent xanh lá + vàng
/// nắng, bo góc lớn kiểu hữu cơ, bóng mềm thay vì glow neon — đối lập có chủ đích với v1.5.
class AppColors {
  AppColors._();

  static const Color background = Color(0xFFF6F3E7); // kem/am, nhu giay duoi nang
  static const Color surface = Color(0xFFFFFDF7); // card/app bar, trang am
  static const Color surfaceMuted = Color(0xFFECE6D1); // input fill, cat am
  static const Color primary = Color(0xFF2F9E44); // xanh la
  static const Color secondary = Color(0xFFF2B705); // vang nang (solar)
  static const Color textPrimary = Color(0xFF223324); // xanh la dam, gan den
  static const Color textSecondary = Color(0xFF6B7A63); // xam-xanh la nhat
  static const Color border = Color(0xFFDCD5BA); // vien cat am
  static const Color danger = Color(0xFFD9480F); // dat nung/terracotta - thay cho do gat
  static const Color warning = Color(0xFFE8871E); // hoang hon/amber
  static const Color success = Color(0xFF2F9E44); // trung voi primary - "thanh cong = xanh la"

  /// Mau chu/icon dung TREN be mat accent dac (nut gradient, selected segment...) - rieng vi moi
  /// theme can mot mau tuong phan KHAC NHAU tuy do sang/toi cua background (bai hoc tu lan doi
  /// theme truoc: ban Cyberpunk dung AppColors.background lam mau chu tren nut - hop ly vi nen
  /// toi gan den, nhung se SAI neu dung lai y nguyen cho theme nen sang nhu theme nay).
  static const Color onAccent = Color(0xFF162415);
}

class AppGradients {
  AppGradients._();

  static const LinearGradient primary = LinearGradient(
    colors: [AppColors.primary, AppColors.secondary],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  /// Bong mem "duoi nang" quanh CTA chinh va duoi AppBar - thay cho hieu ung glow neon nhieu lop
  /// cua ban Cyberpunk; o day chi can 1 lop bong nhe, am, khong "phat sang".
  static List<BoxShadow> glow({double opacity = 0.18, double blurRadius = 16}) => [
        BoxShadow(
          color: AppColors.primary.withValues(alpha: opacity),
          blurRadius: blurRadius,
          spreadRadius: -6,
          offset: const Offset(0, 6),
        ),
      ];
}

class AppTheme {
  AppTheme._();

  static ThemeData get solarpunk {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      brightness: Brightness.light,
    ).copyWith(
      primary: AppColors.primary,
      secondary: AppColors.secondary,
      surface: AppColors.surface,
      error: AppColors.danger,
      onPrimary: AppColors.onAccent,
      onSecondary: AppColors.onAccent,
      onSurface: AppColors.textPrimary,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.background,
      fontFamily: 'Roboto',
      textTheme: ThemeData.light().textTheme.apply(
            bodyColor: AppColors.textPrimary,
            displayColor: AppColors.textPrimary,
          ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.surface,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          color: AppColors.primary,
          fontSize: 20,
          fontWeight: FontWeight.w700,
          letterSpacing: 0.2,
        ),
      ),
      cardTheme: CardThemeData(
        color: AppColors.surface,
        elevation: 0,
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(22),
          side: const BorderSide(color: AppColors.border),
        ),
      ),
      listTileTheme: const ListTileThemeData(
        iconColor: AppColors.primary,
        textColor: AppColors.textPrimary,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppColors.surfaceMuted,
        labelStyle: const TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w600),
        side: const BorderSide(color: AppColors.border),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.surfaceMuted,
        hintStyle: const TextStyle(color: AppColors.textSecondary),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.danger, width: 1.2),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: AppColors.onAccent,
          disabledBackgroundColor: AppColors.primary.withValues(alpha: 0.35),
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          textStyle: const TextStyle(fontWeight: FontWeight.w700, letterSpacing: 0.2),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.primary,
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
      segmentedButtonTheme: SegmentedButtonThemeData(
        style: SegmentedButton.styleFrom(
          backgroundColor: AppColors.surfaceMuted,
          selectedBackgroundColor: AppColors.primary,
          selectedForegroundColor: AppColors.onAccent,
          foregroundColor: AppColors.textSecondary,
          side: const BorderSide(color: AppColors.border),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: AppColors.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
          side: const BorderSide(color: AppColors.border),
        ),
        titleTextStyle: const TextStyle(
          color: AppColors.textPrimary,
          fontSize: 18,
          fontWeight: FontWeight.w700,
        ),
        contentTextStyle: const TextStyle(color: AppColors.textSecondary, fontSize: 14),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppColors.textPrimary,
        contentTextStyle: const TextStyle(color: AppColors.background),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        behavior: SnackBarBehavior.floating,
      ),
      dividerColor: AppColors.border,
      iconTheme: const IconThemeData(color: AppColors.primary),
    );
  }
}
