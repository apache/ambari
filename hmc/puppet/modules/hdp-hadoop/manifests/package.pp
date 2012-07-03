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
#singleton, but using define so can use collections to override params
define hdp-hadoop::package(
  $ensure = 'present',
  $include_32_bit = false,
  $include_64_bit = false
)
{
  #just use 32 if its specifically requested and no 64 bit requests
  if ($include_32_bit == true) and ($include_64_bit != true) {
    $size = 32
  } else  {
    $size = 64
  }
  $package = "hadoop ${size}"
  $lzo_enabled = $hdp::params::lzo_enabled

  hdp::package{ $package:
    ensure       => $ensure,
    package_type => 'hadoop',
    size         => $size,
    lzo_needed   => $lzo_enabled
  }
  anchor{ 'hdp-hadoop::package::helper::begin': } -> Hdp::Package[$package] -> anchor{ 'hdp-hadoop::package::helper::end': }
}
