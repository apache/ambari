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
class hdp::params()
{

  ##Constants##
  $NOTHING='NOTHING'
  $NOBODY_USER='nobody'

  ###### environment variables
  if (hdp_is_empty($configuration) == false) {
    $core-site = $configuration['core-site']
    $hbase-site = $configuration['hbase-site']
    $hdfs-site = $configuration['hdfs-site']
    $hive-site = $configuration['hive-site']
    $hue-site = $configuration['hue-site']
    $mapred-site = $configuration['mapred-site']
    $oozie-site = $configuration['oozie-site']
    $sqoop-site = $configuration['sqoop-site']
    $webhcat-site = $configuration['webhcat-site']
    $yarn-site = $configuration['yarn-site']
  }

  ##### global state defaults ####
  $cluster_service_state = hdp_default("cluster_service_state","running")
  $cluster_client_state = hdp_default("cluster_client_state","installed_and_configured")

  ## Hostname defaults
  $hostname = hdp_default("myhostname", $::fqdn)
  $public_hostname = hdp_default("public_hostname")

  ##### for secure install
  $security_enabled = hdp_default("security_enabled",false)
  $kerberos_domain = hdp_default("kerberos_domain","EXAMPLE.COM")
  $smoketest_user_secure_uid = hdp_default("smoketest_user_secure_uid",1012)
  ## $smoketest_user_secure_uid = 1012
  $kinit_path_local = hdp_default("kinit_path_local","/usr/bin/kinit")
  $keytab_path = hdp_default("keytab_path", "/etc/security/keytabs")
  $use_hostname_in_principal = hdp_default("instance_name", false)

  ###### hostnames
  $namenode_host = hdp_default("namenode_host")
  $snamenode_host = hdp_default("snamenode_host")
  $jtnode_host = hdp_default("jtnode_host")
  $slave_hosts = hdp_default("slave_hosts")

  $rm_host = hdp_default("rm_host")
  $nm_hosts = hdp_default("nm_hosts")
  $hs_host = hdp_default("hs_host")

  $zookeeper_hosts = hdp_default("zookeeper_hosts")

  $hbase_master_hosts = hdp_default("hbase_master_hosts", "")

  #if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves
  $hbase_rs_hosts = hdp_default("hbase_rs_hosts", $slave_hosts)

  #if mapred_tt_hosts not given it is assumed that tasktracker servers on same nodes as slaves
  $mapred_tt_hosts = hdp_default("mapred_tt_hosts", $slave_hosts)

  $all_hosts = hdp_default("all_hosts")

  $hive_server_host = hdp_default("hive_server_host", "")
  $oozie_server =  hdp_default("oozie_server", "")
  $webhcat_server_host = hdp_default("webhcat_server_host", "")
  $gateway_host = hdp_default("gateway_host")
  $hue_server_host = hdp_default("hue_server_host", "")
  
  $nagios_server_host = hdp_default("nagios_server_host")
  $ganglia_server_host = hdp_default("ganglia_server_host")
  
  $dashboard_host = hdp_default("dashboard_host")

  $hdp_os = $::operatingsystem
  $hdp_os_version = $::operatingsystemrelease
  
  
  ## Stack version
  $stack_version = hdp_default("stack_version", "1.3.0")

  
  case $::operatingsystem {
    centos: {
      case $::operatingsystemrelease {
        /^5\..+$/: { $hdp_os_type = "centos5" }
        /^6\..+$/: { $hdp_os_type = "centos6" }
      }
    }
    redhat: {
      case $::operatingsystemrelease {
        /^5\..+$/: { $hdp_os_type = "redhat5" }
        /^6\..+$/: { $hdp_os_type = "redhat6" }
      }
    }
    oraclelinux: {
      case $::operatingsystemrelease {
        /^5\..+$/: { $hdp_os_type = "oraclelinux5" }
        /^6\..+$/: { $hdp_os_type = "oraclelinux6" }
      }
    }
    suse: {
      $hdp_os_type = "suse"
    }
    SLES: {
      $hdp_os_type = "suse"
    }

    default: {
      hdp_fail("No support for os $::operatingsystem  ${hdp_os} ${hdp_os_version}")
    }
  }

