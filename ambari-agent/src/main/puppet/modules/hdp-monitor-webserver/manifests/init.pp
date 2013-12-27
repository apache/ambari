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
class hdp-monitor-webserver( 
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp::params
{

  
  if hdp_is_empty($hdp::params::services_names[httpd]) {
      hdp_fail("There is no service name for service httpd")
    }
    else {
      $service_name_by_os = $hdp::params::services_names[httpd]
    }

    if hdp_is_empty($service_name_by_os[$hdp::params::hdp_os_type]) {
      
      if hdp_is_empty($service_name_by_os['ALL']) {
        hdp_fail("There is no service name for service httpd")
      }
      else {
        $service_name = $service_name_by_os['ALL']
      }
    }
    else {
      $service_name = $service_name_by_os[$hdp::params::hdp_os_type]
    }

    if hdp_is_empty($hdp::params::pathes[httpd_conf_dir]) {
      hdp_fail("There is no config dir path for service httpd")
    }
    else {
      $path_by_os = $hdp::params::pathes[httpd_conf_dir]
    }

    if hdp_is_empty($path_by_os[$hdp::params::hdp_os_type]) {
      
      if hdp_is_empty($path_by_os['ALL']) {
        hdp_fail("There is no config dir path for service httpd")
      }
      else {
        $httpd_conf_dir = $path_by_os['ALL']
      }
    }
    else {
      $httpd_conf_dir = $path_by_os[$hdp::params::hdp_os_type]
    }


  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured', 'restart']) {


    if ($service_state == 'running') {
      #TODO: refine by using notify/subscribe
      hdp::exec { 'monitor webserver start':
        command => "/etc/init.d/$service_name start",
        unless => "/etc/init.d/$service_name status",
        require => Hdp::Exec['enabling keepalive for httpd']

      } 

      hdp::package { 'httpd' :
        size   => 64
      }
    hdp::exec {'enabling keepalive for httpd':
      command     => "grep -E 'KeepAlive (On|Off)' ${httpd_conf_dir}/httpd.conf && sed -i 's/KeepAlive Off/KeepAlive On/' ${httpd_conf_dir}/httpd.conf || echo 'KeepAlive On' >> ${httpd_conf_dir}/httpd.conf",
      require => Hdp::Package['httpd']

    }

    } elsif ($service_state == 'stopped') {
      # stop should never fail if process already stopped
      hdp::exec { 'monitor webserver stop':
        command => "/etc/init.d/$service_name stop"
      }
    } elsif ($service_state == 'restart') {
      hdp::exec { 'monitor webserver restart':
        command => "/etc/init.d/$service_name restart",
        require => Hdp::Exec['enabling keepalive for httpd']
      }
      hdp::package { 'httpd' :
        size   => 64
      }

    hdp::exec {'enabling keepalive for httpd':
      command     => "grep -E 'KeepAlive (On|Off)' ${httpd_conf_dir}/httpd.conf && sed -i 's/KeepAlive Off/KeepAlive On/' ${httpd_conf_dir}/httpd.conf || echo 'KeepAlive On' >> ${httpd_conf_dir}/httpd.conf",
      require => Hdp::Package['httpd']
    }

    } elsif ($service_state == 'installed_and_configured') {
      hdp::package { 'httpd' :
        size   => 64
      }

    hdp::exec {'enabling keepalive for httpd':
      command     => "grep -E 'KeepAlive (On|Off)' ${httpd_conf_dir}/httpd.conf && sed -i 's/KeepAlive Off/KeepAlive On/' ${httpd_conf_dir}/httpd.conf || echo 'KeepAlive On' >> ${httpd_conf_dir}/httpd.conf",
      require => Hdp::Package['httpd']
    }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
