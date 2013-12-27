# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

class kerberos {
  class site {
    # The following is our interface to the world. This is what we allow
    # users to tweak from the outside (see tests/init.pp for a complete
    # example) before instantiating target classes.
    # Once we migrate to Puppet 2.6 we can potentially start using 
    # parametrized classes instead.
    $domain     = $kerberos_domain     ? { '' => inline_template('<%= domain %>'),
                                           default => $kerberos_domain }
    $realm      = $kerberos_realm      ? { '' => inline_template('<%= domain.upcase %>'),
                                           default => $kerberos_realm } 
    $kdc_server = $kerberos_kdc_server ? { '' => 'localhost',
                                           default => $kerberos_kdc_server }
    $kdc_port   = $kerberos_kdc_port   ? { '' => '88', 
                                           default => $kerberos_kdc_port } 
    $admin_port = 749 /* BUG: linux daemon packaging doesn't let us tweak this */

    $keytab_export_dir = "/var/lib/bigtop_keytabs"

    case $operatingsystem {
        'ubuntu': {
            $package_name_kdc    = 'krb5-kdc'
            $service_name_kdc    = 'krb5-kdc'
            $package_name_admin  = 'krb5-admin-server'
            $service_name_admin  = 'krb5-admin-server'
            $package_name_client = 'krb5-user'
            $exec_path           = '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin'
            $kdc_etc_path        = '/etc/krb5kdc/'
        }
        # default assumes CentOS, Redhat 5 series (just look at how random it all looks :-()
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

    file { "/etc/krb5.conf":
      content => template('kerberos/krb5.conf'),
      owner => "root",
      group => "root",
      mode => "0644",
    }

    @file { $keytab_export_dir:
      ensure => directory,
      owner  => "root",
      group  => "root",
    }

    # Required for SPNEGO
    @principal { "HTTP": 

    }
  }

  class kdc inherits kerberos::site {
    package { $package_name_kdc:
      ensure => installed,
    }

    file { $kdc_etc_path:
    	ensure => directory,
        owner => root,
        group => root,
        mode => "0700",
        require => Package["$package_name_kdc"],
    }
    file { "${kdc_etc_path}/kdc.conf":
      content => template('kerberos/kdc.conf'),
      require => Package["$package_name_kdc"],
      owner => "root",
      group => "root",
      mode => "0644",
    }
    file { "${kdc_etc_path}/kadm5.acl":
      content => template('kerberos/kadm5.acl'),
      require => Package["$package_name_kdc"],
      owner => "root",
      group => "root",
      mode => "0644",
    }

    exec { "kdb5_util":
      path => $exec_path,
      command => "rm -f /etc/kadm5.keytab ; kdb5_util -P cthulhu -r ${realm} create -s && kadmin.local -q 'cpw -pw secure kadmin/admin'",
      
      creates => "${kdc_etc_path}/stash",

      subscribe => File["${kdc_etc_path}/kdc.conf"],
      # refreshonly => true, 

      require => [Package["$package_name_kdc"], File["${kdc_etc_path}/kdc.conf"], File["/etc/krb5.conf"]],
    }

    service { $service_name_kdc:
      ensure => running,
      require => [Package["$package_name_kdc"], File["${kdc_etc_path}/kdc.conf"], Exec["kdb5_util"]],
      subscribe => File["${kdc_etc_path}/kdc.conf"],
      hasrestart => true,
    }


    class admin_server inherits kerberos::kdc {
      $se_hack = "setsebool -P kadmind_disable_trans  1 ; setsebool -P krb5kdc_disable_trans 1"

      package { "$package_name_admin":
        ensure => installed,
        require => Package["$package_name_kdc"],
      } 
  
      service { "$service_name_admin":
        ensure => running,
        require => [Package["$package_name_admin"], Service["$service_name_kdc"]],
        hasrestart => true,
        restart => "${se_hack} ; service ${service_name_admin} restart",
        start => "${se_hack} ; service ${service_name_admin} start",
      }
    }
  }

  class client inherits kerberos::site {
    package { $package_name_client:
      ensure => installed,
    }
  }

  class server {
    include kerberos::client

    class { "kerberos::kdc": } 
    ->
    Class["kerberos::client"] 

    class { "kerberos::kdc::admin_server": }
    -> 
    Class["kerberos::client"]
  }

  define principal {
    require "kerberos::client"

    realize(File[$kerberos::site::keytab_export_dir])

    $principal = "$title/$hdp::params::hostname"
    $keytab    = "$kerberos::site::keytab_export_dir/$title.keytab"

    exec { "addprinc.$title":
      path => $kerberos::site::exec_path,
      command => "kadmin -w secure -p kadmin/admin -q 'addprinc -randkey $principal'",
      unless => "kadmin -w secure -p kadmin/admin -q listprincs | grep -q $principal",
      require => Package[$kerberos::site::package_name_client],
    } 
    ->
    exec { "xst.$title":
      path    => $kerberos::site::exec_path, 
      command => "kadmin -w secure -p kadmin/admin -q 'xst -k $keytab $principal'",
      unless  => "klist -kt $keytab 2>/dev/null | grep -q $principal",
      require => File[$kerberos::site::keytab_export_dir],
    }
  }

  define host_keytab($princs = undef, $spnego = disabled) {
    $keytab = "/etc/$title.keytab"

    $requested_princs = $princs ? { 
      undef   => [ $title ],
      default => $princs,
    }

    $internal_princs = $spnego ? {
      /(true|enabled)/ => [ 'HTTP' ],
      default          => [ ],
    }
    realize(Kerberos::Principal[$internal_princs])

    $includes = inline_template("<%=
      [requested_princs, internal_princs].flatten.map { |x|
        \"rkt $kerberos::site::keytab_export_dir/#{x}.keytab\"
      }.join(\"\n\")
    %>")

    kerberos::principal { $requested_princs:
    }

    exec { "ktinject.$title":
      path     => $kerberos::site::exec_path,
      command  => "/usr/bin/ktutil <<EOF
        $includes
        wkt $keytab
EOF
        chown $title $keytab",
      creates => $keytab,
      require => [ Kerberos::Principal[$requested_princs],
                   Kerberos::Principal[$internal_princs] ],
    }
  }
}
