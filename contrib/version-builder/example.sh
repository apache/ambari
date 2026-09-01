#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


filename="version_241-12345.xml"
PYTHON=${PYTHON:-python3}

if ! command -v "$PYTHON" >/dev/null 2>&1 \
  || ! "$PYTHON" -c 'import sys; raise SystemExit(sys.version_info < (3, 9, 2))'; then
  echo "version-builder requires Python 3.9.2 or newer" >&2
  exit 1
fi

rm -f "$filename"

"$PYTHON" version_builder.py --file "$filename" --release-type STANDARD
"$PYTHON" version_builder.py --file "$filename" --release-stack BIGTOP-3.2
"$PYTHON" version_builder.py --file "$filename" --release-version 3.2.1.0
"$PYTHON" version_builder.py --file "$filename" --release-build 12345
"$PYTHON" version_builder.py --file "$filename" --release-notes https://example.com
"$PYTHON" version_builder.py --file "$filename" --release-display BIGTOP-3.2.1.0-12345
"$PYTHON" version_builder.py --file "$filename" --release-compatible '3.2.[0-1].0'

# call any number of times for each service in the repo
"$PYTHON" version_builder.py --file "$filename" --manifest --manifest-id HDFS-321 --manifest-service HDFS --manifest-version 3.3.6 --manifest-release-version 3.2.1.0
"$PYTHON" version_builder.py --file "$filename" --manifest --manifest-id HBASE-321 --manifest-service HBASE --manifest-version 2.4.18

#call any number of times for the target services to upgrade
"$PYTHON" version_builder.py --file "$filename" --available --manifest-id HDFS-321
"$PYTHON" version_builder.py --file "$filename" --available --manifest-id HBASE-321 --release-version 3.2.1

# must be before repo calls
"$PYTHON" version_builder.py --file "$filename" --os --os-family redhat8 --os-package-version 3_2_1_0_12345

#call any number of times for repo per os
"$PYTHON" version_builder.py --file "$filename" --repo --repo-os redhat8 --repo-id BIGTOP-3.2 --repo-name BIGTOP --repo-url https://example.com/bigtop/3.2/redhat8 --repo-unique true


"$PYTHON" version_builder.py --file "$filename" --finalize --xsd ../../ambari-server/src/main/resources/version_definition.xsd

# to upload this to running Ambari instance on localhost:
# curl -u admin:admin -H 'Content-Type: text/xml' -X POST -d @$filename http://localhost:8080/api/v1/version_definitions
