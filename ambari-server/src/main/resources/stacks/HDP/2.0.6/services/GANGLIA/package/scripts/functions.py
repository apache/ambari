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


def turn_off_autostart(service):
  if System.get_instance().os_family == "ubuntu":
    Execute(format("update-rc.d {service} disable"),
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    )
    Execute(format("service {service} stop"), ignore_failures=True)
    Execute(format("echo 'manual' > /etc/init/{service}.override")) # disbale upstart job
  else:
    Execute(format("chkconfig {service} off"),
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    )
