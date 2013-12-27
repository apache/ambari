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

if [ -d "/etc/ambari-agent/conf.save" ]
then
    mv /etc/ambari-agent/conf.save /etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save
fi

getent group puppet >/dev/null || groupadd -r puppet
getent passwd puppet >/dev/null || /usr/sbin/useradd -g puppet -M -d / puppet

BAK=/etc/ambari-agent/conf/ambari-agent.ini.old
ORIG=/etc/ambari-agent/conf/ambari-agent.ini

[ -f $ORIG ] && mv -f $ORIG $BAK

exit 0
