#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cong cu GUI gia lap cuoc goi den de test SafeCall tren Android Emulator (AVD).

Chay:
    python scripts/simulate_incoming_call_gui.py

Yeu cau:
    - Python 3.8+ (tkinter co san trong ban CPython chuan, khong can pip install gi).
    - 'adb' co trong PATH (cung yeu cau nhu scripts/build_push_run.sh).
    - Mot Android Emulator (AVD) dang chay (vi du qua build_push_run.sh truoc).

GIOI HAN QUAN TRONG - CHI HOAT DONG TREN EMULATOR, KHONG HOAT DONG TREN DEVICE THAT:
    Tool nay dung lenh console cua Android Emulator (`adb emu gsm call|accept|cancel`)
    de gia lap tin hieu modem GSM gia - day la co che CHINH THUC cua AVD, cuoc goi gia
    lap nay di qua dung TelephonyManager/CallScreeningService nhu cuoc goi thuc nen
    test duoc chinh xac luong cua SafeCall. Tren DEVICE THAT, Android KHONG cho bat ky
    app/adb nao inject cuoc goi GSM gia vao bang tan modem thuc neu khong root hoac la
    app Dialer/Telephony he thong duoc cap quyen dac biet cua OEM - day la gioi han cua
    nen tang Android, khong phai gioi han cua tool nay. Muon test tren device thuc, chi
    co cach hop le la goi THAT tu mot SIM/so khac (vi du dung chinh may test voi SIM
    phu, hoac dung app SIP/VoIP) toi so may dang test.

