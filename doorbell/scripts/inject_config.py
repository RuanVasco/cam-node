"""Injects the device configuration from environment variables at build time.

Rule: SPEC-003 #1. A pre script instead of `${sysenv.*}` inside `build_flags`:
build_flags is split like a shell line, so an SSID with spaces ("IFRS Alunos")
or a password with quotes breaks the define; StringifyMacro escapes any value.
"""

import os

Import("env")  # noqa: F821 (provided by PlatformIO's SCons)

REQUIRED = ("WIFI_SSID", "WIFI_PASS", "API_URL", "DEVICE_API_KEY")

missing = [name for name in REQUIRED if not os.environ.get(name)]
if missing:
    raise SystemExit("Missing environment variables: " + ", ".join(missing))

if not os.environ["API_URL"].startswith("https://"):
    raise SystemExit("API_URL must start with https:// (SPEC-003 #2)")

button_pin = os.environ.get("BUTTON_PIN", "0")
if not button_pin.isdigit():
    raise SystemExit("BUTTON_PIN must be a GPIO number, got: " + button_pin)

defines = [(name, env.StringifyMacro(os.environ[name])) for name in REQUIRED]  # noqa: F821
defines.append(("BUTTON_PIN", int(button_pin)))
env.Append(CPPDEFINES=defines)  # noqa: F821
