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
class hdp-yarn::mapred2::service_check() inherits hdp-yarn::params
{
  $smoke_test_user = $hdp::params::smokeuser
  $hadoopMapredExamplesJarName = $hdp-yarn::params::hadoopMapredExamplesJarName
  $jar_path = "$hdp::params::hadoop_mapred2_jar_location/$hadoopMapredExamplesJarName"
  $input_file = "/user/${smoke_test_user}/mapredsmokeinput"
  $output_file = "/user/${smoke_test_user}/mapredsmokeoutput"
  $hadoop_conf_dir = $hdp::params::hadoop_conf_dir

  $cleanup_cmd = "fs -rm -r -f ${output_file} ${input_file}"
  $create_file_cmd = "fs -put /etc/passwd ${input_file}"
  $test_cmd = "fs -test -e ${output_file}"
  $run_wordcount_job = "jar $jar_path wordcount ${input_file} ${output_file}"

  anchor { 'hdp-yarn::mapred2::service_check::begin':}

  hdp-hadoop::exec-hadoop { 'mapred::service_check::cleanup_before':
    command   => $cleanup_cmd,
    tries     => 1,
    try_sleep => 5,
    user      => $smoke_test_user
  }

  hdp-hadoop::exec-hadoop { 'mapred::service_check::create_file':
    command   => $create_file_cmd,
    tries     => 1,
    try_sleep => 5,
    user      => $smoke_test_user
  }

  hdp-hadoop::exec-hadoop { 'mapred::service_check::run_wordcount':
    command   => $run_wordcount_job,
    tries     => 1,
    try_sleep => 5,
    user      => $smoke_test_user,
    logoutput => "true"
  }

  hdp-hadoop::exec-hadoop { 'mapred::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    user        => $smoke_test_user
  }

  anchor { 'hdp-yarn::mapred2::service_check::end':}

  Anchor['hdp-yarn::mapred2::service_check::begin'] -> Hdp-hadoop::Exec-hadoop['mapred::service_check::cleanup_before'] -> Hdp-hadoop::Exec-hadoop['mapred::service_check::create_file'] -> Hdp-hadoop::Exec-hadoop['mapred::service_check::run_wordcount'] -> Hdp-hadoop::Exec-hadoop['mapred::service_check::test'] -> Anchor['hdp-yarn::mapred2::service_check::end']

}
