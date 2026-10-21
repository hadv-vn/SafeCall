package com.safecall.callscreening.db

/**
 * Nem ra khi ghi `blocklist`/`confirmed_safe` loi (khac loi UNIQUE - loi UNIQUE la nhanh binh
 * thuong "da ton tai", khong phai exception nay). LLD bang "Xu ly loi/exception":
 * `resolvePromptAtomically` → ghi blocklist: retry 2 lan, cuoi cung WRITE_FAILED.
 */
class BlocklistWriteException(message: String, cause: Throwable? = null) : Exception(message, cause)
