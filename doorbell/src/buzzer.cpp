#include "buzzer.h"

#include <Arduino.h>

namespace buzzer {

#if BUZZER_PIN >= 0

namespace {

QueueHandle_t queue = nullptr;

void playNow(const BeepPattern& pattern) {
  for (size_t step = 0; step < pattern.stepCount; step++) {
    digitalWrite(BUZZER_PIN, step % 2 == 0 ? HIGH : LOW);
    vTaskDelay(pdMS_TO_TICKS(pattern.stepsMs[step]));
  }
  digitalWrite(BUZZER_PIN, LOW);
  vTaskDelay(pdMS_TO_TICKS(beep_pattern::kGapMs));
}

void run(void*) {
  const BeepPattern* pattern = nullptr;
  for (;;) {
    if (xQueueReceive(queue, &pattern, portMAX_DELAY) == pdTRUE) {
      playNow(*pattern);
    }
  }
}

}  // namespace

void begin() {
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  queue = xQueueCreate(4, sizeof(const BeepPattern*));
  xTaskCreate(run, "buzzer", 2048, nullptr, 1, nullptr);
}

void play(const BeepPattern& pattern) {
  if (queue == nullptr) {
    return;
  }
  const BeepPattern* item = &pattern;
  xQueueSend(queue, &item, 0);
}

#else

void begin() {}
void play(const BeepPattern&) {}

#endif

}  // namespace buzzer
