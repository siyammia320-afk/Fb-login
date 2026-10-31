package com.example.network

import android.content.Context
import com.example.data.AccountItem
import com.example.util.SecurityGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.Proxy
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class LoginResult {
    data class Success(val account: AccountItem) : LoginResult()
    data class Failure(val status: String, val message: String) : LoginResult()
}

class WBloksLoginRepository(private val context: Context) {

    private fun createHttpClient(cookieStore: MutableMap<String, String>): OkHttpClient {
        return OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY) // Block any HTTP/HTTPS proxy interception (HttpCanary, Reqable, etc.)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    for (cookie in cookies) {
                        if (cookie.value.isNotEmpty() && cookie.value != "deleted") {
                            cookieStore[cookie.name] = cookie.value
                        }
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val list = mutableListOf<Cookie>()
                    for ((k, v) in cookieStore) {
                        Cookie.Builder()
                            .domain("facebook.com")
                            .path("/")
                            .name(k)
                            .value(v)
                            .build()
                            .let { list.add(it) }
                    }
                    return list
                }
            })
            .build()
    }

    private fun buildExactParamsJson(phone: String, formattedPassword: String): String {
        val serverParams = JSONObject().apply {
            put("credential_type", "password")
            put("username_text_input_id", "s7hi9y:57")
            put("password_text_input_id", "s7hi9y:58")
            put("login_source", "Login")
            put("login_credential_type", "none")
            put("server_login_source", "login")
            put("ar_event_source", "login_home_page")
            put("should_trigger_override_login_success_action", 0)
            put("should_trigger_override_login_2fa_action", 0)
            put("is_caa_perf_enabled", 0)
            put("reg_flow_source", "login_home_native_integration_point")
            put("caller", "gslr")
            put("is_from_landing_page", 0)
            put("is_from_empty_password", 0)
            put("is_from_aymh", 0)
            put("is_from_password_entry_page", 0)
            put("is_from_assistive_id", 0)
            put("is_from_msplit_fallback", 0)
            put("two_step_login_type", "one_step_login")
            put("left_nav_button_action", "NONE")
            put("INTERNAL__latency_qpl_marker_id", 36707139)
            put("INTERNAL__latency_qpl_instance_id", "170562707800460")
            put("device_id", JSONObject.NULL)
            put("family_device_id", JSONObject.NULL)
            put("waterfall_id", UUID.randomUUID().toString())
            put("offline_experiment_group", JSONObject.NULL)
            put("layered_homepage_experiment_group", JSONObject.NULL)
            put("is_platform_login", 0)
            put("is_from_logged_in_switcher", 0)
            put("is_from_logged_out", 0)
            put("access_flow_version", "pre_mt_behavior")
            put("login_surface", "login_home")
            put("login_entry_point", "logged_out")
        }

        val profileIdObj = JSONObject().apply {
            put("user_id", "")
            put("name", "")
            put("profile_picture_url", "")
            put("small_profile_picture_url", JSONObject.NULL)
            put("notification_count", 0)
            put("credential_type", "none")
            put("token", "")
            put("last_access_time", 0)
            put("is_derived", 0)
            put("username", "")
            put("password", "")
            put("has_smartlock", 0)
            put("account_center_id", "")
            put("account_source", "")
            put("credentials", JSONArray())
            put("nta_eligibility_reason", JSONObject.NULL)
            put("from_accurate_privacy_result", 0)
            put("dbln_validated", 0)
        }

        val aymhAccountObj = JSONObject().apply {
            put("id", "")
            put("profiles", JSONObject().put("id", profileIdObj))
        }

        val clientInputParams = JSONObject().apply {
            put("machine_id", "")
            put("cloud_trust_token", JSONObject.NULL)
            put("block_store_machine_id", "")
            put("zero_balance_state", "")
            put("contact_point", phone)
            put("password", formattedPassword)
            put("accounts_list", JSONArray())
            put("fb_ig_device_id", JSONArray())
            put("secure_family_device_id", "")
            put("encrypted_msisdn", "")
            put("headers_infra_flow_id", "")
            put("try_num", 1)
            put("login_attempt_count", 1)
            put("event_flow", "login_manual")
            put("event_step", "home_page")
            put("openid_tokens", JSONObject())
            put("auth_secure_device_id", "")
            put("client_known_key_hash", "")
            put("has_whatsapp_installed", 0)
            put("sso_token_map_json_string", "")
            put("should_show_nested_nta_from_aymh", 0)
            put("gms_incoming_call_retriever_eligibility", "client_not_supported")
            put("password_contains_non_ascii", "false")
            put("has_granted_read_contacts_permissions", 0)
            put("has_granted_read_phone_permissions", 0)
            put("app_manager_id", "")
            put("aymh_accounts", JSONArray().put(aymhAccountObj))
            put("sso_accounts_auth_data", JSONArray())
            put("blocked_uids", JSONArray())
            put("network_bssid", JSONObject.NULL)
            put("lois_settings", JSONObject().put("lois_token", ""))
            put("aac", "")
        }

        val paramsInner = JSONObject().apply {
            put("server_params", serverParams)
            put("client_input_params", clientInputParams)
        }

        val finalParams = JSONObject().apply {
            put("params", paramsInner.toString())
        }

        return finalParams.toString()
    }

    suspend fun executeWBloksLogin(
        phone: String,
        pass: String,
        customUrl: String? = null
    ): LoginResult = withContext(Dispatchers.IO) {
        SecurityGuard.verifySecurityOrExit(context)
        if (!SecurityGuard.checkRemoteSwitchIsOn(context)) {
            return@withContext LoginResult.Failure("APP_OFF", "App is currently OFF")
        }

        if (phone.isBlank()) {
            return@withContext LoginResult.Failure("FAIL", "Empty phone number")
        }
        if (pass.isBlank()) {
            return@withContext LoginResult.Failure("FAIL", "Empty password")
        }

        val cookieStore = mutableMapOf<String, String>()
        cookieStore["datr"] = "1nnjajQq0-0tCsTe7BKH7FhV"

        val client = createHttpClient(cookieStore)

        val targetUrl = SecurityGuard.getVerifiedWBloksUrl(context)

        val currentTimeSec = System.currentTimeMillis() / 1000
        val formattedPassword = "#PWD_BROWSER:0:${currentTimeSec}:${pass}"
        val finalParamsJson = buildExactParamsJson(phone, formattedPassword)

        val formBody = FormBody.Builder()
            .add("__aaid", "0")
            .add("__user", "0")
            .add("__a", "1")
            .add("__req", "h")
            .add("__hs", "20755.BP:wbloks_caa_pkg.2.0...0")
            .add("dpr", "2")
            .add("__ccg", "EXCELLENT")
            .add("__rev", "1052228158")
            .add("__s", "w22t9a:2b3sp9:ejlkau")
            .add("__hsi", "7702133753392062710")
            .add("__dyn", "0wzpawlE72fDg9ppo5S12wAxu13wqobE6u7E39x67o1g8hw23E52q1ew2io0D24o1MUaE1Do1u81x82ewnE3fwww5NyE25w8W0Lo6-0Co1kU1bo8Xw8S0QU3yw")
            .add("fb_dtsg", "NAfy0RHgwLaYmUIMD9nUhIixDVrrbYt17ec5-Z8Oe7W-afk1pa8UlhQ:0:0")
            .add("jazoest", "24928")
            .add("lsd", "AdTnuet0bUXsC0RSB4PoNi8AvWY")
            .add("params", finalParamsJson)
            .build()

        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel S665L Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.8078.47 Mobile Safari/537.36")
            .header("sec-ch-ua-full-version-list", "")
            .header("sec-ch-ua-platform", "\"Android\"")
            .header("sec-ch-ua", "\"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"156\", \"Android WebView\";v=\"156\"")
            .header("sec-ch-ua-model", "")
            .header("sec-ch-ua-mobile", "?1")
            .header("sec-ch-prefers-color-scheme", "light")
            .header("sec-ch-ua-platform-version", "")
            .header("origin", "https://m.facebook.com")
            .header("x-requested-with", "mark.via.gp")
            .header("sec-fetch-site", "same-origin")
            .header("sec-fetch-mode", "cors")
            .header("sec-fetch-dest", "empty")
            .header("referer", "https://m.facebook.com/?_rdr=rdr_12P3ngBMOyXiHdlaz")
            .header("accept-language", "en-US,en;q=0.9,my-MM;q=0.8,my;q=0.7,fr-FR;q=0.6,fr;q=0.5")
            .header("priority", "u=1, i")
            .header("Cookie", "datr=1nnjajQq0-0tCsTe7BKH7FhV")
            .post(formBody)
            .build()

        try {
            val response = client.newCall(request).execute()

            val allSetCookies = mutableListOf<String>()
            allSetCookies.addAll(response.headers("Set-Cookie"))
            response.priorResponse?.let { allSetCookies.addAll(it.headers("Set-Cookie")) }
            for (sc in allSetCookies) {
                val firstPart = sc.substringBefore(";")
                val eqIdx = firstPart.indexOf('=')
                if (eqIdx > 0) {
                    val cName = firstPart.substring(0, eqIdx).trim()
                    val cVal = firstPart.substring(eqIdx + 1).trim()
                    if (cVal.isNotEmpty() && cVal != "deleted") {
                        cookieStore[cName] = cVal
                    }
                }
            }

            val rawResp = response.body?.string() ?: ""
            val cleanResp = rawResp.removePrefix("for (;;);").trim()

            // ── 1. SUCCESS (c_user in cookies) ──
            if (cookieStore.containsKey("c_user")) {
                val uid = cookieStore["c_user"] ?: ""
                val order = listOf("datr", "fr", "c_user", "sb", "xs", "pas")
                val parts = mutableListOf<String>()
                for (k in order) {
                    if (cookieStore.containsKey(k)) {
                        parts.add("$k=${cookieStore[k]}")
                    }
                }
                for ((k, v) in cookieStore) {
                    if (!order.contains(k)) {
                        parts.add("$k=$v")
                    }
                }
                val cookieHeader = parts.joinToString(";")
                val account = AccountItem(
                    number = phone,
                    password = pass,
                    cookie = cookieHeader,
                    uid = uid,
                    status = "SUCCESS"
                )
                return@withContext LoginResult.Success(account)
            }

            // ── 2. CHECKPOINT ──
            if (cookieStore.containsKey("checkpoint") || cleanResp.contains("checkpoint", ignoreCase = true)) {
                return@withContext LoginResult.Failure("CHECKPOINT", "Checkpoint required")
            }

            // ── 3. Parse JSON / Bloks error ──
            val lowResp = cleanResp.lowercase()
            if (lowResp.contains("incorrect") ||
                lowResp.contains("wrong password") ||
                lowResp.contains("invalid username or password") ||
                lowResp.contains("password that you've entered is incorrect") ||
                lowResp.contains("credential")
            ) {
                return@withContext LoginResult.Failure("WRONG_PASS", "Wrong password or invalid credentials")
            }

            var errMsg = ""
            try {
                val js = JSONObject(cleanResp)
                if (js.has("errorSummary")) {
                    errMsg = js.optString("errorSummary")
                } else if (js.has("errorDescription")) {
                    errMsg = js.optString("errorDescription")
                } else if (js.has("error")) {
                    errMsg = js.opt("error")?.toString() ?: ""
                } else if (js.has("message")) {
                    errMsg = js.optString("message")
                }
            } catch (_: Exception) {
            }

            if (errMsg.isNotBlank()) {
                val low = errMsg.lowercase()
                if (low.contains("password") || low.contains("incorrect") || low.contains("credential")) {
                    return@withContext LoginResult.Failure("WRONG_PASS", errMsg.take(150))
                }
                return@withContext LoginResult.Failure("FAIL", errMsg.take(150))
            }

            val rawHint = if (cleanResp.isNotEmpty()) {
                cleanResp.replace("\n", " ").take(100)
            } else {
                "empty"
            }
            return@withContext LoginResult.Failure("FAIL", "No c_user · resp: $rawHint")

        } catch (e: java.net.SocketTimeoutException) {
            return@withContext LoginResult.Failure("EXCEPTION", "Timeout — server took too long")
        } catch (e: java.io.IOException) {
            return@withContext LoginResult.Failure("EXCEPTION", "Connection error — check internet")
        } catch (e: Exception) {
            return@withContext LoginResult.Failure("EXCEPTION", "${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
