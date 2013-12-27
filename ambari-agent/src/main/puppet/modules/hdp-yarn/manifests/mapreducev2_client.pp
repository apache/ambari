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

class hdp-yarn::mapreducev2_client(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-yarn::params
{
  if ($service_state == 'no_op') {
  } elsif ($service_state in 'installed_and_configured') {

    include hdp-yarn::initialize

    hdp-yarn::package{'hadoop-mapreduce-client':}

    hdp::configfile {"${hdp::params::limits_conf_dir}/mapreduce.conf":
      component => 'yarn',
      owner => 'root',
      group => 'root',
      mode => 644,
      require => Hdp-yarn::Package['hadoop-mapreduce-client']
    }

  }
}
