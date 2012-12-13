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
class hdp-sqoop::mysql-connector()
{
  include hdp-sqoop::params
  include hdp-hive::params

  $target = "${hdp::params::artifact_dir}/${zip_name}"
  $sqoop_lib = $hdp-sqoop::params::sqoop_lib

  anchor { 'hdp-sqoop::mysql-connector::begin':}

  hdp::package { 'mysql-connector-java' :
    require   => Anchor['hdp-sqoop::mysql-connector::begin']
  }

   file { "${sqoop_lib}/mysql-connector-java.jar" :
       ensure => link,
       target => "/usr/share/java/mysql-connector-java.jar",
       require => Hdp::Package['mysql-connector-java'],
       notify  =>  Anchor['hdp-sqoop::mysql-connector::end'],
   }

   anchor { 'hdp-sqoop::mysql-connector::end':}
  
}
