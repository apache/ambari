# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information rega4rding copyright ownership.
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

#!/bin/bash
# run this script with root
# $1 <Weave internal IP with mask>
# $2 <hostname files with all agents>

if [ $# -lt 2 ]; then
    echo "usage: ./sever_setup.sh <Weave internal IP with mask> <hostname file with all agents>"
    echo "example: ./server_setup.sh 192.168.10.10/16 /user/simulator-script/hosts.txt"
    echo "note: the hostname file is generated automatically when you request a cluster"
    exit 1
fi

# install weave
chmod 755 ./Linux/CentOS7/weave_install.sh
./Linux/CentOS7/weave_install.sh

# reset weave
weave reset

# launch weave
weave launch

# expose IP
weave expose $1

# add hosname of all agents
cat $2 >> /etc/hosts3