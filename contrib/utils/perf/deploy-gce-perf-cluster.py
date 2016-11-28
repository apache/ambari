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

cluster_prefix = "perf"
ambari_repo_file_url = "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos6/2.x/latest/trunk/ambaribn.repo"

public_hostname_script = "foo"
hostname_script = "foo"

NUMBER_OF_AGENTS_ON_HOST = 50


class SSH:
  """
  Ssh implementation of this
  """

  def __init__(self, user, sshkey_file, host, command, custom_option='', errorMessage = None):
    self.user = user
    self.sshkey_file = sshkey_file
    self.host = host
    self.command = command
    self.errorMessage = errorMessage
    self.custom_option = custom_option

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

    return {"exitstatus": sshstat.returncode, "log": log, "errormsg": errorMsg}


class SCP:
  """
  SCP implementation that is thread based. The status can be returned using
  status val
  """

  def __init__(self, user, sshkey_file, host, inputFile, remote, errorMessage = None):
    self.user = user
    self.sshkey_file = sshkey_file
    self.host = host
    self.inputFile = inputFile
    self.remote = remote
    self.errorMessage = errorMessage

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

  parser.add_argument('--cluster-suffix', type=str,
                      action='store', help='Cluster name suffix.')

  parser.add_argument('--agent-prefix', type=str,
                      action='store', help='Agent name prefix.')

  parser.add_argument('--agents-count', type=int,
                      action='store', help='Agents count for whole cluster (multiples of 50).')

  if len(sys.argv) <= 1:
    parser.print_help()
    sys.exit(-1)

  args = parser.parse_args()
  do_work(args)

def deploy_cluster(args):
  """
  Process cluster deployment
  :param args: Command line args.
  """
  # When dividing, need to get the ceil.
  number_of_nodes = ((args.agents_count - 1) / NUMBER_OF_AGENTS_ON_HOST) + 1

  # trying to create cluster with needed params
  print "Creating cluster {0}-{1} with {2} large nodes on centos6...".format(cluster_prefix, args.cluster_suffix, str(number_of_nodes))
  execute_command(args, args.controller, "/usr/sbin/gce up {0}-{1} {2} --centos6 --large".format(cluster_prefix, args.cluster_suffix, str(number_of_nodes)),
                  "Failed to create cluster, probably not enough resources!", "-tt")

  # VMs are not accessible immediately
  time.sleep(10)

  # getting list of vms information like hostname and ip address
  print "Getting list of virtual machines from cluster..."
  # Dictionary from host name to IP
  vms = get_vms_list(args)

  # check number of nodes in cluster to be the same as user asked
  print "Checking count of created nodes in cluster..."
  if not vms or len(vms) < number_of_nodes:
    raise Exception("Cannot bring up enough nodes. Requested {0}, but got {1}. Probably not enough resources!".format(number_of_nodes, len(vms)))

  print "GCE cluster was successfully created!"
  pretty_print_vms(vms)

  # installing/starting ambari-server and ambari-agents on each host
  server_host_name = sorted(vms.items())[0][0]
  server_installed = False

  print "Creating server.sh script (which will be executed on server to install/configure/start ambari-server and ambari-agent)..."
  create_server_script(args, server_host_name)

  print "Creating agent.sh script (which will be executed on agent hosts to install/configure/start ambari-agent..."
  create_agent_script(args, server_host_name)

  time.sleep(10)

  # If the user asks for a number of agents that is not a multiple of 50, then only create how many are needed instead
  # of 50 on every VM.
  num_agents_left_to_create = args.agents_count

  start_num = 1
  for (hostname, ip) in sorted(vms.items()):
    num_agents_on_this_host = min(num_agents_left_to_create, NUMBER_OF_AGENTS_ON_HOST)

    print "=========================="
    print "Working on VM {0} that will contain hosts %d - %d".format(hostname, start_num, start_num + num_agents_on_this_host - 1)

    # The agent multiplier config will be different on each VM.

    cmd_generate_multiplier_conf = "mkdir -p /etc/ambari-agent/conf/ ; printf \"start={0}\\nnum={1}\\nprefix={2}\" > /etc/ambari-agent/conf/agent-multiplier.conf".format(start_num, num_agents_on_this_host, args.agent_prefix)
    start_num += num_agents_on_this_host
    num_agents_left_to_create -= num_agents_on_this_host

    if not server_installed:
      remote_path = "/server.sh"
      local_path = "server.sh"
      print "Copying server.sh to {0}...".format(hostname)
      put_file(args, ip, local_path, remote_path, "Failed to copy file!")

      print "Generating agent-multiplier.conf"
      execute_command(args, ip, cmd_generate_multiplier_conf, "Failed to generate agent-multiplier.conf on host {0}".format(hostname))

      print "Executing remote ssh command (set correct permissions and start executing server.sh in separate process) on {0}...".format(hostname)
      execute_command(args, ip, "cd /; chmod 777 server.sh; nohup ./server.sh >/server.log 2>&1 &",
                    "Install/configure/start server script failed!")
      server_installed = True
    else:
      remote_path = "/agent.sh"
      local_path = "agent.sh"
      print "Copying agent.sh to {0}...".format(hostname)
      put_file(args, ip, local_path, remote_path, "Failed to copy file!")

      print "Generating agent-multiplier.conf"
      execute_command(args, ip, cmd_generate_multiplier_conf, "Failed to generate agent-multiplier.conf on host {0}".format(hostname))

      print "Executing remote ssh command (set correct permissions and start executing agent.sh in separate process) on {0}...".format(hostname)
      execute_command(args, ip, "cd /; chmod 777 agent.sh; nohup ./agent.sh >/agent.log 2>&1 &",
                    "Install/configure start agent script failed!")

  print "All scripts where successfully copied and started on all hosts. " \
        "\nPay attention that server.sh script need 5 minutes to finish and agent.sh need 3 minutes!"


