#!/usr/bin/env bash
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

export ttonhost=$1
export smoke_test_user=$2
export templeton_port=$3
export ttonTestScript=$4
export has_pig=$5
export smoke_user_keytab=$6
export security_enabled=$7
export kinit_path_local=$8
export smokeuser_principal=$9
export tmp_dir=${10}
export ttonurl="http://${ttonhost}:${templeton_port}/templeton/v1"

if [[ $security_enabled == "true" ]]; then
  kinitcmd="${kinit_path_local}  -kt ${smoke_user_keytab} ${smokeuser_principal}; "
else
  kinitcmd=""
fi

export no_proxy=$ttonhost
cmd="${kinitcmd}curl --negotiate -u : -s -w 'http_code <%{http_code}>'  $ttonurl/status 2>&1"
retVal=`/var/lib/ambari-agent/ambari-sudo.sh su ${smoke_test_user} -s /bin/bash - -c "$cmd"`
httpExitCode=`echo $retVal |sed 's/.*http_code <\([0-9]*\)>.*/\1/'`

# try again for 2.3 username requirement
if [[ "$httpExitCode" == "500" ]] ; then
  cmd="${kinitcmd}curl --negotiate -u : -s -w 'http_code <%{http_code}>'  $ttonurl/status?user.name=$smoke_test_user 2>&1"
  retVal=`/var/lib/ambari-agent/ambari-sudo.sh su ${smoke_test_user} -s /bin/bash - -c "$cmd"`
  httpExitCode=`echo $retVal |sed 's/.*http_code <\([0-9]*\)>.*/\1/'`
fi

if [[ "$httpExitCode" -ne "200" ]] ; then
  echo "Templeton Smoke Test (status cmd): Failed. : $retVal"
  export TEMPLETON_EXIT_CODE=1
  exit 1
fi

#try hcat ddl command
/var/lib/ambari-agent/ambari-sudo.sh rm -f ${tmp_dir}/show_db.post.txt
echo "user.name=${smoke_test_user}&exec=show databases;" > ${tmp_dir}/show_db.post.txt
/var/lib/ambari-agent/ambari-sudo.sh chown ${smoke_test_user} ${tmp_dir}/show_db.post.txt
cmd="${kinitcmd}curl --negotiate -u : -s -w 'http_code <%{http_code}>' -d  @${tmp_dir}/show_db.post.txt  $ttonurl/ddl 2>&1"
retVal=`/var/lib/ambari-agent/ambari-sudo.sh su ${smoke_test_user} -s /bin/bash - -c "$cmd"`
httpExitCode=`echo $retVal |sed 's/.*http_code <\([0-9]*\)>.*/\1/'`

if [[ "$httpExitCode" -ne "200" ]] ; then
  echo "Templeton Smoke Test (ddl cmd): Failed. : $retVal"
  export TEMPLETON_EXIT_CODE=1
  exit  1
fi

# NOT SURE?? SUHAS
if [[ $security_enabled == "true" ]]; then
  echo "Templeton Pig Smoke Tests not run in secure mode"
  exit 0
fi

if [[ $has_pig != "True" ]]; then
  echo "Templeton Pig Smoke Tests are not run, because Pig is not installed"
  exit 0
fi

#try pig query

#create, copy post args file
/var/lib/ambari-agent/ambari-sudo.sh rm -f ${tmp_dir}/pig_post.txt
echo -n "user.name=${smoke_test_user}&file=/tmp/$ttonTestScript" > ${tmp_dir}/pig_post.txt
/var/lib/ambari-agent/ambari-sudo.sh chown ${smoke_test_user} ${tmp_dir}/pig_post.txt

#submit pig query
cmd="curl --negotiate -u : -s -w 'http_code <%{http_code}>' -d  @${tmp_dir}/pig_post.txt  $ttonurl/pig 2>&1"
retVal=`/var/lib/ambari-agent/ambari-sudo.sh su ${smoke_test_user} -s /bin/bash - -c "$cmd"`
httpExitCode=`echo $retVal |sed 's/.*http_code <\([0-9]*\)>.*/\1/'`
if [[ "$httpExitCode" -ne "200" ]] ; then
  echo "Templeton Smoke Test (pig cmd): Failed. : $retVal"
  export TEMPLETON_EXIT_CODE=1
  exit 1
fi

exit 0
