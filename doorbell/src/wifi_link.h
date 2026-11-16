#pragma once

#include <cstdint>

namespace wifi_link {

void begin();

// Logs link changes and restarts the association when auto reconnect stalls.
void maintain(uint32_t nowMs);

bool connected();

}  // namespace wifi_link
