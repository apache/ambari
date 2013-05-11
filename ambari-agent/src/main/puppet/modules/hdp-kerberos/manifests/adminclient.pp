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

class hdp-kerberos::adminclient(
  $service_state = $hdp::params::cluster_service_state
) inherits hdp-kerberos::params
{
  import 'hdp'

  $kadmin_pw = "bla123"
  $kadmin_admin = "kadmin/admin"
  $realm = $kerberos_domain
  $krb_realm = $kerberos_domain
  $hdp::params::service_exists['hdp-kerberos::adminclient'] = true
  $krbContext = {}
  $krbContext['kadmin_pw'] = $kadmin_pw
  $krbContext['kadmin_admin'] = $kadmin_admin
  $krbContext['realm' ] = $kerberos_domain
  $krbContext['local_or_remote'] = 'remote'
  $krbContext['principals_to_create'] = $principals_to_create
  $krbContext['keytabs_to_create'] = $keytabs_to_create
  $krbContext['principals_in_keytabs'] = $principals_in_keytabs

  $kdc_server = $kdc_host

  package { $package_name_client:
    ensure => installed,
  }
  if ($hdp::params::service_exists['hdp-kerberos::server'] != true) {
    file { "/etc/krb5.conf":
      content => template('hdp-kerberos/krb5.conf'),
      owner => "root",
      group => "root",
      mode => "0644",
      require => Package[$package_name_client],
    }
  }
 
  if ($create_principals_keytabs == "yes") {
    notice("Creating principals and keytabs..")
    hdp-kerberos::principals_and_keytabs::services { 'alphabeta': 
      krb_context => $krbContext
    }
  }
}


define hdp-kerberos::principals_and_keytabs::services(
  $krb_context
)
{
  include hdp-kerberos::params
  $principals_to_create = $krb_context[principals_to_create]
  $keytabs_to_create = $krb_context[keytabs_to_create]

  hdp-kerberos::principal {$principals_to_create:
    krb_context => $krb_context,
  }
  
  hdp-kerberos::keytab { $keytabs_to_create :
    krb_context => $krb_context,
    require => Hdp-kerberos::Principal[$principals_to_create]
  }
}

define hdp-kerberos::keytab(
  $krb_context,
  $keytable_file_owner = undef,
  $keytable_file_mode  = undef
)
{
  include hdp-kerberos::params
  $keytab = $name
  $realm = $krb_context['realm']
  $local_or_remote = $krb_context['local_or_remote']
  $kadmin_pw = $krb_context['kadmin_pw']
  $kadmin_admin = $krb_context['kadmin_admin']
  $kadmin_cmd = "kadmin -w ${kadmin_pw} -p ${kadmin_admin}"
  if ($local_or_remote == 'local') {
    $kadmin_cmd = 'kadmin.local'
  }
  $principals_in_keytabs = $krb_context['principals_in_keytabs']

  $principals = $principals_in_keytabs[$keytab]
  $principals_list = inline_template("<%= principals.join(' ')%>")
  $keytab_filename = $keytab

  exec { "xst ${keytab}":
    command => "rm -rf ${keytab_filename}; ${kadmin_cmd} -q 'xst -k ${keytab_filename} ${principals_list}'; chown puppet:apache ${keytab_filename}",
    unless  => "klist -kt ${keytab_filename} 2>/dev/null | grep -q ' ${principals[0]}'", #TODO may make more robust test
    path   => $hdp-kerberos::params::exec_path,
  }

  if (($keytable_file_owner != undef) or ($keytable_file_mode != undef)) {
    file { $keytab_filename:
      owner => $keytable_file_owner,
      mode  => $keytable_file_mode,
      require => Exec["xst ${keytab}"]
    }
  }
}

define hdp-kerberos::principal(
  $krb_context
)
{
  include hdp-kerberos::params
  $realm = $krb_context['realm']
  $local_or_remote = $krb_context['local_or_remote']
  $kadmin_pw = $krb_context['kadmin_pw']
  $kadmin_admin = $krb_context['kadmin_admin']
  $kadmin_cmd =  "kadmin -w ${kadmin_pw} -p ${kadmin_admin}"
  if ($local_or_remote == 'local') {
    $kadmin_cmd = 'kadmin.local'
  }
  $principal = $name
  exec { "addprinc ${principal}":
    command => "${kadmin_cmd} -q 'addprinc -randkey ${principal}'",
    unless => "${kadmin_cmd} -q listprincs | grep -q '^${principal}$'",
    path => $hdp-kerberos::params::exec_path
  }
}
