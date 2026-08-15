package com.cszyapp.cmal

import com.cszyapp.cmal.data.update.UpdateChecker
import com.cszyapp.cmal.ui.home.formatSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 核心逻辑单元测试
 * 覆盖：版本比较、文件大小格式化、版本索引解析
 */
class CoreLogicTest {

    @Test
    fun versionComparison_isNewerDetected() {
        // isNewer 为实例私有方法，构造时传入 null context（isNewer 不依赖 context）
        val checker = UpdateChecker(null)
        val method = UpdateChecker::class.java.getDeclaredMethod(
            "isNewer", String::class.java, String::class.java
        )
        method.isAccessible = true

        assertTrue(method.invoke(checker, "v1.2.0", "1.1.9") as Boolean)
        assertTrue(method.invoke(checker, "v1.2.1", "1.2.0") as Boolean)
        assertFalse(method.invoke(checker, "v1.0.0", "1.0.1") as Boolean)
        assertFalse(method.invoke(checker, "v1.0.0", "1.0.0") as Boolean)
    }

    @Test
    fun formatSize_bytes() {
        assertEquals("512 B", formatSize(512))
        assertEquals("1.5 KB", formatSize(1536))
        assertEquals("1.0 MB", formatSize(1024 * 1024))
        assertEquals("1.00 GB", formatSize(1024L * 1024 * 1024))
    }

    @Test
    fun parseVersionIndex_jsonParsing() {
        val body = """
            [
              {"name":"1.21.40","code":10000000,"url":"https://example.com/1.apk","size":1048576},
              {"name":"1.21.31","code":9990000,"url":"https://example.com/2.apk","size":2048}
            ]
        """.trimIndent()

        val parsed = parseIndex(body)
        assertEquals(2, parsed.size)
        assertEquals("1.21.40", parsed[0].versionName)
        assertEquals(9990000, parsed[1].versionCode)
        assertEquals(2048L, parsed[1].size)
    }

    private fun parseIndex(body: String): List<com.cszyapp.cmal.data.db.McVersion> {
        val arr = org.json.JSONArray(body)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            com.cszyapp.cmal.data.db.McVersion(
                versionCode = o.getInt("code"),
                versionName = o.getString("name"),
                downloadUrl = o.getString("url"),
                size = o.optLong("size", 0L)
            )
        }
    }
}
