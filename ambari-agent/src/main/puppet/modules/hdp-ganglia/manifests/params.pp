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
class hdp-ganglia::params() inherits hdp::params
{
  $ganglia_conf_dir = "/etc/ganglia/hdp"
  $ganglia_dir = "/etc/ganglia"
  $ganglia_runtime_dir = "/var/run/ganglia/hdp"

  $ganglia_shell_cmds_dir = hdp_default("ganglia_shell_cmd_dir","/usr/libexec/hdp/ganglia")
  
  $gmetad_user = $hdp::params::gmetad_user
  $gmond_user = $hdp::params::gmond_user

  $webserver_group = hdp_default("webserver_group","apache")
  $rrdcached_default_base_dir = "/var/lib/ganglia/rrds"
  $rrdcached_base_dir = hdp_default("rrdcached_base_dir", "/var/lib/ganglia/rrds")
}
