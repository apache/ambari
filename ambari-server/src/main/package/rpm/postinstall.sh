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

if [ -e "/usr/sbin/ambari-server" ]; then # Check is needed for upgrade
    # Remove link created by previous package version
    rm -f /usr/sbin/ambari-server
fi

ln -s /etc/init.d/ambari-server /usr/sbin/ambari-server

case "$1" in
  1) # Action install
    if [ -f "/var/lib/ambari-server/install-helper.sh" ]; then
        /var/lib/ambari-server/install-helper.sh install
    fi
    chkconfig --add ambari-server
  ;;
  2) # Action upgrade
    if [ -f "/var/lib/ambari-server/install-helper.sh" ]; then
        /var/lib/ambari-server/install-helper.sh upgrade
    fi
  ;;
esac

exit 0
