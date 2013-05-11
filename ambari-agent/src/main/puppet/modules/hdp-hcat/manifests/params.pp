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
  $hcat_conf_dir = $hdp::params::hcat_conf_dir

  $hcat_metastore_port = hdp_default("hcat_metastore_port",9933)
  $hcat_lib = hdp_default("hcat_lib","/usr/lib/hcatalog/share/hcatalog") #TODO: should I remove and just use hcat_dbroot

  ### hcat-env
  $hcat_dbroot = hdp_default("hcat_dbroot",$hcat_lib)

  $hcat_log_dir = hdp_default("hcat_log_dir","/var/log/hcatalog")

  $hcat_pid_dir = hdp_default("hcat_pid_dir","/var/run/hcatalog")

  $keytab_path = hdp_default("keytab_path","/etc/security/keytabs")
}
