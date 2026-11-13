#include "http.h"

#include <curl/curl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    char *data;
    size_t size;
} Buffer;

static size_t append_body(char *chunk, size_t size, size_t count, void *userdata) {
    Buffer *buffer = userdata;
    size_t length = size * count;
    char *grown = realloc(buffer->data, buffer->size + length + 1);
    if (grown == NULL) {
        return 0;
    }
    memcpy(grown + buffer->size, chunk, length);
    buffer->size += length;
    grown[buffer->size] = '\0';
    buffer->data = grown;
    return length;
}

static struct curl_slist *build_headers(const char *const *headers) {
    struct curl_slist *list = NULL;
    for (const char *const *header = headers; header != NULL && *header != NULL; header++) {
        struct curl_slist *appended = curl_slist_append(list, *header);
        if (appended == NULL) {
            curl_slist_free_all(list);
            return NULL;
        }
        list = appended;
    }
    return list;
}

static int perform(CURL *curl, HttpResponse *response) {
    Buffer buffer = {calloc(1, 1), 0};
    if (buffer.data == NULL) {
        return -1;
    }
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, append_body);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &buffer);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);

    CURLcode result = curl_easy_perform(curl);
    if (result != CURLE_OK) {
        fprintf(stderr, "http: %s\n", curl_easy_strerror(result));
        free(buffer.data);
        return -1;
    }
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &response->status_code);
    response->body = buffer.data;
    return 0;
}

int http_get(const char *url, HttpResponse *response) {
    CURL *curl = curl_easy_init();
    if (curl == NULL) {
        return -1;
    }
    curl_easy_setopt(curl, CURLOPT_URL, url);
    int result = perform(curl, response);
    curl_easy_cleanup(curl);
    return result;
}

int http_post_file(const char *url, const char *const *headers, const char *field,
                   const char *file_path, const char *content_type, HttpResponse *response) {
    CURL *curl = curl_easy_init();
    if (curl == NULL) {
        return -1;
    }
    int result = -1;
    struct curl_slist *header_list = NULL;
    curl_mime *mime = curl_mime_init(curl);
    curl_mimepart *part = curl_mime_addpart(mime);

    if (curl_mime_name(part, field) != CURLE_OK || curl_mime_filedata(part, file_path) != CURLE_OK ||
        curl_mime_type(part, content_type) != CURLE_OK) {
        fprintf(stderr, "http: cannot read %s\n", file_path);
        goto cleanup;
    }
    if (headers != NULL && (header_list = build_headers(headers)) == NULL) {
        goto cleanup;
    }

    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_MIMEPOST, mime);
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, header_list);
    result = perform(curl, response);

cleanup:
    curl_slist_free_all(header_list);
    curl_mime_free(mime);
    curl_easy_cleanup(curl);
    return result;
}

void http_response_free(HttpResponse *response) {
    free(response->body);
    response->body = NULL;
}
