package com.example.network

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.example.data.AccountItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class LoginResult {
    data class Success(val account: AccountItem) : LoginResult()
    data class Failure(val status: String, val message: String) : LoginResult()
}

sealed class ProfilePicResult {
    data class Success(val message: String) : ProfilePicResult()
    data class Failure(val message: String) : ProfilePicResult()
}

class WBloksLoginRepository {

    private fun createHttpClient(cookieStore: MutableMap<String, String>): OkHttpClient {
        return OkHttpClient.Builder()
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
            put("login_surface", "login_home", )
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
            "https://m.facebook.com/async/wbloks/fetch/?appid=com.bloks.www.bloks.caa.login.async.send_login_request&type=action&__bkv=297f3cb2970a4ddbea7e63c512d94165047d214cd35e59546e9a48dadc54952c"
        }

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

    private fun fetchFreshDtsgAndLsd(
        client: OkHttpClient,
        uid: String,
        cookie: String,
        userAgent: String,
        defaultDtsg: String,
        defaultLsd: String
    ): Pair<String, String> {
        var fbDtsg = defaultDtsg
        var lsd = defaultLsd
        try {
            val getReq = Request.Builder()
                .url("https://www.facebook.com/profile.php?id=$uid")
                .header("User-Agent", userAgent)
                .header("Cookie", cookie)
                .header("accept-language", "en-US,en;q=0.9")
                .get()
                .build()

            client.newCall(getReq).execute().use { getResp ->
                val html = getResp.body?.string() ?: ""
                val dtsgMatcher = Pattern.compile("\"DTSGInitialData\",\\[\\],\\{\"token\":\"([^\"]+)\"").matcher(html)
                if (dtsgMatcher.find()) {
                    dtsgMatcher.group(1)?.let { if (it.isNotEmpty()) fbDtsg = it }
                } else {
                    val altDtsg = Pattern.compile("name=\"fb_dtsg\" value=\"([^\"]+)\"").matcher(html)
                    if (altDtsg.find()) {
                        altDtsg.group(1)?.let { if (it.isNotEmpty()) fbDtsg = it }
                    }
                }

                val lsdMatcher = Pattern.compile("\"LSD\",\\[\\],\\{\"token\":\"([^\"]+)\"").matcher(html)
                if (lsdMatcher.find()) {
                    lsdMatcher.group(1)?.let { if (it.isNotEmpty()) lsd = it }
                }
            }
        } catch (_: Exception) {
        }
        return Pair(fbDtsg, lsd)
    }

