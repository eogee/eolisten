#!/usr/bin/env bash
# EoListen 正式包构建：unsigned 构建 → zipalign → apksigner v1+v2 双签名
# 为什么必须有 v1 (JAR) 签名：minSdk>=24 时 AGP 自动签名只打 v2，系统安装器能装，
# 但应用宝等国产安装器用老式 JAR 解析，缺 v1 直接报"安装文件损坏"。
# 为什么验证要加 --min-sdk-version 23：apksigner verify 在 minSdk>=24 时跳过 v1
# 校验并显示 false（意思是"不参与验证"而非"无效"），传 23 才会真正校验 v1。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID="$ROOT/android"
export JAVA_HOME="${JAVA_HOME:-C:/ProgramMine/tools/jdk21}"
BT="C:/ProgramMine/tools/android-sdk/build-tools/36.0.0"
KS="$ROOT/eolisten.keystore"

PROPS="$ANDROID/keystore.properties"
[ -f "$PROPS" ] || { echo "missing $PROPS (storeFile/storePassword/keyAlias/keyPassword)"; exit 1; }
storeFile=$(grep '^storeFile=' "$PROPS" | cut -d= -f2-)
storePassword=$(grep '^storePassword=' "$PROPS" | cut -d= -f2-)
keyAlias=$(grep '^keyAlias=' "$PROPS" | cut -d= -f2-)
keyPassword=$(grep '^keyPassword=' "$PROPS" | cut -d= -f2-)
[ -n "$storeFile" ] && KS="$storeFile"

VERSION=$(grep -o 'versionName "[^"]*"' "$ANDROID/app/build.gradle" | head -1 | cut -d'"' -f2)
OUT="$ROOT/EoListen-$VERSION.apk"
UNSIGNED="$ANDROID/app/build/outputs/apk/release/app-release-unsigned.apk"

echo "==> gradlew assembleRelease (unsigned)"
(cd "$ANDROID" && JAVA_HOME="$JAVA_HOME" ./gradlew assembleRelease -q)
[ -f "$UNSIGNED" ] || { echo "not found: $UNSIGNED"; exit 1; }

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

echo "==> zipalign (must run before v2 signing)"
"$BT/zipalign.exe" -f 4 "$UNSIGNED" "$WORK/aligned.apk"

echo "==> apksigner v1+v2"
"$BT/apksigner.bat" sign --ks "$KS" --ks-key-alias "$keyAlias" \
  --ks-pass "pass:$storePassword" --key-pass "pass:$keyPassword" \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$OUT" "$WORK/aligned.apk"

echo "==> verify (min-sdk 23 forces v1 verification)"
"$BT/apksigner.bat" verify --verbose --min-sdk-version 23 "$OUT" | head -4
"$BT/zipalign.exe" -c 4 "$OUT" && echo "zipalign OK"
echo "==> output: $OUT ($(du -h "$OUT" | cut -f1))"
md5sum "$OUT"
