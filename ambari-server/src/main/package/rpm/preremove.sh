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

# WARNING: This script is performed not only on uninstall, but also
# during package update. See http://www.ibm.com/developerworks/library/l-rpm2/
# for details

if [ "$1" -eq 0 ]; then  # Action is uninstall
    /usr/sbin/ambari-server stop > /dev/null 2>&1
    if [ -d "/etc/ambari-server/conf.save" ]; then
        mv /etc/ambari-server/conf.save /etc/ambari-server/conf_$(date '+%d_%m_%y_%H_%M').save
    fi

    if [ -e "/usr/sbin/ambari-server" ]; then
        # Remove link created during install
        rm /usr/sbin/ambari-server
    fi

    mv /etc/ambari-server/conf /etc/ambari-server/conf.save

    if [ -f "/var/lib/ambari-server/install-helper.sh" ]; then
      /var/lib/ambari-server/install-helper.sh remove
    fi

    chkconfig --list | grep ambari-server && chkconfig --del ambari-server
fi

exit 0
