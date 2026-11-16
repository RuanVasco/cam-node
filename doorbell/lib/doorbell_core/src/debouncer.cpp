#include "debouncer.h"

Debouncer::Debouncer(uint32_t stableMs) : stableMs_(stableMs) {}

bool Debouncer::pressed(bool rawPressed, uint32_t nowMs) {
  if (rawPressed != rawLevel_) {
    rawLevel_ = rawPressed;
    rawChangedAtMs_ = nowMs;
  }
  if (rawLevel_ == stableLevel_ || nowMs - rawChangedAtMs_ < stableMs_) {
    return false;
  }
  stableLevel_ = rawLevel_;
  return stableLevel_;
}
