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
class hdp-hadoop::glusterfs_client(
  $service_state = $hdp::params::cluster_client_state,
  $opts = {}
) inherits hdp-hadoop::params
{
  $hdp::params::service_exists['hdp-hadoop::glusterfs_client'] = true
  Hdp-hadoop::Common<||>{service_state => $service_state}
  Hdp-hadoop::Package<||>{include_64_bit => true}
  Hdp-hadoop::Configfile<||>{sizes +> 64}
  
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
  	#adds package, users and directories, and common hadoop configs
  	include hdp-hadoop::initialize
  }
}