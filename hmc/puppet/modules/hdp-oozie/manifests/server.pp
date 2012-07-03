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
class hdp-oozie::server(
  $service_state = $hdp::params::cluster_service_state,
  $setup = false,
  $opts = {}
) inherits  hdp-oozie::params
{   
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    $hdp::params::service_exists['hdp-oozie::server'] = true

    #installs package, creates user, sets configuration
    class{ 'hdp-oozie' : 
      service_state => $service_state,
      server        => true
    } 
  
    Hdp-Oozie::Configfile<||>{oozie_server => $hdp::params::oozie_server}

    class { 'hdp-oozie::hdfs-directories' : 
      service_state => $service_state
    }

    class { 'hdp-oozie::service' :
      ensure       => $service_state,
      setup         => $setup
    }
  
    #top level does not need anchors
    Class['hdp-oozie'] -> Class['hdp-oozie::hdfs-directories'] -> Class['hdp-oozie::service']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-oozie::hdfs-directories($service_state)
{
 $oozie_user = $hdp-oozie::params::oozie_user
  hdp-hadoop::hdfs::directory{ '/user/oozie':
    service_state => $service_state,
    owner => $oozie_user,
    mode  => '770',
    recursive_chmod => true
  }  
}

