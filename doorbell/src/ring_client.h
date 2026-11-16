#pragma once

#include <Arduino.h>

#include <cstddef>
#include <cstdint>

namespace ring_client {

struct Result {
  // HTTP status, or a negative HTTPClient error for transport failures.
  int status;
  String description;
};

Result send(const uint8_t* jpeg, size_t length);

}  // namespace ring_client
