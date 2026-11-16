#include "wifi_link.h"

#include <Arduino.h>
#include <WiFi.h>

namespace wifi_link {

namespace {

constexpr uint32_t kStalledReconnectMs = 20000;

bool wasConnected = false;
uint32_t disconnectedSinceMs = 0;

}  // namespace

void begin() {
  WiFi.onEvent(
      [](arduino_event_id_t, arduino_event_info_t info) {
        auto reason = static_cast<wifi_err_reason_t>(info.wifi_sta_disconnected.reason);
        // STA_LEAVING is the station's own teardown before each retry, not a cause.
        if (reason != WIFI_REASON_STA_LEAVING) {
          Serial.printf("[wifi] link down: %s (%u)\n", WiFi.disconnectReasonName(reason), static_cast<unsigned>(reason));
        }
      },
      ARDUINO_EVENT_WIFI_STA_DISCONNECTED);
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.printf("[wifi] connecting to \"%s\"\n", WIFI_SSID);
}

void maintain(uint32_t nowMs) {
  bool isConnected = WiFi.status() == WL_CONNECTED;
  if (isConnected && !wasConnected) {
    Serial.printf("[wifi] connected, ip %s, rssi %d dBm\n", WiFi.localIP().toString().c_str(), WiFi.RSSI());
  } else if (!isConnected && wasConnected) {
    Serial.println("[wifi] disconnected, reconnecting");
    disconnectedSinceMs = nowMs;
  }
  wasConnected = isConnected;

  // Rule: SPEC-003 #7. Auto reconnect gives up on some disconnect reasons
  // (router power cycle), so a stalled link is restarted instead of rebooting.
  if (!isConnected && nowMs - disconnectedSinceMs >= kStalledReconnectMs) {
    Serial.println("[wifi] still offline, restarting association");
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    disconnectedSinceMs = nowMs;
  }
}

bool connected() {
  return WiFi.status() == WL_CONNECTED;
}

}  // namespace wifi_link
