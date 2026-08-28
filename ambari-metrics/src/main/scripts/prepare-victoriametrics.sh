#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -euo pipefail

if [[ $# -ne 10 ]]; then
  echo "Usage: $0 VERSION RPM_ARCH CACHE_DIR OUTPUT_DIR BASE_URL CHECKSUMS LICENSE NOTICE STANDALONE_COMMIT CLUSTER_COMMIT" >&2
  exit 2
fi

version=$1
rpm_arch=$2
cache_dir=$3
output_dir=$4
base_url=${5%/}
checksums_file=$6
license_file=$7
notice_file=$8
standalone_commit=$9
cluster_commit=${10}

case "$rpm_arch" in
  x86_64|amd64)
    asset_arch=amd64
    rpm_arch=x86_64
    ;;
  aarch64|arm64)
    asset_arch=arm64
    rpm_arch=aarch64
    ;;
  *)
    echo "Unsupported RPM architecture: $rpm_arch" >&2
    exit 1
    ;;
esac

for command in curl tar awk install; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Required command is unavailable: $command" >&2
    exit 1
  fi
done

if [[ ! -f "$checksums_file" || ! -f "$license_file" || ! -f "$notice_file" ]]; then
  echo "Checksum or license input is missing" >&2
  exit 1
fi

checksum_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    echo "A SHA-256 utility is required" >&2
    return 1
  fi
}

expected_checksum() {
  local kind=$1
  local name=$2
  awk -v arch="$asset_arch" -v kind="$kind" -v name="$name" \
    '$1 == arch && $2 == kind && $4 == name { print $3 }' "$checksums_file"
}

verify_file() {
  local kind=$1
  local name=$2
  local path=$3
  local expected
  local actual

  expected=$(expected_checksum "$kind" "$name")
  if [[ -z "$expected" ]]; then
    echo "No pinned checksum for $asset_arch $kind $name" >&2
    return 1
  fi
  actual=$(checksum_file "$path")
  if [[ "$actual" != "$expected" ]]; then
    echo "SHA-256 mismatch for $name: expected $expected, got $actual" >&2
    return 1
  fi
}

download_asset() {
  local name=$1
  local destination="$cache_dir/$name"
  local temporary="${destination}.part.$$"

  if [[ -f "$destination" ]] && verify_file archive "$name" "$destination"; then
    echo "Using cached VictoriaMetrics asset: $destination"
    return
  fi

  rm -f "$destination" "$temporary"
  echo "Downloading $base_url/$name"
  curl --fail --location --retry 3 --retry-delay 2 --connect-timeout 30 \
    --output "$temporary" "$base_url/$name"
  verify_file archive "$name" "$temporary"
  mv "$temporary" "$destination"
}

validate_archive_paths() {
  local archive=$1
  local member
  while IFS= read -r member; do
    case "$member" in
      /*|../*|*/../*|*/..)
        echo "Unsafe path in $archive: $member" >&2
        return 1
        ;;
    esac
  done < <(tar -tzf "$archive")
}

standalone_asset="victoria-metrics-linux-${asset_arch}-v${version}.tar.gz"
cluster_asset="victoria-metrics-linux-${asset_arch}-v${version}-cluster.tar.gz"
vmutils_asset="vmutils-linux-${asset_arch}-v${version}.tar.gz"

mkdir -p "$cache_dir"
download_asset "$standalone_asset"
download_asset "$cluster_asset"
download_asset "$vmutils_asset"

if [[ -z "$output_dir" || "$output_dir" == "/" ]]; then
  echo "Refusing to use unsafe output directory: $output_dir" >&2
  exit 1
fi

work_dir="${output_dir}.work"
rm -rf "$output_dir" "$work_dir"
mkdir -p "$work_dir/standalone" "$work_dir/cluster" "$work_dir/vmutils"
trap 'rm -rf "$work_dir"' EXIT

for entry in \
  "$standalone_asset:standalone" \
  "$cluster_asset:cluster" \
  "$vmutils_asset:vmutils"; do
  asset=${entry%%:*}
  directory=${entry##*:}
  validate_archive_paths "$cache_dir/$asset"
  tar -xzf "$cache_dir/$asset" -C "$work_dir/$directory"
done

binary_dir="$output_dir/usr/lib/ambari-metrics/providers/victoriametrics/bin"
metadata_dir="$output_dir/usr/share/ambari-metrics/providers/victoriametrics"
licenses_dir="$output_dir/usr/share/licenses/ambari-metrics"
install -d -m 0755 "$binary_dir" "$metadata_dir" "$licenses_dir"

install -m 0755 "$work_dir/standalone/victoria-metrics-prod" "$binary_dir/victoria-metrics-prod"
for binary in vminsert-prod vmselect-prod vmstorage-prod; do
  install -m 0755 "$work_dir/cluster/$binary" "$binary_dir/$binary"
done
for binary in vmagent-prod vmauth-prod; do
  install -m 0755 "$work_dir/vmutils/$binary" "$binary_dir/$binary"
done

for binary in victoria-metrics-prod vminsert-prod vmselect-prod vmstorage-prod vmagent-prod vmauth-prod; do
  verify_file binary "$binary" "$binary_dir/$binary"
done

standalone_sha=$(expected_checksum archive "$standalone_asset")
cluster_sha=$(expected_checksum archive "$cluster_asset")
vmutils_sha=$(expected_checksum archive "$vmutils_asset")
cat > "$metadata_dir/provider.properties" <<EOF
provider.id=victoriametrics
provider.version=$version
provider.architecture=$rpm_arch
provider.release.url=$base_url
provider.standalone.source.commit=$standalone_commit
provider.cluster.source.commit=$cluster_commit
provider.standalone.asset=$standalone_asset
provider.standalone.asset.sha256=$standalone_sha
provider.cluster.asset=$cluster_asset
provider.cluster.asset.sha256=$cluster_sha
provider.vmutils.asset=$vmutils_asset
provider.vmutils.asset.sha256=$vmutils_sha
EOF
chmod 0644 "$metadata_dir/provider.properties"

install -m 0644 "$license_file" "$licenses_dir/LICENSE.txt"
install -m 0644 "$notice_file" "$licenses_dir/NOTICE.txt"
