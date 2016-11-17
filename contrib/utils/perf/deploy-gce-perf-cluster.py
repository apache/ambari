#!/usr/bin/env python
"""
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
"""

import argparse
import os
import subprocess
import sys
import pprint
import time
import traceback
import re
import socket

cluster_name="perf-cluster"
ambari_repo_file_url="http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/latest/trunk/ambaribn.repo"

public_hostname_script="foo"
hostname_script="foo"

start_number=1
number_of_agents_on_host=50


class SSH:
  """ Ssh implementation of this """
  def __init__(self, user, sshkey_file, host, command, custom_option='', errorMessage = None):
    self.user = user
    self.sshkey_file = sshkey_file
    self.host = host
    self.command = command
    self.errorMessage = errorMessage
    self.custom_option = custom_option
    pass


  def run(self):
    sshcommand = ["ssh",
                  "-o", "ConnectTimeOut=180",
                  "-o", "StrictHostKeyChecking=no",
                  "-o", "BatchMode=yes",
                  self.custom_option,
                  "-i", self.sshkey_file,
                  self.user + "@" + self.host, self.command]

    if not self.custom_option:
      del sshcommand[7]

    i = 1
    while True:
      try:
        sshstat = subprocess.Popen(sshcommand, stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE)
        log = sshstat.communicate()
        if sshstat.returncode != 0:
          print "Executing SSH command on {0} failed: {1}".format(self.host, log)
          print "\nRetrying SSH command one more time!"
          if i >= 3:
            break
          i += 1
          time.sleep(10)
          continue
        break
      except:
        print "Could not SSH to {0}, waiting for it to start".format(self.host)
        i += 1
        time.sleep(10)

    if i >= 3:
      print "Could not execute remote ssh command: " + ' '.join(sshcommand)
      raise Exception("Could not connect to {0}. Giving up with erros: {1}".format(self.host, log))

    errorMsg = log[1]
    if self.errorMessage and sshstat.returncode != 0:
      errorMsg = self.errorMessage + "\n" + errorMsg

    print "SSH command execution finished"

    return  {"exitstatus": sshstat.returncode, "log": log, "errormsg": errorMsg}


