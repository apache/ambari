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
class hdp-zookeeper::params() inherits hdp::params 
{
  $conf_dir = $hdp::params::zk_conf_dir
  $zk_user = $hdp::params::zk_user
  $hostname = $hdp::params::hostname
  
  $zk_log_dir = hdp_default("zk_log_dir","/var/log/zookeeper")
  $zk_data_dir = hdp_default("zk_data_dir","/var/lib/zookeeper/data")
  $zk_pid_dir = hdp_default("zk_pid_dir","/var/run/zookeeper")
  $zk_pid_file = "${zk_pid_dir}/zookeeper_server.pid"
  $zk_server_heapsize = hdp_default("zk_server_heapsize","-Xmx1024m")

  $tickTime = hdp_default("tickTime","2000")
  $initLimit = hdp_default("initLimit","10")
  $syncLimit = hdp_default("syncLimit","5")
  $clientPort = hdp_default("clientPort","2181")

  $zk_primary_name = hdp_default("zookeeper_primary_name", "zookeeper")
  $zk_principal_name = hdp_default("zookeeper_principal_name", "zookeeper/_HOST@EXAMPLE.COM")
  $zk_principal = regsubst($zk_principal_name, '_HOST', $hostname)

  $zk_keytab_path = hdp_default("zookeeper_keytab_path", "${keytab_path}/zk.service.keytab")
  $zk_server_jaas_file = hdp_default("zk_server_jaas_conf_file", "${conf_dir}/zookeeper_jaas.conf")
  $zk_client_jaas_file = hdp_default("zk_client_jaas_conf_file", "${conf_dir}/zookeeper_client_jaas.conf")
}
