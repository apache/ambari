#!/usr/bin/env bash
#
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

# Requires a CPython 3.9.2+ interpreter with venv support.

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd -P)
BUILD_VENV="$SCRIPT_DIR/target/ambari-python-build-venv"
BUILD_PYTHON="$BUILD_VENV/bin/python"
WHEELHOUSE=""

function print_help() {
  cat << EOF
   Usage: ./install-ambari-python.sh [additional options]

   -c, --clean                  clean generated python distribution directories
   -d, --deploy                 deploy ambari-python artifact to maven remote repository
   -v, --version <version>      override ambari-python artifact versison
   -i, --repository-id <id>     repository id in settings.xml for remote repository
   -r, --repository-url <url>   repository url of remote repository
   -w, --wheelhouse <path>      install locked build tools from an offline wheelhouse
   -h, --help                   print help
EOF
}

function get_python_artifact_file() {
  local artifacts=()
  while IFS= read -r artifact; do
    artifacts+=("$artifact")
  done < <(find "$SCRIPT_DIR/dist" -maxdepth 1 -type f \
    \( -name 'ambari-python-*.tar.gz' -o -name 'ambari_python-*.tar.gz' \) \
    -print 2>/dev/null)
  if [[ ${#artifacts[@]} -ne 1 ]]; then
    echo "Expected exactly one ambari-python source distribution" >&2
    return 1
  fi
  basename "${artifacts[0]}"
}

function get_version() {
  local artifact_file
  artifact_file=$(get_python_artifact_file)
  local artifact_version=${artifact_file#ambari-python-}
  artifact_version=${artifact_version#ambari_python-}
  echo "${artifact_version%.tar.gz}"
}

function clean() {
  if [[ -d "$SCRIPT_DIR/dist" ]]; then
    echo "Removing '$SCRIPT_DIR/dist' directoy ..."
    rm -r "$SCRIPT_DIR/dist"
    echo "Directory '$SCRIPT_DIR/dist' successfully deleted."
  fi
  if [[ -d "$SCRIPT_DIR/ambari_python.egg-info" ]]; then
    echo "Removing '$SCRIPT_DIR/ambari_python.egg-info' directoy ..."
    rm -r "$SCRIPT_DIR/ambari_python.egg-info"
    echo "Directory '$SCRIPT_DIR/ambari_python.egg-info' successfully deleted."
  fi
  if [[ -d "$SCRIPT_DIR/target/ambari-python-dist" ]]; then
    echo "Removing '$SCRIPT_DIR/target/ambari-python' directoy ..."
    rm -r "$SCRIPT_DIR/target/ambari-python-dist"
    echo "Directory '$SCRIPT_DIR/target/ambari-python' successfully deleted."
  fi
  if [[ -d "$BUILD_VENV" ]]; then
    echo "Removing '$BUILD_VENV' directory ..."
    rm -r "$BUILD_VENV"
  fi
}

function select_python() {
  local candidates=()
  if [[ -n ${AMBARI_PYTHON:-} ]]; then
    candidates+=("$AMBARI_PYTHON")
  fi
  candidates+=(/usr/bin/python3.9 /usr/local/bin/python3 python3)

  local candidate
  for candidate in "${candidates[@]}"; do
    if command -v "$candidate" >/dev/null 2>&1 \
        && "$candidate" -c 'import sys; raise SystemExit(sys.version_info < (3, 9, 2))'; then
      AMBARI_PYTHON=$(command -v "$candidate")
      return 0
    fi
  done
  echo "A Python 3.9.2+ interpreter is required; set AMBARI_PYTHON explicitly" >&2
  return 1
}

function prepare_build_environment() {
  mkdir -p "$SCRIPT_DIR/target"
  "$AMBARI_PYTHON" -m venv "$BUILD_VENV"
  local pip_args=(
    install
    --disable-pip-version-check
    --only-binary=:all:
    --require-hashes
    --requirement "$SCRIPT_DIR/requirements-build.lock"
  )
  if [[ -n "$WHEELHOUSE" ]]; then
    pip_args+=(--no-index --find-links "$WHEELHOUSE")
  fi
  "$BUILD_PYTHON" -m pip "${pip_args[@]}"
}

function generate_site_packages() {
  local artifact="$1"
  local destination="$SCRIPT_DIR/target/ambari-python-dist/site-packages"
  mkdir -p "$destination"
  "$BUILD_PYTHON" -m pip install \
    "$SCRIPT_DIR/dist/$artifact" \
    --disable-pip-version-check \
    --no-build-isolation \
    --no-compile \
    --no-deps \
    --target "$destination"
}

function archive_python_dist() {
  local artifact="$1"
  local base_dir="$SCRIPT_DIR/target/ambari-python-dist"
  if [[ -f "$SCRIPT_DIR/target/$artifact" ]]; then
    echo "Removing '$SCRIPT_DIR/target/$artifact' file ..."
    echo "File '$SCRIPT_DIR/target/$artifact' successfully deleted."
  fi
  tar -zcf "$SCRIPT_DIR/target/$artifact" -C "$base_dir" site-packages
}

function install() {
  local artifact_file="$1"
  local version="$2"
  mvn install:install-file -Dfile="$artifact_file" -DgeneratePom=true -Dversion="$version" -DartifactId=ambari-python -DgroupId=org.apache.ambari -Dpackaging=tar.gz
}

function deploy() {
  local artifact_file="$1"
  local version="$2"
  local repo_id="$3"
  local repo_url="$4"
  mvn gpg:sign-and-deploy-file -Dfile="$artifact_file" -Dpackaging=tar.gz -DgeneratePom=true -Dversion="$version" -DartifactId=ambari-python -DgroupId=org.apache.ambari -Durl="$repo_url" -DrepositoryId="$repo_id"
}

function build_sdist() {
  local version="$1"
  if [[ -n "$version" ]]; then
    (cd "$SCRIPT_DIR" && env AMBARI_VERSION="$version" "$BUILD_PYTHON" -m build --sdist --no-isolation)
  else
    (cd "$SCRIPT_DIR" && "$BUILD_PYTHON" -m build --sdist --no-isolation)
  fi
}

function main() {
  local DEPLOY="false"
  local CLEAN="false"
  local VERSION=""
  local REPOSITORY_ID=""
  local REPOSITORY_URL=""
  while [[ $# -gt 0 ]]
    do
      key="$1"
      case $key in
        -d|--deploy)
          DEPLOY="true"
          shift 1
        ;;
        -c|--clean)
          CLEAN="true"
          shift 1
        ;;
        -v|--version)
          VERSION="$2"
          shift 2
        ;;
        -i|--repository-id)
          REPOSITORY_ID="$2"
          shift 2
        ;;
        -r|--repository-url)
          REPOSITORY_URL="$2"
          shift 2
        ;;
        -w|--wheelhouse)
          WHEELHOUSE="$2"
          shift 2
        ;;
        -h|--help)
          shift 1
          print_help
          exit 0
        ;;
        *)
          echo "Unknown option: $1"
          exit 1
        ;;
      esac
  done

  clean
  if [[ "$CLEAN" == "true" ]]; then
    return 0
  fi

  select_python
  if [[ -n "$WHEELHOUSE" && ! -d "$WHEELHOUSE" ]]; then
    echo "Wheelhouse does not exist: $WHEELHOUSE" >&2
    return 1
  fi
  prepare_build_environment
  build_sdist "$VERSION"
  local artifact_name
  artifact_name=$(get_python_artifact_file)
  local artifact_version
  artifact_version=$(get_version)

  generate_site_packages "$artifact_name"
  archive_python_dist "$artifact_name"

  install "$SCRIPT_DIR/target/$artifact_name" "$artifact_version"

  if [[ "$DEPLOY" == "true" ]] ; then
    if [[ -z "$REPOSITORY_ID" ]] ; then
      echo "Repository id option is required  for deploying ambari-python artifact (-i or --repository-id)"
      exit 1
    fi
    if [[ -z "$REPOSITORY_URL" ]] ; then
      echo "Repository url option is required for deploying ambari-python artifact (-r or --repository-url)"
      exit 1
    fi
    deploy "$SCRIPT_DIR/target/$artifact_name" "$artifact_version" "$REPOSITORY_ID" "$REPOSITORY_URL"
  else
    echo "Skip deploying ambari-python artifact to remote repository."
  fi
}

main "$@"