class SCP:
  """ SCP implementation that is thread based. The status can be returned using
   status val """
  def __init__(self, user, sshkey_file, host, inputFile, remote, errorMessage = None):
    self.user = user
    self.sshkey_file = sshkey_file
    self.host = host
    self.inputFile = inputFile
    self.remote = remote
    self.errorMessage = errorMessage
    pass


  def run(self):
    scpcommand = ["scp",
                  "-r",
                  "-o", "ConnectTimeout=60",
                  "-o", "BatchMode=yes",
                  "-o", "StrictHostKeyChecking=no",
                  "-i", self.sshkey_file, self.inputFile, self.user + "@" +
                                                          self.host + ":" + self.remote]

    i = 1
    while True:
      try:
        scpstat = subprocess.Popen(scpcommand, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        log = scpstat.communicate()
        if scpstat.returncode != 0:
          print "Executing SCP command on {0} failed: {1}".format(self.host, log)
          print "\nRetrying SCP command one more time!"
          if i >= 3:
            break
          i += 1
          time.sleep(10)
          continue
        break
      except:
        print "Could not SCP to {0}, waiting for it to start".format(self.host)
        i += 1
        time.sleep(10)

      if i >= 3:
        print "Could not execute remote scp command: " + ' '.join(scpcommand)
        raise Exception("Could not connect to {0}. Giving up with erros: {1}".format(self.host, log))

    errorMsg = log[1]
    if self.errorMessage and scpstat.returncode != 0:
      errorMsg = self.errorMessage + "\n" + errorMsg

    print "SCP command execution finished"

    return {"exitstatus": scpstat.returncode, "log": log, "errormsg": errorMsg}

# main method to parse arguments from user and start work
def main():
  parser = argparse.ArgumentParser(
    description='This script brings up a cluster with ambari installed, configured and started',
    epilog='Only GCE is supported as of now!'
  )

  # options
  parser.add_argument('--controller', type=str,
                      action='store', help='GCE controller ip address.')

  parser.add_argument('--key', type=str,
                      action='store', help='Path to GCE ssh key.')

  parser.add_argument('--cluster-prefix', type=str,
                      action='store', help='Cluster name prefix.')

  parser.add_argument('--agent-prefix', type=str,
                      action='store', help='Agent name prefix.')

  parser.add_argument('--agents-count', type=int,
                      action='store', help='Agents count for whole cluster(multiples of 50).')

  if len(sys.argv) <= 1:
    parser.print_help()
  else:
    args = parser.parse_args()
    do_work(args)

# base method which process cluster deployment
def deploy_cluster(args):
  number_of_nodes=args.agents_count/number_of_agents_on_host
  # trying to create cluster with needed params
  print "Creating cluster {0}-{1} with {2} large nodes on centos6...".format(args.cluster_prefix, cluster_name, str(number_of_nodes))
  execute_command(args, args.controller, "/usr/sbin/gce up {0}-{1} {2} --centos6 --large".format(args.cluster_prefix, cluster_name, str(number_of_nodes)),
                  "Failed to create cluster, probably not enough resources!", "-tt")

  # VMs are not accessible immediately
  time.sleep(10)

  # getting list of vms information like hostname and ip address
  print "Getting list of virtual machines from cluster..."
  vms = get_vms_list(args)

  # check number of nodes in cluster to be the same as user asked
  print "Checking count of created nodes in cluster..."
  if not vms or len(vms) < number_of_nodes:
    raise Exception("Can not bring up enough nodes. Requested {0}, but got: {1}. Probably not enough resources!".format(number_of_nodes, len(vms)))

  print "GCE cluster was successfully created!"
  pretty_print_vms(vms)

  # installing/starting ambari-server and ambari-agents on each host
  server_host_name = sorted(vms.items())[0][0]
  server_installed=False

  print "Creating server.sh script (which will be executed on server to install/configure/start ambari-server and ambari-agent)..."
  create_server_script(args, server_host_name)

  print "Creating agent.sh script (which will be executed on agent hosts to install/configure/start ambari-agent..."
  create_agent_script(args, server_host_name)

  time.sleep(10)
  for (hostname, ip) in sorted(vms.items()):
    print "=========================="
    print "Working on {0}".format(hostname)
    if not server_installed:
      remote_path = "/server.sh"
      local_path = "server.sh"
      print "Copying server.sh to {0}...".format(hostname)
      put_file(args, ip, local_path, remote_path, "Failed to copy file!")
      print "Executing remote ssh command (set correct permissions and start executing server.sh in separate process) on {0}...".format(hostname)
      execute_command(args, ip, "cd /; chmod 777 server.sh; nohup ./server.sh >/server.log 2>&1 &",
                    "Install/configure/start server script failed!")
      server_installed = True
    else:
      remote_path = "/agent.sh"
      local_path = "agent.sh"
      print "Copying agent.sh to {0}...".format(hostname)
      put_file(args, ip, local_path, remote_path, "Failed to copy file!")
      print "Executing remote ssh command (set correct permissions and start executing agent.sh in separate process) on {0}...".format(hostname)
      execute_command(args, ip, "cd /; chmod 777 agent.sh; nohup ./agent.sh >/agent.log 2>&1 &",
                    "Install/configure start agent script failed!")

  print "All scripts where successfully copied and started on all hosts. " \
        "\nPay attention that server.sh script need 5 minutes to finish and agent.sh need 3 minutes!"

# check if all required params were passed by user
# if all needed params available then start cluster deploy
def do_work(args):
  if not args.controller:
    raise Failure("GCE controller ip address not defined!")

  if not args.key:
    raise Failure("Path to gce ssh key not defined!")

  if not args.cluster_prefix:
    raise Failure("Cluster name prefix not defined!")

  if not args.agent_prefix:
    raise Failure("Agent name prefix not defined!")

  if not args.agents_count:
    raise Failure("Agents count for whole cluster(multiples of 50) not defined!")

  deploy_cluster(args)

# creating server.sh script in the same dir where current script is located
# server.sh script will install, configure and start ambari-server and ambari-agent on host
def create_server_script(args, server_host_name):
  file = open("server.sh", "w")

  file.write("#!/bin/bash\n")

  file.write("wget -O /etc/yum.repos.d/ambari.repo {0}\n".format(ambari_repo_file_url))
  file.write("yum clean all; yum install git ambari-server ambari-agent -y\n")
  file.write("cd /home; git clone https://github.com/apache/ambari.git\n")

  file.write("cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-server/resources/stacks/PERF\n")
  file.write("cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-agent/cache/stacks/PERF\n")

  file.write("ambari-server setup -s\n")
  file.write("sed -i -e 's/false/true/g' /var/lib/ambari-server/resources/stacks/PERF/1.0/metainfo.xml\n")
  file.write("ambari-server start --skip-database-check\n")

  file.write("sed -i -e 's/hostname=localhost/hostname={0}/g' /etc/ambari-agent/conf/ambari-agent.ini\n".format(server_host_name))
  file.write("sed -i -e 's/agent]/agent]\\nhostname_script={0}\\npublic_hostname_script={1}\\n/1' /etc/ambari-agent/conf/ambari-agent.ini\n".format(hostname_script, public_hostname_script))
  file.write("printf \"start={0}\\nnum={1}\\nprefix={2}\" > /etc/ambari-agent/conf/agent-multiplier.conf\n".format(start_number, number_of_agents_on_host, args.agent_prefix))
  file.write("python /home/ambari/ambari-agent/conf/unix/agent-multiplier.py start\n")

  file.write("exit 0")

  file.close()

# creating agent.sh script in the same dir where current script is located
# agent.sh script will install, configure and start ambari-agent on host
def create_agent_script(args, server_host_name):
  file = open("agent.sh", "w")

  file.write("#!/bin/bash\n")

  file.write("wget -O /etc/yum.repos.d/ambari.repo {0}\n".format(ambari_repo_file_url))
  file.write("yum clean all; yum install git ambari-agent -y\n")
  file.write("cd /home; git clone https://github.com/apache/ambari.git\n")
  file.write("cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-agent/cache/stacks/PERF\n")

  file.write("sed -i -e 's/hostname=localhost/hostname={0}/g' /etc/ambari-agent/conf/ambari-agent.ini\n".format(server_host_name))
  file.write("sed -i -e 's/agent]/agent]\\nhostname_script={0}\\npublic_hostname_script={1}\\n/1' /etc/ambari-agent/conf/ambari-agent.ini\n".format(hostname_script, public_hostname_script))
  file.write("printf \"start={0}\\nnum={1}\\nprefix={2}\" > /etc/ambari-agent/conf/agent-multiplier.conf\n".format(start_number, number_of_agents_on_host, args.agent_prefix))
  file.write("python /home/ambari/ambari-agent/conf/unix/agent-multiplier.py start\n")
  file.write("exit 0")

  file.close()

# method to execute ssh commands via SSH class
def execute_command(args, ip, cmd, fail_message, custom_option='', login='root'):
  ssh = SSH(login, args.key, ip, cmd, custom_option, fail_message)
  ssh_result = ssh.run()
  status_code = ssh_result["exitstatus"]
  if status_code != 0:
    raise Exception(ssh_result["errormsg"])

  return ssh_result["log"][0]

# method to copy file from local to remote host via SCP class
def put_file(args, ip, local_file, remote_file, fail_message, login='root'):
  scp = SCP(login, args.key, ip, local_file,
            remote_file, fail_message)
  scp_result = scp.run()
  status_code = scp_result["exitstatus"]
  if status_code != 0:
    raise Exception(scp_result["errormsg"])

  return scp_result["log"][0]

# method to parse "gce fqdn {cluster-name}" command output and get hosts and ips pairs for every host in cluster
def get_vms_list(args):
  gce_fqdb_cmd = '/usr/sbin/gce fqdn {0}-{1}'.format(
    args.cluster_prefix, cluster_name)
  out = execute_command(args, args.controller, gce_fqdb_cmd, "Failed to get VMs list!", "-tt")
  lines = out.split('\n')
  #print "LINES=" + str(lines)
  if lines[0].startswith("Using profile") and not lines[1].strip():
    result = {}
    for s in lines[2:]:  # Ignore non-meaningful lines
      if not s:
        continue
      match = re.match(r'^([\d\.]*)\s+([\w\.-]*)\s+([\w\.-]*)\s$', s, re.M)
      if match:
        result[match.group(2)] = match.group(1)
      else:
        raise Exception('Can not parse "{0}"'.format(s))
    return result
  else:
    raise Exception('Can not parse "{0}"'.format(lines))


def pretty_print_vms(vms):
  print "----------------------------"
  print "server ip: {0}".format(sorted(vms.items())[0][1])
  print "Hostnames of nodes in cluster:"
  for (hostname, ip) in sorted(vms.items()):
    print hostname
  print "----------------------------"


if __name__ == "__main__":
  main()