def do_work(args):
  """
  Check that all required args are passed in. If so, deploy the cluster.
  :param args: Command line args
  """
  if not args.controller:
    raise Exception("GCE controller ip address is not defined!")

  if not args.key:
    raise Exception("Path to gce ssh key is not defined!")

  if not args.cluster_suffix:
    raise Exception("Cluster name suffix is not defined!")

  if not args.agent_prefix:
    raise Exception("Agent name prefix is not defined!")

  if not args.agents_count:
    raise Exception("Agents count for whole cluster is not defined (will put 50 Agents per VM)!")

  deploy_cluster(args)


def create_server_script(args, server_host_name):
  """
  Creating server.sh script in the same dir where current script is located
  server.sh script will install, configure and start ambari-server and ambari-agent on host
  :param args: Command line args
  :param server_host_name: Server host name
  """

  contents = "#!/bin/bash\n" + \
  "wget -O /etc/yum.repos.d/ambari.repo {0}\n".format(ambari_repo_file_url) + \
  "yum clean all; yum install git ambari-server ambari-agent -y\n" + \
  "cd /home; git clone https://github.com/apache/ambari.git\n" + \
  "cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-server/resources/stacks/PERF\n" + \
  "cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-agent/cache/stacks/PERF\n" + \
  "ambari-server setup -s\n" + \
  "sed -i -e 's/false/true/g' /var/lib/ambari-server/resources/stacks/PERF/1.0/metainfo.xml\n" + \
  "ambari-server start --skip-database-check\n" + \
  "sed -i -e 's/hostname=localhost/hostname={0}/g' /etc/ambari-agent/conf/ambari-agent.ini\n".format(server_host_name) + \
  "sed -i -e 's/agent]/agent]\\nhostname_script={0}\\npublic_hostname_script={1}\\n/1' /etc/ambari-agent/conf/ambari-agent.ini\n".format(hostname_script, public_hostname_script) + \
  "python /home/ambari/ambari-agent/conf/unix/agent-multiplier.py start\n" + \
  "exit 0"

  with open("server.sh", "w") as f:
    f.write(contents)


def create_agent_script(args, server_host_name):
  """
  Creating agent.sh script in the same dir where current script is located
  agent.sh script will install, configure and start ambari-agent on host
  :param args: Command line args
  :param server_host_name: Server host name
  """

  # TODO, instead of cloning Ambari repo on each VM, do it on the server once and distribute to all of the agents.
  contents = "#!/bin/bash\n" + \
  "wget -O /etc/yum.repos.d/ambari.repo {0}\n".format(ambari_repo_file_url) + \
  "yum clean all; yum install git ambari-agent -y\n" + \
  "cd /home; git clone https://github.com/apache/ambari.git\n" + \
  "cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-agent/cache/stacks/PERF\n" + \
  "sed -i -e 's/hostname=localhost/hostname={0}/g' /etc/ambari-agent/conf/ambari-agent.ini\n".format(server_host_name) + \
  "sed -i -e 's/agent]/agent]\\nhostname_script={0}\\npublic_hostname_script={1}\\n/1' /etc/ambari-agent/conf/ambari-agent.ini\n".format(hostname_script, public_hostname_script) + \
  "python /home/ambari/ambari-agent/conf/unix/agent-multiplier.py start\n" + \
  "exit 0"

  with open("agent.sh", "w") as f:
    f.write(contents)


def execute_command(args, ip, cmd, fail_message, custom_option='', login='root'):
  """
  Method to execute ssh commands via SSH class
  :param args: Command line args
  :param ip: IP to ssh to
  :param cmd: Command to execute
  :param fail_message: In case of an error, what to report
  :param custom_option: Custom flags
  :param login: Login user
  :return: Return execute log message
  """
  ssh = SSH(login, args.key, ip, cmd, custom_option, fail_message)
  ssh_result = ssh.run()
  status_code = ssh_result["exitstatus"]
  if status_code != 0:
    raise Exception(ssh_result["errormsg"])

  return ssh_result["log"][0]


def put_file(args, ip, local_file, remote_file, fail_message, login='root'):
  """
  Method to copy file from local to remote host via SCP class
  :param args: Command line args
  :param ip: IP to ssh to
  :param local_file: Path to local file
  :param remote_file: Path to remote file
  :param fail_message: In case of an error, what to report
  :param login: Login user.
  :return: Return copy log message
  """
  scp = SCP(login, args.key, ip, local_file,
            remote_file, fail_message)
  scp_result = scp.run()
  status_code = scp_result["exitstatus"]
  if status_code != 0:
    raise Exception(scp_result["errormsg"])

  return scp_result["log"][0]


def get_vms_list(args):
  """
  Method to parse "gce fqdn {cluster-name}" command output and get hosts and ips pairs for every host in cluster
  :param args: Command line args
  :return: Mapping of VM host name to ip.
  """

  gce_fqdb_cmd = '/usr/sbin/gce fqdn {0}-{1}'.format(cluster_prefix, args.cluster_suffix)
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
        raise Exception('Cannot parse "{0}"'.format(s))
    return result
  else:
    raise Exception('Cannot parse "{0}"'.format(lines))


def pretty_print_vms(vms):
  print "----------------------------"
  print "Server IP: {0}".format(sorted(vms.items())[0][1])
  print "Hostnames of nodes in cluster:"
  for (hostname, ip) in sorted(vms.items()):
    print hostname
  print "----------------------------"


if __name__ == "__main__":
  main()


