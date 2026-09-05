#!/usr/bin/env bash
set -euo pipefail

dockerfile=${1:?dockerfile path is required}
output_ref_file=${2:?Tilt output ref file is required}
image_repository=${3:?image repository is required}

for command in kubectl docker; do
  command -v "$command" >/dev/null || {
    echo "missing required command: $command" >&2
    exit 1
  }
done

pod=$(kubectl -n buildkit get pods \
  -l app.kubernetes.io/name=buildkitd \
  --field-selector=status.phase=Running \
  -o jsonpath='{.items[0].metadata.name}')
[[ -n "$pod" ]] || {
  echo "BuildKit is not ready in the buildkit namespace" >&2
  exit 1
}

builder=lumie-remote-buildkit
endpoint="kube-pod://${pod}?namespace=buildkit&container=buildkitd"
image_ref="${image_repository}:tilt-$(date -u +%Y%m%d%H%M%S)-${RANDOM}"

docker buildx rm --force "$builder" >/dev/null 2>&1 || true
docker buildx create --name "$builder" --driver remote "$endpoint" >/dev/null
docker buildx build \
  --builder "$builder" \
  --platform linux/arm64 \
  --file "$dockerfile" \
  --tag "$image_ref" \
  --push \
  .

printf '%s\n' "$image_ref" > "$output_ref_file"
