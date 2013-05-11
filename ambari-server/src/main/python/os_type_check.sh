#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

C5="centos5"
C6="centos6"
RH5="redhat5"
RH6="redhat6"
SLES11="sles11"
SUSE11="suse11"
OL5="oraclelinux5"
OL6="oraclelinux6"

cluster_os=$1
current_os="N/A"

pattern='[^[:digit:]]*'

# OS type detection
if [ -f  "/etc/centos-release" ] 
then
  grep -qE "${pattern}6" /etc/centos-release && current_os=$C6
  grep -qE "${pattern}5" /etc/centos-release && current_os=$C5
elif [ -f  "/etc/oracle-release" ] || [ -f  "/etc/ovs-release" ] || [ -f  "/etc/enterprise-release" ] 
then
  grep -sqE "${pattern}6" /etc/oracle-release || grep -sqE "${pattern}6" /etc/ovs-release || \
    grep -sqE "${pattern}6" /etc/enterprise-release && current_os=$OL6
  grep -sqE "${pattern}5" /etc/oracle-release || grep -sqE "${pattern}5" /etc/ovs-release || \
      grep -sqE "${pattern}5" /etc/enterprise-release && current_os=$OL5
elif [ -f  "/etc/redhat-release" ]
then
  grep -sqiE "^centos ${pattern}5" /etc/redhat-release && current_os=$C5
  grep -sqE "^Red Hat Enterprise Linux Server ${pattern}6" /etc/redhat-release && current_os=$RH6
  grep -sqE "^Red Hat Enterprise Linux Server ${pattern}5" /etc/redhat-release && current_os=$RH5
elif [ -f  "/etc/SuSE-release" ]
then
  grep -sqE "${pattern}11" /etc/SuSE-release && current_os=$SUSE11
  grep -sqi "SUSE LINUX Enterprise Server" /etc/SuSE-release && [ "$current_os" = "$SUSE11" ] && current_os=$SLES11
fi


echo "Cluster primary OS type is $cluster_os and local OS type is $current_os"

# Compatibility check
res=1
case "$cluster_os" in

  $C5|$RH5|$OL5)
    [ "$current_os" = "$C5" ] || [ "$current_os" = "$RH5" ] || [ "$current_os" = "$OL5" ] && res=0 
  ;;

  $C6|$RH6|$OL6)
    [ "$current_os" = "$C6" ] || [ "$current_os" = "$RH6" ] || [ "$current_os" = "$OL6" ] && res=0 
  ;;

  $SUSE11|$SLES11)
    [ "$current_os" = "$SUSE11" ] || [ "$current_os" = "$SLES11" ] && res=0 
  ;;

  *)
    res=1
  ;;
esac

[[ $res -ne 0 ]] && echo "Local OS is not compatible with cluster primary OS. Please perform manual bootstrap on this host."

exit $res
