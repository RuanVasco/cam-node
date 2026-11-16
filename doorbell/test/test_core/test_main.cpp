#include <unity.h>

#include "beep_pattern.h"
#include "debouncer.h"
#include "multipart.h"
#include "retry_policy.h"
#include "ring_gate.h"
#include "send_timing.h"

void setUp() {}
void tearDown() {}

void test_spec003_r3_noise_shorter_than_50ms_is_not_a_press() {
  Debouncer debouncer(50);
  TEST_ASSERT_FALSE(debouncer.pressed(true, 0));
  TEST_ASSERT_FALSE(debouncer.pressed(true, 30));
  TEST_ASSERT_FALSE(debouncer.pressed(false, 40));
  TEST_ASSERT_FALSE(debouncer.pressed(true, 45));
  TEST_ASSERT_FALSE(debouncer.pressed(true, 90));
}

void test_spec003_r3_level_stable_for_50ms_is_one_press() {
  Debouncer debouncer(50);
  TEST_ASSERT_FALSE(debouncer.pressed(true, 0));
  TEST_ASSERT_FALSE(debouncer.pressed(true, 49));
  TEST_ASSERT_TRUE(debouncer.pressed(true, 50));
  TEST_ASSERT_FALSE(debouncer.pressed(true, 500));
}

void test_spec003_r3_release_then_press_again_is_a_new_press() {
  Debouncer debouncer(50);
  debouncer.pressed(true, 0);
  TEST_ASSERT_TRUE(debouncer.pressed(true, 50));
  debouncer.pressed(false, 100);
  TEST_ASSERT_FALSE(debouncer.pressed(false, 150));
  debouncer.pressed(true, 200);
  TEST_ASSERT_TRUE(debouncer.pressed(true, 250));
}

void test_spec003_r3_works_across_millis_wraparound() {
  Debouncer debouncer(50);
  debouncer.pressed(true, UINT32_MAX - 20);
  TEST_ASSERT_TRUE(debouncer.pressed(true, 30));
}

void test_spec001_r8_presses_within_5s_after_send_are_ignored() {
  RingGate gate(5000);
  TEST_ASSERT_TRUE(gate.allows(0));
  gate.recordSend(1000);
  TEST_ASSERT_FALSE(gate.allows(1500));
  TEST_ASSERT_FALSE(gate.allows(2000));
  TEST_ASSERT_FALSE(gate.allows(5999));
  TEST_ASSERT_TRUE(gate.allows(6000));
}

void test_spec001_r8_first_press_after_boot_is_allowed_even_near_zero() {
  RingGate gate(5000);
  TEST_ASSERT_TRUE(gate.allows(10));
}

void test_spec003_r6_transport_error_and_5xx_retry_once() {
  TEST_ASSERT_TRUE(retry_policy::shouldRetry(-1, 1));
  TEST_ASSERT_TRUE(retry_policy::shouldRetry(503, 1));
  TEST_ASSERT_FALSE(retry_policy::shouldRetry(-1, 2));
  TEST_ASSERT_FALSE(retry_policy::shouldRetry(503, 2));
  TEST_ASSERT_EQUAL_UINT32(2000, retry_policy::kRetryDelayMs);
}

void test_spec003_r6_success_and_4xx_never_retry() {
  TEST_ASSERT_FALSE(retry_policy::shouldRetry(201, 1));
  TEST_ASSERT_FALSE(retry_policy::shouldRetry(401, 1));
  TEST_ASSERT_FALSE(retry_policy::shouldRetry(413, 1));
}

void test_spec001_contract_multipart_carries_jpeg_in_photo_field() {
  PhotoMultipart body = buildPhotoMultipart("b0und");
  TEST_ASSERT_EQUAL_STRING("multipart/form-data; boundary=b0und", body.contentType.c_str());
  TEST_ASSERT_EQUAL_STRING(
      "--b0und\r\n"
      "Content-Disposition: form-data; name=\"photo\"; filename=\"ring.jpg\"\r\n"
      "Content-Type: image/jpeg\r\n"
      "\r\n",
      body.head.c_str());
  TEST_ASSERT_EQUAL_STRING("\r\n--b0und--\r\n", body.tail.c_str());
}

void test_spec003_r9_one_slow_attempt_fits_the_watchdog_with_room_for_dns() {
  // Between two feeds the loop runs the retry pause (or the capture) plus one attempt;
  // DNS lookup is not bounded by the attempt limits, so it needs its own slack.
  constexpr uint32_t kDnsSlackMs = 5000;
  TEST_ASSERT_LESS_OR_EQUAL_UINT32(send_timing::kWatchdogMs,
                                   retry_policy::kRetryDelayMs + send_timing::worstAttemptMs() + kDnsSlackMs);
}