Danh sach so test mau: scripts/test_numbers.json (tu sua/them theo nhu cau, cac so nen
bat dau bang '0' hoac '00' de giong dinh dang PhoneNumberNormalizer dang ky doi).
"""

from __future__ import annotations

import json
import re
import subprocess
import threading
import tkinter as tk
from pathlib import Path
from tkinter import messagebox, ttk
from typing import Optional

REPO_ROOT = Path(__file__).resolve().parent.parent
PRESETS_PATH = Path(__file__).resolve().parent / "test_numbers.json"

NUMBER_RE = re.compile(r"^\+?\d{4,16}$")


def adb_devices() -> list[tuple[str, str, bool]]:
    """Tra ve list (serial, state, is_emulator) tu 'adb devices'."""
    try:
        out = subprocess.run(
            ["adb", "devices"], capture_output=True, text=True, timeout=10, check=False
        )
    except FileNotFoundError:
        raise RuntimeError("Khong tim thay 'adb' trong PATH.")
    devices: list[tuple[str, str, bool]] = []
    for line in out.stdout.splitlines()[1:]:
        line = line.strip()
        if not line or line.startswith("*"):
            continue
        parts = line.split()
        if len(parts) < 2:
            continue
        serial, state = parts[0], parts[1]
        devices.append((serial, state, serial.startswith("emulator-")))
    return devices


def adb_emu_gsm(serial: str, action: str, number: str) -> tuple[bool, str]:
    """Goi 'adb -s <serial> emu gsm <action> <number>'. Tra ve (ok, output_hoac_loi)."""
    try:
        result = subprocess.run(
            ["adb", "-s", serial, "emu", "gsm", action, number],
            capture_output=True,
            text=True,
            timeout=10,
            check=False,
        )
    except FileNotFoundError:
        return False, "Khong tim thay 'adb' trong PATH."
    except subprocess.TimeoutExpired:
        return False, "Lenh adb emu het thoi gian cho (timeout)."
    output = (result.stdout or "") + (result.stderr or "")
    ok = result.returncode == 0 and "KO" not in output
    return ok, output.strip() or "(khong co output - co the da thanh cong)"


def load_presets() -> dict:
    if not PRESETS_PATH.exists():
        return {}
    try:
        return json.loads(PRESETS_PATH.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        messagebox.showwarning(
            "Khong doc duoc test_numbers.json", f"Loi: {exc}\nDung danh sach preset rong."
        )
        return {}


class SimulateCallApp(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("SafeCall - Gia lap cuoc goi den (chi Emulator)")
        self.geometry("720x560")

        self.presets = load_presets()
        self.selected_serial: Optional[str] = None
        self.selected_is_emulator = False
        self.stop_batch = threading.Event()
        self.batch_running = False

        self._build_device_section()
        self._build_number_section()
        self._build_batch_section()
        self._build_log_section()

        self.refresh_devices()

    # --- UI: chon device --------------------------------------------------------------
    def _build_device_section(self) -> None:
        frame = ttk.LabelFrame(self, text="1. Chon thiet bi (adb)")
        frame.pack(fill="x", padx=10, pady=(10, 5))

        self.device_combo = ttk.Combobox(frame, state="readonly", width=50)
        self.device_combo.grid(row=0, column=0, padx=5, pady=5, sticky="w")
        self.device_combo.bind("<<ComboboxSelected>>", self._on_device_selected)

        ttk.Button(frame, text="Lam moi danh sach", command=self.refresh_devices).grid(
            row=0, column=1, padx=5, pady=5
        )

        self.device_warning_label = ttk.Label(frame, text="", foreground="red", wraplength=680)
        self.device_warning_label.grid(row=1, column=0, columnspan=2, padx=5, pady=(0, 5), sticky="w")

    def refresh_devices(self) -> None:
        try:
            devices = adb_devices()
        except RuntimeError as exc:
            messagebox.showerror("Loi adb", str(exc))
            return

        self._devices = devices
        labels = []
        for serial, state, is_emu in devices:
            kind = "Emulator" if is_emu else "Device THAT"
            labels.append(f"{serial}  [{state}]  ({kind})")
        self.device_combo["values"] = labels

        if devices:
            self.device_combo.current(0)
            self._on_device_selected()
        else:
            self.device_combo.set("")
            self.selected_serial = None
            self.selected_is_emulator = False
            self.device_warning_label.config(
                text="Khong tim thay thiet bi/emulator nao qua 'adb devices'. "
                "Khoi dong emulator (vi du scripts/build_push_run.sh) roi bam 'Lam moi'."
            )
            self._set_actions_enabled(False)

    def _on_device_selected(self, _event=None) -> None:
        idx = self.device_combo.current()
        if idx < 0 or idx >= len(self._devices):
            return
        serial, state, is_emu = self._devices[idx]
        self.selected_serial = serial
        self.selected_is_emulator = is_emu

        if not is_emu:
            self.device_warning_label.config(
                text="Thiet bi nay la DEVICE THAT - khong the gia lap cuoc goi den bang "
                "adb (gioi han cua Android, khong phai cua tool). Muon test tren may nay, "
                "hay goi THAT toi so may tu mot SIM/so khac."
            )
            self._set_actions_enabled(False)
        elif state != "device":
            self.device_warning_label.config(
                text=f"Emulator dang o trang thai '{state}', can doi boot xong (trang thai 'device')."
            )
            self._set_actions_enabled(False)
        else:
            self.device_warning_label.config(text="")
            self._set_actions_enabled(True)

    def _set_actions_enabled(self, enabled: bool) -> None:
        state = "normal" if enabled else "disabled"
        for widget in (self.btn_call, self.btn_accept, self.btn_cancel, self.btn_run_batch):
            widget.config(state=state)

    # --- UI: nhap so / goi don le ------------------------------------------------------
    def _build_number_section(self) -> None:
        frame = ttk.LabelFrame(self, text="2. Goi don le theo so bat ky")
        frame.pack(fill="x", padx=10, pady=5)

        ttk.Label(frame, text="So dien thoai:").grid(row=0, column=0, padx=5, pady=5, sticky="w")
        self.number_entry = ttk.Entry(frame, width=30)
        self.number_entry.grid(row=0, column=1, padx=5, pady=5, sticky="w")
        self.number_entry.insert(0, "0912345678")

        self.preset_combo = ttk.Combobox(frame, state="readonly", width=55)
        self.preset_combo.grid(row=0, column=2, padx=5, pady=5, sticky="w")
        self._reload_preset_combo()
        self.preset_combo.bind("<<ComboboxSelected>>", self._on_preset_selected)

        btn_frame = ttk.Frame(frame)
        btn_frame.grid(row=1, column=0, columnspan=3, pady=(0, 5))

        self.btn_call = ttk.Button(
            btn_frame, text="Goi den (gsm call)", command=self._on_call_clicked
        )
        self.btn_call.pack(side="left", padx=5)

        self.btn_accept = ttk.Button(
            btn_frame, text="Nghe may (gsm accept)", command=self._on_accept_clicked
        )
        self.btn_accept.pack(side="left", padx=5)

        self.btn_cancel = ttk.Button(
            btn_frame, text="Tu choi / Huy (gsm cancel)", command=self._on_cancel_clicked
        )
        self.btn_cancel.pack(side="left", padx=5)

    def _reload_preset_combo(self) -> None:
        self._preset_flat: list[tuple[str, str]] = []
        for category, items in self.presets.items():
            for item in items:
                label = f"[{category}] {item.get('label', '')} - {item.get('number', '')}"
                self._preset_flat.append((label, item.get("number", "")))
        self.preset_combo["values"] = [label for label, _ in self._preset_flat]

    def _on_preset_selected(self, _event=None) -> None:
        idx = self.preset_combo.current()
        if 0 <= idx < len(self._preset_flat):
            _, number = self._preset_flat[idx]
            self.number_entry.delete(0, tk.END)
            self.number_entry.insert(0, number)

    def _validated_number(self) -> Optional[str]:
        number = self.number_entry.get().strip()
        if not NUMBER_RE.match(number):
            messagebox.showerror(
                "So khong hop le", "So phai la chu so (co the bat dau bang '+'), dai 4-16 ky tu."
            )
            return None
        return number

    def _run_gsm_action(self, action: str, number: str) -> None:
        if not self.selected_serial:
            messagebox.showerror("Chua chon thiet bi", "Hay chon mot emulator truoc.")
            return
        self.log(f"adb -s {self.selected_serial} emu gsm {action} {number} ...")

        def worker() -> None:
            ok, output = adb_emu_gsm(self.selected_serial, action, number)
            prefix = "OK" if ok else "LOI"
            self.after(0, lambda: self.log(f"  -> {prefix}: {output}"))

        threading.Thread(target=worker, daemon=True).start()

    def _on_call_clicked(self) -> None:
        number = self._validated_number()
        if number:
            self._run_gsm_action("call", number)

    def _on_accept_clicked(self) -> None:
        number = self._validated_number()
        if number:
            self._run_gsm_action("accept", number)

    def _on_cancel_clicked(self) -> None:
        number = self._validated_number()
        if number:
            self._run_gsm_action("cancel", number)

    # --- UI: chay bo so test theo batch -------------------------------------------------
    def _build_batch_section(self) -> None:
        frame = ttk.LabelFrame(self, text="3. Chay lan luot bo so test (goi -> cho -> huy)")
        frame.pack(fill="x", padx=10, pady=5)

        ttk.Label(frame, text="Nhom so:").grid(row=0, column=0, padx=5, pady=5, sticky="w")
        self.category_combo = ttk.Combobox(
            frame, state="readonly", width=30, values=list(self.presets.keys())
        )
        self.category_combo.grid(row=0, column=1, padx=5, pady=5, sticky="w")
        if self.presets:
            self.category_combo.current(0)

        ttk.Label(frame, text="Thoi gian do chuong moi so (giay):").grid(
            row=0, column=2, padx=5, pady=5, sticky="w"
        )
        self.ring_seconds_entry = ttk.Entry(frame, width=5)
        self.ring_seconds_entry.insert(0, "5")
        self.ring_seconds_entry.grid(row=0, column=3, padx=5, pady=5, sticky="w")

        self.btn_run_batch = ttk.Button(
            frame, text="Chay lan luot", command=self._on_run_batch_clicked
        )
        self.btn_run_batch.grid(row=1, column=0, padx=5, pady=5, sticky="w")

        self.btn_stop_batch = ttk.Button(
            frame, text="Dung", command=self._on_stop_batch_clicked, state="disabled"
        )
        self.btn_stop_batch.grid(row=1, column=1, padx=5, pady=5, sticky="w")

    def _on_run_batch_clicked(self) -> None:
        if self.batch_running:
            return
        category = self.category_combo.get()
        items = self.presets.get(category, [])
        if not items:
            messagebox.showinfo("Trong", "Nhom nay khong co so nao trong test_numbers.json.")
            return
        try:
            ring_seconds = max(1, int(self.ring_seconds_entry.get().strip()))
        except ValueError:
            messagebox.showerror("Sai gia tri", "Thoi gian do chuong phai la so nguyen.")
            return
        if not self.selected_serial:
            messagebox.showerror("Chua chon thiet bi", "Hay chon mot emulator truoc.")
            return

        self.stop_batch.clear()
        self.batch_running = True
        self.btn_run_batch.config(state="disabled")
        self.btn_stop_batch.config(state="normal")

        def worker() -> None:
            serial = self.selected_serial
            for item in items:
                if self.stop_batch.is_set():
                    break
                number = item.get("number", "")
                label = item.get("label", number)
                self.after(0, lambda label=label, number=number: self.log(f"--- {label} ({number}) ---"))

                ok, output = adb_emu_gsm(serial, "call", number)
                self.after(0, lambda ok=ok, output=output: self.log(f"  call -> {'OK' if ok else 'LOI'}: {output}"))

                if self.stop_batch.wait(timeout=ring_seconds):
                    break

                ok, output = adb_emu_gsm(serial, "cancel", number)
                self.after(0, lambda ok=ok, output=output: self.log(f"  cancel -> {'OK' if ok else 'LOI'}: {output}"))

            self.after(0, self._on_batch_finished)

        threading.Thread(target=worker, daemon=True).start()

    def _on_stop_batch_clicked(self) -> None:
        self.stop_batch.set()

    def _on_batch_finished(self) -> None:
        self.batch_running = False
        self.btn_run_batch.config(state="normal")
        self.btn_stop_batch.config(state="disabled")
        self.log("=== Da chay xong bo so test (hoac bi dung) ===")

    # --- UI: log panel -------------------------------------------------------------------
    def _build_log_section(self) -> None:
        frame = ttk.LabelFrame(self, text="Log")
        frame.pack(fill="both", expand=True, padx=10, pady=(5, 10))

        self.log_text = tk.Text(frame, height=15, state="disabled", wrap="word")
        self.log_text.pack(fill="both", expand=True, padx=5, pady=5)

    def log(self, message: str) -> None:
        self.log_text.config(state="normal")
        self.log_text.insert(tk.END, message + "\n")
        self.log_text.see(tk.END)
        self.log_text.config(state="disabled")


if __name__ == "__main__":
    app = SimulateCallApp()
    app.mainloop()
