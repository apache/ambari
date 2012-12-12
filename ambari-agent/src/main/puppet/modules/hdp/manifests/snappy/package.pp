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
class hdp::snappy::package()
{
 hdp::package {'snappy':
    package_type  => 'snappy',
    java_needed   => false
  }
  
  hdp::snappy::package::ln{ 64:} 
  hdp::snappy::package::ln{ 32:} 
  
  anchor{'hdp::snappy::package::begin':} ->  Hdp::Package['snappy'] -> Hdp::Snappy::Package::Ln<||> -> anchor{'hdp::snappy::package::end':}
}

define hdp::snappy::package::ln()
{
  $size = $name
  $hadoop_home = $hdp::params::hadoop_home  
  $snappy_so = $hdp::params::snappy_so
  $so_target_dir = $hdp::params::snappy_compression_so_dirs[$size]
  $so_target = "${so_target_dir}/libsnappy.so"
  $so_src_dir = $hdp::params::snappy_so_src_dir[$size]
  $so_src = "${so_src_dir}/${snappy_so}" 
  
  if ($so_target != $so_src) { 
    $ln_cmd = "mkdir -p $so_target_dir; ln -sf ${so_src} ${so_target}"
    hdp::exec{ "hdp::snappy::package::ln ${name}":
      command => $ln_cmd,
      unless  => "test -f ${so_target}",
      creates => $so_target
    }
  }
}
