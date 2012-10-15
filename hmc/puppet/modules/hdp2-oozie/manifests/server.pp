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
class hdp2-oozie::server(
  $service_state = $hdp2::params::cluster_service_state,
  $setup = false,
  $opts = {}
) inherits  hdp2-oozie::params
{   
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    $hdp2::params::service_exists['hdp2-oozie::server'] = true

    #installs package, creates user, sets configuration
    class{ 'hdp2-oozie' : 
      service_state => $service_state,
      server        => true
    } 
  
    Hdp2-Oozie::Configfile<||>{oozie_server => $hdp2::params::oozie_server}

    class { 'hdp2-oozie::hdfs-directories' : 
      service_state => $service_state
    }

    class { 'hdp2-oozie::service' :
      ensure       => $service_state,
      setup         => $setup
    }
  
    #top level does not need anchors
    Class['hdp2-oozie'] -> Class['hdp2-oozie::hdfs-directories'] -> Class['hdp2-oozie::service']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp2-oozie::hdfs-directories($service_state)
{
 $oozie_user = $hdp2-oozie::params::oozie_user
  hdp2-hadoop::hdfs::directory{ '/user/oozie':
    service_state => $service_state,
    owner => $oozie_user,
    mode  => '775',
    recursive_chmod => true
  }  
}

