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

class hdp-kerberos::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-kerberos::params
{ 
  import 'hdp'

  $hdp::params::service_exists['hdp-kerberos::server'] = true

  $krb_realm = $kerberos_domain
  $kadmin_pw = "bla123"
  $kadmin_admin = "kadmin/admin"

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    # Install kdc server and client
    package { $package_name_kdc:
      ensure => installed
    }

    # set the realm
    $realm = $krb_realm
    # SUHAS: This should be set on all the nodes in addition to kdc server
    file { "/etc/krb5.conf":
      content => template('hdp-kerberos/krb5.conf'),
      owner => "root",
      group => "root",
      mode => "0644",
      require => Package[$package_name_kdc],
      }

    file { $kdc_etc_path:
      ensure => directory,
      owner => root,
      group => root,
      mode => "0700",
      require => Package[$package_name_kdc],
    }

    file { "${kdc_etc_path}/kdc.conf":
      content => template('hdp-kerberos/kdc.conf'),
      require => Package["$package_name_kdc"],
      owner => "root",
      group => "root",
      mode => "0644",
    }

    # SUHAS: kadm5.acl file template is missing in gsInsaller
    # SUHAS: gsInstaller stops stopIptables at this point (sequence is not relevant here).
    file { "${kdc_etc_path}/kadm5.acl":
      content => template('hdp-kerberos/kadm5.acl'),
      require => Package["$package_name_kdc"],
      owner => "root",
      group => "root",
      mode => "0644",
    }

    exec { "kdb5_util":
      path => $exec_path,
      command => "rm -f ${kdc_etc_path}/kadm5.keytab; kdb5_util -P x86yzh12 -r ${realm} create -s && kadmin.local -q 'cpw -pw ${kadmin_pw} ${kadmin_admin}'",
      creates => "${kdc_etc_path}/stash",
      subscribe => File["${kdc_etc_path}/kdc.conf"],
      require => [Package[$package_name_kdc], File["${kdc_etc_path}/kdc.conf"], File["/etc/krb5.conf"]]
    }

    # SUHAS: gsInstaller has checkconfig_on
    exec { "chkconfig_krb5kdc_on":
      path => $exec_path,
      command => "chkconfig krb5kdc on",
      require => [Package["$package_name_kdc"], File["${kdc_etc_path}/kdc.conf"], Exec["kdb5_util"]],
    }
    
    # Start KDC Server
    if ($service_state in ['running','stopped']) {
      service { $service_name_kdc:
        ensure => $service_state,
        require => [Exec["chkconfig_krb5kdc_on"]],
        subscribe => File["${kdc_etc_path}/kdc.conf"],
        hasrestart => true,
      }

      # SUHAS: This is to be done on HMC not KDC Server??
      $se_hack = "setsebool -P kadmind_disable_trans  1 ; setsebool -P krb5kdc_disable_trans 1"
      service { $service_name_admin:
        ensure => $service_state,
        require => Service[$service_name_kdc],
        hasrestart => true,
        restart => "${se_hack} ; service ${service_name_admin} restart",
        start => "${se_hack} ; service ${service_name_admin} start",
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
