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

   hdp::exec { 'yum install -y mysql-connector-java':
       command => "yum install -y mysql-connector-java",
       unless  => "rpm -qa | grep mysql-connector-java",
       path    => ["/bin","/usr/bin/"],
       require   => Anchor['hdp-sqoop::mysql-connector::begin']
   }

   hdp::exec { 'sqoop mkdir -p ${artifact_dir} ;  cp /usr/share/java/mysql-connector-java.jar  ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /usr/share/java/mysql-connector-java.jar  ${target}",
       unless  => "test -f ${target}",
       creates => $target,
       path    => ["/bin","/usr/bin/"],
       require => Hdp::Exec['yum install -y mysql-connector-java'],
       notify  =>  Anchor['hdp-sqoop::mysql-connector::end'],
   }

   anchor { 'hdp-sqoop::mysql-connector::end':}
  
}
