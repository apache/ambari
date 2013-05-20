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
    
  ## yarn-env 
  $hadoop_libexec_dir = hdp_default("yarn/yarn-env/hadoop_libexec_dir","/usr/lib/hadoop/libexec")
  
  $hadoop_common_home = hdp_default("yarn/yarn-env/hadoop_common_home","/usr/lib/hadoop")
  $hadoop_hdfs_home = hdp_default("yarn/yarn-env/hadoop_hdfs_home","/usr/lib/hadoop-hdfs")
  $hadoop_mapred_home = hdp_default("yarn/yarn-env/hadoop_mapred_home","/usr/lib/hadoop-yarn")
  $hadoop_yarn_home = hdp_default("yarn/yarn-env/hadoop_yarn_home","/usr/lib/hadoop-yarn")
  
  $yarn_log_dir_prefix = hdp_default("hadoop/yarn-env/yarn_log_dir_prefix","/var/log/hadoop-yarn")
  $yarn_pid_dir_prefix = hdp_default("hadoop/yarn-env/yarn_pid_dir_prefix","/var/run/hadoop-yarn")
  
  ## yarn-site
  $rm_webui_port = hdp_default("yarn-site/yarn.resourcemanager.webapp.address", "8088")
  $nm_webui_port = hdp_default("yarn-site/yarn.nodemanager.webapp.address", "8042")
  $hs_webui_port = hdp_default("yarn-site/mapreduce.jobhistory.address", "19888")

}
