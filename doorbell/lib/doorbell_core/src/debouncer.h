#pragma once

#include <cstdint>

class Debouncer {
 public:
  explicit Debouncer(uint32_t stableMs);

  // Returns true exactly once per press, when the pressed level has held for stableMs.
  bool pressed(bool rawPressed, uint32_t nowMs);

 private:
  uint32_t stableMs_;
  bool rawLevel_ = false;
  bool stableLevel_ = false;
  uint32_t rawChangedAtMs_ = 0;
};
