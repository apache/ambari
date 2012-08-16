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

  ##### global state defaults ####
  $cluster_service_state = hdp_default("cluster_service_state","running")
  $cluster_client_state = hdp_default("cluster_client_state","installed_and_configured")

  ###### hostnames
  $namenode_host = hdp_default("namenode_host")
  $snamenode_host = hdp_default("snamenode_host")
  $jtnode_host = hdp_default("jtnode_host")
  $slave_hosts = hdp_default("slave_hosts")
  
  $zookeeper_hosts = hdp_default("zookeeper_hosts")

  $hbase_master_host = hdp_default("hbase_master_host")
  $hbase_rs_hosts = hdp_default("hbase_rs_hosts",$slave_hosts) #if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves

  $hive_server_host = hdp_default("hive_server_host")
  $oozie_server =  hdp_default("oozie_server")
  $templeton_server_host = hdp_default("templeton_server_host")
  $gateway_host = hdp_default("gateway_host")
  
  $nagios_server_host = hdp_default("nagios_server_host")
  $ganglia_server_host = hdp_default("ganglia_server_host")
  
  $dashboard_host = hdp_default("dashboard_host")

  $hdp_os = $::operatingsystem
  $hdp_os_version = $::operatingsystemrelease
  case $::operatingsystem {
    centos: {
      case $::operatingsystemrelease {
        /^5\..+$/: { $hdp_os_type = "centos5" }
        /^6\..+$/: { $hdp_os_type = "centos6" }
      }
    }
    redhat: {
      case $::operatingsystemrelease {
        /^5\..+$/: { $hdp_os_type = "rhel5" }
        /^6\..+$/: { $hdp_os_type = "rhel6" }
      }
    }
    default: {
      hdp_fail("No support for os  ${hdp_os} ${hdp_os_version}")
    }
  }

  if ($hostAttributes != undef) {
    $public_namenode_host = hdp_host_attribute($hostAttributes,"publicfqdn",$namenode_host)
    $public_snamenode_host = hdp_host_attribute($hostAttributes,"publicfqdn",$snamenode_host)
    $public_jtnode_host = hdp_host_attribute($hostAttributes,"publicfqdn",$jtnode_host)
    $public_hbase_master_host = hdp_host_attribute($hostAttributes,"publicfqdn",$hbase_master_host)
    $public_zookeeper_hosts = hdp_host_attribute($hostAttributes,"publicfqdn",$zookeeper_hosts)
    $public_ganglia_server_host = hdp_host_attribute($hostAttributes,"publicfqdn",$ganglia_server_host)
    $public_nagios_server_host = hdp_host_attribute($hostAttributes,"publicfqdn",$nagios_server_host)
    $public_dashboard_host = hdp_host_attribute($hostAttributes,"publicfqdn",$dashboard_host)
    $public_hive_server_host = hdp_host_attribute($hostAttributes,"publicfqdn",$hive_server_host)
    $public_oozie_server = hdp_host_attribute($hostAttributes,"publicfqdn",$oozie_server)
    $public_templeton_server_host = hdp_host_attribute($hostAttributes,"publicfqdn",$templeton_server_host)
  } else {
    $public_namenode_host = hdp_default("namenode_host")
    $public_snamenode_host = hdp_default("snamenode_host")
    $public_jtnode_host = hdp_default("jtnode_host")
    $public_hbase_master_host = hdp_default("hbase_master_host")
    $public_zookeeper_hosts = hdp_default("zookeeper_hosts")
    $public_ganglia_server_host = hdp_default("ganglia_server_host")
    $public_nagios_server_host = hdp_default("nagios_server_host")
    $public_dashboard_host = hdp_default("dashboard_host")
    $public_hive_server_host = hdp_default("hive_server_host")
    $public_oozie_server = hdp_default("oozie_server")
    $public_templeton_server_host = hdp_default("templeton_server_host")
  }


  ############ users
  $user_info = hdp_default("user_info",{})

  $hdfs_user = hdp_default("hdfs_user","hdfs")
  $mapred_user = hdp_default("mapred_user","mapred")

  $zk_user = hdp_default("zk_user","zookeeper") 
  $hbase_user = hdp_default("hbase_user","hbase")

  $hive_user = hdp_default("hive_user","hive")
  $hcat_user = hdp_default("hcat_user","hcat")

  $oozie_user = hdp_default("oozie_user","oozie")
  $templeton_user = hdp_default("templeton_user","templeton")

  $gmetad_user = hdp_default("gmetad_user","nobody")
  $gmond_user = hdp_default("gmond_user","nobody")

  $smokeuser = hdp_default("smokeuser","ambari_qa")
  $smoke_user_group = hdp_default("smoke_user_group","users")

  #because of Puppet user resource issue make sure that $hadoop_user is different from hadoop_user_group
  $hadoop_user = hdp_default("hadoop_user", "hadoop_deploy")
  $hadoop_user_group = hdp_default("hadoop_user_group","hadoop")

  $ganglia_enabled = hdp_default("ganglia_enabled",true) 

  #TODO: either remove or make conditional on ec2
  $host_address = undef 

  ##### java 
  $java32_home = hdp_default("java32_home","/usr/jdk32/jdk1.6.0_26")
  $java64_home = hdp_default("java64_home","/usr/jdk64/jdk1.6.0_26")
  
  $wipeoff_data =  hdp_default("wipeoff_data",false) 

  $jdk_location = hdp_default("jdk_location","http://download.oracle.com/otn-pub/java/jdk/6u26-b03")
  $jdk_bins = hdp_default("jdk_bins",{
    32 => "jdk-6u26-linux-i586.bin",
    64 => "jdk-6u26-linux-x64.bin"
  })
  
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
  $dfs_data_dir = hdp_default("hadoop/hdfs-site/dfs_data_dir","/tmp/hadoop-hdfs/dfs/data")

  ### artifact dir
  $artifact_dir = hdp_default("artifact_dir","/tmp/HDP-artifacts/")

  ### artifacts download url ##
  $apache_artifacts_download_url = hdp_default("apache_artifacts_download_url","")
  $gpl_artifacts_download_url = hdp_default("gpl_artifacts_download_url","") 

  ### related to package resources  
 
  $package_names = {
   # hadoop => {
   #   32 => 'hadoop.i386',
   #   64 => 'hadoop.x86_64'
   # },
   # zookeeper => {
   #   64 => 'zookeeper.x86_64'
   # },
   # hbase => {
   #   64 => 'hbase.x86_64'
   # },
   # hcat-server => {
   #   64 => 'hcatalog-server.x86_64'
   # },
   # hcat-base => {
   #   64 => 'hcatalog.x86_64'
   # },
   # pig => {
   #   32 => 'pig.i386'
   # },
    ganglia-monitor => {
      64 => 'ganglia-gmond-3.2.0'
    },
    ganglia-server => {
      64 => ['ganglia-gmetad-3.2.0']
    },
    ganglia-gweb => {
      64 => 'gweb'
    },
    ganglia-hdp-gweb-addons => {
      64 => 'hdp_mon_ganglia_addons'
    },
    glibc-rhel6 => {
      32 => ['glibc','glibc.i686'],
      64 => ['glibc','glibc.i686']
    },
    nagios-addons => {
      64 => 'hdp_mon_nagios_addons'
    },
    nagios-server => {
      64 => 'nagios-3.2.3'
    },
    nagios-plugins => {
      64 => 'nagios-plugins-1.4.9'
    },
    nagios-fping => {
      64 =>'fping'
    },
    nagios-php-pecl-json => {
      64 => 'php-pecl-json.x86_64'
    },
    snmp => {
      64 => ['net-snmp','net-snmp-utils'],
    },
    dashboard => {
      64 => 'hdp_mon_dashboard'
    },
    # sqoop => {
    #   32 => 'sqoop-1.4.1-1.noarch'
    #},
    templeton => {
       32 => 'templeton',
       64 => 'templeton'
    },
    oozie-client => {
      64 => 'oozie-client.noarch'
    },
    oozie-server => {
      64 => 'oozie.noarch'
    },
    lzo-rhel5 => {
      32 => ['lzo','lzo.i386','lzo-devel','lzo-devel.i386'],
      64 => ['lzo','lzo.i386','lzo-devel','lzo-devel.i386']
    },
    lzo-rhel6 => {
      32 => ['lzo','lzo.i686','lzo-devel','lzo-devel.i686'],
      64 => ['lzo','lzo.i686','lzo-devel','lzo-devel.i686']
    },
    #TODO: make these two consistent on whether case of 64/32 bits
    snappy => {
      32 =>  ['snappy','snappy-devel'],
      64 => ['snappy','snappy-devel']
    },
    mysql => {
      32 =>  ['mysql','mysql-server']
    },
    mysql-connector => {
      64 =>  ['mysql-connector-java']
    },
    extjs => {
      64 =>  ['extjs-2.2-1']
    },
    templeton-tar-hive => {
      64 => ['templeton-tar-hive-0.0.1.14-1']
    },
    templeton-tar-pig => {
      64 => ['templeton-tar-pig-0.0.1.14-1']
    }
  }
  $packages = 'bigtop' 
  if ($packages == 'hdp') {
    $package_names[hadoop] = { 32 => ['hadoop.i386'], 64 => ['hadoop.x86_64']}
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
    $package_names[zookeeper] = {64 => 'zookeeper.x86_64'}
    $package_names[hbase] = {64 => 'hbase.x86_64'}
    $package_names[sqoop] = {32 => 'sqoop-1.4.1-1.noarch'}
    $package_names[pig] = { 32 => 'pig.i386'}
    $package_names[hcat-server] = { 64 => 'hcatalog-server.x86_64'}
    $package_names[hcat-base] = { 64 => 'hcatalog.x86_64'}
    $zk_bin = '/usr/sbin'
    $zk_smoke_test_script = '/usr/bin/zkCli.sh'
    $update_zk_shell_files = false

    $hcat_server_host = hdp_default("hcat_server_host")
    $hcat_mysql_host = hdp_default("hcat_mysql_host")

  } elsif ($packages == 'bigtop') {  

    $package_names[hadoop] = {32 => ['hadoop','hadoop-libhdfs.i386','hadoop-native.i386','hadoop-pipes.i386','hadoop-sbin.i386','hadoop-lzo', 'hadoop-lzo-native.i386'], 64 => ['hadoop','hadoop-libhdfs','hadoop-native','hadoop-pipes','hadoop-sbin','hadoop-lzo', 'hadoop-lzo-native']}
    #$package_names[hadoop] = {32 => ['hadoop.i386','hadoop-native.i386'], 64 => ['hadoop.x86_64','hadoop-native.x86_64']}
   
    $mapred_smoke_test_script = "/usr/lib/hadoop/sbin/hadoop-validate-setup.sh"
    $hadoop_bin = "/usr/lib/hadoop/bin"
    $hadoop_conf_dir = "/etc/hadoop/conf"
    $zk_conf_dir = "/etc/zookeeper/conf"
    $hbase_conf_dir = "/etc/hbase/conf/"
    $sqoop_conf_dir = "/usr/lib/sqoop/conf"
    $pig_conf_dir = "/etc/pig/conf"
    $oozie_conf_dir = "/etc/oozie/conf"
    $hive_conf_dir = "/etc/hive/conf"
    $hcat_conf_dir = "/etc/hcatalog/conf"
    $hadoop_jar_location = "/usr/lib/hadoop/"
    $hbase_daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"
    $use_32_bits_on_slaves = false
    $package_names[zookeeper] = {64 => ['zookeeper']}
    $package_names[hbase] = {64 => ['hbase']}
    $package_names[sqoop] = {32 => ['sqoop'], 64 => ['sqoop']}
    $package_names[pig] = {32 => ['pig.noarch'], 64 => ['pig.noarch']}
    $package_names[hcat] = {32 => ['hcatalog'], 64 => ['hcatalog']}
    $package_names[hive] = {64 => ['hive']}
    $zk_bin = '/usr/lib/zookeeper/bin'
    $zk_smoke_test_script = "/usr/lib/zookeeper/bin/zkCli.sh"
    $update_zk_shell_files = false

    $hive_mysql_host = hdp_default("hive_mysql_host","localhost")

    $hcat_server_host = hdp_default("hive_server_host")
    $hcat_mysql_host = hdp_default("hive_mysql_host")

  }
 
###### snmp

  $snmp_conf_dir = hdp_default("snmp_conf_dir","/etc/snmp/")
  $snmp_source = hdp_default("snmp_source","0.0.0.0/0") ##TODO!!! for testing needs to be closed up
  $snmp_community = hdp_default("snmp_community","hadoop")

###### aux
  #used by ganglia monitor to tell what components and services are present
  $component_exists = {} 
  $service_exists = {} 
}

