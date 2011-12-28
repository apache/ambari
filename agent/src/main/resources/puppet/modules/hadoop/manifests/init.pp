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

class hadoop {
  /**
   * Common definitions for hadoop nodes.
   * They all need these files so we can access hdfs/jobs from any node
   */
  class common {
    package { "hadoop":
      ensure => latest,
      require => [Package["jdk"]],
    }

    package { "hadoop-native":
      ensure => latest,
      require => [Package["hadoop"]],
    }
  }
  
  define role ($ambari_role_name = "datanode", 
               $ambari_role_prefix, $user = "hdfs", $group = "hdfs", $auth_type = "simple") {

    include common 

    realize Group[$group]
    realize User[$user]

    /*
     * Create conf directory for datanode 
     */
    $hadoop_conf_dir = "${ambari_role_prefix}/etc/hadoop"
    file {["${ambari_role_prefix}", "${ambari_role_prefix}/etc", "${ambari_role_prefix}/etc/hadoop"]:
      ensure => directory,
      owner => $user,
      group => $group,
      mode => 755
    }     
    notice ($ambari_role_prefix)
    notice ($ambari_role_name)
    $files = get_files ($hadoop_conf_dir, $::hadoop_stack_conf, $ambari_role_name)
    notice($files)

    /* Create config files for each category */
    create_config_file {$files:
                           conf_map => $::hadoop_stack_conf[$title],
                           require => [Package["hadoop"]],
                           owner => $user,
                           group => $group,
                           mode => 644
                       }

    package { "hadoop-${ambari_role_name}":
      ensure => latest,
      require => [Package["hadoop"]],
    }

    if ($ambari_role_name == "datanode") {
      if ($auth_type == "kerberos") {
        package { "hadoop-sbin":
          ensure => latest,
          require => [Package["hadoop"]],
        }
      }
    }
  }

  define client ($ambari_role_name = "client", $ambari_role_prefix,
                 $user = "hadoop", $group = "hadoop") {

    include common 

    realize Group[$group]
    realize User[$user]

    $hadoop_conf_dir = "${ambari_role_prefix}/etc/conf"
    file {["${ambari_role_prefix}", "${ambari_role_prefix}/etc", "${ambari_role_prefix}/etc/conf"]:
      ensure => directory,
      owner => $user,
      group => $group,
      mode => 755
    }     
    notice ($ambari_role_prefix)
    $files = get_files ($hadoop_conf_dir, $::hadoop_stack_conf, $ambari_role_name)
    notice($files)

    /* Create config files for each category */
    create_config_file {$files:
                           conf_map => $::hadoop_stack_conf[$title],
                           require => [Package["hadoop"]],
                           owner => $user,
                           group => $group,
                           mode => 644
                       }

    package { ["hadoop-doc", "hadoop-source", "hadoop-debuginfo", 
               "hadoop-fuse", "hadoop-libhdfs", "hadoop-pipes"]:
      ensure => latest,
      require => [Package["hadoop"]],  
    }
  }

  define create_config_file ($conf_map, $owner, $group, $mode) {
    $category = get_category_name ($title)
    $conf_category_map = $conf_map[$category]
    if $category == 'hadoop-env.sh' {
      file {"$title":
        ensure => present,
        content => template('hadoop/config_env.erb'),
        owner => $owner,
        group => $group,
        mode => 755
      } 
    } else {
      file {"$title":
        ensure => present,
        content => template('hadoop/config_properties.erb'),
        owner => $owner,
        group => $group,
        mode => $mode
      } 
    }
  }
}
