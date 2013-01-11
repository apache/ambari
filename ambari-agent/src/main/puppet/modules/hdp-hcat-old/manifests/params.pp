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
class hdp-hcat::params() inherits hdp::params
{

  #TODO: will move to globals
  $hcat_metastore_user_name = hdp_default("hadoop/hive-site/hcat_metastore_user_name","dbusername")
  $hcat_metastore_user_passwd = hdp_default("hadoop/hive-site/hcat_metastore_user_passwd","dbpassword")
 
 ####### users
 
  
  ### common
  $hcat_metastore_port = hdp_default("hcat_metastore_port",9933)
  $hcat_lib = hdp_default("hcat_lib","/usr/share/hcatalog/lib") #TODO: should I remove and just use hcat_dbroot

  ### hcat-env
  $hcat_conf_dir = hdp_default("hadoop/hcat-env/hcat_conf_dir","/etc/hcatalog")

  $hcat_dbroot = hdp_default("hadoop/hcat-env/hcat_dbroot",$hcat_lib)

  $hcat_log_dir = hdp_default("hadoop/hcat-env/hcat_log_dir","/var/log/hcatalog")

  $hcat_pid_dir = hdp_default("hadoop/hcat-env/hcat_pid_dir","/var/run/hcatalog")
#  $hcat_pid_dir = "${hcat_piddirprefix}/${hdp::params::hcat_user}"
  
  ### hive-site
  $hcat_database_name = hdp_default("hadoop/hive-site/hcat_database_name","hive")

  $hcat_metastore_principal = hdp_default("hadoop/hive-site/hcat_metastore_principal")

  $hcat_metastore_sasl_enabled = hdp_default("hadoop/hive-site/hcat_metastore_sasl_enabled",false)

  #TODO: using instead hcat_server_host in hdp::params $hcat_metastore_server_host = hdp_default("hadoop/hive-site/hcat_metastore_server_host")

  $keytab_path = hdp_default("hadoop/hive-site/keytab_path")
  
  ###mysql connector
  $download_url = $hdp::params::gpl_artifacts_download_url
  $mysql_connector_url = "${download_url}/mysql-connector-java-5.1.18.zip"
}
