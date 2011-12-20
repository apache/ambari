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

stage {"pre": before => Stage["main"]}

yumrepo { "Bigtop":
    baseurl => "http://bigtop01.cloudera.org:8080/job/Bigtop-trunk-matrix/label=centos5/lastSuccessfulBuild/artifact/output/",
    descr => "Bigtop packages",
    enabled => 1,
    gpgcheck => 0,
}

package { "jdk":
   ensure => "installed",
}

node default {
  notice($fqdn)

  /* Assign defaults */
  if !$ambari_stack_install_dir {
    $ambari_stack_install_dir = "/var/ambari/"
  } 
  notice ($ambari_stack_install_dir)

  /* Create group and users */
  group {$ambari_admin_group:
    ensure => present
  }

  /*
   * Create user for HDFS component
   * TODO: currently uid is auto selected which may cause different uids on each node..
   *       No home directory specified. Need it? 
   *       Is user specified per role or per component? 
   */
  user {$ambari_hdfs_user:
    ensure => present,
    gid => $ambari_admin_group
  }

  /* 
   * Ensure cluster directory path is present 
   * Owned by root up to cluster directory
   */
  $stack_path = "${ambari_stack_install_dir}/${ambari_cluster_name}"
  $stack_path_intermediate_dirs = dirs_between ($stack_path)
  file {$stack_path_intermediate_dirs:
    ensure => directory,
    owner => root,
    group => $ambari_admin_group,
    mode => 755
  }

  if ($fqdn in $role_to_nodes[namenode]) {
    hadoop::namenode {"namenode":
        ambari_role_name => "namenode",
        ambari_role_prefix => "${stack_path}/namenode",
        user => $ambari_hdfs_user,
        group => $ambari_admin_group
    }
  } 

  /* hadoop.security.authentication make global variable */
  if ($fqdn in $role_to_nodes[datanode]) {
    hadoop::datanode {"datanode":
        ambari_role_name => "datanode",
        ambari_role_prefix => "${stack_path}/datanode",
        user => $ambari_hdfs_user,
        group => $ambari_admin_group,
        auth_type => "simple"
    }
  } 

  if ($fqdn in $role_to_nodes[client]) {
    hadoop::client {"client":
        ambari_role_name => "client",
        ambari_role_prefix => "${stack_path}/client",
        user => $ambari_hdfs_user,
        group => $ambari_admin_group
    }
  } 
}

Yumrepo<||> -> Package<||>
