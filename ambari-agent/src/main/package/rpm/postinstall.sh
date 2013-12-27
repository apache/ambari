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

if [ "$1" -eq 1 ]; # Action is install
then
  chkconfig --add ambari-agent
fi

if [ "$1" -eq 2 ]; # Action is upgrade
then
  if [ -d "/etc/ambari-agent/conf.save" ]
  then
      cp -f /etc/ambari-agent/conf.save/* /etc/ambari-agent/conf
      mv /etc/ambari-agent/conf.save /etc/ambari-agent/conf_$(date '+%d_%m_%y_%H_%M').save
  fi
fi

chmod 755 /usr/lib/ambari-agent/lib/facter-1.6.10/bin/facter /usr/lib/ambari-agent/lib/puppet-2.7.9/bin/filebucket /usr/lib/ambari-agent/lib/puppet-2.7.9/bin/pi /usr/lib/ambari-agent/lib/puppet-2.7.9/bin/puppet /usr/lib/ambari-agent/lib/puppet-2.7.9/bin/puppetdoc /usr/lib/ambari-agent/lib/puppet-2.7.9/bin/ralsh /usr/lib/ambari-agent/lib/ruby-1.8.7-p370/bin/*

BAK=/etc/ambari-agent/conf/ambari-agent.ini.old
ORIG=/etc/ambari-agent/conf/ambari-agent.ini

if [ -f $BAK ];
then
  SERV_HOST=`grep -e hostname\s*= $BAK | sed -r -e 's/hostname\s*=//' -e 's/\./\\\./g'`
  sed -i -r -e "s/(hostname\s*=).*/\1$SERV_HOST/" $ORIG
  rm $BAK -f
fi
exit 0