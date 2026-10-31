package com.example.network

import com.example.data.AccountItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class LoginResult {
    data class Success(val account: AccountItem) : LoginResult()
    data class Failure(val status: String, val message: String) : LoginResult()
}

class WBloksLoginRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun createHttpClient(cookieStore: MutableMap<String, String>): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    for (cookie in cookies) {
                        cookieStore[cookie.name] = cookie.value
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val list = mutableListOf<Cookie>()
                    for ((k, v) in cookieStore) {
                        Cookie.Builder()
                            .domain(url.host)
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

    suspend fun executeWBloksLogin(
        phone: String,
        pass: String,
        customUrl: String? = null
    ): LoginResult = withContext(Dispatchers.IO) {
        if (phone.isBlank()) {
            return@withContext LoginResult.Failure("FAIL", "Empty phone number")
        }
        if (pass.isBlank()) {
            return@withContext LoginResult.Failure("FAIL", "Empty password")
        }

        val cookieStore = mutableMapOf<String, String>()
        cookieStore["datr"] = "1nnjajQq0-0tCsTe7BKH7FhV"

        val client = createHttpClient(cookieStore)

        val targetUrl = if (!customUrl.isNullOrBlank()) {
            customUrl.trim()
        } else {
            "https://www.facebook.com/api/graphql?appid=com.bloks.www.bloks.caa.login.async.send_login_request&type=action&__bkv=297f3cb2970a4ddbea7e63c512d94165047d214cd35e59546e9a48dadc54952c"
        }

        val currentTimeSec = System.currentTimeMillis() / 1000
        val formattedPassword = "#PWD_BROWSER:0:${currentTimeSec}:${pass}"

        val serverParams = mapOf(
            "credential_type" to "password",
            "username_text_input_id" to "s7hi9y:57",
            "password_text_input_id" to "s7hi9y:58",
            "login_source" to "Login",
            "login_credential_type" to "none",
            "server_login_source" to "login",
            "ar_event_source" to "login_home_page",
            "should_trigger_override_login_success_action" to 0,
            "should_trigger_override_login_2fa_action" to 0,
            "is_caa_perf_enabled" to 0,
            "reg_flow_source" to "login_home_native_integration_point",
            "caller" to "gslr",
            "is_from_landing_page" to 0,
            "is_from_empty_password" to 0,
            "is_from_aymh" to 0,
            "is_from_password_entry_page" to 0,
            "is_from_assistive_id" to 0,
            "is_from_msplit_fallback" to 0,
            "two_step_login_type" to "one_step_login",
            "left_nav_button_action" to "NONE",
            "INTERNAL__latency_qpl_marker_id" to 36707139,
            "INTERNAL__latency_qpl_instance_id" to "170562707800460",
            "device_id" to null,
            "family_device_id" to null,
            "waterfall_id" to UUID.randomUUID().toString(),
            "offline_experiment_group" to null,
            "layered_homepage_experiment_group" to null,
            "is_platform_login" to 0,
            "is_from_logged_in_switcher" to 0,
            "is_from_logged_out" to 0,
            "access_flow_version" to "pre_mt_behavior",
            "login_surface" to "login_home",
            "login_entry_point" to "logged_out"
        )

        val clientInputParams = mapOf(
            "machine_id" to "",
            "cloud_trust_token" to null,
            "block_store_machine_id" to "",
            "zero_balance_state" to "",
            "contact_point" to phone,
            "password" to formattedPassword,
            "accounts_list" to emptyList<Any>(),
            "fb_ig_device_id" to emptyList<Any>(),
            "secure_family_device_id" to "",
            "encrypted_msisdn" to "",
            "headers_infra_flow_id" to "",
            "try_num" to 1,
            "login_attempt_count" to 1,
            "event_flow" to "login_manual",
            "event_step" to "home_page",
            "openid_tokens" to emptyMap<String, Any>(),
            "auth_secure_device_id" to "",
            "client_known_key_hash" to "",
            "has_whatsapp_installed" to 0,
            "sso_token_map_json_string" to "",
            "should_show_nested_nta_from_aymh" to 0,
            "gms_incoming_call_retriever_eligibility" to "client_not_supported",
            "password_contains_non_ascii" to "false",
            "has_granted_read_contacts_permissions" to 0,
            "has_granted_read_phone_permissions" to 0,
            "app_manager_id" to "",
            "sso_accounts_auth_data" to emptyList<Any>(),
            "blocked_uids" to emptyList<Any>(),
            "network_bssid" to null,
            "lois_settings" to mapOf("lois_token" to ""),
            "aac" to ""
        )

        val paramsInnerMap = mapOf(
            "server_params" to serverParams,
            "client_input_params" to clientInputParams
        )

        val moshiAdapter = moshi.adapter(Any::class.java)
        val paramsInnerJson = moshiAdapter.toJson(paramsInnerMap)
        val finalParamsMap = mapOf("params" to paramsInnerJson)
        val finalParamsJson = moshiAdapter.toJson(finalParamsMap)

        val formBodyBuilder = FormBody.Builder()
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

        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel S665L Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.8078.47 Mobile Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9,my-MM;q=0.8,my;q=0.7,fr-FR;q=0.6,fr;q=0.5")
            .header("sec-ch-ua", "\"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"156\", \"Android WebView\";v=\"156\"")
            .header("sec-ch-ua-mobile", "?1")
            .header("sec-ch-ua-platform", "\"Android\"")
            .header("origin", "https://www.facebook.com")
            .header("x-requested-with", "mark.via.gp")
            .header("sec-fetch-site", "same-origin")
            .header("sec-fetch-mode", "cors")
            .header("sec-fetch-dest", "empty")
            .header("referer", "https://www.facebook.com/")
            .header("priority", "u=1, i")
            .post(formBodyBuilder.build())
            .build()

        try {
            val response = client.newCall(request).execute()
            val rawResp = response.body?.string() ?: ""

            // Clean Facebook JSON prefix for security
            val cleanResp = rawResp.replace("for (;;);", "").trim()

            // 1. Check Cookies
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
            } else if (cookieStore.containsKey("checkpoint")) {
                return@withContext LoginResult.Failure("CHECKPOINT", "Checkpoint Required for $phone")
            }

            // 2. Parse Clean JSON response for errors
            var errMessage = ""
            try {
                val mapAdapter = moshi.adapter(Map::class.java)
                @Suppress("UNCHECKED_CAST")
                val js = mapAdapter.fromJson(cleanResp) as? Map<String, Any>
                if (js != null) {
                    val err = js["error"] ?: js["errorSummary"] ?: js["message"]
                    if (err != null) {
                        errMessage = err.toString()
                    }
                }
            } catch (e: Exception) {
                // Ignore JSON parse exception
            }

            if (errMessage.isNotBlank()) {
                val low = errMessage.lowercase()
                val status = if (low.contains("password") || low.contains("incorrect") || low.contains("credential")) {
                    "WRONG_PASS"
                } else {
                    "FAIL"
                }
                return@withContext LoginResult.Failure(status, errMessage)
            }

            // Fallback response string preview if no explicit error field found
            val hint = if (cleanResp.length > 100) cleanResp.substring(0, 100) else cleanResp
            return@withContext LoginResult.Failure("FAIL", "Response: $hint")

        } catch (e: Exception) {
            return@withContext LoginResult.Failure("EXCEPTION", "${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
