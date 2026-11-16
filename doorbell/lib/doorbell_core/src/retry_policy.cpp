#include "retry_policy.h"

namespace retry_policy {

bool shouldRetry(int httpStatus, int attemptsMade) {
  bool transient = httpStatus < 0 || httpStatus >= 500;
  return transient && attemptsMade < kMaxAttempts;
}

}  // namespace retry_policy
