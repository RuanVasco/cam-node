#pragma once

#include <cstdint>

// Sized from the IF network log: a successful send took up to 17 s and 4 s
// connects were cut at 4005 ms, so tighter limits drop rings that would land.
namespace send_timing {

constexpr uint32_t kConnectTimeoutMs = 8000;
// NetworkClientSecure takes the handshake limit in whole seconds.
constexpr uint32_t kHandshakeTimeoutS = 10;
constexpr uint32_t kResponseTimeoutMs = 8000;
// Rule: SPEC-003 #9. Fed before each attempt, so it only has to cover one.
constexpr uint32_t kWatchdogMs = 45000;

// The core checks the handshake limit only between reads, and each read can
// block for the socket timeout, which the core sets to the connect timeout.
constexpr uint32_t worstAttemptMs() {
  return kConnectTimeoutMs + kHandshakeTimeoutS * 1000 + kConnectTimeoutMs + kResponseTimeoutMs;
}

}  // namespace send_timing
