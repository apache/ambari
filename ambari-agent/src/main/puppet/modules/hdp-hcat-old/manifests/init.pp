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
class hdp-hcat(
  $server = false
) 
{
  include hdp-hcat::params

# Configs generation  

  if has_key($configuration, 'hdp_hcat_old__hive_site') {
    configgenerator::configfile{'hive_site_xml': 
      filename => 'hive-site.xml',
      module => 'hdp-hcat-old',
      configuration => $configuration['hdp_hcat_old__hive_site']
    }
  }

  $hcat_user = $hdp::params::hcat_user
  $hcat_config_dir = $hdp-hcat::params::hcat_conf_dir
 
  hdp::package { 'hcat-base' : }
  if ($server == true ) {
    hdp::package { 'hcat-server':} 
    class { 'hdp-hcat::mysql-connector': }
  }
  
  hdp::user{ $hcat_user:}
  
  hdp::directory { $hcat_config_dir: }

  hdp-hcat::configfile { ['hcat-env.sh','hive-env.sh','hive-site.xml']: }
  
  anchor { 'hdp-hcat::begin': } -> Hdp::Package['hcat-base'] -> Hdp::User[$hcat_user] -> 
   Hdp::Directory[$hcat_config_dir] -> Hdp-hcat::Configfile<||> ->  anchor { 'hdp-hcat::end': }

   if ($server == true ) {
     Hdp::Package['hcat-base'] -> Hdp::Package['hcat-server'] ->  Hdp::User[$hcat_user] -> Class['hdp-hcat::mysql-connector'] -> Anchor['hdp-hcat::end']
  }
}

### config files
define hdp-hcat::configfile(
  $mode = undef,
  $hcat_server_host = undef
) 
{
  hdp::configfile { "${hdp-hcat::params::hcat_conf_dir}/${name}":
    component        => 'hcat',
    owner            => $hdp::params::hcat_user,
    mode             => $mode,
    hcat_server_host => $hcat_server_host 
  }
}
