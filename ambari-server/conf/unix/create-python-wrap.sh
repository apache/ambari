#!/bin/bash
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

PYTHON_WRAPER_DIR="${ROOT}/usr/bin/"
PYTHON_WRAPER_TARGET="${PYTHON_WRAPER_DIR}/ambari-python-wrap"

AMBARI_PYTHON=""
python_binaries=("/usr/bin/python3.9" "/usr/bin/python3")
for python_binary in "${python_binaries[@]}"
do
  if [ -x "$python_binary" ] && "$python_binary" -c "import sys; sys.exit(sys.version_info < (3, 9, 2))" >/dev/null 2>&1; then
    AMBARI_PYTHON="$python_binary"
    break
  fi
done

if [ -z "$AMBARI_PYTHON" ] ; then
  >&2 echo "Cannot detect Python 3.9.2 or newer for Ambari. Please install a supported Python runtime or manually set $PYTHON_WRAPER_TARGET."
  exit 1
fi

mkdir -p "$PYTHON_WRAPER_DIR"
TEMPORARY_WRAPPER="${PYTHON_WRAPER_TARGET}.tmp.$$"
rm -f "$TEMPORARY_WRAPPER"
ln -s "$AMBARI_PYTHON" "$TEMPORARY_WRAPPER"
if ! mv -f "$TEMPORARY_WRAPPER" "$PYTHON_WRAPER_TARGET"; then
  rm -f "$TEMPORARY_WRAPPER"
  exit 1
fi
