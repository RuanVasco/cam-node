#include "ring_client.h"

#include <HTTPClient.h>
#include <NetworkClientSecure.h>

#include <cstring>
#include <memory>

#include "multipart.h"

namespace ring_client {

namespace {

constexpr char kRingsUrl[] = API_URL "/api/v1/rings";
constexpr char kBoundary[] = "camnode-doorbell-photo";
// SPEC-003 CA-1 gives 5 s from press to screen; a slower request already missed it.
constexpr int32_t kConnectTimeoutMs = 4000;
constexpr uint16_t kResponseTimeoutMs = 4000;

struct PsramDeleter {
  void operator()(uint8_t* buffer) const { free(buffer); }
};

String transportError(NetworkClientSecure& client, int status) {
  char tlsError[100] = {};
  if (client.lastError(tlsError, sizeof(tlsError)) != 0) {
    return String("tls error: ") + tlsError;
  }
  return String("network error: ") + HTTPClient::errorToString(status);
}

}  // namespace

Result send(const uint8_t* jpeg, size_t length) {
  PhotoMultipart multipart = buildPhotoMultipart(kBoundary);
  size_t bodyLength = multipart.head.size() + length + multipart.tail.size();
  std::unique_ptr<uint8_t, PsramDeleter> body(static_cast<uint8_t*>(ps_malloc(bodyLength)));
  if (!body) {
    return {HTTPC_ERROR_TOO_LESS_RAM, "network error: no memory for request body"};
  }
  uint8_t* cursor = body.get();
  memcpy(cursor, multipart.head.data(), multipart.head.size());
  cursor += multipart.head.size();
  memcpy(cursor, jpeg, length);
  cursor += length;
  memcpy(cursor, multipart.tail.data(), multipart.tail.size());

  // Rule: SPEC-003 #2. Server certificate checked against the core's CA bundle, never setInsecure.
  NetworkClientSecure client;
  client.useBuiltinCACertBundle();

  HTTPClient http;
  http.setConnectTimeout(kConnectTimeoutMs);
  http.setTimeout(kResponseTimeoutMs);
  if (!http.begin(client, kRingsUrl)) {
    return {HTTPC_ERROR_CONNECTION_REFUSED, "network error: invalid API_URL"};
  }
  http.addHeader("X-Device-Key", DEVICE_API_KEY);
  http.addHeader("Content-Type", multipart.contentType.c_str());

  int status = http.POST(body.get(), bodyLength);
  Result result = {status, status > 0 ? String("http ") + status : transportError(client, status)};
  http.end();
  return result;
}

}  // namespace ring_client
