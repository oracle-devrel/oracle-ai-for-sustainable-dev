#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 VIDEO.mp4 CAPTIONS.srt BLOG.html" >&2
  exit 2
fi

video="$1"
captions="$2"
blog="$3"

for file in "$video" "$captions" "$blog"; do
  test -s "$file" || { echo "Missing or empty: $file" >&2; exit 1; }
done

if command -v mdls >/dev/null 2>&1; then
  duration="$(mdls -raw -name kMDItemDurationSeconds "$video")"
  width="$(mdls -raw -name kMDItemPixelWidth "$video")"
  height="$(mdls -raw -name kMDItemPixelHeight "$video")"
  test "$duration" != "(null)" || { echo "Video duration metadata is unavailable" >&2; exit 1; }
  test "$width" != "(null)" || { echo "Video width metadata is unavailable" >&2; exit 1; }
  test "$height" != "(null)" || { echo "Video height metadata is unavailable" >&2; exit 1; }
  echo "Video metadata: ${width}x${height}, ${duration}s"
fi

case "$captions" in
  *.srt)
    grep -Eq '^[0-9]+$' "$captions"
    grep -Eq '^[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} --> [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}$' "$captions"
    ;;
  *.vtt)
    grep -Fqx 'WEBVTT' "$captions"
    grep -Eq '^[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3} --> [0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3}$' "$captions"
    ;;
  *)
    echo "Captions must be .srt or .vtt" >&2
    exit 1
    ;;
esac

if grep -Eiq '(password[[:space:]]*=|bearer[[:space:]]+[A-Za-z0-9._-]+|BEGIN (RSA |OPENSSH )?PRIVATE KEY)' "$captions"; then
  echo "Caption secret-pattern check failed" >&2
  exit 1
fi

grep -Fq "$(basename "$video")" "$blog"
grep -Fq "$(basename "$captions")" "$blog"
grep -Eq '<video[^>]*controls' "$blog"
grep -Eq '<track[^>]*kind="captions"' "$blog"

echo "Video, captions, and blog embed audit passed."
