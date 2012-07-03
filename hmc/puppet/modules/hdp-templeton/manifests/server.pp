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
class hdp-templeton::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-templeton::params
{  

  if ($service_state == 'no_op') { 
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
  $hdp::params::service_exists['hdp-templeton::server'] = true

  class{ 'hdp-templeton' :
    service_state => $service_state,
    server        => true
  }

  class { 'hdp-templeton::hdfs-directories' : 
    service_state => $service_state
  }

  class { 'hdp-templeton::copy-hdfs-directories' :
    service_state => $service_state
  }

  class { 'hdp-templeton::service' :
    ensure       => $service_state,
  }
  
  #top level does not need anchors
  Class['hdp-templeton'] -> Class['hdp-templeton::hdfs-directories'] -> Class['hdp-templeton::copy-hdfs-directories'] -> Class['hdp-templeton::service']
  } else { 
  hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-templeton::hdfs-directories($service_state)
{
 $templeton_user = $hdp-templeton::params::templeton_user
 #TODO: need to make sure that hdfs service is running
  hdp-hadoop::hdfs::directory{ '/user/templeton':
    service_state => $service_state,
    owner => $templeton_user,
    mode  => '755',
    recursive_chmod => true
  }  

  hdp-hadoop::hdfs::directory{ '/apps/templeton':
    service_state => $service_state,
    owner => $templeton_user,
    mode  => '755',
    recursive_chmod => true  
  }
}


class hdp-templeton::copy-hdfs-directories($service_state)
{
 $templeton_user = $hdp-templeton::params::templeton_user
 $pig_src_tar = "$hdp::params::artifact_dir/pig.tar.gz"

  hdp-hadoop::hdfs::copyfromlocal { '/usr/share/templeton/templeton*jar':
    service_state => $service_state,
    owner => $hdp-templeton::params::templeton_user,
    mode  => '755',
    dest_dir => '/apps/templeton/ugi.jar'
  }
  hdp-hadoop::hdfs::copyfromlocal { '/usr/lib/hadoop/contrib/streaming/hadoop-streaming*.jar':
   service_state => $service_state,
   owner => $hdp-templeton::params::templeton_user,
   mode  => '755',
   dest_dir => '/apps/templeton/hadoop-streaming.jar'
 }
  #TODO: Use ${hdp::params::artifact_dir}/${hdp-templeton::params::pig_tar_name} instead
  hdp-hadoop::hdfs::copyfromlocal { '/tmp/HDP-artifacts/pig.tar.gz' :
    service_state => $service_state,
    owner => $hdp-templeton::params::templeton_user,
    mode  => '755',
    dest_dir => '/apps/templeton/pig.tar.gz'
  }
  #TODO: Use ${hdp::params::artifact_dir}/${hdp-templeton::params::hive_tar_name} instead
  hdp-hadoop::hdfs::copyfromlocal { '/tmp/HDP-artifacts/hive.tar.gz' :
    service_state => $service_state,
    owner => $hdp-templeton::params::templeton_user,
    mode  => '755',
    dest_dir => '/apps/templeton/hive.tar.gz'
  }
}
