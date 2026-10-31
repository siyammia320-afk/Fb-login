
import os
import sys
import time
import json
import uuid
import platform
import requests

# ══════════════════════════════════════════════════════════════════════════════
# OUTPUT FILE (login line saves here)
# ══════════════════════════════════════════════════════════════════════════════
OUTPUT_FILE = "/sdcard/Login_Accounts.txt" if os.path.exists("/sdcard") else "Login_Accounts.txt"

# ══════════════════════════════════════════════════════════════════════════════
# COLORS
# ══════════════════════════════════════════════════════════════════════════════
W = '\033[97m'
G = '\033[92m'
R = '\033[91m'
Y = '\033[93m'
C = '\033[96m'
DIM = '\033[2m'
BOLD = '\033[1m'
RESET = '\033[0m'


def clear_screen():
    os.system('cls' if platform.system().lower() == 'windows' else 'clear')


def linex():
    print(f"{W}─────────────────────────────────────────────{W}")


def banner():
    clear_screen()
    print(f"""{C}{BOLD}
╔══════════════════════════════════════════════════════╗
║     FACEBOOK LOGIN  ·  W-Bloks  ·  Termux Tool      ║
╚══════════════════════════════════════════════════════╝{RESET}""")


# ══════════════════════════════════════════════════════════════════════════════
# LOGIN
# ══════════════════════════════════════════════════════════════════════════════
def login_wbloks(phone, password):
    """
    Attempt W-Bloks login.
    Returns dict:
      {"status": "SUCCESS"|"WRONG_PASS"|"CHECKPOINT"|"FAIL"|"EXCEPTION",
       "cookie": str, "uid": str, "message": str}
    """
    url = ("arafatbhai07"
           "?appid=com.bloks.www.bloks.caa.login.async.send_login_request"
           "&type=action"
           "&__bkv=297f3cb2970a4ddbea7e63c512d94165047d214cd35e59546e9a48dadc54952c")

    headers = {
        "User-Agent": "Mozilla/5.0 (Linux; Android 12; itel S665L Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.8078.47 Mobile Safari/537.36",
        "Accept-Encoding": "gzip, deflate, br, zstd",
        "sec-ch-ua-full-version-list": "",
        "sec-ch-ua-platform": '"Android"',
        "sec-ch-ua": '"Not:A-Brand";v="8", "Chromium";v="156", "Android WebView";v="156"',
        "sec-ch-ua-model": "",
        "sec-ch-ua-mobile": "?1",
        "sec-ch-prefers-color-scheme": "light",
        "content-type": "application/x-www-form-urlencoded;charset=UTF-8",
        "sec-ch-ua-platform-version": "",
        "origin": "arafat.com",
        "x-requested-with": "mark.via.gp",
        "sec-fetch-site": "same-origin",
        "sec-fetch-mode": "cors",
        "sec-fetch-dest": "empty",
        "referer": "arafatbhai009=rdr_12P3ngBMOyXiHdlaz",
        "accept-language": "en-US,en;q=0.9,my-MM;q=0.8,my;q=0.7,fr-FR;q=0.6,fr;q=0.5",
        "priority": "u=1, i",
        "Cookie": "datr=1nnjajQq0-0tCsTe7BKH7FhV",
    }

    formatted_password = f"#PWD_BROWSER:0:{int(time.time())}:{password}"

    server_params = {
        "credential_type": "password",
        "username_text_input_id": "s7hi9y:57",
        "password_text_input_id": "s7hi9y:58",
        "login_source": "Login",
        "login_credential_type": "none",
        "server_login_source": "login",
        "ar_event_source": "login_home_page",
        "should_trigger_override_login_success_action": 0,
        "should_trigger_override_login_2fa_action": 0,
        "is_caa_perf_enabled": 0,
        "reg_flow_source": "login_home_native_integration_point",
        "caller": "gslr",
        "is_from_landing_page": 0,
        "is_from_empty_password": 0,
        "is_from_aymh": 0,
        "is_from_password_entry_page": 0,
        "is_from_assistive_id": 0,
        "is_from_msplit_fallback": 0,
        "two_step_login_type": "one_step_login",
        "left_nav_button_action": "NONE",
        "INTERNAL__latency_qpl_marker_id": 36707139,
        "INTERNAL__latency_qpl_instance_id": "170562707800460",
        "device_id": None,
        "family_device_id": None,
        "waterfall_id": str(uuid.uuid4()),
        "offline_experiment_group": None,
        "layered_homepage_experiment_group": None,
        "is_platform_login": 0,
        "is_from_logged_in_switcher": 0,
        "is_from_logged_out": 0,
        "access_flow_version": "pre_mt_behavior",
        "login_surface": "login_home",
        "login_entry_point": "logged_out",
    }

    client_input_params = {
        "machine_id": "",
        "cloud_trust_token": None,
        "block_store_machine_id": "",
        "zero_balance_state": "",
        "contact_point": phone,
        "password": formatted_password,
        "accounts_list": [],
        "fb_ig_device_id": [],
        "secure_family_device_id": "",
        "encrypted_msisdn": "",
        "headers_infra_flow_id": "",
        "try_num": 1,
        "login_attempt_count": 1,
        "event_flow": "login_manual",
        "event_step": "home_page",
        "openid_tokens": {},
        "auth_secure_device_id": "",
        "client_known_key_hash": "",
        "has_whatsapp_installed": 0,
        "sso_token_map_json_string": "",
        "should_show_nested_nta_from_aymh": 0,
        "gms_incoming_call_retriever_eligibility": "client_not_supported",
        "password_contains_non_ascii": "false",
        "has_granted_read_contacts_permissions": 0,
        "has_granted_read_phone_permissions": 0,
        "app_manager_id": "",
        "aymh_accounts": [{
            "id": "",
            "profiles": {
                "id": {
                    "user_id": "", "name": "", "profile_picture_url": "",
                    "small_profile_picture_url": None, "notification_count": 0,
                    "credential_type": "none", "token": "", "last_access_time": 0,
                    "is_derived": 0, "username": "", "password": "",
                    "has_smartlock": 0, "account_center_id": "", "account_source": "",
                    "credentials": [], "nta_eligibility_reason": None,
                    "from_accurate_privacy_result": 0, "dbln_validated": 0,
                }
            },
        }],
        "sso_accounts_auth_data": [],
        "blocked_uids": [],
        "network_bssid": None,
        "lois_settings": {"lois_token": ""},
        "aac": "",
    }

    params_inner = {"server_params": server_params, "client_input_params": client_input_params}
    final_params = {"params": json.dumps(params_inner)}

    payload = {
        "__aaid": "0", "__user": "0", "__a": "1", "__req": "h",
        "__hs": "20755.BP:wbloks_caa_pkg.2.0...0", "dpr": "2",
        "__ccg": "EXCELLENT", "__rev": "1052228158",
        "__s": "w22t9a:2b3sp9:ejlkau",
        "__hsi": "7702133753392062710",
        "__dyn": "0wzpawlE72fDg9ppo5S12wAxu13wqobE6u7E39x67o1g8hw23E52q1ew2io0D24o1MUaE1Do1u81x82ewnE3fwww5NyE25w8W0Lo6-0Co1kU1bo8Xw8S0QU3yw",
        "fb_dtsg": "NAfy0RHgwLaYmUIMD9nUhIixDVrrbYt17ec5-Z8Oe7W-afk1pa8UlhQ:0:0",
        "jazoest": "24928", "lsd": "AdTnuet0bUXsC0RSB4PoNi8AvWY",
        "params": json.dumps(final_params),
    }

    try:
        session = requests.Session()
        r = session.post(url, headers=headers, data=payload, timeout=30)
        cookies = session.cookies.get_dict()
        response_text = (r.text or "")[:300]

        # ── SUCCESS ──
        if "c_user" in cookies:
            uid = cookies["c_user"]
            order = ['datr', 'fr', 'c_user', 'sb', 'xs', 'pas']
            parts = [f"{k}={cookies[k]}" for k in order if k in cookies]
            for k, v in cookies.items():
                if k not in order: parts.append(f"{k}={v}")
            cookie_str = ";".join(parts)
            return {"status": "SUCCESS", "cookie": cookie_str, "uid": uid,
                    "message": "Login successful"}

        # ── CHECKPOINT ──
        if "checkpoint" in cookies:
            return {"status": "CHECKPOINT", "cookie": "", "uid": "",
                    "message": "Checkpoint required"}

        # ── Parse JSON error ──
        err_msg = ""
        try:
            js = r.json()
            err = (js.get("error") or js.get("errorSummary") or js.get("message")
                   or (js.get("errors") and js["errors"][0].get("summary")))
            if err: err_msg = str(err)[:150]
        except Exception:
            pass

        if err_msg:
            low = err_msg.lower()
            if "password" in low or "incorrect" in low or "credential" in low:
                return {"status": "WRONG_PASS", "cookie": "", "uid": "",
                        "message": err_msg}
            return {"status": "FAIL", "cookie": "", "uid": "", "message": err_msg}

        # fallback: raw response hint
        raw_hint = response_text[:100].replace("\n", " ") if response_text else "empty"
        return {"status": "FAIL", "cookie": "", "uid": "",
                "message": f"No c_user · resp: {raw_hint}"}

    except requests.exceptions.Timeout:
        return {"status": "EXCEPTION", "cookie": "", "uid": "",
                "message": "Timeout — server took too long"}
    except requests.exceptions.ConnectionError:
        return {"status": "EXCEPTION", "cookie": "", "uid": "",
                "message": "Connection error — check internet"}
    except Exception as e:
        return {"status": "EXCEPTION", "cookie": "", "uid": "",
                "message": f"{type(e).__name__}: {e}"}


