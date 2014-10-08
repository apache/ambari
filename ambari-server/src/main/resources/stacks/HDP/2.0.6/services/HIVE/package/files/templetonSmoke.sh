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
export smoke_user_keytab=$3
export security_enabled=$4
export kinit_path_local=$5
export ttonurl="http://${ttonhost}:50111/templeton/v1"

if [[ $security_enabled == "true" ]]; then
  kinitcmd="${kinit_path_local}  -kt ${smoke_user_keytab} ${smoke_test_user}; "
else
  kinitcmd=""
fi

export no_proxy=$ttonhost
cmd="${kinitcmd}curl --negotiate -u : -s -w 'http_code <%{http_code}>'    $ttonurl/status 2>&1"
retVal=`su -s /bin/bash - ${smoke_test_user} -c "$cmd"`
httpExitCode=`echo $retVal |sed 's/.*http_code <\([0-9]*\)>.*/\1/'`

if [[ "$httpExitCode" -ne "200" ]] ; then
  echo "Templeton Smoke Test (status cmd): Failed. : $retVal"
  export TEMPLETON_EXIT_CODE=1
  exit 1
fi

exit 0

#try hcat ddl command
echo "user.name=${smoke_test_user}&exec=show databases;" /tmp/show_db.post.txt
cmd="${kinitcmd}curl --negotiate -u : -s -w 'http_code <%{http_code}>' -d  \@${destdir}/show_db.post.txt  $ttonurl/ddl 2>&1"
retVal=`su -s /bin/bash - ${smoke_test_user} -c "$cmd"`
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

#try pig query
outname=${smoke_test_user}.`date +"%M%d%y"`.$$;
ttonTestOutput="/tmp/idtest.${outname}.out";
ttonTestInput="/tmp/idtest.${outname}.in";
ttonTestScript="idtest.${outname}.pig"

echo "A = load '$ttonTestInput' using PigStorage(':');"  > /tmp/$ttonTestScript
echo "B = foreach A generate \$0 as id; " >> /tmp/$ttonTestScript
echo "store B into '$ttonTestOutput';" >> /tmp/$ttonTestScript

#copy pig script to hdfs
su -s /bin/bash - ${smoke_test_user} -c "hadoop dfs -copyFromLocal /tmp/$ttonTestScript /tmp/$ttonTestScript"

#copy input file to hdfs
su -s /bin/bash - ${smoke_test_user} -c "hadoop dfs -copyFromLocal /etc/passwd $ttonTestInput"

#create, copy post args file
echo -n "user.name=${smoke_test_user}&file=/tmp/$ttonTestScript" > /tmp/pig_post.txt

#submit pig query
cmd="curl -s -w 'http_code <%{http_code}>' -d  \@${destdir}/pig_post.txt  $ttonurl/pig 2>&1"
retVal=`su -s /bin/bash - ${smoke_test_user} -c "$cmd"`
httpExitCode=`echo $retVal |sed 's/.*http_code <\([0-9]*\)>.*/\1/'`
if [[ "$httpExitCode" -ne "200" ]] ; then
  echo "Templeton Smoke Test (pig cmd): Failed. : $retVal"
  export TEMPLETON_EXIT_CODE=1
  exit 1
fi

exit 0
