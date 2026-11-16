#pragma once

#include <cstddef>
#include <cstdint>

// Durations alternate on and off, starting on.
struct BeepPattern {
  static constexpr size_t kMaxSteps = 3;
  uint16_t stepsMs[kMaxSteps];
  size_t stepCount;
};

namespace beep_pattern {

// Silence after each pattern, so a press beep followed at once by a loss beep
// (no WiFi) is heard as two sounds, not one long one.
constexpr uint16_t kGapMs = 150;

extern const BeepPattern kPressAccepted;  // Rule: SPEC-011 #1
extern const BeepPattern kDelivered;      // Rule: SPEC-011 #3
extern const BeepPattern kLost;           // Rule: SPEC-011 #4

// httpStatus follows HTTPClient: negative values are transport errors.
const BeepPattern& forSendResult(int httpStatus);

}  // namespace beep_pattern
