FROM alpine:latest AS builder

RUN apk add --no-cache build-base

WORKDIR /src
COPY src/ .

RUN mkdir -p /build && find . -name "*.c" | xargs gcc -Wall -Wextra -o /build/app.exe
