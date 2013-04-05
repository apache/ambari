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
class hdp-hadoop::smoketest(
  $opts={}
)
{
  #TODO: put in wait
  #TODO: look for better way to compute outname
  $date_format = '"%M%d%y"'
  $outname = inline_template("<%=  `date +${date_format}`.chomp %>")

  #TODO: hardwired to run on namenode and to use user hdfs

  $put = "dfs -put /etc/passwd passwd-${outname}"
  $exec = "jar /usr/share/hadoop/hadoop-examples-*.jar wordcount passwd-${outname} ${outname}.out"
  $result = "fs -test -e ${outname}.out /dev/null 2>&1"
  anchor{ "hdp-hadoop::smoketest::begin" :} ->
  hdp-hadoop::exec-hadoop{ $put:
    command => $put
  } ->
  hdp-hadoop::exec-hadoop{ $exec:
    command =>  $exec
  } ->
  hdp-hadoop::exec-hadoop{ $result:
    command =>  $result
  } ->
  anchor{ "hdp-hadoop::smoketest::end" :}
}
