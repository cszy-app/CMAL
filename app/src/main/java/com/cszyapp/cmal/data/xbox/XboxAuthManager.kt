package com.cszyapp.cmal.data.xbox

import com.cszyapp.cmal.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Xbox 账户（登录成功后保存）
 */
data class XboxAccount(
    val gamertag: String,
    val xuid: String,
    val uhs: String,
    val xstsToken: String,
    val refreshToken: String,
    val expiresAt: Long
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("gamertag", gamertag)
            .put("xuid", xuid)
            .put("uhs", uhs)
            .put("xstsToken", xstsToken)
            .put("refreshToken", refreshToken)
            .put("expiresAt", expiresAt)

    companion object {
        fun fromJson(obj: JSONObject): XboxAccount =
            XboxAccount(
                gamertag = obj.optString("gamertag"),
                xuid = obj.optString("xuid"),
                uhs = obj.optString("uhs"),
                xstsToken = obj.optString("xstsToken"),
                refreshToken = obj.optString("refreshToken"),
                expiresAt = obj.optLong("expiresAt", 0L)
            )
    }
}

/**
 * 设备码登录信息（需要用户在浏览器确认）
 */
data class DeviceCodeInfo(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Long,
    val interval: Long
)

/**
 * Xbox 登录管理器（Microsoft OAuth 设备码流）
 *
 * 流程：
 * 1. 请求 devicecode（展示给用户的 code + microsoft.com/link 链接）
 * 2. 用户确认后轮询换取 access_token
 * 3. 用 access_token 换 Xbox Live (XBL) 令牌
 * 4. 用 XBL 令牌换 XSTS 令牌（包含玩家代号）
 */
class XboxAuthManager(private val preferences: Preferences) {

