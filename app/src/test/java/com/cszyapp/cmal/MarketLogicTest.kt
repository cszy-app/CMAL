package com.cszyapp.cmal

import com.cszyapp.cmal.data.download.DownloadManager
import com.cszyapp.cmal.data.xbox.XboxAuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 资源市场 / 错误码 单元测试
 */
class MarketLogicTest {

    @Test
    fun downloadManager_sizeString_formats() {
        assertEquals("", DownloadManager.sizeString(0))
        assertEquals("512 B", DownloadManager.sizeString(512))
        assertEquals("1.0 MB", DownloadManager.sizeString(1024 * 1024))
        assertEquals("1.0 GB", DownloadManager.sizeString(1024L * 1024 * 1024))
    }

    @Test
    fun xboxAuthException_describesKnownCodes() {
        // 0x8015DC09 服务不可用：已知码返回描述（含十六进制对照）
        assertTrue(XboxAuthException.describe("2148916233", "unknown").contains("0x8015DC09"))
        // 未知名错误码回退到 fallback
        assertEquals("unknown", XboxAuthException.describe("9999999999", "unknown"))
        // 0x89235001 封禁
        assertTrue(XboxAuthException.describe("2147957413", "unknown").contains("0x89235001"))
    }

    @Test
    fun downloadManager_buildFileName_sanitizes() {
        val name = DownloadManager.buildFileName("My: Cool/Mod <Test>", "mod")
        // 非法字符全部转成下划线，不含 : / < >
        assertTrue(!name.contains(':') && !name.contains('/') && !name.contains('<') && !name.contains('>'))
        assertTrue(name.startsWith("My_"))
        assertTrue(name.endsWith(".mcpack"))
    }

    @Test
    fun downloadManager_buildFileName_worldExtension() {
        val name = DownloadManager.buildFileName("My World", "world")
        assertTrue(name.endsWith(".mcworld"))
    }
}