#pragma once

#include <cstdint>

class RingGate {
 public:
  explicit RingGate(uint32_t cooldownMs);

  bool allows(uint32_t nowMs) const;
  void recordSend(uint32_t nowMs);

 private:
  uint32_t cooldownMs_;
  bool hasSent_ = false;
  uint32_t lastSendMs_ = 0;
};
