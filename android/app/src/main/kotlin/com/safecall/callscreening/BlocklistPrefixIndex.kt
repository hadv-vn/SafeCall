package com.safecall.callscreening

import com.safecall.callscreening.db.BlocklistRepository

private class TrieNode {
    val children = HashMap<Char, TrieNode>()
    var isTerminal = false
    var value: String? = null
}

/**
 * `BlocklistPrefixIndex` (LLD, MOI - v1.3/FR-9) - cau truc tra cuu prefix hieu qua (trie trong bo
 * nho), tranh so khop tuyen tinh tren hot-path ~1500ms cua `CallScreeningService.onScreenCall`.
 *
 * QUYET DINH CAU TRUC (LLD da ly giai, KHONG dung sorted-list + "tim phan tu lon nhat <= X"): cach
 * do SAI logic trong truong hop chung - vd tap {"12", "1229"}, so goi den "123456": phan tu lon
 * nhat <= X la "1229" (khong phai prefix thuc), nhung "12" MOI la prefix thuc. Trie traversal
 * theo tung ky tu, dung o node terminal SAU CUNG gap duoc, luon dung, O(do dai so goi den).
 *
 * Singleton theo PROCESS (object, giong `PendingPromptRegistry`) - song doc lap voi vong doi
 * `CallScreeningService` (co the bi OS tao/huy lai nhieu lan), lam moi an toan CROSS-PROCESS qua
 * version-counter SQLite (`blocklist_meta.prefix_version`) - KHONG cay callback in-process tu
 * Dart, vi `CallScreeningService` CO THE chay process rieng voi Flutter engine (gia dinh LLD, chua
 * duoc `system-architect` xac nhan cau hinh manifest thuc - hien tai Manifest CHUA khai bao
 * `android:process` rieng cho bat ky thanh phan nao, nen trong trien khai nay thuc te la CUNG
 * process; thiet ke van dung ca 2 truong hop).
 */
object BlocklistPrefixIndex {
    @Volatile
    private var root = TrieNode()

    @Volatile
    private var loadedVersion: Long = -1L

    private val rebuildLock = Any()

    /** Goi tai MOI lan `onScreenCall` TRUOC khi `matchLongestPrefix` - point-read version, hiem khi rebuild. */
    fun ensureFresh(repository: BlocklistRepository) {
        val currentVersion = repository.getPrefixVersion()
        if (currentVersion == loadedVersion) return
        synchronized(rebuildLock) {
            if (currentVersion == loadedVersion) return // double-check, trong 2 thread dong thoi
            rebuild(repository.getAllPrefixValues())
            loadedVersion = currentVersion
        }
    }

    /** Tra ve dau so THUC SU khop DAI NHAT, hoac null. Goi SAU [ensureFresh]. */
    fun matchLongestPrefix(normalizedNumber: String): String? {
        var node = root
        var longestMatch: String? = null
        for (ch in normalizedNumber) {
            node = node.children[ch] ?: break
            if (node.isTerminal) longestMatch = node.value
        }
        return longestMatch
    }

    private fun rebuild(allPrefixes: List<String>) {
        val newRoot = TrieNode()
        for (prefix in allPrefixes) {
            var node = newRoot
            for (ch in prefix) {
                node = node.children.getOrPut(ch) { TrieNode() }
            }
            node.isTerminal = true
            node.value = prefix
        }
        root = newRoot
    }
}
