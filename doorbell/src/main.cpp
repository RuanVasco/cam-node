#include <Arduino.h>
#include <esp_task_wdt.h>

#include "camera.h"
#include "debouncer.h"
#include "retry_policy.h"
#include "ring_client.h"
#include "ring_gate.h"
#include "send_timing.h"
#include "wifi_link.h"

namespace {

constexpr uint32_t kDebounceMs = 50;      // Rule: SPEC-003 #3
constexpr uint32_t kCooldownMs = 5000;    // Rule: SPEC-001 #8
constexpr size_t kMaxPhotoBytes = 2 * 1024 * 1024;  // Rule: SPEC-001 #4

Debouncer debouncer(kDebounceMs);
RingGate ringGate(kCooldownMs);

void restartAfter(const char* reason) {
  Serial.printf("[boot] %s, restarting\n", reason);
  Serial.flush();
  delay(2000);
  ESP.restart();
}

void startWatchdog() {
  esp_task_wdt_config_t config = {
      .timeout_ms = send_timing::kWatchdogMs,
      .idle_core_mask = 0,
      .trigger_panic = true,
  };
  esp_task_wdt_reconfigure(&config);
  esp_task_wdt_add(nullptr);
}

void sendRing() {
  if (!wifi_link::connected()) {
    Serial.println("[ring] no wifi, ring dropped");
    return;
  }

  camera_fb_t* photo = camera::captureFresh();
  if (photo == nullptr) {
    Serial.println("[ring] capture failed, ring dropped");
    return;
  }
  if (photo->len > kMaxPhotoBytes) {
    Serial.printf("[ring] photo too large (%u bytes), ring dropped\n", static_cast<unsigned>(photo->len));
    esp_camera_fb_return(photo);
    return;
  }

  for (int attempt = 1;; attempt++) {
    // Each attempt is bounded on its own; a hang inside one still trips the watchdog.
    esp_task_wdt_reset();
    uint32_t startedMs = millis();
    ring_client::Result result = ring_client::send(photo->buf, photo->len);
    Serial.printf("[ring] attempt %d: %s (%u bytes, %lu ms)\n", attempt, result.description.c_str(),
                  static_cast<unsigned>(photo->len), static_cast<unsigned long>(millis() - startedMs));
    if (!retry_policy::shouldRetry(result.status, attempt)) {
      break;
    }
    delay(retry_policy::kRetryDelayMs);
  }

  esp_camera_fb_return(photo);
  ringGate.recordSend(millis());
}

}  // namespace

void setup() {
  Serial.begin(115200);
  Serial.println("[boot] cam-node doorbell");
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  startWatchdog();

  if (!psramFound()) {
    restartAfter("psram not found");
  }
  if (!camera::begin()) {
    restartAfter("camera init failed");
  }
  Serial.printf("[boot] camera ready, button on gpio %d\n", BUTTON_PIN);

  wifi_link::begin();
}

void loop() {
  esp_task_wdt_reset();
  uint32_t nowMs = millis();
  wifi_link::maintain(nowMs);

  bool rawPressed = digitalRead(BUTTON_PIN) == LOW;
  if (debouncer.pressed(rawPressed, nowMs)) {
    if (ringGate.allows(nowMs)) {
      Serial.println("[ring] button pressed");
      sendRing();
    } else {
      Serial.println("[ring] button pressed during cooldown, ignored");
    }
  }
  delay(5);
}
