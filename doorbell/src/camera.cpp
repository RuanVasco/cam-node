#include "camera.h"

namespace camera {

namespace {

// GOOUUU ESP32-S3-CAM V1.5 shares the ESP32-S3-EYE camera wiring.
camera_config_t boardConfig() {
  camera_config_t config = {};
  config.pin_pwdn = -1;
  config.pin_reset = -1;
  config.pin_xclk = 15;
  config.pin_sccb_sda = 4;
  config.pin_sccb_scl = 5;
  config.pin_d7 = 16;
  config.pin_d6 = 17;
  config.pin_d5 = 18;
  config.pin_d4 = 12;
  config.pin_d3 = 10;
  config.pin_d2 = 8;
  config.pin_d1 = 9;
  config.pin_d0 = 11;
  config.pin_vsync = 6;
  config.pin_href = 7;
  config.pin_pclk = 13;
  config.xclk_freq_hz = 20000000;
  config.ledc_timer = LEDC_TIMER_0;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.pixel_format = PIXFORMAT_JPEG;
  // Rule: SPEC-003 #5. SVGA at quality 12 shows a face at 1 m in roughly 30 to 80 KB.
  config.frame_size = FRAMESIZE_SVGA;
  config.jpeg_quality = 12;
  config.fb_count = 1;
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  return config;
}

void discardFrame() {
  camera_fb_t* frame = esp_camera_fb_get();
  if (frame != nullptr) {
    esp_camera_fb_return(frame);
  }
}

}  // namespace

bool begin() {
  camera_config_t config = boardConfig();
  if (esp_camera_init(&config) != ESP_OK) {
    return false;
  }
  // The first frames after power-up come out with unsettled exposure.
  for (int i = 0; i < 3; i++) {
    discardFrame();
  }
  return true;
}

camera_fb_t* captureFresh() {
  // Rule: SPEC-003 #4. With GRAB_WHEN_EMPTY the buffer holds a frame taken
  // when the previous one was returned, possibly minutes ago.
  discardFrame();
  return esp_camera_fb_get();
}

}  // namespace camera