    companion object {
        // 需要在 Azure 门户注册应用后替换为你的 Client ID
        const val CLIENT_ID = "YOUR_AZURE_CLIENT_ID"
        const val SCOPE = "XboxLive.signin offline_access"

        const val DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
        const val TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
        const val XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
        const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** 读取已保存的账户（当前激活的） */
    fun load(): XboxAccount? {
        val raw = preferences.xboxAccount
        if (raw.isBlank()) return null
        return try {
            XboxAccount.fromJson(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }

    /** 保存账户（加入多账户列表，并设为激活） */
    fun save(account: XboxAccount) {
        preferences.xboxAccount = account.toJson().toString()
        val list = allAccounts().toMutableList()
        list.removeAll { it.xuid == account.xuid }
        list.add(0, account)
        preferences.xboxAccounts = JSONArray().apply {
            list.forEach { put(JSONObject(it.toJson().toString())) }
        }.toString()
        preferences.activeXuid = account.xuid
    }

    /** 全部已保存账户 */
    fun allAccounts(): List<XboxAccount> {
        val raw = preferences.xboxAccounts
        if (raw.isBlank() || raw == "[]") {
            // 兼容旧版本单账户数据
            val single = load()
            return if (single != null) listOf(single) else emptyList()
        }
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { XboxAccount.fromJson(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 切换激活账户 */
    fun switchTo(xuid: String): XboxAccount? {
        val acc = allAccounts().firstOrNull { it.xuid == xuid } ?: return null
        preferences.xboxAccount = acc.toJson().toString()
        preferences.activeXuid = xuid
        return acc
    }

    /** 移除账户 */
    fun removeAccount(xuid: String) {
        val list = allAccounts().filterNot { it.xuid == xuid }
        preferences.xboxAccounts = JSONArray().apply {
            list.forEach { put(JSONObject(it.toJson().toString())) }
        }.toString()
        if (preferences.activeXuid == xuid) {
            val next = list.firstOrNull()
            preferences.activeXuid = next?.xuid ?: ""
            preferences.xboxAccount = next?.toJson()?.toString() ?: ""
        }
    }

    /** 清除全部账户 */
    fun clear() {
        preferences.xboxAccount = ""
        preferences.xboxAccounts = "[]"
        preferences.activeXuid = ""
    }

    /** 是否已登录（且未过期） */
    fun isLoggedIn(): Boolean {
        val acc = load() ?: return false
        return acc.expiresAt > System.currentTimeMillis()
    }

    /**
     * 第 1 步：请求设备码
     */
    suspend fun requestDeviceCode(): DeviceCodeInfo = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("scope", SCOPE)
            .build()
        val request = Request.Builder()
            .url(DEVICE_CODE_URL)
            .post(form)
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: throw IllegalStateException("http_${resp.code}")
            val json = JSONObject(body)
            if (!resp.isSuccessful) {
                throw IllegalStateException(json.optString("error_description", "http_${resp.code}"))
            }
            DeviceCodeInfo(
                deviceCode = json.getString("device_code"),
                userCode = json.getString("user_code"),
                verificationUri = json.getString("verification_uri"),
                expiresIn = json.optLong("expires_in", 900),
                interval = json.optLong("interval", 5)
            )
        }
    }

    /**
     * 第 2 步：轮询等待用户确认，返回 access_token + refresh_token
     */
    suspend fun pollForToken(info: DeviceCodeInfo): Pair<String, String> = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + info.expiresIn * 1000
        while (System.currentTimeMillis() < deadline) {
            delay(info.interval * 1000)
            val form = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .add("device_code", info.deviceCode)
                .build()
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(form)
                .build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                if (resp.isSuccessful && json.has("access_token")) {
                    return@withContext json.getString("access_token") to json.optString("refresh_token", "")
                }
                val error = json.optString("error")
                when (error) {
                    "authorization_pending", "slow_down" -> { /* 继续等待 */ }
                    else -> throw IllegalStateException(json.optString("error_description", error.ifEmpty { "http_${resp.code}" }))
                }
            }
        }
        throw IllegalStateException("device_code_expired")
    }

    /**
     * 第 3+4 步：access_token -> XBL -> XSTS，得到玩家信息
     */
    suspend fun completeLogin(accessToken: String): XboxAccount = withContext(Dispatchers.IO) {
        val xblToken = exchangeToXbl(accessToken)
        val (xstsToken, uhs, gamertag, xuid) = exchangeToXsts(xblToken)
        XboxAccount(
            gamertag = gamertag,
            xuid = xuid,
            uhs = uhs,
            xstsToken = xstsToken,
            refreshToken = "",
            expiresAt = System.currentTimeMillis() + 3600 * 1000
        )
    }

    private fun exchangeToXbl(accessToken: String): String {
        val payload = JSONObject()
            .put("Properties", JSONObject()
                .put("AuthMethod", "RPS")
                .put("SiteName", "user.auth.xboxlive.com")
                .put("RpsTicket", "d=$accessToken"))
            .put("RelyingParty", "http://auth.xboxlive.com")
            .put("TokenType", "JWT")
        val request = Request.Builder()
            .url(XBL_AUTH_URL)
            .post(payload.toString().toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: throw IllegalStateException("http_${resp.code}")
            val json = JSONObject(body)
            if (!resp.isSuccessful || !json.has("Token")) {
                throw XboxAuthException(
                    json.optString("XErr", "xbl_failed_${resp.code}"),
                    "XBL"
                )
            }
            return json.getString("Token")
        }
    }

    private fun exchangeToXsts(xblToken: String): TokenInfo {
        val payload = JSONObject()
            .put("Properties", JSONObject()
                .put("SandboxId", "RETAIL")
                .put("UserTokens", JSONArray().put(xblToken)))
            .put("RelyingParty", "rp://xboxlive.com")
            .put("TokenType", "JWT")
        val request = Request.Builder()
            .url(XSTS_AUTH_URL)
            .post(payload.toString().toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: throw IllegalStateException("http_${resp.code}")
            val json = JSONObject(body)
            if (!resp.isSuccessful || !json.has("Token")) {
                throw XboxAuthException(
                    json.optString("XErr", "xsts_failed_${resp.code}"),
                    "XSTS"
                )
            }
            val token = json.getString("Token")
            val xui = json.optJSONObject("DisplayClaims")?.optJSONArray("xui")?.optJSONObject(0)
            val uhs = xui?.optString("uhs", "") ?: ""
            val gamertag = xui?.optString("gtg", "") ?: ""
            val xuid = xui?.optString("xid", "") ?: ""
            return TokenInfo(token, uhs, gamertag, xuid)
        }
    }

    private data class TokenInfo(
        val token: String,
        val uhs: String,
        val gamertag: String,
        val xuid: String
    )
}

/**
 * Xbox 登录异常，携带 XErr 错误码
 */
class XboxAuthException(val errorCode: String, val stage: String) :
    IllegalStateException("Xbox auth failed ($stage): $errorCode") {

    companion object {
        /** 已知 XErr 错误码 -> 说明 */
        private val KNOWN_CODES = mapOf(
            // 0x8015DCxx 系列（MSA→Xbox）
            "2148916224" to "请求失败，请检查网络连接（0x8015DC00）",
            "2148916225" to "Xbox 登录被拒绝，请稍后重试（0x8015DC01）",
            "2148916226" to "该账号无有效的 Microsoft 凭据（0x8015DC02）",
            "2148916227" to "该账号为未成年/家庭账号，需要家长授权（0x8015DC03）",
            "2148916228" to "该账号的 Xbox 服务地区不可用（0x8015DC04）",
            "2148916229" to "Xbox 服务需要身份验证（0x8015DC05）",
            "2148916230" to "Xbox 服务需要更多身份验证（0x8015DC06）",
            "2148916231" to "账户设置或数据有问题（0x8015DC07）",
            "2148916232" to "访问被拒绝，请联系 Xbox 支持（0x8015DC08）",
            "2148916233" to "Xbox 服务暂时不可用，请稍后重试（0x8015DC09）",
            "2148916234" to "需要登录 Microsoft 账户（0x8015DC0A）",
            "2148916235" to "该账号无法在您所在地区使用（0x8015DC0B）",
            "2148916236" to "账号不存在或已被禁用（0x8015DC0C）",
            "2148916237" to "请检查账户的出生日期设置（0x8015DC0D）",
            "2148916238" to "该账号的 Xbox 档案不可用（0x8015DC0E）",
            "2148916239" to "该账号的 Xbox 档案权限受限（0x8015DC0F）",
            "2148916240" to "Xbox 服务出现意外错误（0x8015DC10）",
            // XSTS 0x8923xxxx 系列
            "2147957412" to "账号因违反协议被限制（0x89235000）",
            "2147957413" to "Xbox 账号已被封禁（0x89235001）",
            "2147957414" to "Xbox 账号被永久封禁（0x89235002）",
            "2147957415" to "账号被限制，需要登录 Xbox 查看详情（0x89235003）",
            "2147957416" to "需要验证账户信息（0x89235004）",
            "2147957417" to "账号地区与服务器不符，请更改地区（0x89235005）",
            "2147957418" to "账号存在风险，已被临时限制（0x89235006）",
            "2147957419" to "Xbox 服务出现技术问题（0x89235007）",
            "2147957420" to "XSTS 令牌无效或过期，请重新登录（0x89235008）",
            "2147957421" to "该账号的 Xbox 服务未激活（0x89235009）",
            "2147957422" to "账号服务不可用，请检查账户状态（0x8923500A）",
            "2147957423" to "需要家长同意才能继续（0x8923500B）",
            "2147957424" to "账号数据读取失败（0x8923500C）",
            "2147957425" to "请求被服务器拒绝（0x8923500D）",
            "2147957426" to "该账号已被禁止使用 Xbox 服务（0x8923500E）",
            "2147957427" to "账号因多次异常行为被限制（0x8923500F）"
        )

        fun describe(code: String, fallback: String = code): String =
            KNOWN_CODES[code] ?: fallback
    }
}