  if ($hostAttributes != undef) {
    $public_namenode_host = hdp_host_attribute($hostAttributes,"publicfqdn",$namenode_host)
    $public_snamenode_host = hdp_host_attribute($hostAttributes,"publicfqdn",$snamenode_host)
    $public_rm_host = hdp_host_attribute($hostAttributes,"publicfqdn",$rm_host)
    $public_nm_hosts = hdp_host_attribute($hostAttributes,"publicfqdn",$nm_hosts)
    $public_hs_host = hdp_host_attribute($hostAttributes,"publicfqdn",$hs_host)
    $public_jtnode_host = hdp_host_attribute($hostAttributes,"publicfqdn",$jtnode_host)
    $public_hbase_master_hosts = hdp_host_attribute($hostAttributes,"publicfqdn",$hbase_master_hosts)
    $public_zookeeper_hosts = hdp_host_attribute($hostAttributes,"publicfqdn",$zookeeper_hosts)
    $public_ganglia_server_host = hdp_host_attribute($hostAttributes,"publicfqdn",$ganglia_server_host)
    $public_nagios_server_host = hdp_host_attribute($hostAttributes,"publicfqdn",$nagios_server_host)
    $public_dashboard_host = hdp_host_attribute($hostAttributes,"publicfqdn",$dashboard_host)
    $public_hive_server_host = hdp_host_attribute($hostAttributes,"publicfqdn",$hive_server_host)
    $public_oozie_server = hdp_host_attribute($hostAttributes,"publicfqdn",$oozie_server)
    $public_webhcat_server_host = hdp_host_attribute($hostAttributes,"publicfqdn",$webhcat_server_host)
  } else {
    $public_namenode_host = hdp_default("namenode_host")
    $public_snamenode_host = hdp_default("snamenode_host")
    $public_rm_host = hdp_default("rm_host")
    $public_nm_hosts = hdp_default("nm_hosts")
    $public_hs_host = hdp_default("hs_host")
    $public_jtnode_host = hdp_default("jtnode_host")
    $public_hbase_master_hosts = hdp_default("hbase_master_hosts")
    $public_zookeeper_hosts = hdp_default("zookeeper_hosts")
    $public_ganglia_server_host = hdp_default("ganglia_server_host")
    $public_nagios_server_host = hdp_default("nagios_server_host")
    $public_dashboard_host = hdp_default("dashboard_host")
    $public_hive_server_host = hdp_default("hive_server_host")
    $public_oozie_server = hdp_default("oozie_server")
    $public_webhcat_server_host = hdp_default("webhcat_server_host")
  }


  ############ users
  $user_info = hdp_default("user_info",{})

  $hdfs_user = hdp_default("hdfs_user","hdfs")
  $mapred_user = hdp_default("mapred_user","mapred")
  $yarn_user = hdp_default("yarn_user","yarn")

  $zk_user = hdp_default("zk_user","zookeeper") 
  $hbase_user = hdp_default("hbase_user","hbase")

  $hive_user = hdp_default("hive_user","hive")
  $hcat_user = hdp_default("hcat_user","hcat")
  $webhcat_user = hdp_default("webhcat_user","hcat")

  $oozie_user = hdp_default("oozie_user","oozie")
  $templeton_user = hdp_default("templeton_user","hcat")

  $gmetad_user = hdp_default("gmetad_user","nobody")
  $gmond_user = hdp_default("gmond_user","nobody")

  $smokeuser = hdp_default("smokeuser","ambari_qa")
  $smoke_user_group = hdp_default("smoke_user_group","users")

  $sqoop_user = hdp_default("sqoop_user","sqoop")
  
  ############ Hdfs users directories
  $oozie_hdfs_user_dir = hdp_default("oozie_hdfs_user_dir", "/user/${oozie_user}")
  $oozie_hdfs_user_mode = 775
  $hcat_hdfs_user_dir = hdp_default("hcat_hdfs_user_dir", "/user/${hcat_user}")
  $hcat_hdfs_user_mode = 755
  $webhcat_hdfs_user_dir = hdp_default("hcat_hdfs_user_dir", "/user/${webhcat_user}")
  $webhcat_hdfs_user_mode = 755
  $hive_hdfs_user_dir = hdp_default("hive_hdfs_user_dir", "/user/${hive_user}")
  $hive_hdfs_user_mode = 700
  $smoke_hdfs_user_dir = hdp_default("smoke_hdfs_user_dir", "/user/${smokeuser}")
  $smoke_hdfs_user_mode = 770
  
