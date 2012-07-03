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
class hdp-hcat::mysql-connector()
{
  include hdp-hcat::params

  $url = $hdp-hcat::params::mysql_connector_url
  $zip_name = regsubst($url,'^.+/([^/]+$)','\1')
  $jar_name = regsubst($zip_name,'zip$','-bin.jar')
  $target = "${hdp::params::artifact_dir}/${zip_name}"
  $hcat_lib = $hdp-hcat::params::hcat_lib
  
  exec{ "curl ${url}":
    command => "mkdir -p ${artifact_dir} ; curl -f --retry 10 ${url} -o ${target} ",
    creates => $target,
    path    => ["/bin","/usr/bin/"]
  }
  exec{ "unzip ${target}":
    command => "unzip -o -j ${target} '*.jar' -x */lib/*",
    cwd     => $hcat_lib,
    user    => $hdp::params::hcat_user,
    group   => $hdp::params::hadoop_user_group,
    creates => "${hcat_lib}/${$jar_name}",
    path    => ["/bin","/usr/bin/"]
  }

  Exec["curl ${url}"] -> Exec["unzip ${target}"]
}
