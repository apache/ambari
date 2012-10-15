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
class hdp2-hive::server(
  $service_state = $hdp2::params::cluster_service_state,
  $opts = {}
) inherits  hdp2-hive::params
{ 

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 

    $hdp2::params::service_exists['hdp2-hive::server'] = true

    #installs package, creates user, sets configuration
    class{ 'hdp2-hive' : 
      service_state => $service_state,
      server        => true
    } 
  
    Hdp2-Hive::Configfile<||>{hive_server_host => $hdp2::params::host_address}

    class { 'hdp2-hive::hdfs-directories' : 
      service_state => $service_state
    }

    class { 'hdp2-hive::service' :
      ensure => $service_state
    }
  
    #top level does not need anchors
    Class['hdp2-hive'] -> Class['hdp2-hive::hdfs-directories'] -> Class['hdp2-hive::service']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp2-hive::hdfs-directories($service_state)
{
  $hive_user = $hdp2-hive::params::hive_user
 
  hdp2-hadoop::hdfs::directory{ '/apps/hive/warehouse':
    service_state   => $service_state,
    owner            => $hive_user,
    mode             => '777',
    recursive_chmod  => true
  }  
  hdp2-hadoop::hdfs::directory{ "/user/${hive_user}":
    service_state => $service_state,
    owner         => $hive_user
  }
}
