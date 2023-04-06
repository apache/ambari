#!/usr/bin/env bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
conf_dir=$1
data=$2
hbase_cmd=$3
echo "scan 'ambarismoketest'" | $hbase_cmd --config $conf_dir shell > /tmp/hbase_chk_verify
cat /tmp/hbase_chk_verify
echo "Looking for $data"
tr -d '\n|\t| ' < /tmp/hbase_chk_verify | grep -q $data
if [ "$?" -ne 0 ]
then
  exit 1
fi

grep -q '1 row(s)' /tmp/hbase_chk_verify
