#ifndef HTTP_H
#define HTTP_H

typedef struct {
    char *body;
    long status_code;
} HttpResponse;

/*
 * Both calls return 0 when an HTTP response arrived (any status) and -1 on a transport
 * error, so the caller decides what each status means. On success the caller must
 * release the response with http_response_free.
 */
int http_get(const char *url, HttpResponse *response);

/* headers: NULL-terminated list of "Name: value" lines, or NULL. */
int http_post_file(const char *url, const char *const *headers, const char *field,
                   const char *file_path, const char *content_type, HttpResponse *response);

void http_response_free(HttpResponse *response);

#endif
