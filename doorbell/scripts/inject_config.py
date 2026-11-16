"""Injects the device configuration from the environment at build time.

Rule: SPEC-003 #1. A pre script instead of `${sysenv.*}` inside `build_flags`:
build_flags is split like a shell line, so an SSID with spaces ("IFRS Alunos")
or a password with quotes breaks the define; StringifyMacro escapes any value.
Values come from the process environment first, then from doorbell/.env
(gitignored), so secrets never need to be typed on a shared command line.
"""

import os

Import("env")  # noqa: F821 (provided by PlatformIO's SCons)

REQUIRED = ("WIFI_SSID", "WIFI_PASS", "API_URL", "DEVICE_API_KEY")


def read_dotenv(path):
    values = {}
    if not os.path.isfile(path):
        return values
    with open(path, encoding="utf-8-sig") as file:
        for line in file:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


dotenv = read_dotenv(os.path.join(env.subst("$PROJECT_DIR"), ".env"))  # noqa: F821
config = {name: os.environ.get(name) or dotenv.get(name, "") for name in REQUIRED + ("BUTTON_PIN",)}

missing = [name for name in REQUIRED if not config[name]]
if missing:
    raise SystemExit("Missing configuration (environment or doorbell/.env): " + ", ".join(missing))

if not config["API_URL"].startswith("https://"):
    raise SystemExit("API_URL must start with https:// (SPEC-003 #2)")

config["API_URL"] = config["API_URL"].rstrip("/")

button_pin = config["BUTTON_PIN"] or "0"
if not button_pin.isdigit():
    raise SystemExit("BUTTON_PIN must be a GPIO number, got: " + button_pin)

defines = [(name, env.StringifyMacro(config[name])) for name in REQUIRED]  # noqa: F821
defines.append(("BUTTON_PIN", int(button_pin)))
env.Append(CPPDEFINES=defines)  # noqa: F821
