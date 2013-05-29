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
class hdp-hive::jdbc-connector()
{
  include hdp-hive::params

  $jdbc_jar_name = $hdp-hive::params::jdbc_jar_name
  
  $hive_lib = $hdp-hive::params::hive_lib
  $target = "${hive_lib}/${jdbc_jar_name}"
  
  anchor { 'hdp-hive::jdbc-connector::begin':}

   hdp::package { 'mysql-connector-java' :
     require   => Anchor['hdp-hive::jdbc-connector::begin']
   }


  if ($hive_jdbc_driver == "com.mysql.jdbc.Driver"){
   hdp::exec { 'hive mkdir -p ${artifact_dir} ;  cp /usr/share/java/${jdbc_jar_name}  ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /usr/share/java/${jdbc_jar_name}  ${target}",
       unless  => "test -f ${target}",
       creates => $target,
       path    => ["/bin","/usr/bin/"],
       require => Hdp::Package['mysql-connector-java'],
       notify  =>  Anchor['hdp-hive::jdbc-connector::end'],
   }
  } elsif ($hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver") {
   hdp::exec { 'hive mkdir -p ${artifact_dir} ;  cp /usr/share/java/${jdbc_jar_name}  ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /usr/share/java/${jdbc_jar_name}  ${target}",
       unless  => "test -f ${target}",
       path    => ["/bin","/usr/bin/"],
       notify  =>  Anchor['hdp-hive::jdbc-connector::end'],
     }  
  }


   anchor { 'hdp-hive::jdbc-connector::end':}

}
