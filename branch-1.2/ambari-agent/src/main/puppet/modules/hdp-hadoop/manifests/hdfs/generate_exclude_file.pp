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

define hdp-hadoop::hdfs::generate_exclude_file()
{
  $exlude_file_path = $configuration['hdfs-site']['dfs.hosts.exclude']
  ## Generate exclude file if exists value of $configuration['hdfs-exclude-file']['datanodes']
  ## or value for $configuration['hdfs-exclude-file']['datanodes'] is empty
  if (hdp_is_empty($configuration) == false and
    hdp_is_empty($configuration['hdfs-exclude-file']) == false) and
    (hdp_is_empty($configuration['hdfs-exclude-file']['datanodes']) == false)
    or has_key($configuration['hdfs-exclude-file'], 'datanodes') {
    ##Create file with list of excluding hosts
    $exlude_hosts_list = hdp_array_from_comma_list($configuration['hdfs-exclude-file']['datanodes'])
    file { $exlude_file_path :
      ensure => file,
      content => template('hdp-hadoop/exclude_hosts_list.erb')
    }
  }
}




