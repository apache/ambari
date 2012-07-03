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
class hdp-hive::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-hive::params
{ 

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 

    $hdp::params::service_exists['hdp-hive::server'] = true

    #installs package, creates user, sets configuration
    class{ 'hdp-hive' : 
      service_state => $service_state,
      server        => true
    } 
  
    Hdp-Hive::Configfile<||>{hive_server_host => $hdp::params::host_address}

    class { 'hdp-hive::hdfs-directories' : 
      service_state => $service_state
    }

    class { 'hdp-hive::service' :
      ensure => $service_state
    }
  
    #top level does not need anchors
    Class['hdp-hive'] -> Class['hdp-hive::hdfs-directories'] -> Class['hdp-hive::service']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-hive::hdfs-directories($service_state)
{
  $hive_user = $hdp-hive::params::hive_user
 
  hdp-hadoop::hdfs::directory{ '/apps/hive/warehouse':
    service_state   => $service_state,
    owner            => $hive_user,
    mode             => '777',
    recursive_chmod  => true
  }  
  hdp-hadoop::hdfs::directory{ "/user/${hive_user}":
    service_state => $service_state,
    owner         => $hive_user
  }
}
