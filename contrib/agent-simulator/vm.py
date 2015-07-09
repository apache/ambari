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


import subprocess
from config import Config

class VM:
    def __init__(self, external_ip):
        self.external_ip = external_ip
        self.docker_list = []

    def add_docker(self, docker):
        self.docker_list.append(docker)

    # install Weave on this VM
    def __centos7_weave_install__(self):
        subprocess.call(["sudo", "chmod", "755", "Linux/CentOS7/weave_install.sh"])
        subprocess.call("./Linux/CentOS7/weave_install.sh")

    # launch Weave, make this VM connect with other VM
    def __set_weave_network__(self, VMs_external_IP_list, server_external_ip):
        # add other VMs and the ambari-server to set up connections
        command = ["sudo", "weave", "launch"]
        command.extend(VMs_external_IP_list)
        command.append(server_external_ip)
        subprocess.call(command)

    # install Docker on this VM
    def __centos7_docker_install__(self):
        subprocess.call(["sudo", "chmod", "755", "Linux/CentOS7/docker_install.sh"])
        subprocess.call("./Linux/CentOS7/docker_install.sh")

    # build docker image
    def __build_docker_image__(self, image_name):
        subprocess.call(["sudo", "docker", "build", "-t", image_name, "Docker/"])

    # launch Docker containers, issue the script to install, configure and launch Agent inside Docker.
    def __launch_containers__(self, docker_image, server_weave_ip):
        # print docker_ip_list
        for docker in self.docker_list:
            docker_IP = docker.IP
            docker_mask = docker.mask
            docker_hostname = docker.hostname

            cmd = "python /launcher_agent.py " + server_weave_ip + " " + docker_hostname + "; /bin/bash"
            command = ["sudo", "weave", "run", docker_IP + "/" + docker_mask, "-d", "-it", "-h", docker_hostname, \
                       docker_image, "bash", "-c", cmd]
            print command
            subprocess.call(command)

    def run_docker(self, server_weave_IP, server_external_IP, cluster):
        config = Config()
        config.load()

        VMs_IP_list = []
        for vm in cluster.VM_list:
            VMs_IP_list.append(vm.external_ip)

        cluster.export_hostnames("./Docker/hosts")

        self.__centos7_docker_install__()
        self.__centos7_weave_install__()
        self.__build_docker_image__(config.ATTRIBUTES["Docker_image_name"])
        self.__set_weave_network__(VMs_IP_list, server_external_IP)
        self.__launch_containers__(config.ATTRIBUTES["Docker_image_name"], server_weave_IP)