  ############ Hdfs apps directories
  $hive_apps_whs_dir = hdp_default("hive_apps_whs_dir", "/apps/hive/warehouse")
  $webhcat_apps_dir = hdp_default("webhcat_apps_dir", "/apps/webhcat")
  $hbase_hdfs_root_dir = hdp_default("hbase-site/hbase.hdfs.root.dir","/apps/hbase/data")

  $yarn_nm_app_log_dir = hdp_default("yarn-site/yarn.nodemanager.remote-app-log-dir","/app-logs")

  $yarn_log_aggregation_enabled = hdp_default("yarn-site/yarn.log-aggregation-enable","true")

  $mapreduce_jobhistory_intermediate_done_dir = hdp_default("mapred-site/mapreduce.jobhistory.intermediate-done-dir","/mr-history/tmp")
  
  $mapreduce_jobhistory_done_dir = hdp_default("mapred-site/mapreduce.jobhistory.done-dir","/mr-history/done")
  
  $user_group = hdp_default("user_group","hadoop")

  $ganglia_enabled = hdp_default("ganglia_enabled",true) 

  #TODO: either remove or make conditional on ec2
  $host_address = undef 

  ##### java 
  $java32_home = hdp_default("java32_home","/usr/jdk32/jdk1.6.0_31")
  $java64_home = hdp_default("java64_home","/usr/jdk64/jdk1.6.0_31")
  
  $wipeoff_data =  hdp_default("wipeoff_data",false) 

  $jdk_location = hdp_default("jdk_location","http://download.oracle.com/otn-pub/java/jdk/6u31-b03")
  $jdk_bins = hdp_default("jdk_bins",{
    32 => "jdk-6u31-linux-i586.bin",
    64 => "jdk-6u31-linux-x64.bin"
  })

  $jce_policy_zip = "jce_policy-6.zip"
  $jce_location = hdp_default("jce_location","http://download.oracle.com/otn-pub/java/jce_policy/6")

  #####
  $hadoop_home = hdp_default("hadoop_home","/usr")
  $hadoop_lib_home = hdp_default("hadoop_lib_home","/usr/lib/hadoop/lib")

  #####compression related

  $lzo_enabled = hdp_default("lzo_enabled",false)
  $snappy_enabled = hdp_default("snappy_enabled",true)
  
  $lzo_compression_so_dirs = {
    32 => "${hadoop_lib_home}/native/Linux-i386-32/",
    64 => "${hadoop_lib_home}/native/Linux-amd64-64/"
  }
  
  $snappy_so_src_dir = {
    32 => "${hadoop_home}/lib",
    64 => "${hadoop_home}/lib64"
  }
  $snappy_compression_so_dirs = {
    32 => "${hadoop_lib_home}/native/Linux-i386-32/",
    64 => "${hadoop_lib_home}/native/Linux-amd64-64/"
  }

  $lzo_tar_name = hdp_default("lzo_tar_name","hadoop-lzo-0.5.0")
  
  $snappy_so = hdp_default("snappy_so","libsnappy.so")
  #####
 
  $exec_path = ["/bin","/usr/bin", "/usr/sbin"]

   #### params used on multiple modules
  $dfs_data_dir = hdp_default("hdfs-site/dfs.data.dir","/tmp/hadoop-hdfs/dfs/data")

  ### artifact dir
  $artifact_dir = hdp_default("artifact_dir","/tmp/HDP-artifacts/")

  ### artifacts download url ##
  $apache_artifacts_download_url = hdp_default("apache_artifacts_download_url","")
  $gpl_artifacts_download_url = hdp_default("gpl_artifacts_download_url","") 

