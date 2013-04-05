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

class hdp-kerberos::client(
  $service_state = $hdp::params::cluster_service_state
) inherits hdp-kerberos::params
{
  import 'hdp'

  $hdp::params::service_exists['hdp-kerberos::client'] = true

  $kdc_server = $kdc_host
  $krb_realm = $kerberos_domain
  $realm = $kerberos_domain

  if ($hdp::params::service_exists['hdp-kerberos::adminclient'] != true)  {
    package { $package_name_client:
      ensure => installed,
    }
  }

  if (($hdp::params::service_exists['hdp-kerberos::server'] != true) and
      ($hdp::params::service_exists['hdp-kerberos::adminclient'] != true) ) {
    file { "/etc/krb5.conf":
      content => template('hdp-kerberos/krb5.conf'),
      owner => "root",
      group => "root",
      mode => "0644",
      require => Package[$package_name_client],
    }
  }
}