void test_spec003_r9_handshake_is_bounded_well_below_the_core_default() {
  constexpr uint32_t kCoreDefaultHandshakeS = 120;
  TEST_ASSERT_LESS_THAN_UINT32(kCoreDefaultHandshakeS, send_timing::kHandshakeTimeoutS);
  TEST_ASSERT_GREATER_THAN_UINT32(0, send_timing::kHandshakeTimeoutS);
}

uint32_t totalOnMs(const BeepPattern& pattern) {
  uint32_t total = 0;
  for (size_t step = 0; step < pattern.stepCount; step += 2) {
    total += pattern.stepsMs[step];
  }
  return total;
}

void test_spec011_r1_press_beep_is_one_short_tone() {
  TEST_ASSERT_EQUAL_UINT32(1, beep_pattern::kPressAccepted.stepCount);
  TEST_ASSERT_EQUAL_UINT16(100, beep_pattern::kPressAccepted.stepsMs[0]);
}

void test_spec011_r3_delivered_is_two_short_tones() {
  const BeepPattern& delivered = beep_pattern::kDelivered;
  TEST_ASSERT_EQUAL_UINT32(3, delivered.stepCount);
  TEST_ASSERT_EQUAL_UINT16(80, delivered.stepsMs[0]);
  TEST_ASSERT_EQUAL_UINT16(80, delivered.stepsMs[1]);
  TEST_ASSERT_EQUAL_UINT16(80, delivered.stepsMs[2]);
}

void test_spec011_r4_lost_is_one_tone_much_longer_than_the_press_beep() {
  TEST_ASSERT_EQUAL_UINT32(1, beep_pattern::kLost.stepCount);
  TEST_ASSERT_EQUAL_UINT16(600, beep_pattern::kLost.stepsMs[0]);
  TEST_ASSERT_GREATER_OR_EQUAL_UINT32(4 * totalOnMs(beep_pattern::kPressAccepted), totalOnMs(beep_pattern::kLost));
}

void test_spec011_r4_patterns_back_to_back_stay_separate_sounds() {
  TEST_ASSERT_GREATER_OR_EQUAL_UINT16(beep_pattern::kDelivered.stepsMs[1], beep_pattern::kGapMs);
}

void test_spec011_r3_r4_only_2xx_counts_as_delivered() {
  TEST_ASSERT_EQUAL_PTR(&beep_pattern::kDelivered, &beep_pattern::forSendResult(200));
  TEST_ASSERT_EQUAL_PTR(&beep_pattern::kDelivered, &beep_pattern::forSendResult(201));
  TEST_ASSERT_EQUAL_PTR(&beep_pattern::kLost, &beep_pattern::forSendResult(-1));
  TEST_ASSERT_EQUAL_PTR(&beep_pattern::kLost, &beep_pattern::forSendResult(0));
  TEST_ASSERT_EQUAL_PTR(&beep_pattern::kLost, &beep_pattern::forSendResult(301));
  TEST_ASSERT_EQUAL_PTR(&beep_pattern::kLost, &beep_pattern::forSendResult(401));
  TEST_ASSERT_EQUAL_PTR(&beep_pattern::kLost, &beep_pattern::forSendResult(503));
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_spec003_r3_noise_shorter_than_50ms_is_not_a_press);
  RUN_TEST(test_spec003_r3_level_stable_for_50ms_is_one_press);
  RUN_TEST(test_spec003_r3_release_then_press_again_is_a_new_press);
  RUN_TEST(test_spec003_r3_works_across_millis_wraparound);
  RUN_TEST(test_spec001_r8_presses_within_5s_after_send_are_ignored);
  RUN_TEST(test_spec001_r8_first_press_after_boot_is_allowed_even_near_zero);
  RUN_TEST(test_spec003_r6_transport_error_and_5xx_retry_once);
  RUN_TEST(test_spec003_r6_success_and_4xx_never_retry);
  RUN_TEST(test_spec001_contract_multipart_carries_jpeg_in_photo_field);
  RUN_TEST(test_spec003_r9_one_slow_attempt_fits_the_watchdog_with_room_for_dns);
  RUN_TEST(test_spec003_r9_handshake_is_bounded_well_below_the_core_default);
  RUN_TEST(test_spec011_r1_press_beep_is_one_short_tone);
  RUN_TEST(test_spec011_r3_delivered_is_two_short_tones);
  RUN_TEST(test_spec011_r4_lost_is_one_tone_much_longer_than_the_press_beep);
  RUN_TEST(test_spec011_r4_patterns_back_to_back_stay_separate_sounds);
  RUN_TEST(test_spec011_r3_r4_only_2xx_counts_as_delivered);
  return UNITY_END();
}
