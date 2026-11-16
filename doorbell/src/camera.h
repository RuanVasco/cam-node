#pragma once

#include <esp_camera.h>

namespace camera {

bool begin();

// Caller must release the frame with esp_camera_fb_return. Returns nullptr on failure.
camera_fb_t* captureFresh();

}  // namespace camera
