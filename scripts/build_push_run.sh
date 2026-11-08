#!/usr/bin/env bash
# Build APK Flutter, cai (push) vao emulator/device Android, va chay app SafeCall.
#
# Dung (Git Bash / WSL / Linux / macOS):
#   ./scripts/build_push_run.sh [debug|release] [emulator|device] [avd_name]
#
# Vi du:
#   ./scripts/build_push_run.sh                      # debug, emulator, AVD mac dinh
#   ./scripts/build_push_run.sh debug device          # debug, push len device THAT dang cam/adb connect
#   ./scripts/build_push_run.sh release emulator Pixel_7a_API_34
#
# Tham so 2 tuong thich nguoc: neu khong phai "device"/"emulator", duoc hieu la
# avd_name (hanh vi cu, target mac dinh = emulator).
#
# target=emulator: dam bao co emulator dang chay (khoi dong AVD neu chua co thiet bi
#   nao online, cho boot xong).
# target=device: KHONG tu khoi dong gi - doi hoi da co dung 1 device THAT (khong phai
#   emulator-*) nhan dien duoc qua 'adb devices' (USB da cap quyen debug, hoac da
#   'adb connect' qua Wi-Fi). Neu co nhieu device thi set DEVICE_SERIAL de chon.
#
# Yeu cau: ANDROID_HOME/ANDROID_SDK_ROOT da duoc set (chi can cho target=emulator),
# flutter/adb co trong PATH.

set -uo pipefail

MODE="${1:-debug}"
ARG2="${2:-emulator}"
APP_ID="com.safecall"
BOOT_TIMEOUT_SECS=180
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$REPO_ROOT/logs"

mkdir -p "$LOG_DIR"

log() { printf '[build_push_run] %s\n' "$1"; }
die() { printf '[build_push_run] LOI: %s\n' "$1" >&2; exit 1; }

if [[ "$MODE" != "debug" && "$MODE" != "release" ]]; then
  die "mode phai la 'debug' hoac 'release', nhan duoc: '$MODE'"
fi

if [[ "$ARG2" == "device" || "$ARG2" == "emulator" ]]; then
  TARGET="$ARG2"
  AVD_NAME="${3:-${AVD_NAME:-SafeCall_API34}}"
else
  # Tuong thich nguoc: tham so 2 la avd_name (phong cach cu).
  TARGET="emulator"
  AVD_NAME="$ARG2"
fi

command -v adb >/dev/null 2>&1 || die "Khong tim thay 'adb' trong PATH."
command -v flutter >/dev/null 2>&1 || die "Khong tim thay 'flutter' trong PATH."

SERIAL=""

