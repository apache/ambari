'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import sys
import subprocess
import time

def replace_conf(server_ip):
    f = open("/etc/ambari-agent/conf/ambari-agent.ini")
    lines = f.readlines()
    f.close()

    f = open("/etc/ambari-agent/conf/ambari-agent.ini", "w+")
    for line in lines:
        line = line.replace("hostname=localhost", "hostname=" + server_ip)
        f.write(line)
    f.close()

def run_ambari_agent():
    # command = ["sudo", "ambari-agent", "start"]
    # subprocess.call(command)
    subprocess.call("./ambari_agent_start.sh")

# add all the hostnames of other containers to /etc/hosts
def add_hostnames():
    etc_hosts = open("/etc/hosts", "a")
    etc_hosts.write("\n")

    docker_hosts = open("/hosts")
    for line in docker_hosts.readlines():
        etc_hosts.write(line)
    docker_hosts.close()

    etc_hosts.close()

def remove_default_hostname(hostname):
    etc_hosts = open("/etc/hosts")
    all_resolution = etc_hosts.readlines()
    etc_hosts.close()

    etc_hosts = open("/etc/hosts", "w")
    for line in all_resolution:
        if hostname not in line:
            etc_hosts.write(line)
        else:
            etc_hosts.write("#")
            etc_hosts.write(line)
    etc_hosts.close()

ambari_server_ip = sys.argv[1]
my_hostname = sys.argv[2]
replace_conf(ambari_server_ip)
remove_default_hostname(my_hostname)
add_hostnames()
run_ambari_agent()