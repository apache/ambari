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
class hdp-ganglia::hdp-gmond::service_check() 
{
  
  anchor { 'hdp-ganglia::hdp-gmond::service_check::begin':}

  exec { 'hdp-gmond':
    command   => "/etc/init.d/hdp-gmond status | grep -v failed",
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    before      => Anchor['hdp-ganglia::hdp-gmond::service_check::end'],
    logoutput => "true"
  }

  anchor{ 'hdp-ganglia::hdp-gmond::service_check::end':}
}
