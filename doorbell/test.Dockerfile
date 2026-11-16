FROM alpine:3.24

RUN apk add --no-cache build-base python3 py3-pip \
    && pip install --no-cache-dir --break-system-packages platformio==6.2.0 \
    && adduser -D -u 10001 builder

USER 10001
RUN mkdir -p /home/builder/.platformio
WORKDIR /doorbell
ENTRYPOINT ["pio", "test", "-e", "native"]
