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
class hdp-hadoop::namenode::format(
  $force = false
)
{
  $mark_dir = $hdp-hadoop::params::namenode_formatted_mark_dir
  $dfs_name_dir = $hdp-hadoop::params::dfs_name_dir
  $hdfs_user = $hdp::params::hdfs_user
  $hadoop_conf_dir = $hdp-hadoop::params::conf_dir

  # Avoid formatting standby namenode in a HA cluster
  if ($hdp::params::dfs_ha_enabled == false) {
    if ($force == true) {
        hdp-hadoop::exec-hadoop { 'namenode -format' :
        command => 'namenode -format',
        kinit_override => true,
        notify  => Hdp::Exec['set namenode mark']
      }
    } else {

      file { '/tmp/checkForFormat.sh':
        ensure => present,
        source => "puppet:///modules/hdp-hadoop/checkForFormat.sh",
        mode => '0755'
      }

      exec { '/tmp/checkForFormat.sh':
        command   => "sh /tmp/checkForFormat.sh ${hdfs_user} ${hadoop_conf_dir} ${mark_dir} ${dfs_name_dir} ",
        unless   => "test -d ${mark_dir}",
        require   => File['/tmp/checkForFormat.sh'],
        path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
        logoutput => "true",
        notify   => Hdp::Exec['set namenode mark']
      }
    }

    hdp::exec { 'set namenode mark' :
      command     => "mkdir -p ${mark_dir}",
      refreshonly => true
    }
  }
}