if [[ "$TARGET" == "device" ]]; then
  # --- 1a. Target = device THAT: khong tu khoi dong gi, chi phat hien ----------------
  mapfile -t REAL_DEVICES < <(adb devices 2>/dev/null | awk '
    NR==1 { next }
    NF==0 { next }
    $1 !~ /^emulator-/ { print $0 }
  ')

  [[ ${#REAL_DEVICES[@]} -gt 0 ]] || die "Khong thay device thuc nao qua 'adb devices'. Cam USB (bat USB debugging + xac nhan popup RSA fingerprint tren may) hoac 'adb connect <ip>:<port>' truoc, roi chay lai."

  NOT_READY=()
  for line in "${REAL_DEVICES[@]}"; do
    state="$(awk '{print $2}' <<<"$line")"
    [[ "$state" == "device" ]] || NOT_READY+=("$line")
  done
  if [[ ${#NOT_READY[@]} -gt 0 ]]; then
    log "Phat hien device chua san sang (unauthorized/offline) - kiem tra popup xac nhan RSA fingerprint tren man hinh may:"
    printf '  %s\n' "${NOT_READY[@]}" >&2
  fi

  if [[ -n "${DEVICE_SERIAL:-}" ]]; then
    SERIAL="$DEVICE_SERIAL"
  elif [[ ${#REAL_DEVICES[@]} -eq 1 ]]; then
    SERIAL="$(awk '{print $1}' <<<"${REAL_DEVICES[0]}")"
  else
    log "Tim thay nhieu device thuc - set DEVICE_SERIAL=<serial> roi chay lai. Danh sach:"
    printf '  %s\n' "${REAL_DEVICES[@]}" >&2
    die "Nhieu device thuc, can chon 1 qua DEVICE_SERIAL."
  fi

  state="$(adb devices 2>/dev/null | awk -v s="$SERIAL" '$1==s{print $2}')"
  [[ "$state" == "device" ]] || die "Device '$SERIAL' dang o trang thai '$state' (can 'device'). Mo khoa man hinh / xac nhan popup USB debugging roi chay lai."

  log "Dung device thuc: $SERIAL"
else
  # --- 1b. Target = emulator: dam bao co 1 emulator dang chay ------------------------
  SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  [[ -n "$SDK_ROOT" ]] || die "Chua set ANDROID_HOME/ANDROID_SDK_ROOT."

  EMULATOR_BIN="$SDK_ROOT/emulator/emulator"
  [[ -x "$EMULATOR_BIN" ]] || EMULATOR_BIN="$SDK_ROOT/emulator/emulator.exe"
  [[ -x "$EMULATOR_BIN" ]] || die "Khong tim thay emulator binary tai $SDK_ROOT/emulator/"

  EMULATOR_LOG="$LOG_DIR/emulator-${AVD_NAME}.log"

  existing_emulator_serial() {
    adb devices 2>/dev/null | awk '/^emulator-/{print $1; exit}'
  }

  SERIAL="$(existing_emulator_serial)"

  if [[ -z "$SERIAL" ]]; then
    "$EMULATOR_BIN" -list-avds 2>/dev/null | grep -qx "$AVD_NAME" \
      || die "AVD '$AVD_NAME' khong ton tai. Chay '$EMULATOR_BIN -list-avds' de xem danh sach."

    log "Chua co emulator nao online - khoi dong AVD '$AVD_NAME' (log: $EMULATOR_LOG)..."
    nohup "$EMULATOR_BIN" -avd "$AVD_NAME" -netdelay none -netspeed full \
      > "$EMULATOR_LOG" 2>&1 &
    disown || true

    log "Cho emulator xuat hien trong 'adb devices'..."
    adb wait-for-device

    SERIAL="$(existing_emulator_serial)"
    [[ -n "$SERIAL" ]] || die "Khong tim thay emulator serial sau wait-for-device."
  else
    log "Da co emulator dang chay ($SERIAL) - bo qua buoc khoi dong."
  fi

  log "Doi bang Android boot xong tren $SERIAL (toi da ${BOOT_TIMEOUT_SECS}s)..."
  elapsed=0
  while true; do
    booted="$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n')"
    [[ "$booted" == "1" ]] && break
    (( elapsed >= BOOT_TIMEOUT_SECS )) && die "Emulator khong boot xong sau ${BOOT_TIMEOUT_SECS}s. Xem log: $EMULATOR_LOG"
    sleep 3
    elapsed=$(( elapsed + 3 ))
  done
  log "Emulator ($SERIAL) da boot xong."
fi

# --- 2. Build APK -----------------------------------------------------------------------
cd "$REPO_ROOT"
log "flutter build apk --${MODE} ..."
flutter build apk --"$MODE" || die "flutter build apk --${MODE} that bai."

if [[ "$MODE" == "debug" ]]; then
  APK_PATH="$REPO_ROOT/build/app/outputs/flutter-apk/app-debug.apk"
else
  APK_PATH="$REPO_ROOT/build/app/outputs/flutter-apk/app-release.apk"
fi
[[ -f "$APK_PATH" ]] || die "Khong tim thay APK sau khi build: $APK_PATH"
log "Build xong: $APK_PATH"

# --- 3. Push (install) APK --------------------------------------------------------------
# Thuc (wake) man hinh TRUOC khi cai: xac nhan thuc te tren device ColorOS (Oppo/Realme) -
# khi man hinh dang Dozing, Android chan truy cap mang nen (Doze network restriction) nen
# buoc xac minh app online cua Play Protect khong goi duoc -> ROM tu choi cai voi loi
# INSTALL_FAILED_VERIFICATION_FAILURE (fail-closed, khong phai fail-open). Man hinh Awake
# thi cai thanh cong on dinh - khong lien quan streaming vs push install nhu tuong luc dau.
log "Thuc man hinh truoc khi cai (tranh Doze chan mang nen luc Play Protect verify)..."
adb -s "$SERIAL" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true

log "adb install --no-streaming -r (push APK vao $SERIAL) ..."
adb -s "$SERIAL" install --no-streaming -r "$APK_PATH" || die "adb install that bai. Neu van gap INSTALL_FAILED_VERIFICATION_FAILURE: mo khoa man hinh thiet bi (giu Awake) roi chay lai - ROM tu choi cai khi man hinh Dozing do Doze chan mang nen luc Play Protect xac minh online."

# --- 4. Chay app (mo man hinh chinh qua intent LAUNCHER) --------------------------------
log "Mo app $APP_ID tren $SERIAL..."
adb -s "$SERIAL" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 \
  || log "Khong tu mo app duoc (khong fatal) - mo tay tren thiet bi neu can."

log "Hoan tat: target=$TARGET, build=$MODE, serial=$SERIAL, apk=$APK_PATH"
