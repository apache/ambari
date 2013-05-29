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

class hdp-kerberos::params(
) inherits hdp::params
{
  $domain  = 'hadoop.com'
  $realm = inline_template('<%= @domain.upcase %>')
  $kdc_server = $hdp::params::hostname
  $kdc_port = 88
  $keytab_export_base_dir = '/etc/security/'
  $keytab_export_dir = "${keytab_export_base_dir}/keytabs"

  $keytab_map = {
    'hdp-hadoop::namenode' =>  
      {keytab    => 'nn.service.keytab',
       primaries => ['nn', 'host', 'HTTP']},
    'hdp-hadoop::snamenode' =>  
      {keytab    => 'nn.service.keytab',
       primaries => ['nn', 'host', 'HTTP']},
    'hdp-hadoop::datanode' =>  
      {keytab    => 'dn.service.keytab',
       primaries => ['dn']},
    'hdp-hadoop::jobtracker' =>  
      {keytab    => 'jt.service.keytab',
       primaries => ['jt']},
    'hdp-hadoop::tasktracker' =>  
      {keytab    => 'tt.service.keytab',
       primaries => ['tt']}
  }

  case $::operatingsystem {
    'ubuntu': {
      $package_name_kdc    = 'krb5-kdc'
      $service_name_kdc    = 'krb5-kdc'
      $package_name_admin  = 'krb5-admin-server'
      $service_name_admin  = 'krb5-admin-server'
      $package_name_client = 'krb5-user'
      $exec_path           = '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin'
      $kdc_etc_path        = '/etc/krb5kdc/'
     }
     default: {
       $package_name_kdc    = 'krb5-server'
       $service_name_kdc    = 'krb5kdc'
       $package_name_admin  = 'krb5-libs'
       $service_name_admin  = 'kadmin'
       $package_name_client = 'krb5-workstation' 
       $exec_path           = '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/kerberos/sbin:/usr/kerberos/bin'
       $kdc_etc_path        = '/var/kerberos/krb5kdc/'
    }
  }
}
