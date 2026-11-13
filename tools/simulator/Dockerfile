FROM alpine:3.24 AS build

RUN apk add --no-cache build-base curl-dev

WORKDIR /src
COPY src/ .

RUN mkdir -p /build && gcc -std=c17 -O2 -Wall -Wextra -Werror -o /build/simulator $(find . -name "*.c") -lcurl

FROM alpine:3.24

RUN apk add --no-cache libcurl && adduser -D -H -u 10001 simulator

COPY --from=build /build/simulator /usr/local/bin/simulator

USER 10001
ENTRYPOINT ["simulator"]
