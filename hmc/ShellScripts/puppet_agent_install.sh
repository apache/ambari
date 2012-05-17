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

usage() {
  echo "
Usage: $0 with the following parameters
    --puppet-master            Puppet Master
    --repo-file         Repo File
    --gpg-key-files     GPG Key files - comma-separated
  "
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'puppet-master:' \
  -l 'repo-file:' \
  -l 'gpg-key-files:' \
  -l 'help' \
  -- "$@")

if [ $? != 0 ] ; then
  usage
  echo "Invalid args" >&2
  exit 3
fi

echo "DEBUG: opts ${OPTS}"

eval set -- "${OPTS}"
while true ; do
  case "$1" in
    --puppet-master)
      MASTER=$2 ; shift 2
      ;;
    --repo-file)
      REPOFILE=$2 ; shift 2
      ;;
    --gpg-key-files)
      GPGKEYFILESTR=$2 ; shift 2
      ;;
    --help)
      usage ;
      exit 0
      ;;
    --)
      shift ; break
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ "x" == "x${MASTER}" ]]; then
  echo "Error: Puppet master not specified" >&2
  exit 3
fi

if [[ "x" == "x${REPOFILE}" ]]; then
  echo "Error: Repo file not specified" >&2
  exit 3
fi

if [[ "x" != "x${GPGKEYFILESTR}" ]]; then
  GPGKEYFILES=$(echo ${GPGKEYFILESTR} | tr "," " ")
fi

master=${MASTER}
repoFile=${REPOFILE}
gpgKeyFiles=${GPGKEYFILES}

echo "DEBUG: Puppet Master: ${master}"
echo "DEBUG: Repo File: ${repoFile}"
echo "DEBUG: GPG Key File Locations: ${gpgKeyFiles}"

if [[ ! -f ${repoFile} ]]; then
  echo "Error: Repo file ${repoFile} does not exist" >&2
  exit 3
else
  echo "Copying $repoFile to /etc/yum.repos.d/"
  cp -f $repoFile /etc/yum.repos.d/
fi

repoFileName=`basename $repoFile`
if [[ ! -f "/etc/yum.repos.d/${repoFileName}" ]]; then
  echo "Error: Repo file ${repoFile} not copied over to /etc/yum.repos.d/" >&2
  exit 3
fi

for gpgKeyFile in ${gpgKeyFiles}
do
  if [[ ! -f ${gpgKeyFile} ]]; then
    echo "Error: Specified GPG key file ${gpgKeyFile} does not exist" >&2
    exit 3
  fi
  echo "Copying ${gpgKeyFile} to /etc/pki/rpm-gpg/"
  cp -f ${gpgKeyFile} /etc/pki/rpm-gpg/
  gpgKeyFileName=`basename ${gpgKeyFile}`
  if [[ ! -f "/etc/pki/rpm-gpg/${gpgKeyFileName}" ]]; then
    echo "Error: GPG key file ${gpgKeyFile} not copied over to /etc/pki/rpm-gpg/" >&2
    exit 3
  fi
done

host=`hostname -f | tr '[:upper:]' '[:lower:]'`

out=`/etc/init.d/iptables stop 1>/dev/null`

echo "Installing puppet using yum"
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
echo "Stopping puppet agent using service stop command"
out=`service puppet stop`
ret=$?

echo "Starting puppet agent for HMC"
out=`puppet agent --verbose --confdir=/etc/puppet/agent --listen --runinterval 5 --server $master --report --no-client --waitforcert 10 --configtimeout 600 --debug --logdest=/var/log/puppet_agent.log --httplog /var/log/puppet_agent_http.log --autoflush 2>&1`
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
  exit 1
fi
exit 0
