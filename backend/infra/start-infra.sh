#!/bin/bash
set -e

echo "=========================================="
echo "  사주 백엔드 인프라 시작"
echo "=========================================="

cd "$(dirname "$0")"

echo ""
echo "[1/5] Podman 컨테이너 시작 중..."
podman-compose up -d

echo ""
echo "[2/5] MySQL 준비 대기 중..."
for i in {1..30}; do
    if podman exec saju-mysql mysqladmin ping -h localhost -u root -prootpassword --silent 2>/dev/null; then
        echo "✓ MySQL 준비 완료"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "✗ MySQL 시작 실패 (타임아웃)"
        exit 1
    fi
    echo "  대기 중... ($i/30)"
    sleep 2
done

echo ""
echo "[3/5] 데이터베이스 초기화..."
if podman exec -i saju-mysql mysql -u root -prootpassword < db/init-saju-db.sql 2>/dev/null; then
    echo "✓ 데이터베이스 초기화 완료"
else
    echo "⚠ 데이터베이스 초기화 실패 (이미 존재할 수 있음)"
fi

echo ""
echo "[4/5] LocalStack 준비 대기 중..."
for i in {1..30}; do
    if curl -sf http://localhost:4566/_localstack/health > /dev/null 2>&1; then
        echo "✓ LocalStack 준비 완료"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "✗ LocalStack 시작 실패 (타임아웃)"
        exit 1
    fi
    echo "  대기 중... ($i/30)"
    sleep 2
done

echo ""
echo "[5/5] LocalStack 리소스 초기화..."
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-northeast-2

# S3 버킷 생성 (중복 무시)
aws --endpoint-url=http://localhost:4566 s3api create-bucket \
    --bucket saju-local \
    --region ap-northeast-2 \
    --create-bucket-configuration LocationConstraint=ap-northeast-2 2>/dev/null || true
echo "  ✓ S3 버킷 saju-local 준비"

# Secrets Manager: DB 자격증명
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret \
    --name saju/local/db \
    --secret-string '{"username":"saju","password":"saju1234"}' 2>/dev/null || \
aws --endpoint-url=http://localhost:4566 secretsmanager update-secret \
    --secret-id saju/local/db \
    --secret-string '{"username":"saju","password":"saju1234"}' 2>/dev/null || true
echo "  ✓ Secret saju/local/db 준비"

echo ""
echo "=========================================="
echo "  ✓ 인프라 준비 완료!"
echo "=========================================="
echo ""
echo "실행 중인 서비스:"
echo "  - MySQL:      localhost:3306"
echo "  - LocalStack: localhost:4566"
echo ""
echo "다음 단계:"
echo "  백엔드 서버 실행: cd .. && ./start-server.sh"
echo ""