  $packages = 'bigtop' 
  if ($packages == 'hdp') {
    $mapred_smoke_test_script = "/usr/sbin/hadoop-validate-setup.sh"
    $hadoop_bin = "/usr/sbin"
    $hadoop_conf_dir = "/etc/hadoop"
    $zk_conf_dir = "/etc/zookeeper"
    $hbase_conf_dir = "/etc/hbase"
    $sqoop_conf_dir = "/etc/sqoop"
    $pig_conf_dir = "/etc/pig"
    $oozie_conf_dir = "/etc/oozie"
    $hadoop_jar_location = "/usr/share/hadoop"
    $hbase_daemon_script = "/usr/bin/hbase-daemon.sh"
    $use_32_bits_on_slaves = false
    $zk_bin = '/usr/sbin'
    $zk_smoke_test_script = '/usr/bin/zkCli.sh'
    $update_zk_shell_files = false

    $hcat_server_host = hdp_default("hcat_server_host")
    $hcat_mysql_host = hdp_default("hcat_mysql_host")
    $hue_conf_dir = "/etc/hue/conf"
    $hive_conf_dir = "/etc/hive/conf"

  } elsif ($packages == 'bigtop') {  
   
    $mapred_smoke_test_script = "/usr/lib/hadoop/sbin/hadoop-validate-setup.sh"

    if $stack_version in ("2.0.1") {
      $hadoop_bin = "/usr/lib/hadoop/sbin"
    } else {
      $hadoop_bin = "/usr/lib/hadoop/bin"
    }
    $yarn_bin = "/usr/lib/hadoop-yarn/sbin"
    $mapred_bin = "/usr/lib/hadoop-mapreduce/sbin"
    $hadoop_conf_dir = "/etc/hadoop/conf"
    $yarn_conf_dir = "/etc/hadoop/conf"
    $zk_conf_dir = "/etc/zookeeper/conf"
    $hbase_conf_dir = "/etc/hbase/conf"
    $sqoop_conf_dir = "/usr/lib/sqoop/conf"
    $pig_conf_dir = "/etc/pig/conf"
    $oozie_conf_dir = "/etc/oozie/conf"
    $hive_conf_dir = "/etc/hive/conf"
    $hcat_conf_dir = "/etc/hcatalog/conf"
    $hadoop_jar_location = "/usr/lib/hadoop/"
    $hbase_daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"
    $use_32_bits_on_slaves = false
    $zk_bin = '/usr/lib/zookeeper/bin'
    $zk_smoke_test_script = "/usr/lib/zookeeper/bin/zkCli.sh"
    $update_zk_shell_files = false

    $hive_mysql_host = hdp_default("hive_mysql_host","localhost")

    $hcat_server_host = hdp_default("hive_server_host")
    $hcat_mysql_host = hdp_default("hive_mysql_host")
    $hue_conf_dir = "/etc/hue/conf"


    $pathes = {
      nagios_p1_pl => {
      'ALL' => '/usr/bin/p1.pl',
      suse => '/usr/lib/nagios/p1.pl'
      }
    }

    $services_names = {
      mysql => {
        'ALL' => 'mysqld',
        suse => 'mysql'},
      httpd => {  
      'ALL' => 'httpd',
      suse => 'apache2'}
    }

    $cmds = {
    htpasswd => {
    'ALL' => 'htpasswd',
     suse => 'htpasswd2'} 

    }
    
    # StackId => Arch => Os
    $package_names = 
    {
      snmp => {
        'ALL' => {
          64 => {
            suse =>['net-snmp'],
            'ALL' => ['net-snmp', 'net-snmp-utils']
          }
        }
      },

      oozie-server => {
        'ALL' => {
          64 => {
            'ALL' => 'oozie.noarch'
          }
        }
      },

      snappy => {
        'ALL' => {
          64 => {
            'ALL' => ['snappy','snappy-devel']
          }
        }
      },

      hadoop => {
        'ALL' => {
          32 => {
            'ALL' => ['hadoop','hadoop-libhdfs.i386','hadoop-native.i386','hadoop-pipes.i386','hadoop-sbin.i386','hadoop-lzo', 'hadoop-lzo-native.i386']
          },
          64 => {
            'ALL' => ['hadoop','hadoop-libhdfs','hadoop-native','hadoop-pipes','hadoop-sbin','hadoop-lzo', 'hadoop-lzo-native']
          }
        },
        '2.0.1' => {
          64 => {
            'ALL' => ['hadoop','hadoop-libhdfs','hadoop-lzo', 'hadoop-lzo-native']
          }
        }
      },

    hadoop-mapreduce-client => {
      'ALL' => {
        64 => {
          'ALL' => ['hadoop-mapreduce']
        }
      }
    },

    yarn-common => { 
      'ALL' => {
        64 => {
          'ALL' => ['hadoop-yarn']
        }
      }
    },

    yarn-nodemanager => { 
      'ALL' => {
        64 => {
          'ALL' => ['hadoop-yarn-nodemanager']
        }
      }
    },

    yarn-proxyserver => { 
      'ALL' => {
        64 => {
          'ALL' => ['hadoop-yarn-proxyserver']
        }
      }
    },

    yarn-resourcemanager => { 
      'ALL' => {
        64 => {
          'ALL' => ['hadoop-yarn-resourcemanager', 'hadoop-mapreduce']
        }
      }
    },

    mapreduce-historyserver => { 
      'ALL' => {
        64 => {
          'ALL' => ['hadoop-mapreduce-historyserver']
        }
      }
    },

    tez_client => { 
      'ALL' => {
        64 => {
          'ALL' => ['tez']
        }
      }
    },

    lzo => {
      'ALL' => {
        'ALL' => {
          'ALL' => ['lzo', 'lzo-devel'],
          suse => ['lzo-devel']
        }
      }
    },

    glibc=> {
      'ALL' => {
        'ALL' => {
          'ALL' => ['glibc','glibc.i686'],
          suse => ['glibc']
        }
      }
    },

    zookeeper=> {
      'ALL' => {64 => {'ALL' => 'zookeeper'}}
    },

    hbase=> {
      'ALL' => {64 => {'ALL' => 'hbase'}}
    },

    pig=> { 
      'ALL' => {'ALL' => {'ALL'=>['pig.noarch']}}
    },

    sqoop=> {
      'ALL' => {'ALL' =>{'ALL' => ['sqoop']}}
    },

    mysql-connector-java=> {
      'ALL' => {'ALL' =>{'ALL' => ['mysql-connector-java']}}
    },
    oozie-client=> {
      'ALL' => {'64' =>{'ALL' => ['oozie-client.noarch']}}
    },
    extjs=> {
      'ALL' => {64 =>{'ALL' => ['extjs-2.2-1']}}
    },
    hive=> {
      'ALL' => {64 =>{'ALL' => ['hive']}}
    },
    hcat=> {
      'ALL' => {'ALL' =>{'ALL' => ['hcatalog']}}
    },

    mysql => {
      'ALL' => {
        64 =>  {
          'ALL' => ['mysql','mysql-server'],
          suse => ['mysql-client','mysql']
        }
      }
    },

    webhcat => {
      'ALL' => {'ALL' => {'ALL' => 'hcatalog'}}
    },

    webhcat-tar-hive => {
      'ALL' => {64 => {'ALL' => 'webhcat-tar-hive'}}
    },

    webhcat-tar-pig => {
      'ALL' => {64 => {'ALL' =>'webhcat-tar-pig'}}
    },

    dashboard => {
      'ALL' => {64 => {'ALL' => 'hdp_mon_dashboard'}}
    },

    perl =>
    {
      'ALL' => {64 => {'ALL' => 'perl'}}
    },

    perl-Net-SNMP =>
    {
      'ALL' => {64 => {'ALL' => 'perl-Net-SNMP'}}
    },

    nagios-server => {
      'ALL' => {
        64 => {
            'ALL' => 'nagios-3.2.3',
            suse => ['nagios-3.2.3','nagios-www-3.2.3']
          }
        }
    },

    nagios-fping => {
      'ALL' => {64 =>{'ALL' => 'fping'}}
    },

    nagios-plugins => {
      'ALL' => {64 => {'ALL' => 'nagios-plugins-1.4.9'}}
    },

    nagios-addons => {
      'ALL' => {64 => {'ALL' => 'hdp_mon_nagios_addons'}}
    },

    nagios-php-pecl-json => {
      'ALL' => {
        64 => {
          'ALL' => $NOTHING,
          suse => 'php5-json',
          centos6 => $NOTHING,
          redhat6 => $NOTHING,
          oraclelinux6 => $NOTHING,
          centos5 => 'php-pecl-json.x86_64',
          redhat5 => 'php-pecl-json.x86_64',
          oraclelinux5 => 'php-pecl-json.x86_64'
        }
      }
    },

    ganglia-server => {
      'ALL' => {64 => {'ALL' => 'ganglia-gmetad-3.2.0'}}
    },

    ganglia-gweb => {
      'ALL' => {64 => {'ALL' => 'gweb'}}
    },

    ganglia-hdp-gweb-addons => {
      'ALL' => {64 => {'ALL' => 'hdp_mon_ganglia_addons'}}
    },

    ganglia-monitor => {
      'ALL' => {64 => {'ALL' =>'ganglia-gmond-3.2.0'}}
    },

    rrdtool-python => {
      'ALL' => {64 => {'ALL' =>'python-rrdtool.x86_64'}}
    },

    # The 32bit version of package rrdtool-devel is removed on centos 5/6 to prevent conflict ( BUG-2881)
    rrdtool-devel => {
      'ALL' => {
        64 => {
          'ALL' => 'rrdtool-devel.i686',
          'centos6' => 'rrdtool-devel.i686',
          'centos5' => 'rrdtool-devel.i386',
          'redhat6' => 'rrdtool-devel.i686',
          'redhat5' => 'rrdtool-devel.i386',
          'oraclelinux6' => 'rrdtool-devel.i686',
          'oraclelinux5' => 'rrdtool-devel.i386'
        }
      }
    },

    # The 32bit version of package rrdtool is removed on centos 5/6 to prevent conflict ( BUG-2408)
    rrdtool => {
      'ALL' => {
        64 => {
          'ALL' => 'rrdtool.i686',
          'centos6' => 'rrdtool.i686',
          'centos5' => 'rrdtool.i386',
          'redhat6' => 'rrdtool.i686',
          'redhat5' => 'rrdtool.i386',
          'oraclelinux6' => 'rrdtool.i686',
          'oraclelinux5' => 'rrdtool.i386'
        }
      }
    },

    hue-server => {
      'ALL' => {64 => {'ALL' => 'hue.noarch'}}
    },

    ambari-log4j => {
      'ALL' => {
        64 => {
          'ALL' => 'ambari-log4j'
        }
      }
    },

    httpd => {
      'ALL' => {
        64 => {
          'ALL' => 'httpd',
          suse => ['apache2', 'apache2-mod_php5']
        }
      }
    }
}

  $repos_paths = 
  {
    centos6 => '/etc/yum.repos.d',
    centos5 => '/etc/yum.repos.d',
    suse => '/etc/zypp/repos.d',
    redhat6 => '/etc/yum.repos.d',
    redhat5 => '/etc/yum.repos.d',
    oraclelinux6 => '/etc/yum.repos.d',
    oraclelinux5 => '/etc/yum.repos.d'
  }

  $rrd_py_path =
  {
    suse => '/srv/www/cgi-bin',
    centos6 => '/var/www/cgi-bin',
    centos5 => '/var/www/cgi-bin',
    redhat6 => '/var/www/cgi-bin',
    redhat5 => '/var/www/cgi-bin',
    oraclelinux6 => '/var/www/cgi-bin',
    oraclelinux5 => '/var/www/cgi-bin'
  }
  
  $nagios_lookup_daemon_strs = 
  {
    suse => '/usr/sbin/nagios',
    centos6 => '/usr/bin/nagios',
    centos5 => '/usr/bin/nagios',
    redhat6 => '/usr/bin/nagios',
    redhat5 => '/usr/bin/nagios',
    oraclelinux6 => '/usr/bin/nagios',
    oraclelinux5 => '/usr/bin/nagios'
  }




  }

 
###### snmp

  $snmp_conf_dir = hdp_default("snmp_conf_dir","/etc/snmp/")
  $snmp_source = hdp_default("snmp_source","0.0.0.0/0") ##TODO!!! for testing needs to be closed up
  $snmp_community = hdp_default("snmp_community","hadoop")

###### aux
  #used by ganglia monitor to tell what components and services are present
  $component_exists = {} 
  $service_exists = {}

  $is_namenode_master = $::fqdn in $namenode_host
  $is_jtnode_master   = $::fqdn in $jtnode_host
  $is_hbase_master    = $::fqdn in $hbase_master_hosts
}
