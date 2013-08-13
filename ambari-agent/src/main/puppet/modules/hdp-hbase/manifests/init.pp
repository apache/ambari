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
  $smokeuser = $hdp::params::smokeuser
  $security_enabled = $hdp::params::security_enabled

  #Configs generation  

  if has_key($configuration, 'hbase-site') {
    configgenerator::configfile{'hbase-site': 
      modulespath => $hdp-hbase::params::conf_dir,
      filename => 'hbase-site.xml',
      module => 'hdp-hbase',
      configuration => $configuration['hbase-site'],
      owner => $hbase_user,
      group => $hdp::params::user_group
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hbase::params::conf_dir}/hbase-site.xml":
      owner => $hbase_user,
      group => $hdp::params::user_group
    }
  }

  if has_key($configuration, 'hdfs-site') {
    configgenerator::configfile{'hdfs-site':
      modulespath => $hdp-hbase::params::conf_dir,
      filename => 'hdfs-site.xml',
      module => 'hdp-hbase',
      configuration => $configuration['hdfs-site'],
      owner => $hbase_user,
      group => $hdp::params::user_group
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hbase::params::conf_dir}/hdfs-site.xml":
      owner => $hbase_user,
      group => $hdp::params::user_group
    }
  }

  if has_key($configuration, 'hbase-policy') {
    configgenerator::configfile{'hbase-policy': 
      modulespath => $hdp-hbase::params::conf_dir,
      filename => 'hbase-policy.xml',
      module => 'hdp-hbase',
      configuration => $configuration['hbase-policy'],
      owner => $hbase_user,
      group => $hdp::params::user_group
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hbase::params::conf_dir}/hbase-policy.xml":
      owner => $hbase_user,
      group => $hdp::params::user_group
    }
  }

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
  
    hdp::directory { $config_dir: 
      service_state => $service_state,
      force => true,
      owner => $hbase_user,
      group => $hdp::params::user_group,
      override_owner => true
    }

   hdp-hbase::configfile { ['hbase-env.sh',  $hdp-hbase::params::metric-prop-file-name ]: 
      type => $type
    }

    hdp-hbase::configfile { 'regionservers':}

    if ($security_enabled == true) {
      if ($type == 'master' and $service_state == 'running') {
        hdp-hbase::configfile { 'hbase_master_jaas.conf' : }
      } elsif ($type == 'regionserver' and $service_state == 'running') {
        hdp-hbase::configfile { 'hbase_regionserver_jaas.conf' : }
      } elsif ($type == 'client') {
        hdp-hbase::configfile { 'hbase_client_jaas.conf' : }
      }
    }
    Anchor['hdp-hbase::begin'] -> Hdp::Package['hbase'] -> Hdp::Directory[$config_dir] ->
    Hdp-hbase::Configfile<||> ->  Anchor['hdp-hbase::end']
  }
}

### config files
define hdp-hbase::configfile(
  $mode = undef,
  $hbase_master_hosts = undef,
  $template_tag = undef,
  $type = undef,
  $conf_dir = $hdp-hbase::params::conf_dir
) 
{
  if ($name == $hdp-hbase::params::metric-prop-file-name) {
    if ($type == 'master') {
      $tag = GANGLIA-MASTER
    } else {
      $tag = GANGLIA-RS
    }
  } else {
    $tag = $template_tag
  }

  hdp::configfile { "${conf_dir}/${name}":
    component         => 'hbase',
    owner             => $hdp-hbase::params::hbase_user,
    mode              => $mode,
    hbase_master_hosts => $hbase_master_hosts,
    template_tag      => $tag
  }
}
