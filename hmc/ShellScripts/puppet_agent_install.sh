#!/bin/bash
#*
#/*
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

#
#  /* This script takes three arguments,
#   * - sshkey: if not specified then ssh w/o key
#   * - repository information : to be added to remote node
#   * - list of hosts
#  */
#set -e
#set -x 
trap 'pp_cmd=$ppp_cmd; ppp_cmd=$previous_command; previous_command=$this_command; this_command=$BASH_COMMAND' DEBUG
#trap 'echo "$host: retcode:[$?] command:[$previous_command], out:[$out]"' EXIT
#printf 'Argument is __%s__\n' "$@"

master=$1
repo_name=$2
repo_desc=$3
repourl=$4
gpgkeyurl=$5

host=`hostname -f | tr '[:upper:]' '[:lower:]'`

#echo "$host:_ERROR_:$master, $repo_name, $repo_desc, $repourl, $gpgkeyurl"

repo_file_content=''
if [[ -z "$gpgkeyurl" ]]; then
  repo_file_content="[$repo_name]\nname=$repo_desc\nbaseurl=$repourl\nenabled=1\ngpgcheck=0"
else 
  repo_file_content="[$repo_name]\nname=$repo_desc\nbaseurl=$repourl\nenabled=1\ngpgcheck=1\ngpgkey=$gpgkeyurl"
fi

out=`echo -e $repo_file_content > /etc/yum.repos.d/$repo_name.repo`
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
  exit 1
fi


if [[ ! -z "$gpgkeyurl" ]]; then
  out=`rpm --import $gpgkeyurl`
  ret=$?
  if [[ "$ret" != "0" ]]; then
    echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
    exit 1
  fi
fi

out=`rpm -Uvh http://dl.fedoraproject.org/pub/epel/5/i386/epel-release-5-4.noarch.rpm 2>&1`
out=`/etc/init.d/iptables stop 1>/dev/null`
out=`yum install -y puppet-2.7.9-2`
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
  exit 1
fi
out=`mkdir -p /etc/puppet/agent 2>&1`
agent_auth_conf="path /run\nauth any\nallow $master\n\npath /\nauth any"
out=`echo -e $agent_auth_conf > /etc/puppet/agent/auth.conf`
out=`touch /etc/puppet/agent/namespaceauth.conf`
out=`cp -f /etc/puppet/puppet.conf /etc/puppet/agent/ 2>&1`
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
  exit 1
fi
#TODO clean this up for better fix. For now make sure we stop puppet agent. The issue here is we do not know if we started this puppet agent during our run or not.
out=`service puppet stop`
ret=$?
out=`puppet agent --verbose --confdir=/etc/puppet/agent --listen --runinterval 5 --server $master --report --no-client --waitforcert 10 --configtimeout 600 --debug --logdest=/var/log/puppet_agent.log --httplog /var/log/puppet_agent_http.log --autoflush 2>&1`
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
  exit 1
fi
exit 0
