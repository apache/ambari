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
class hdp-templeton::download-pig-tar()
{
  include hdp-templeton::params

  $src_tar_name = $hdp-templeton::params::src_pig_tar_name
  $dest_tar_name = $hdp-templeton::params::dest_pig_tar_name
  $target = "${hdp::params::artifact_dir}/${dest_tar_name}"

  anchor { 'hdp-templeton::download-pig-tar::begin':}

   hdp::package { 'webhcat-tar-pig' :
     require   => Anchor['hdp-templeton::download-pig-tar::begin']
   }

#   hdp::exec { 'pig ; mkdir -p ${artifact_dir} ;  cp /tmp/HDP-templeton/${src_tar_name} ${target}':
#       command => "mkdir -p ${artifact_dir} ;  cp /tmp/HDP-templeton/${src_tar_name} ${target}",
#       unless  => "test -f ${target}",
#       creates => $target,
#       path    => ["/bin","/usr/bin/"],
#       require => Hdp::Package['webhcat-tar-pig'],
#       notify  =>  Anchor['hdp-templeton::download-pig-tar::end'],
#   }

   anchor { 'hdp-templeton::download-pig-tar::end':}

}
