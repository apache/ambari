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
import time
from config import Config
from docker import Docker
from vm import VM



class Cluster:
    def __init__(self):
        self.cluster_name = ""
        self.VMs_num = 0
        self.VM_list = []

    # read cluster info from a file
    def load_cluster_info(self, filename):
        file = open(filename)

        self.cluster_name = file.next().split()[1]
        self.VMs_num = int(file.next().split()[1])
        for VM_index in range(0, self.VMs_num):
            vm = VM(file.next().split()[1])
            docker_num = int(file.next().split()[1])
            for Docker_index in range(0, docker_num):
                line = file.next()
                IP = line.split()[0].split("/")[0]
                mask = line.split()[0].split("/")[1]
                hostname = line.split()[1]
                docker = Docker(IP, mask, hostname)
                vm.add_docker(docker)
            self.VM_list.append(vm)

        file.close()

    def __extract_VM_IP__(self, GCE_info_file_name):
        f = open(GCE_info_file_name)
        lines = f.readlines()
        f.close()

        ip_list = []
        for line in lines:
            tokens = line.split()
            ip_list.append(tokens[1])
        return ip_list[1:]

    # request a new cluster
    def request_GCE_cluster(self, vms_num, docker_num, cluster_name):
        # reload configuration file
        config = Config()
        config.load()
        # request cluster
        gce_key = config.ATTRIBUTES["GCE_controller_key_file"]
        gce_login = config.ATTRIBUTES["GCE_controller_user"] + "@" + config.ATTRIBUTES["GCE_controller_IP"]
        gce_up_cmd = "gce up " + cluster_name + " " + str(vms_num) + " " + config.ATTRIBUTES["GCE_VM_type"] + \
            " " + config.ATTRIBUTES["GCE_VM_OS"]
        subprocess.call(["ssh", "-o", "StrictHostKeyChecking=no", "-i", gce_key, gce_login, gce_up_cmd])

        print "cluster launched successufully, wait 5 seconds for cluster info ... ..."
        time.sleep(5)

        # request cluster info
        gce_info_output_file = open(config.ATTRIBUTES["GCE_info_output"], "w")
        gce_info_cmd = "gce info " + cluster_name
        subprocess.call(["ssh", "-o", "StrictHostKeyChecking=no", "-i", gce_key, gce_login, gce_info_cmd], \
                        stdout=gce_info_output_file)
        gce_info_output_file.close()
        print "cluster info is saved to file " + config.ATTRIBUTES["GCE_info_output"]

        # prepare all attributes of the cluster, write to a file
        VM_IP_list = self.__extract_VM_IP__(config.ATTRIBUTES["GCE_info_output"])
        self.generate_cluster_info(VM_IP_list, cluster_name, docker_num)
        self.overwrite_to_file(config.ATTRIBUTES["cluster_info_file"])
        # server need this file to resolve the host names of the agents
        self.export_hostnames(config.ATTRIBUTES["Docker_hostname_info"])

    # save info to file
    def overwrite_to_file(self, filename):
        file = open(filename, "w")
        file.write("cluster_name: " + self.cluster_name + "\n")
        file.write("VMs_num: " + str(self.VMs_num) + "\n")

        for vm in self.VM_list:
            file.write("\t\t")
            file.write("VM_IP: " + vm.external_ip + "\n")
            file.write("\t\t")
            file.write("Docker_num: " + str(len(vm.docker_list)) + "\n")
            for docker in vm.docker_list:
                file.write("\t\t\t\t")
                file.write(docker.IP + "/" + docker.mask + " " + docker.hostname + "\n")

        file.close()

    def __increase_IP__(self, base_IP, increase):
        IP = [int(base_IP[0]), int(base_IP[1]), int(base_IP[2]), int(base_IP[3])]
        IP[3] = IP[3] + increase
        for index in reversed(range(0, 4)):
            if IP[index] > 255:
                IP[index - 1] = IP[index - 1] + IP[index] / 256
                IP[index] = IP[index] % 256
        return IP

    # generate VM and docker info for this cluster
    # set up parameter as this info
    def generate_cluster_info(self, VM_IP_list, cluster_name, docker_num):
        config = Config()
        config.load()
        Docker_IP_base = config.ATTRIBUTES["Docker_IP_base"].split(".")
        Docker_IP_mask = config.ATTRIBUTES["Docker_IP_mask"]

        VM_index = 0
        for VM_IP in VM_IP_list:
            vm = VM(VM_IP)

            for Docker_index in range(0, docker_num):
                total_Docker_index = VM_index * docker_num + Docker_index
                docker_IP = self.__increase_IP__(Docker_IP_base, total_Docker_index)
                docker_IP_str = str(docker_IP[0]) + "." + str(docker_IP[1]) + "." + \
                                str(docker_IP[2]) + "." + str(docker_IP[3])
                docker_hostname = cluster_name + "-" + str(VM_index) + "-" + str(Docker_index)
                docker = Docker(docker_IP_str, str(Docker_IP_mask), docker_hostname)
                # print docker
                vm.add_docker(docker)
            VM_index = VM_index + 1
            self.VM_list.append(vm)

        self.VMs_num = len(VM_IP_list)
        self.cluster_name = cluster_name

    # run all dockers for all the VMs in the cluster
    # upload necessary file to each machine in cluster, run launcher_docker.py in each machine with parameter
    def run_docker_on_cluster(self, server_external_IP, server_Weave_IP):
        config = Config()
        config.load()

        for vm in self.VM_list:
            # upload necessary file to each machine in cluster
            VM_external_IP = vm.external_ip
            VM_directory = "root@" + VM_external_IP + ":" + config.ATTRIBUTES["VM_code_directory"]
            VM_key = config.ATTRIBUTES["GCE_VM_key_file"]
            subprocess.call(["scp", "-o", "StrictHostKeyChecking=no", "-i", VM_key, "-r", ".", VM_directory])

            # run launcher_docker.py in each machine with parameters
            subprocess.call(["ssh", "-o", "StrictHostKeyChecking=no", "-t", "-i", VM_key, \
                             "root@" + VM_external_IP, \
                             "cd " + config.ATTRIBUTES["VM_code_directory"] + "; python launcher_docker.py" + \
                             " " + VM_external_IP + " " + server_Weave_IP + " " + server_external_IP])

    # export host names to a file
    def export_hostnames(self, filename):
        hostname_file = open(filename, "w")
        for vm in self.VM_list:
            for docker in vm.docker_list:
                hostname_file.write(docker.IP)
                hostname_file.write(" ")
                hostname_file.write(docker.hostname)
                hostname_file.write("\n")
        hostname_file.close()
