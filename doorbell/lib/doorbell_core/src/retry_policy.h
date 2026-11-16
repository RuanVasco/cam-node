#pragma once

#include <cstdint>

namespace retry_policy {

constexpr int kMaxAttempts = 2;
constexpr uint32_t kRetryDelayMs = 2000;

// httpStatus follows HTTPClient: negative values are transport errors.
bool shouldRetry(int httpStatus, int attemptsMade);

}  // namespace retry_policy
