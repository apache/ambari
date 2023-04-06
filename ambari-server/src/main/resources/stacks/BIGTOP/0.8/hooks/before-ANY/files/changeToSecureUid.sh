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

username=$1
directories=$2

function find_available_uid() {
 for ((i=1001; i<=2000; i++))
 do
   grep -q $i /etc/passwd
   if [ "$?" -ne 0 ]
   then
    newUid=$i
    break
   fi
 done
}

find_available_uid

if [ $newUid -eq 0 ]
then
  echo "Failed to find Uid between 1000 and 2000"
  exit 1
fi

dir_array=($(echo $directories | sed 's/,/\n/g'))
old_uid=$(id -u $username)
echo "Changing uid of $username from $old_uid to $newUid"
echo "Changing directory permisions for ${dir_array[@]}"
usermod -u $newUid $username && for dir in ${dir_array[@]} ; do chown -Rh $newUid $dir ; done
exit 0
