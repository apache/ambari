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
# limitations under the License

STACKS_FOLDER="/var/lib/ambari-agent/cache/stacks"
STACKS_FOLDER_OLD=/var/lib/ambari-agent/cache/stacks_$(date '+%d_%m_%y_%H_%M').old

COMMON_SERVICES_FOLDER="/var/lib/ambari-agent/cache/common-services"
COMMON_SERVICES_FOLDER_OLD=/var/lib/ambari-agent/cache/common-services_$(date '+%d_%m_%y_%H_%M').old

if [ -d "/etc/ambari-agent/conf.save" ]
then
    mv /etc/ambari-agent/conf.save /etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save
fi

BAK=/etc/ambari-agent/conf/ambari-agent.ini.old
ORIG=/etc/ambari-agent/conf/ambari-agent.ini

BAK_SUDOERS=/etc/sudoers.d/ambari-agent.bak
ORIG_SUDOERS=/etc/sudoers.d/ambari-agent

[ -f $ORIG ] && mv -f $ORIG $BAK
[ -f $ORIG_SUDOERS ] && echo "Moving $ORIG_SUDOERS to $BAK_SUDOERS. Please restore the file if you were using it for ambari-agent non-root functionality" && mv -f $ORIG_SUDOERS $BAK_SUDOERS

if [ -d "$STACKS_FOLDER" ]
then
    mv -f "$STACKS_FOLDER" "$STACKS_FOLDER_OLD"
fi

if [ -d "$COMMON_SERVICES_FOLDER_OLD" ]
then
    mv -f "$COMMON_SERVICES_FOLDER" "$COMMON_SERVICES_FOLDER_OLD"
fi

exit 0
