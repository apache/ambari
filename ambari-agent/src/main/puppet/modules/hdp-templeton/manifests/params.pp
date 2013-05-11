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
class hdp-templeton::params() inherits hdp::params
{
  $templeton_user = $hdp::params::templeton_user

  ###pig and hive tar url connector
  $download_url = $hdp::params::apache_artifacts_download_url

  $dest_pig_tar_name = hdp_default("dest_pig_tar_name","pig.tar.gz")
  $dest_hive_tar_name = hdp_default("dest_hive_tar_name","hive.tar.gz")
  $src_pig_tar_name = hdp_default("src_pig_tar_name","pig.tar.gz")
  $src_hive_tar_name = hdp_default("src_hive_tar_name","hive.tar.gz")

  ### templeton-env
  $conf_dir = hdp_default("conf_dir","/etc/hcatalog/conf")

  ### templeton-env
  $templeton_log_dir = hdp_default("hcat_log_dir","/var/log/webhcat")

  $templeton_pid_dir = hdp_default("hcat_pid_dir","/var/run/webhcat")

#  $templeton_jar_name= hdp_default("templeton_jar_name","templeton-0.1.4.14.jar")
 
#  $hadoop_prefix = hdp_default("hadoop_prefix","/usr")
#  $hive_prefix = hdp_default("hive_prefix","/usr")
  
  ### templeton-site
  $hadoop_conf_dir = hdp_default("webhcat-site/templeton.hadoop.conf.dir")
  $templeton_jar = hdp_default("webhcat-site/templeton.jar","/usr/lib/hcatalog/share/webhcat/svr/webhcat.jar")
  $zookeeper_jar = hdp_default("webhcat-site/zookeeper.jar","/usr/lib/zookeeper/zookeeper.jar")
  $pig_tar_gz = hdp_default("webhcat-site/pig.tar.gz","$dest_pig_tar_name")
  $pig_tar_name_hdfs = hdp_default("webhcat-site/pig.tar.name.hdfs","pig-0.9.2.14")

  $hive_tar_gz = hdp_default("webhcat-site/hive.tar.gz","$dest_hive_tar_name")
  $hive_tar_gz_name = hdp_default("webhcat-site/hive.tar.gz.name","hive-0.9.0.14")
  $hive_metastore_sasl_enabled = hdp_default("webhcat-site/hive.metastore.sasl.enabled",false)

  $templeton_metastore_principal = hdp_default("webhcat-site/templeton.metastore.principal")
  $keytab_path = $hdp::params::keytab_path
  
}
