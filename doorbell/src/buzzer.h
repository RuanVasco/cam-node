#pragma once

#include "beep_pattern.h"

namespace buzzer {

// Without BUZZER_PIN (-1) both calls do nothing. Rule: SPEC-011 #6
void begin();

// Queues the pattern and returns at once; a task plays it while the loop is blocked on HTTPS.
void play(const BeepPattern& pattern);

}  // namespace buzzer
