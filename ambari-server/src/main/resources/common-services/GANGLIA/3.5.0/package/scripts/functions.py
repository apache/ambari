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
from resource_management import *
from ambari_commons.os_check import OSCheck


def turn_off_autostart(service):
  if OSCheck.is_ubuntu_family():
    Execute(('update-rc.d', service, 'disable'),
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            sudo = True
    )
    Execute(('service', service, 'stop'),
            sudo = True,
            ignore_failures=True,
    )
    File(format('/etc/init/{service}.override'), # disable upstart job
         content = 'manual',
    )
  else:
    Execute(('chkconfig', service, 'off'),
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
            sudo = True,
    )
