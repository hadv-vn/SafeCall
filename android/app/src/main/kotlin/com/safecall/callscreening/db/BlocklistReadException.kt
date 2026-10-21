package com.safecall.callscreening.db

/**
 * Nem ra khi doc bang `blocklist` loi (timeout do busy_timeout, hoac loi SQLite khac) -
 * `CallScreeningService.onScreenCall` PHAI bat exception nay va fail-open (xem LLD bang
 * "Xu ly loi/exception": `disallowCall=false`).
 */
class BlocklistReadException(message: String, cause: Throwable? = null) : Exception(message, cause)
