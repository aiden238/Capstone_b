#!/bin/bash
# nginx/ssl/generate-self-signed.sh
# 데모용 self-signed 인증서 생성 스크립트
# 실행: bash nginx/ssl/generate-self-signed.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

openssl req -x509 -nodes \
  -newkey rsa:2048 \
  -keyout "$SCRIPT_DIR/server.key" \
  -out "$SCRIPT_DIR/server.crt" \
  -days 365 \
  -subj "/C=KR/ST=Seoul/L=Seoul/O=TeamBlackbox/OU=Dev/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

echo "Self-signed certificate generated:"
echo "  Key : $SCRIPT_DIR/server.key"
echo "  Cert: $SCRIPT_DIR/server.crt"
echo ""
echo "Valid for 365 days. For production, replace with Let's Encrypt certificates."
