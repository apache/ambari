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
    --puppet-master     Puppet Master
    --repo-file         Repo File
    --gpg-key-files     GPG Key files - comma-separated
    --using-local-repo  Whether local repo is being used
  "
}

OPTS=$(getopt \
  -n $0 \
  -o '' \
  -l 'puppet-master:' \
  -l 'repo-file:' \
  -l 'using-local-repo' \
  -l 'gpg-key-files:' \
  -l 'help' \
  -- "$@")

if [ $? != 0 ] ; then
  usage
  echo "Invalid args" >&2
  exit 3
fi

echo "DEBUG: opts ${OPTS}"

USINGLOCALREPO=0

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
    --using-local-repo)
      USINGLOCALREPO=1; shift
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
usingLocalRepo=${USINGLOCALREPO}

echo "DEBUG: Puppet Master: ${master}"
echo "DEBUG: Repo File: ${repoFile}"
echo "DEBUG: GPG Key File Locations: ${gpgKeyFiles}"

osType=''
osMajorVersion=''
if [ -f /usr/bin/lsb_release ] ; then
  osType=$( lsb_release -sd | tr '[:upper:]' '[:lower:]' | tr '"' ' ' | awk '{ for(i=1; i<=NF; i++) { if ( $i ~ /[0-9]+/ ) { cnt=split($i, arr, "."); if ( cnt > 1) { print arr[1] } else { print $i; } break; } print $i; } }' )
  osMajorVersion=`lsb_release -sd | tr '[:upper:]' '[:lower:]' | tr '"' ' ' | awk '{ for(i=1; i<=NF; i++) { if ( $i ~ /[0-9]+/ ) { cnt=split($i, arr, "."); if ( cnt > 1) { print arr[1] } else { print $i; } break; } } }'`
else
  osType=$( cat `ls /etc/*release | grep "redhat\|SuSE"` | head -1 | awk '{ for(i=1; i<=NF; i++) { if ( $i ~ /[0-9]+/ ) { cnt=split($i, arr, "."); if ( cnt > 1) { print arr[1] } else { print $i; } break; } print $i; } }' | tr '[:upper:]' '[:lower:]' )
  osMajorVersion=`cat \`ls /etc/*release | grep "redhat\|SuSE"\` | head -1 | awk '{ for(i=1; i<=NF; i++) { if ( $i ~ /[0-9]+/ ) { cnt=split($i, arr, "."); if ( cnt > 1) { print arr[1] } else { print $i; } break; } } }' | tr '[:upper:]' '[:lower:]'`
fi

osType=`echo ${osType} | sed -e "s/ *//g"`

osArch=`uname -m`
if [[ "xi686" == "x${osArch}" || "xi386" == "x${osArch}" ]]; then
  osArch="i386"
fi
if [[ "xx86_64" == "x${osArch}" || "xamd64" == "x${osArch}" ]]; then
  osArch="x86_64"
fi

echo "DEBUG: OS Type ${osType}"
echo "DEBUG: OS Arch ${osArch}"
echo "DEBUG: OS Major Version ${osMajorVersion}"

if [[ ! -f ${repoFile} ]]; then
  echo "Error: Repo file ${repoFile} does not exist" >&2
  exit 3
else
  echo "Copying $repoFile to /etc/yum.repos.d/ for ${osType} ${osMajorVersion}"
  if [[ "x${usingLocalRepo}" == "x0" ]]; then
    osReplaceStr=""
    if [[ "x${osMajorVersion}" == "x5" ]]; then
      osReplaceStr="centos5"
    fi
    if [[ "x${osMajorVersion}" == "x6" ]]; then
      osReplaceStr="centos6"
    fi
    if [[ "x${osReplaceStr}" != "x" ]]; then
      sed -i -e "s/centos[0-9]/${osReplaceStr}/g" ${repoFile}
    fi
  fi
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

#check if epel repo is installed if not try installing
#only needed if non-local repo mode

epelVer="";
if [[ "x${osMajorVersion}" == "x5" ]]; then
  epelVer="5-4"
fi
if [[ "x${osMajorVersion}" == "x6" ]]; then
  epelVer="6-7"
fi

# Assumption that earlier stage is already doing a check on valid os types
# so should not reach here for non-CentOS/RHEL 5/6 hosts
epelRPMUrl="http://download.fedoraproject.org/pub/epel/${osMajorVersion}/${osArch}/epel-release-${epelVer}.noarch.rpm"

echo "Using local repo setting is ${usingLocalRepo}"
if [[ "${usingLocalRepo}" == "0" ]]; then
  echo "Checking to see if epel needs to be installed"
  epel_installed=`yum repolist enabled | grep epel`
  if [[ "x$epel_installed" != "x" ]]; then
    echo "Already Installed epel repo"
  else
    echo "Installing epel-release rpm from ${epelRPMUrl}"
    mkdir -p /tmp/HDP-artifacts/
    curl -L -f --retry 10 $epelRPMUrl -o /tmp/HDP-artifacts/epel-release-${osMajorVersion}.noarch.rpm
    rpm -Uvh /tmp/HDP-artifacts/epel-release-${osMajorVersion}.noarch.rpm
    #make sure epel is installed else fail
    epel_installed=`yum repolist enabled | grep epel`
    if [[ "x$epel_installed" == "x" ]]; then
      echo "$host:_ERROR_:retcode:[1], CMD:[rpm -Uvh $epelRPMUrl]: OUT:[Not Installed]" >&2
      exit 1
    fi
  fi
else
  echo "Skipping epel check+install as local repo mode is enabled"
fi

echo "Installing yum priorities plugin"
if [[ "x${osMajorVersion}" == "x5" ]]; then
  out=`yum install -y yum-priorities`
  ret=$?
  if [[ "$ret" != "0" ]]; then
    echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
    exit 1
  fi
fi
if [[ "x${osMajorVersion}" == "x6" ]]; then
  out=`yum install -y yum-plugin-priorities`
  ret=$?
  if [[ "$ret" != "0" ]]; then
    echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
    exit 1
  fi
fi

echo "Installing puppet using yum"
out=`yum install -y ambari-agent`
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
  exit 1
fi

#Install ruby
out=`yum install -y ruby-devel rubygems`
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
  exit 1
fi
out=`echo $master > /etc/hmc/ambari-agent.conf`
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
out=`service ambari-agent stop`
ret=$?

echo "Starting puppet agent for HMC"
out=`service ambari-agent start`
ret=$?
if [[ "$ret" != "0" ]]; then
  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
  exit 1
fi
echo "Setting chkconfig for HMC"
out=`chkconfig --add ambari-agent`
ret=$?
#if [[ "$ret" != "0" ]]; then
#  echo "$host:_ERROR_:retcode:[$ret], CMD:[$pp_cmd]: OUT:[$out]" >&2
#  exit 1
#fi
exit 0
