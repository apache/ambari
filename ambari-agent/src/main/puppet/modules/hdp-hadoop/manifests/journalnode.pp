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
class hdp-hadoop::journalnode(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-hadoop::params 
{

  $hdp::params::service_exists['hdp-hadoop::journalnode'] = true
  
  Hdp-hadoop::Common<||>{service_state => $service_state}
  Hdp-hadoop::Package<||>{include_64_bit => true}
  Hdp-hadoop::Configfile<||>{sizes +> 64}
  
  $jn_edits_dir = $hdp-hadoop::params::jn_edits_dir
  $hdfs_user = $hdp-hadoop::params::hdfs_user
  
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 
  
    #adds package, users and directories, and common hadoop configs
    include hdp-hadoop::initialize
    
    hdp::directory_recursive_create{ $jn_edits_dir:
        service_state => $service_state,
        force => true,
        owner => $hdfs_user
      }
      
    hdp-hadoop::service{ 'journalnode':
      ensure         => $service_state,
      user           => $hdp-hadoop::params::hdfs_user,
      create_pid_dir => true,
      create_log_dir => true
    }
    
    #top level does not need anchors
    Anchor['hdp-hadoop::begin'] -> Hdp::Directory_recursive_create[$jn_edits_dir] -> Hdp-hadoop::Service['journalnode'] -> Anchor['hdp-hadoop::end'] 
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
