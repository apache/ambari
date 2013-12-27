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
  $lzo_needed = false
  )
{

  $pt = $package_type ? {
    undef  => $name,
    default  => $package_type
  }
  
  hdp::package::process_pkg { $name:
    ensure       => $ensure,
    package_type => $pt,
    size         => $size,
    java_needed  => $java_needed,
    lzo_needed   => $lzo_needed
  }
}

define hdp::package::process_pkg(
  $ensure = present,
  $package_type,
  $size,
  $java_needed,
  $lzo_needed
  )
{

  $stack_version = $hdp::params::stack_version
    

  debug("##Processing package:  $ensure,$package_type,$size,$java_needed,$lzo_needed")

  include hdp::params

  if hdp_is_empty($hdp::params::package_names[$package_type]) {
    hdp_fail("No packages for $package_type")
  }
  
  ## Process packages depending on stack
  if hdp_is_empty($hdp::params::package_names[$package_type][$stack_version]) {

    if hdp_is_empty($hdp::params::package_names[$package_type][ALL]) {
      hdp_fail("No packages for $package_type")
    }
    else {
      $packages_list_by_stack = $hdp::params::package_names[$package_type][ALL]
    }
  }
  else {
    $packages_list_by_stack = $hdp::params::package_names[$package_type][$stack_version]
  }
  
  debug("##Pkg for stack: $packages_list_by_stack")
  
  ## Process packages depending on arch
  if hdp_is_empty($packages_list_by_stack[$size]) {

    if hdp_is_empty($packages_list_by_stack[ALL]) {
      hdp_fail("No packages for $package_type")
    }
    else {
      $packages_list_by_size = $packages_list_by_stack[ALL]
    }
  }
  else {
    $packages_list_by_size = $packages_list_by_stack[$size]

  }
  
  debug("##Pkg for arch: $packages_list_by_size")
  
  ## Process packages depending on os
  if hdp_is_empty($packages_list_by_size[$hdp::params::hdp_os_type]) {

    if hdp_is_empty($packages_list_by_size[ALL]) {
      hdp_fail("No packages for $package_type")
    }
    else {
      $packages_list = $packages_list_by_size[ALL]
    }
  }
  else {
    $packages_list = $packages_list_by_size[$hdp::params::hdp_os_type]
  }

  debug("##Packages list: $packages_list")

  if (($java_needed == true) and ($ensure == 'present')){
    hdp::java::package{ $name:
      size                 => $size
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
  if $packages_list != $hdp::params::NOTHING {
    package{ $packages_list:
      ensure   => $ensure_actual,
      tag      => $tag
    }
  }
  anchor{ "hdp::package::${name}::begin": } -> Package<|tag == $tag|> -> anchor{ "hdp::package::${name}::end": }
  
  if (($java_needed == true)and ($ensure == 'present')) {
   Anchor["hdp::package::${name}::begin"] -> Hdp::Java::Package[$name] -> Anchor["hdp::package::${name}::end"] 
  }
}

# Removes the specified package using shell command appropriate for current OS type.
# Method DOES NOT resolve package name via hdp::params.
# If package does not exist or is not installed, command does nothing.
define hdp::package::remove_pkg(
    $package_type,
  )
{

  # TODO: For non-rpm based systems, provide appropriate command
  exec { "remove_package ${package_type}":
    path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
    command => $hdp::params::hdp_os_type ? {
      default => "rpm -e --allmatches ${package_type} ; true"
    },
  }

}
