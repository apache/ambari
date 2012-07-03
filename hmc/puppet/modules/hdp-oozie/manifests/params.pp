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
class hdp-oozie::params() inherits hdp::params
{
  $oozie_user = $hdp::params::oozie_user 

  ###ext url
  $download_url = $hdp::params::gpl_artifacts_download_url
  $ext_zip_url = "${download_url}/ext-2.2.zip"
  $ext_zip_name = hdp_default("ext_zip_name","ext-2.2.zip")

  ### oozie-env
  $conf_dir = $hdp::params::oozie_conf_dir
  $hadoop_prefix = hdp_default("hadoop_prefix","/usr")

  ### oozie-env
  $oozie_log_dir = hdp_default("hadoop/oozie-env/oozie_log_dir","/var/log/oozie")

  $oozie_pid_dir = hdp_default("oozie_pid_dir","/var/run/oozie/")
  $oozie_pid_file = hdp_default("hadoop/oozie-env/oozie_pid_file","$oozie_pid_dir/oozie.pid")

  $oozie_data_dir = hdp_default("hadoop/oozie-env/oozie_data_dir","/var/data/oozie")

  $oozie_tmp_dir = hdp_default("hadoop/oozie-env/oozie_tmp_dir","/var/tmp/oozie")

  $oozie_lib_dir = hdp_default("hadoop/oozie-env/oozie_lib_dir","/var/lib/oozie/")
  ### oozie-site
  $oozie_sasl_enabled = hdp_default("hadoop/oozie-site/oozie_sasl_enabled","false")
  $oozie_security_type = hdp_default("hadoop/oozie-site/oozie_security_type","simple")
  $realm = hdp_default("hadoop/oozie-site/realm","EXAMPLE.COM")
  $keytab_path = hdp_default("hadoop/oozie-site/keytab_path","/etc/security/keytabs/")
}
