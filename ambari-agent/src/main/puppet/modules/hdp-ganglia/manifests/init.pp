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
  if ! ($service_state in ['no_op', 'uninstalled']) {
    include hdp-ganglia::params
    $gmetad_user = $hdp-ganglia::params::gmetad_user
    $gmond_user = $hdp-ganglia::params::gmond_user

    hdp::group { 'gmetad_group' :
      group_name => $gmetad_user,
    }

    hdp::group { 'gmond_group':
      group_name => $gmond_user,
    }

    hdp::user { 'gmond_user': 
      user_name =>  $gmond_user,
      gid    => $gmond_user,
      groups => ["$gmond_user"]
    }
  
    hdp::user { 'gmetad_user':
      user_name => $gmetad_user,
      gid    => $gmetad_user,
      groups => ["$gmetad_user"]
    }

    anchor{'hdp-ganglia::begin':} -> Hdp::Group<|title == 'gmond_group' or title == 'gmetad_group'|> -> Hdp::User['gmond_user'] -> Hdp::User['gmetad_user'] ->  anchor{'hdp-ganglia::end':}
  }
}

