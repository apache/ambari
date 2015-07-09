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


# Set the bas image to CentOS 6.x
FROM centos:7

# Author
MAINTAINER Pengcheng_Xu

# Copy the files into Docker: Agent package file
ADD agent.rpm /agent.rpm
# Copy the files into Docker: launcher_agent.py
ADD launcher_agent.py /launcher_agent.py
# Copy the files into Docker: ambari_agent_start.sh
ADD ambari_agent_start.sh /ambari_agent_start.sh
# Copy hostname file into Docker: hosts
ADD hosts /hosts

RUN chmod 755 /ambari_agent_start.sh

# Install ambari-agent
# RUN yum install -y /agent.rpm
RUN yum install -y wget
RUN wget -O /etc/yum.repos.d/ambari.repo http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos7/2.x/latest/2.1.0/ambaribn.repo
RUN yum install -y epel-release
RUN yum install -y ambari-agent