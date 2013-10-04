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

define hdp::java::jce::package(
  $java_home_dir, $jdk_location, $jdk_bin
)
{
  include hdp::params

  $jce_policy_zip = $hdp::params::jce_policy_zip
  $artifact_dir = $hdp::params::artifact_dir
  $jce_curl_target = "${artifact_dir}/${jce_policy_zip}"

  if $jdk_location != '' and $jdk_bin != ''  {
    $jce_location = regsubst($jdk_location, $jdk_bin, '')
  } else {
    $jce_location = $hdp::params::jce_location
  }

  #TODO:SUHAS how to avoid redownload and install if correct version already present.
  # may be check the file sizes for local_policy and export_US policy jars? 
  # UNLESS  => "test -e ${java_exec}"
  # curl -k - ignoring unverified server ssl sertificate,
  $curl_cmd = "mkdir -p ${artifact_dir}; curl -kf --retry 10 ${jce_location}/${jce_policy_zip} -o ${jce_curl_target}; echo 0"
  exec{ "jce-download ${name}":
    command => $curl_cmd,
    creates => $jce_curl_target,
    path    => ["/bin","/usr/bin/"],
    unless => "test -e ${jce_curl_target}"
  }

  $security_dir = "${java_home_dir}/jre/lib/security"
  $cmd = "rm -f local_policy.jar; rm -f US_export_policy.jar; unzip -o -j -q ${jce_curl_target}" 
  exec { "jce-install ${name}":
    command => $cmd,
    onlyif  => "test -e ${security_dir} && test -f ${jce_curl_target}",
    cwd     => $security_dir,
    path    => ['/bin/','/usr/bin']
  }

  #TODO: SUHAS add ensure code to check local and us export policy files exist -> File["${java_exec} ${name}"]

  anchor{"hdp::java::jce::package::${name}::begin":} -> Exec["jce-download ${name}"] ->  Exec["jce-install ${name}"] -> anchor{"hdp::java::jce::package::${name}::end":}
}
