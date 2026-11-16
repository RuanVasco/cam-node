#include "multipart.h"

PhotoMultipart buildPhotoMultipart(const std::string& boundary) {
  return {
      "multipart/form-data; boundary=" + boundary,
      "--" + boundary +
          "\r\n"
          "Content-Disposition: form-data; name=\"photo\"; filename=\"ring.jpg\"\r\n"
          "Content-Type: image/jpeg\r\n"
          "\r\n",
      "\r\n--" + boundary + "--\r\n",
  };
}
