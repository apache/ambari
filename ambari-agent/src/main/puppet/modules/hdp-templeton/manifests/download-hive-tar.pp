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
class hdp-templeton::download-hive-tar()
{
  include hdp-templeton::params

  $src_tar_name = $hdp-templeton::params::src_hive_tar_name
  $dest_tar_name = $hdp-templeton::params::dest_hive_tar_name
  $target = "${hdp::params::artifact_dir}/${dest_tar_name}"
 
  anchor { 'hdp-templeton::download-hive-tar::begin':}         

   hdp::package { 'webhcat-tar-hive' :
     require   => Anchor['hdp-templeton::download-hive-tar::begin']                                                              
   }
  
#   hdp::exec { 'hive mkdir -p ${artifact_dir} ;  cp /tmp/HDP-templeton/${src_tar_name} ${target}':
#       command => "mkdir -p ${artifact_dir} ;  cp /tmp/HDP-templeton/${src_tar_name} ${target}",
#       unless  => "test -f ${target}",
#       creates => $target,
#       path    => ["/bin","/usr/bin/"],
#       require => Hdp::Package['webhcat-tar-hive'],
#       notify  =>  Anchor['hdp-templeton::download-hive-tar::end'],
#   }

   anchor { 'hdp-templeton::download-hive-tar::end':}       

}