    suspend fun addProfilePicture(uid: String, cookie: String): ProfilePicResult = withContext(Dispatchers.IO) {
        if (uid.isBlank() || cookie.isBlank()) {
            return@withContext ProfilePicResult.Failure("Missing UID or Cookie")
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"

        val (fbDtsg, lsd) = fetchFreshDtsgAndLsd(
            client = client,
            uid = uid,
            cookie = cookie,
            userAgent = userAgent,
            defaultDtsg = "NAfzYhzpL9HPFE0A8I2CBV35878UA_Ehi5WNinn-ukieghagwPRw-wQ:26:1793177692",
            defaultLsd = "LZ3H2uGMs6avKPFlMwv7C8"
        )

        val scaledCropRect = JSONObject().apply {
            put("height", 1)
            put("width", 1)
            put("x", 0)
            put("y", 0)
        }

        val inputObj = JSONObject().apply {
            put("attribution_id_v2", "ProfileCometTimelineListViewRoot.react,comet.profile.timeline.list,unexpected,1793177724140,195522,190055527696468,,;CometHomeRoot.react,comet.home,via_cold_start,1793177721126,948770,4748854339,,")
            put("caption", "")
            put("existing_photo_id", "122101393275523697")
            put("expiration_time", JSONObject.NULL)
            put("profile_id", uid)
            put("profile_pic_method", "EXISTING")
            put("profile_pic_source", "TIMELINE")
            put("scaled_crop_rect", scaledCropRect)
            put("skip_cropping", true)
            put("actor_id", uid)
            put("client_mutation_id", "2")
        }

        val variablesObj = JSONObject().apply {
            put("input", inputObj)
            put("isPage", false)
            put("isProfile", true)
            put("scale", 2)
            put("__relay_internal__pv__ProfileGeminiIsCoinFlipEnabledrelayprovider", false)
        }

        val formBody = FormBody.Builder()
            .add("av", uid)
            .add("__aaid", "0")
            .add("__user", uid)
            .add("__a", "1")
            .add("__req", "1f")
            .add("__hs", "20754.HYP:comet_pkg.2.1...0")
            .add("dpr", "2")
            .add("__ccg", "EXCELLENT")
            .add("__rev", "1052023215")
            .add("__s", "xv29kk:j2he50:dy7tgz")
            .add("__hsi", "7701639655485046666")
            .add("__dyn", "7xe5WK1ixt0mUyEqxemh0noeEb8nwgU76byQdwSwAyUco2qwJyE24wJwpUe8hwaG1sw9u0LVEtwMw6ywIK1Rwwwqo462mcwfG12wOx62G5Usw9m1YwBgK7o6C1uwoE4G17yovwRwlE-U2exi4UaEW2au1jwUBwJK14xm3y11xfxmu3W3rws9ovUaU6a1TxW2-awLyES1TwVwwwOg2ZwhEkxebwHwNxe6Uak0zU8oC1Hg4-6o4e222m3Gfxm2yVU-4FqwIK6E5mEbU5q1Rwgo-0iy5EaUcEK3m0K83jxG2O8yByUgwiUhwAS8BG")
            .add("__csr", "g9Y6Atf4Mvgx3Icl4jOSwL5h4vitb4cI_ddkAaZb994l4E8EBJlJmIzRQQpKykYOZYIG8jlOGjkzTAF6XnFRquWmBbWHVl9KB_HGROLRUxWRlpbIyQAHLx3iGWVevhNsTHiiipbDLBjmu-AGqQWG8IzWA-z4SmOd4F49CAF_imVbWKl26EDCCOVEyit9bFsy9i-lrWZRjujCJ4hLajDKr8vLApep6GmiVkl8VumWmoGWRLBZyFoS9AVpQie-FpHGmDyuVbG-_Xxa4FJqdWyHGqF_V8F4xmt2ku-6appoG8zF5UCu4BxfAxKiiaHx2idKqBx6hkaDJ2FCueG4ogypmfx52pay8-q64ibCJa6E-vx2dz8ky98gJ0CFe4ESmfWV42iayEG7oOexii6UlzXz9USHxa8K3q9gjKq8x6p1Wm7HwBy8nxa8G742Cm322WEuzErwEwyy9Wx7zWG5VEC4A489F8yu8S7ooxTxx289EbEymmaAxG8zojwDK2S2-6VeaDzUWqawiQexqcwvUtwQwio3Sxa8wGwiEK0YU0Zi0V8C12yoSE8U2jw3E9kHxK3a3bg2XgG6828w6bw4dwPwiGxO8xq0hu8wjU3bwPwXCwaa1jBwg8hz45-06M80fPE8S8w0IQg4G17c04ao3lwaq0gS053o1Z408ACw0euS04S83tyoK1-G360fUg0hRG683Xw2FkdDm2a0VA0578nwOgdoig3Aw2n4Ambgtg2sIU0g2BO5U3JwPo0XubwKo1mo5-0wRw4HwPwv83iw2782cw0AdzJ05ZAw40xgw2-whRxy0465g0Vy1f-ltFU09QA2LaibVcObU0giw0x8w3fUB02MF3Q0it0c60hBQ0bhw")
            .add("__hsdp", "gci18gW1Awxt8qihyqaNapi9aCjEup44Cq78yq2q10h8y3IG215h6yCBqEBFE27aob8kg4FGchmQx31lb2h6PHOebXOd9GgUiAPuF7h4kgxwF1QiehyasMEbgwSh7GfFANzaAs984Fkh8y58ky7h9bCy3BiPEFhrlImxsVC8xdfbqR1UpJS4expoMl7i9baQ48x92wQgHS59qF5pCfG6EKx16GyEwxp4h2z9xq-FGEip7ghyber8p6xd4xiQAiC4yfyOxfoyhF3U8Wgy44u5ihtgK-BAdgroLwi5ai1uwgUpgIwbU8EB1NwAx2u1Jgrxi268x3gMhxy3gwW6k2B111G688p8-UnyU3rBLz8rw-F1y3B1y0GE4fgcE3zwZCwCK0Dy2o5K1nwKzE4a6o8bWxmU25wCw7gwDwcu0D9Q0VE32xu2i0zE1xE5208FCwiE5O1Qw860iC781i87C10wfm1sw2GE3gw6gw78w2Jo1oo3Vxa0hi0Io1SU9E7GV83Gw7izU0xq0bbwGw6MwmE4m3u0QU1XU0J-1Zw69w4-w6_w92")
            .add("__hblp", "1GeAQ2Z2Vh4xpEy58ownS1Jwn8d8496ojwpU-1jwtpe1Fxa5UpwRzodUG44awDwpotxq0wagKm4ocLBUgDwGxCEugMwGdwDzUqCzohFbxbx2fzUK223K9wxwIy8hxO4WwIwHg5S9w8WaxyU5i1ex2p121_zEaEizoy688Eho5iicQbz9UgDxaqiq8xyJ283rym0x84l345EdE5K13xO49Urxi5GwGwOw-y8d-8zqwpodohz89Ed84W8wKwxxjz8K1jyo76U-2q6U3Kwq8swpUnzbwi8cEC68kwc6ucyU2kw8O3m5UnUGU7e5E5Ki2OUF5DwVwCwZwXwkbyE8oG1Xwt9E2vyEc8pCyUa98Z0GUO2a1Qwk8mxC2m3OaK8wmoswNByUGm7o7O3W1dwww-wWwg82xixG7U4e2S1RwNwv834wuo4q0wUS3W221Swo8rxecUkwgUpwYwc21FCw_wBwi85O2S5Upz8426Ua9olxi1-wExS1Zwee0ZGxa0hi8wBw8a0Bo2EzU2cwCwywjKqV84e15xC16g5O13wo82nxG78-5od83Qxi543W1jAxem3O1-wgE8EnwBxy6oao3fxdwywDQ0ZEb47Ed8oxGVU4-EkwMwTg6-E5nDwvUkgb982yWx6m78888bAgcU662N0t8eF8Wu2Sq3iU6R1-dyoixWgwSqawIwKo7q1RyU9o7q0P8-4oGq78K8xK78lUoGfwCwsyxy9yUfE9oZ3o22xy3a7UCp0nawhUgw")
            .add("__sjsp", "gci18gW1Awxt8qihyqaNapi9aCjEup44Cq78yqf5E414y8eOE84l4qaqlGymCSgal17aob8kg4FGchmQx31lansghIWYzyVsziqAe4FcTGhQipkmx8F1QiehyasMEA8Mwh4y8iyYgnqgngjUmyC2l4AK7aPEEVeEvRwBATF5Fo9-uchUiwx5hQyiyohy4Aa3gy2aGhAt3UbcN4WyEwwF4gEMrBxV0vQ8gpwEgigcUthU9A21o1iA0jt0fZ09a0ud028ofobrw6tw9OU")
            .add("__comet_req", "15")
            .add("fb_dtsg", fbDtsg)
            .add("jazoest", "25345")
            .add("lsd", lsd)
            .add("__spin_r", "1052023215")
            .add("__spin_b", "trunk")
            .add("__spin_t", "1793177718")
            .add("__crn", "comet.fbweb.CometProfileTimelineListViewRoute")
            .add("fb_api_caller_class", "RelayModern")
            .add("fb_api_req_friendly_name", "ProfileCometProfilePictureSetMutation")
            .add("server_timestamps", "true")
            .add("variables", variablesObj.toString())
            .add("doc_id", "29015178351504159")
            .build()

        val request = Request.Builder()
            .url("https://www.facebook.com/api/graphql/")
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("sec-ch-ua-full-version-list", "\"Not:A-Brand\";v=\"8.0.0.0\", \"Chromium\";v=\"156.0.8078.47\"")
            .header("sec-ch-ua-platform", "\"macOS\"")
            .header("x-fb-navigation-chain", "ProfileCometTimelineListViewRoot.react,comet.profile.timeline.list,unexpected,1793177724140,195522,190055527696468,,;CometHomeRoot.react,comet.home,via_cold_start,1793177721126,948770,4748854339,,")
            .header("sec-ch-ua", "\"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"156\"")
            .header("x-fb-friendly-name", "ProfileCometProfilePictureSetMutation")
            .header("sec-ch-ua-mobile", "?0")
            .header("sec-ch-ua-model", "\"itel S665L\"")
            .header("x-asbd-id", "359341")
            .header("x-fb-lsd", lsd)
            .header("sec-ch-prefers-color-scheme", "light")
            .header("sec-ch-ua-platform-version", "\"12.0.0\"")
            .header("origin", "https://www.facebook.com")
            .header("x-requested-with", "mark.via.gp")
            .header("sec-fetch-site", "same-origin")
            .header("sec-fetch-mode", "cors")
            .header("sec-fetch-dest", "empty")
            .header("referer", "https://www.facebook.com/profile.php?id=$uid")
            .header("accept-language", "en-US,en;q=0.9,my-MM;q=0.8,my;q=0.7,fr-FR;q=0.6,fr;q=0.5")
            .header("priority", "u=1, i")
            .header("Cookie", cookie)
            .post(formBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""
            val cleanBody = rawBody.removePrefix("for (;;);").trim()

            if (response.isSuccessful &&
                (cleanBody.contains("\"profile_picture\"") ||
                        cleanBody.contains("ProfileCometProfilePictureSetMutation") ||
                        cleanBody.contains("\"actor\"") ||
                        cleanBody.contains("\"data\"")) &&
                !cleanBody.contains("\"error\":") &&
                !cleanBody.contains("\"errors\":[")
            ) {
                return@withContext ProfilePicResult.Success("Add Profile Success!")
            }

            try {
                val js = JSONObject(cleanBody)
                if (js.has("errorSummary")) {
                    return@withContext ProfilePicResult.Failure("Failed: ${js.optString("errorSummary")}")
                }
                if (js.has("errors")) {
                    val errs = js.optJSONArray("errors")
                    if (errs != null && errs.length() > 0) {
                        val firstErr = errs.optJSONObject(0)?.optString("message") ?: "GraphQL error"
                        return@withContext ProfilePicResult.Failure("Failed: $firstErr")
                    }
                }
                if (js.has("data")) {
                    return@withContext ProfilePicResult.Success("Add Profile Success!")
                }
            } catch (_: Exception) {
            }

            val snippet = cleanBody.replace("\n", " ").take(80)
            return@withContext ProfilePicResult.Failure("Failed (${response.code}): $snippet")
        } catch (e: Exception) {
            return@withContext ProfilePicResult.Failure("Error: ${e.message}")
        }
    }

    private fun generateCoverPngBytes(): ByteArray {
        val width = 820
        val height = 312
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.parseColor("#1E3A8A"),
                Color.parseColor("#0F172A"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        bitmap.recycle()
        return bos.toByteArray()
    }

    suspend fun addCoverPhoto(uid: String, cookie: String): ProfilePicResult = withContext(Dispatchers.IO) {
        if (uid.isBlank() || cookie.isBlank()) {
            return@withContext ProfilePicResult.Failure("Missing UID or Cookie")
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"

        val (fbDtsg, lsd) = fetchFreshDtsgAndLsd(
            client = client,
            uid = uid,
            cookie = cookie,
            userAgent = userAgent,
            defaultDtsg = "NAfyhaXXLf0ID_IxUFBvVRErrpThWl01FcdtQ7f7_Xq_oqAbQCPw0ug:26:1793177692",
            defaultLsd = "KhxBVU_3I3A379ao1eePcy"
        )

        val uploadUrl = "https://www.facebook.com/profile/cover/comet_upload/".toHttpUrl().newBuilder()
            .addQueryParameter("profile_id", uid)
            .addQueryParameter("av", uid)
            .addQueryParameter("__aaid", "0")
            .addQueryParameter("__user", uid)
            .addQueryParameter("__a", "1")
            .addQueryParameter("__req", "1l")
            .addQueryParameter("__hs", "20754.HYP:comet_pkg.2.1...0")
            .addQueryParameter("dpr", "2")
            .addQueryParameter("__ccg", "EXCELLENT")
            .addQueryParameter("__rev", "1052023215")
            .addQueryParameter("__s", ":j2he50:m8am7r")
            .addQueryParameter("__hsi", "7701642079101044364")
            .addQueryParameter("__dyn", "7xe5WKewgUng5K8G6EjBAgdE9EeFU98nwgU76byQdwSwAyUco2qwJyEiwsobo6u3y4o2Gwfi0LVEtwMw6ywIK1Rwwwqo462mcwfG12wOx62G5Usw9m1YwBgK7o6C1uwoE4G17yovwRwlE-U2exi4UaEW2au1jwUBwJK14xm3y11xfxmu3W3y1MBx_wHwUwa67EbUG2-azo7u3C22390bS16xi4UK2K364VUjwFg2fwxyo6J0jUpwgU889oeefx6UabDzUiBG2OUqwlqwLwlE7m11zU1a8mwHwOyUdo2Uwde6EdoFoK484K4o9dy9qw")
            .addQueryParameter("__csr", "g9Y6AtOM977gx6gNkhfbq7h4Iyf7QDiN3bkDddlH2viTb4ldi2aRpaBlH8ltd7hykY-TZOExd9iGREzGFbCXnFQADK-BbWHVl9KJvAF9sH-K8uJlhFObiiK-4daHHAVZ75OvHiiipbDLCJ2rWiHGHGfVeFfENdBIzhah2pFavQ-VbWKl2eEDCCOUkDiLFsy9i-lrWZWjujCJ4hL8uuVIx--hAVAqFpbBhkzBVrFpyHHm-nS48S9AVpQie-FpHG8u9XAKHX_xq4FJq9-dKFGD_AyAi5pQ9hXUoFBByEyeAnypV8RojV8rAAyGUgAzrCFohAl2FXgGpDzGx648ClzUWA9x2fCxx4yVFaxGfDUgzoO58y5rgakU89ry-Ki2m5oG7oOexi225ojz9USHxa8K3q9xiVEy4pA7FouK2m8xu4EyEsgapoc8couzErwEwyy9Wx7zWG5VEC4UgwCAy9UzotwSUogy2q2W8BByF8qy8S4U5C7ohxKjyFU-eCyE4J3Emz87-7od84C0ZErwGwiEK0YU0Zi0V8C12yoSE8U2jw3_ocJ0bJ0zw8y0oK0gS3e1aG78vw4ny84-0OUcUeVE2ywmo5acgnU0bepC02QO2dy80bd41awhP012C0Ro2Cw4dw1gS081808lCw0euS04S83tyoK1-G360q209ng0hRG683Xw2FkdDm2a0VA0578nwOgdoig3Aw2n4Ambgtg2sIU0g2BO5U3JwPo0XubwKo1mo3zm0iK3e1Ywda08sw8O02gSeQ0nSi0g2520bW17m680gol03C84_VlSDw0DigaYF8LAP8Lw11a024y0c_yk0b2Afg19Q0Mo16ng0J6")
            .addQueryParameter("__hsdp", "gci4422szcxa5EbGwxt8qiibEFjFB8AGpexVAxpCxO8CwCwg4i8wXawwhkhEFFmG9qq0xOC2O541aqz4lJ8gR1fb2h6PHOebXO7qAe4FcTGhQh548o2B6xoyDca5Ld23p4ax9AKtcGhOSbg_20iq58kdh9bCy3BiPEGkrlx0nbCoy4QYGiR1UpJS4expoEp7i9baQ48x92wQgHS59qF9upzWBxiQ944q8ubGmh4gEMrBGqkip7qiQube6EjkwkDhSfEzUyyyEzoyhF3UHxqA8x17zpOhtgK-BAdgrpqG1akF9o52u13xB38co8EB1qgE8UyuuES1sgc8myAueWz10qwQ82x0Fjz46FUgDxCiq8K5-9wdKmS2y3WA2S6U8A2a1fxK3G2fg4hDwKwGwOwUXwg8oG2a12whO2o9Ed8d8p8m2WewDUoG5E8bWzVrwqDlx62q6U1ooiw_wDwzgrwMwOwyw9Ot0MDwaS0MEnxvzE2ax_g5Gfwfy1vw4AzUSfwsoeU88K2S48aEco35xq6o18VosxS0xE5q2K0CE3zG6Evxi0-o8U6e9wbG0kidwdq1wxK4UPxi13wlE30ws8fU9o4y1swJxu6oO10wh9oaE35wvo3zwfqEiw4ky81fE2Uw8O2q1NVHAwgU4m6o4q1twai0BUdU9od817A0AK5E1fU9ooxC2C0PUjo8E9Z04zguwr85G58c8dU72E5nDwvUkgb82DWx6m7888ak3e0zA0HV8Wu1FK1JgvzoC3owS4o5K1Uwau1Gw4-wUxnwckEowl89oZ3o22xy3a2Cq1twiEgw")
            .addQueryParameter("__hblp", "1GeAQ2Z2Vh4wlxi685Zwro5O3i12hC4U6u1zwtpe1Fxa5U4-dwgEggcE6m7omw82AbBx63bVu49U4iEugMwG3mfxGqdx6AK4K22fyU5O9wxwIy8hwMG2O0yEC0zEG6bwl84W49A482ewGwwwxwyx5wl98PgoDx2u4FF9Ey6aUC0SUBwcF345EdE2vxO488UkxqEaEcEfEy3vy8SE6m3m4oO2q3i1ey8bE8okUowkUC1NKfwCxK0W9o6y9xa1DxucK18wOyooxi0MpUObw9i0z8donxvzE2awmV8hxGUF5DwVwCwZwXwl8G26awuU7K0DUG3222bwEAzQ2Gewywcm5EpwBwhXwoEswNByUGm7o7O3W1dwnUeE3yixG7U4e0EUco7O0N81h8S1vwtE626Ujze584e6of830wqpEfU9o4y1swJxu6oO10xK2ym5okzK1JwExS1Zwee0ZGxa0hi8wBw4owayfw8O2q2a1eVHAwgU4m6o4p0n84e1ww9u6EswBwQwfi58kgfE5ei4Vof87W12wyxu2m68pwFwc-4S2a2vg3SwIguwQxy6HDwjWxi323t0rWwluu1_xh0IAwabG4poswwwwKh0Pw8V0t8eF8Wu2Sq8wHK1JgvzoC4E8O3pEG7ES2V0tU7mbwBwtE3czUhyFEsyUy6UsxnxyE-2q1Oa68lw-wBzQdw8a68cEvypA1sG17x2")
            .addQueryParameter("__sjsp", "gci4474szcxa5EbGwxt8qiibEFjFB8AGpexVAxpCxO8CwCwg4jq3IG215heyCBqAlFJA2BghOC2O541aqz4lJ8gR1cyBbbh6PHOebBO7qAZxajdWAt4Cask2B6xp34emhz8jwApbzdF3m7e25EkHcdh9bxOIWaAa9G7Zo9pdWhqm2vDz4u4Ev6hQyiyohy4Aa3gy2aGijDg-2PchcwjGah4ac6Voug7Z246oa44A3e7ku2p0wm0kF02e4")
            .addQueryParameter("__comet_req", "15")
            .addQueryParameter("fb_dtsg", fbDtsg)
            .addQueryParameter("jazoest", "25617")
            .addQueryParameter("lsd", lsd)
            .addQueryParameter("__spin_r", "1052023215")
            .addQueryParameter("__spin_b", "trunk")
            .addQueryParameter("__spin_t", "1793178282")
            .addQueryParameter("__crn", "comet.fbweb.CometProfileTimelineListViewRoute")
            .build()

        val pngBytes = generateCoverPngBytes()
        val filePartBody = pngBytes.toRequestBody("image/png".toMediaType())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "780ef9ae-77a3-46f6-9ec7-576fab613430", filePartBody)
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .header("User-Agent", userAgent)
            .header("sec-ch-ua-full-version-list", "\"Not:A-Brand\";v=\"8.0.0.0\", \"Chromium\";v=\"156.0.8078.47\"")
            .header("sec-ch-ua-platform", "\"macOS\"")
            .header("sec-ch-ua", "\"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"156\"")
            .header("sec-ch-ua-model", "\"itel S665L\"")
            .header("sec-ch-ua-mobile", "?0")
            .header("x-asbd-id", "359341")
            .header("x-fb-lsd", lsd)
            .header("sec-ch-prefers-color-scheme", "light")
            .header("sec-ch-ua-platform-version", "\"12.0.0\"")
            .header("origin", "https://www.facebook.com")
            .header("x-requested-with", "mark.via.gp")
            .header("sec-fetch-site", "same-origin")
            .header("sec-fetch-mode", "cors")
            .header("sec-fetch-dest", "empty")
            .header("referer", "https://www.facebook.com/profile.php?id=$uid")
            .header("accept-language", "en-US,en;q=0.9,my-MM;q=0.8,my;q=0.7,fr-FR;q=0.6,fr;q=0.5")
            .header("priority", "u=1, i")
            .header("Cookie", cookie)
            .post(multipartBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""
            val cleanBody = rawBody.removePrefix("for (;;);").trim()

            if (response.isSuccessful &&
                (cleanBody.contains("\"fbid\"") || cleanBody.contains("\"payload\"")) &&
                !cleanBody.contains("\"error\":")
            ) {
                return@withContext ProfilePicResult.Success("Add Cover Photo Success!")
            }

            try {
                val js = JSONObject(cleanBody)
                if (js.has("errorSummary")) {
                    return@withContext ProfilePicResult.Failure("Failed: ${js.optString("errorSummary")}")
                }
                if (js.has("payload")) {
                    return@withContext ProfilePicResult.Success("Add Cover Photo Success!")
                }
            } catch (_: Exception) {
            }

            val snippet = cleanBody.replace("\n", " ").take(80)
            return@withContext ProfilePicResult.Failure("Failed (${response.code}): $snippet")
        } catch (e: Exception) {
            return@withContext ProfilePicResult.Failure("Error: ${e.message}")
        }
    }
}
