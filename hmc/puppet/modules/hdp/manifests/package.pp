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
define hdp::package(
  $ensure = present,
  $package_type = undef,
  $size = 64,
  $java_needed = true,
  $lzo_needed = false,
  $provider = yum
  )
{
 
  $pt = $package_type ? {
    undef  => $name,
    default  => $package_type
  }
  
  case $provider {
    'yum': { 
      hdp::package::yum { $name:
        ensure       => $ensure,
        package_type => $pt,
        size         => $size,
        java_needed  => $java_needed,
        lzo_needed   => $lzo_needed
      }
    }
    default: {
      hdp_fail("No support for provider ${provider}")
    }
  }
}

define hdp::package::yum(
  $ensure = present,
  $package_type,
  $size,
  $java_needed,
  $lzo_needed
  )
{
    
  include hdp::params
 
  $package_type_info = $hdp::params::package_names[$package_type]
  if hdp_is_empty($package_type_info) {
    hdp_fail("Cannot find info about package type ${package_type}") 
  }
  $package_name = $package_type_info[$size]
  if hdp_is_empty($package_name) {
    hdp_fail("Cannot find package ${package_type} of size ${size}")
  }
  
  if (($java_needed == true) and ($ensure == 'present')){
    hdp::java::package{ $name:
      size                 => $size,
      include_artifact_dir => true
    }
  }

  if (($lzo_needed == true) and ($ensure == 'present')){
    Hdp::Lzo::Package<|title == $size|>
  }

  if ($ensure == 'uninstalled') {
    $ensure_actual = 'purged'
  } else {
    $ensure_actual = $ensure
  }
  $tag = regsubst($name,' ','-',G)
  package{ $package_name:
    ensure   => $ensure_actual,
    provider => yum,
    tag      => $tag
  }
  anchor{ "hdp::package::${name}::begin": } -> Package<|tag == $tag|> -> anchor{ "hdp::package::${name}::end": }
  
  if (($java_needed == true)and ($ensure == 'present')) {
   Anchor["hdp::package::${name}::begin"] -> Hdp::Java::Package[$name] -> Anchor["hdp::package::${name}::end"] 
  }
}

