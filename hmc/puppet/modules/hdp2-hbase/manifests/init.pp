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
class hdp2-hbase(
  $type,
  $service_state) 
{
  include hdp2-hbase::params
 
  $hbase_user = $hdp2-hbase::params::hbase_user
  $config_dir = $hdp2-hbase::params::conf_dir
  
  $hdp2::params::component_exists['hdp2-hbase'] = true

  anchor{'hdp2-hbase::begin':}
  anchor{'hdp2-hbase::end':}

  if ($service_state == 'uninstalled') {
    hdp2::package { 'hbase':
      ensure => 'uninstalled'
    }
    hdp2::directory { $config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp2-hbase::begin'] -> Hdp2::Package['hbase'] -> Hdp2::Directory[$config_dir] -> Anchor['hdp2-hbase::end']

  } else {  
    hdp2::package { 'hbase': }
  
    hdp2::user{ $hbase_user:}
 
    hdp2::directory { $config_dir: 
      service_state => $service_state,
      force => true
    }

   hdp2-hbase::configfile { ['hbase-env.sh','hbase-site.xml','hbase-policy.xml','log4j.properties','hadoop-metrics.properties']: 
      type => $type
    }
    hdp2-hbase::configfile { 'regionservers':}
    Anchor['hdp2-hbase::begin'] -> Hdp2::Package['hbase'] -> Hdp2::User[$hbase_user] -> Hdp2::Directory[$config_dir] -> 
    Hdp2-hbase::Configfile<||> ->  Anchor['hdp2-hbase::end']
  }
}

### config files
define hdp2-hbase::configfile(
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
  hdp2::configfile { "${hdp2-hbase::params::conf_dir}/${name}":
    component         => 'hbase',
    owner             => $hdp2-hbase::params::hbase_user,
    mode              => $mode,
    hbase_master_host => $hbase_master_host,
    template_tag      => $tag
  }
}
