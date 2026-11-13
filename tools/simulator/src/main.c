#include <stdio.h>
#include <stdlib.h>

#include "lib/http/http.h"

#define RINGS_PATH "/api/v1/rings"
#define DEVICE_KEY_HEADER "X-Device-Key: "

static const char *required_env(const char *name) {
    const char *value = getenv(name);
    if (value == NULL || value[0] == '\0') {
        fprintf(stderr, "missing environment variable %s\n", name);
        exit(EXIT_FAILURE);
    }
    return value;
}

static char *format(const char *first, const char *second) {
    int length = snprintf(NULL, 0, "%s%s", first, second);
    char *text = malloc((size_t)length + 1);
    if (text == NULL) {
        exit(EXIT_FAILURE);
    }
    snprintf(text, (size_t)length + 1, "%s%s", first, second);
    return text;
}

/* Usage: simulator [photo.jpg]. Without the argument, PHOTO_PATH is used. Rule: SPEC-001 #1, #2. */
int main(int argc, char **argv) {
    const char *api_url = required_env("API_URL");
    const char *device_key = required_env("DEVICE_API_KEY");
    const char *photo_path = argc > 1 ? argv[1] : required_env("PHOTO_PATH");

    char *url = format(api_url, RINGS_PATH);
    char *key_header = format(DEVICE_KEY_HEADER, device_key);
    const char *headers[] = {key_header, NULL};

    HttpResponse response;
    int sent = http_post_file(url, headers, "photo", photo_path, "image/jpeg", &response);
    free(url);
    free(key_header);
    if (sent != 0) {
        return EXIT_FAILURE;
    }

    printf("%ld\n%s\n", response.status_code, response.body);
    int created = response.status_code == 201;
    http_response_free(&response);
    return created ? EXIT_SUCCESS : EXIT_FAILURE;
}
