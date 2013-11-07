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
class hdp-yarn::params(
) inherits hdp-hadoop::params 
{

  $conf_dir = $hdp::params::yarn_conf_dir
  $stack_version = $hdp::params::stack_version
  $smoke_test_user = $hdp::params::smokeuser
  ## security params
  $security_enabled = $hdp::params::security_enabled
  $smoke_user_keytab = $hdp::params::smokeuser_keytab
  $kinit_cmd = "${hdp::params::kinit_path_local} -kt ${smoke_user_keytab} ${smoke_test_user};"
  $rm_host = $hdp::params::rm_host
  $rm_port = $hdp::rm_port
  $rm_https_port = $hdp::rm_https_port

  ## yarn-env 
  $hadoop_libexec_dir = $hdp-hadoop::params::hadoop_libexec_dir
  $hadoop_yarn_home = hdp_default("hadoop_yarn_home","/usr/lib/hadoop-yarn")
  $yarn_heapsize = hdp_default("yarn_heapsize","1024")
  $resourcemanager_heapsize = hdp_default("resourcemanager_heapsize","1024")
  $nodemanager_heapsize = hdp_default("nodemanager_heapsize","1024")

  $yarn_log_dir_prefix = hdp_default("yarn_log_dir_prefix","/var/log/hadoop-yarn")
  $yarn_pid_dir_prefix = hdp_default("yarn_pid_dir_prefix","/var/run/hadoop-yarn")
  
  ## yarn-site
  $rm_webui_address = "${rm_host}:${rm_port}"
  $rm_webui_https_address = "${rm_host}:${rm_https_port}"
  $nm_webui_address = hdp_default("yarn-site/yarn.nodemanager.webapp.address", "0.0.0.0:8042")
  $hs_webui_address = hdp_default("mapred-site/mapreduce.jobhistory.webapp.address", "0.0.0.0:19888")
  
  $nm_local_dirs = hdp_default("yarn-site/yarn.nodemanager.local-dirs", "${hadoop_tmp_dir}/nm-local-dir")
  $nm_log_dirs = hdp_default("yarn-site/yarn.nodemanager.log-dirs", "/var/log/hadoop-yarn/yarn")

  ##smoke test configs
  $distrAppJarName = "hadoop-yarn-applications-distributedshell-2.*.jar"
  $hadoopMapredExamplesJarName = "hadoop-mapreduce-examples-2.*.jar"
}
