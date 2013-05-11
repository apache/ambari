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
define hdp::lzo::package()
{
  $size = $name

  $pkg_type = "lzo"

  hdp::package {"lzo ${size}":
    package_type  => "${pkg_type}",
    size          => $size,
    java_needed   => false
  }

  $anchor_beg = "hdp::lzo::package::${size}::begin"
  $anchor_end = "hdp::lzo::package::${size}::end"
  anchor{$anchor_beg:} ->  Hdp::Package["lzo ${size}"] -> anchor{$anchor_end:}
}
