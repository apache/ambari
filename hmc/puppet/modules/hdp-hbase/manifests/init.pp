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
class hdp-hbase(
  $type,
  $service_state) 
{
  include hdp-hbase::params
 
  $hbase_user = $hdp-hbase::params::hbase_user
  $config_dir = $hdp-hbase::params::conf_dir
  
  $hdp::params::component_exists['hdp-hbase'] = true

  anchor{'hdp-hbase::begin':}
  anchor{'hdp-hbase::end':}

  if ($service_state == 'uninstalled') {
    hdp::package { 'hbase':
      ensure => 'uninstalled'
    }
    hdp::directory { $config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp-hbase::begin'] -> Hdp::Package['hbase'] -> Hdp::Directory[$config_dir] -> Anchor['hdp-hbase::end']

  } else {  
    hdp::package { 'hbase': }
  
    hdp::user{ $hbase_user:}
 
    hdp::directory { $config_dir: 
      service_state => $service_state,
      force => true
    }

   hdp-hbase::configfile { ['hbase-env.sh','hbase-site.xml','hbase-policy.xml','log4j.properties','hadoop-metrics.properties']: 
      type => $type
    }
    hdp-hbase::configfile { 'regionservers':}
    Anchor['hdp-hbase::begin'] -> Hdp::Package['hbase'] -> Hdp::User[$hbase_user] -> Hdp::Directory[$config_dir] -> 
    Hdp-hbase::Configfile<||> ->  Anchor['hdp-hbase::end']
  }
}

### config files
define hdp-hbase::configfile(
  $mode = undef,
  $hbase_master_host = undef,
  $template_tag = undef,
  $type = undef,
) 
{
  if ($name == 'hadoop-metrics.properties') {
    if ($type == 'master') {
    $tag = GANGLIA-MASTER
  } else {
     $tag = GANGLIA-RS
  }
   } else {
    $tag = $template_tag
}
  hdp::configfile { "${hdp-hbase::params::conf_dir}/${name}":
    component         => 'hbase',
    owner             => $hdp-hbase::params::hbase_user,
    mode              => $mode,
    hbase_master_host => $hbase_master_host,
    template_tag      => $tag
  }
}
