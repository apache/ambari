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

set -e

metrics_user=ambari-metrics
metrics_group=ambari-metrics
metrics_home=/var/lib/ambari-metrics
nologin_shell=/sbin/nologin

if [ ! -x "$nologin_shell" ]; then
  nologin_shell=/bin/false
fi

if ! getent group "$metrics_group" >/dev/null; then
  groupadd --system "$metrics_group"
fi

if ! getent passwd "$metrics_user" >/dev/null; then
  useradd --system --gid "$metrics_group" --home-dir "$metrics_home" \
    --no-create-home --shell "$nologin_shell" --comment "Ambari Metrics" \
    "$metrics_user"
fi

install -d -m 0750 -o "$metrics_user" -g "$metrics_group" \
  /etc/ambari-metrics \
  /var/lib/ambari-metrics \
  /var/lib/ambari-metrics/victoriametrics \
  /var/log/ambari-metrics \
  /var/run/ambari-metrics
