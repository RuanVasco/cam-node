#pragma once

#include <string>

// The photo bytes go between head and tail, so the JPEG is never copied into a string.
struct PhotoMultipart {
  std::string contentType;
  std::string head;
  std::string tail;
};

PhotoMultipart buildPhotoMultipart(const std::string& boundary);