# ══════════════════════════════════════════════════════════════════════════════
# UI
# ══════════════════════════════════════════════════════════════════════════════
def show_success(uid, phone, password, cookie):
    print(f"\n{G}{BOLD}╔══════════════════════════════════════════════════════╗")
    print(f"║              ✔  LOGIN SUCCESSFUL                     ║")
    print(f"╚══════════════════════════════════════════════════════╝{RESET}\n")

    print(f"{W}┌─────────────────────────────────────────────┐{W}")
    print(f"{W}│{G} UID      : {BOLD}{uid}{RESET}")
    print(f"{W}│{G} Phone    : {phone}{RESET}")
    print(f"{W}│{G} Password : {password}{RESET}")
    print(f"{W}└─────────────────────────────────────────────┘{W}\n")

    print(f"{C}{BOLD}🔐 COOKIE:{RESET}")
    print(f"{G}{cookie}{RESET}\n")


def show_failure(status, message, phone):
    labels = {
        "WRONG_PASS": "WRONG PASSWORD",
        "CHECKPOINT": "CHECKPOINT REQUIRED",
        "FAIL": "LOGIN FAILED",
        "EXCEPTION": "EXCEPTION",
    }
    label = labels.get(status, "UNKNOWN")

    print(f"\n{R}{BOLD}╔══════════════════════════════════════════════════════╗")
    print(f"║                ✘  {label:<35}║")
    print(f"╚══════════════════════════════════════════════════════╝{RESET}\n")
    print(f"{W}  Number : {phone}{RESET}")
    print(f"{W}  Reason : {R}{message}{RESET}\n")


