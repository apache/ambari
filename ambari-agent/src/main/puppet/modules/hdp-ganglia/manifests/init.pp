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
class hdp-ganglia(
  $service_state
)
{
  if (($service_state != 'no_op') or ($service_state != 'uninstalled')) {
    include hdp-ganglia::params
    $gmetad_user = $hdp-ganglia::params::gmetad_user
    $gmond_user = $hdp-ganglia::params::gmond_user

    group { $gmetad_user :
      ensure => present
    }

    if ($gmetad_user != $gmond_user) {
      group { $gmond_user :
        ensure => present
      }
    }

    hdp::user { $gmond_user: 
      gid    => $gmond_user,
      groups => ["$gmond_user"]
    }
  
    if ( $gmetad_user != $gmond_user) {
      hdp::user { $gmetad_user: 
        gid    => $gmetad_user,
        groups => ["$gmetad_user"]
      }
    }

    anchor{'hdp-ganglia::begin':} -> Group<|title == $gmond_user or title == $gmetad_user|> -> User<|title == $gmond_user or title == $gmetad_user|> ->  anchor{'hdp-ganglia::end':}
  }
}

