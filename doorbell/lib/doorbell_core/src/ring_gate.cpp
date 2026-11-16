#include "ring_gate.h"

RingGate::RingGate(uint32_t cooldownMs) : cooldownMs_(cooldownMs) {}

bool RingGate::allows(uint32_t nowMs) const {
  return !hasSent_ || nowMs - lastSendMs_ >= cooldownMs_;
}

void RingGate::recordSend(uint32_t nowMs) {
  hasSent_ = true;
  lastSendMs_ = nowMs;
}