def save_login(uid, password, cookie, phone):
    try:
        line = f"{uid}|{password}|{cookie}|{phone}"
        with open(OUTPUT_FILE, "a") as f:
            f.write(line + "\n")
        print(f"{G}[+] Saved to: {W}{OUTPUT_FILE}{RESET}\n")
        return True
    except Exception as e:
        print(f"{R}[!] Save failed: {e}{RESET}\n")
        return False


# ══════════════════════════════════════════════════════════════════════════════
# MAIN
# ══════════════════════════════════════════════════════════════════════════════
def main():
    banner()
    linex()

    phone = input(f"{W}[{G}•{W}]{G} ENTER NUMBER {W}:{G} ").strip()
    if not phone:
        print(f"{R}[!] Empty number.{RESET}")
        return

    pww = input(f"{W}[{G}•{W}]{G} ENTER PASSWORD {W}:{G} ").strip()
    if not pww:
        print(f"{R}[!] Empty password.{RESET}")
        return

    linex()
    print(f"{W}[{G}•{W}]{G} Logging in as {G}{phone}{W}...\n")

    result = login_wbloks(phone, pww)
    status = result.get("status")
    message = result.get("message", "")

    if status == "SUCCESS":
        uid = result.get("uid", "?")
        cookie = result.get("cookie", "")
        show_success(uid, phone, pww, cookie)
        save_login(uid, pww, cookie, phone)
    else:
        show_failure(status, message, phone)


def loop():
    while True:
        try:
            main()
            print(f"{W}[{G}•{W}]{G} Press Enter to continue (or Ctrl+C to exit)...{W}")
            input()
        except KeyboardInterrupt:
            print(f"\n{R}Exiting...{RESET}")
            sys.exit(0)


if __name__ == "__main__":
    sys.stdout.write('\x1b]2; FB LOGIN\x07')
    loop()