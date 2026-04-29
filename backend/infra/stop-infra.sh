#!/bin/bash

# 사주 백엔드 인프라 중단 스크립트

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== 사주 백엔드 인프라 중단 중 ==="

cd "$SCRIPT_DIR"

podman-compose down

echo "=== 인프라 중단 완료 ==="
