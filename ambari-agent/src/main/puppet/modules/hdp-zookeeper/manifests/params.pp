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
  
  $zk_log_dir = hdp_default("zk_log_dir","/var/log/zookeeper")
  $zk_data_dir = hdp_default("zk_data_dir","/var/lib/zookeeper/data")
  $zk_pid_dir = hdp_default("zk_pid_dir","/var/run/zookeeper")
  $zk_pid_file = "${zk_pid_dir}/zookeeper_server.pid"


  $tickTime = hdp_default("tickTime","2000")
  $initLimit = hdp_default("initLimit","10")
  $syncLimit = hdp_default("syncLimit","5")
  $clientPort = hdp_default("clientPort","2181")

  $zk_keytab_path = hdp_default("hadoop/zk-site/zk.service.keytab","${keytab_path}/zk.keytab.file")
  $zk_principal = hdp_default("hadoop/zk-site/zk.kerberos.prinicipal", "zk/_HOST@${kerberos_domain}")

  $zk_server_jaas_file = hdp_default("hadoop/zk-site/zk_server_jaas_conf_file", "${conf_dir}/zookeeper_jaas.conf")
  $zk_client_jaas_file = hdp_default("hadoop/zk-site/zk_client_jaas_conf_file", "${conf_dir}/zookeeper_client_jaas.conf")
}
