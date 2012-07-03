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
class hdp-templeton::client(
  $service_state = $hdp::params::cluster_client_state,
  $templeton_server = undef
) inherits hdp::params
{ 
  if ($service_state == 'no_op') {
   } elsif ($service_state in ['installed_and_configured','uninstalled']) {
     if ($hdp::params::service_exists['hdp-templeton::server'] != true) {
       #installs package, creates user, sets configuration
       class { 'hdp-templeton' :
         service_state => $service_state
       }
      if ($templeton_server != undef) {
        Hdp-Templeton::Configfile<||>{templeton_server => $templeton_server}
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
