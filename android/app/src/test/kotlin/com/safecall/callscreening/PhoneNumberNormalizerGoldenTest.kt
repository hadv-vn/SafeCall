package com.safecall.callscreening

import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Test

/**
 * Golden-file test doi chieu voi ban Dart
 * (test/blocklist/phone_number_normalizer_golden_test.dart) - dong Risk R9
 * (docs/governance/RISK-SAFECALL.md). Ca 2 phia doc CUNG 1 fixture
 * (test/fixtures/phone_normalizer_golden.json, mirror thu cong tai resources/ cua module nay).
 */
class PhoneNumberNormalizerGoldenTest {

    private val normalizer = PhoneNumberNormalizer()

    @Test
    fun goldenCases() {
        val stream = this::class.java.classLoader.getResourceAsStream("phone_normalizer_golden.json")
            ?: error("Khong tim thay fixture phone_normalizer_golden.json tren classpath test")
        val json = JSONObject(stream.bufferedReader().use { it.readText() })
        val cases = json.getJSONArray("cases")

        val failures = mutableListOf<String>()
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val id = c.getString("id")
            val raw = c.getString("raw")
            val entryType = if (c.getString("entryType") == "PREFIX") EntryType.PREFIX else EntryType.EXACT
            val expectThrows = c.optBoolean("expectThrows", false)

            try {
                val actual = normalizer.normalize(raw, entryType)
                if (expectThrows) {
                    failures += "$id: ky vong throw nhung ra ket qua '$actual'"
                } else {
                    val expected = c.getString("expect")
                    if (actual != expected) {
                        failures += "$id: ky vong '$expected' nhung ra '$actual'"
                    }
                }
            } catch (e: InvalidPhoneNumberFormatException) {
                if (!expectThrows) {
                    failures += "$id: khong ky vong throw nhung bi throw '${e.message}'"
                }
            }
        }

        if (failures.isNotEmpty()) {
            fail("Golden-file mismatch (${failures.size}/${cases.length()}):\n" + failures.joinToString("\n"))
        }
    }
}
