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

    /** 读取已保存的账户 */
    fun load(): XboxAccount? {
        val raw = preferences.xboxAccount
        if (raw.isBlank()) return null
        return try {
            XboxAccount.fromJson(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }

    /** 保存账户 */
    fun save(account: XboxAccount) {
        preferences.xboxAccount = account.toJson().toString()
    }

    /** 清除账户 */
    fun clear() {
        preferences.xboxAccount = ""
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
                throw IllegalStateException(json.optString("XErr", "xbl_failed_${resp.code}"))
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
                throw IllegalStateException(json.optString("XErr", "xsts_failed_${resp.code}"))
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