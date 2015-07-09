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


import time
import sys
from cluster import Cluster
from config import Config

def all(argv):
    if len(argv) < 7:
        print_help()
        exit(1)

    cluster_name = argv[2]
    VMs_num = int(argv[3])
    Docker_num_each_VM = int(argv[4])
    server_Weave_IP = argv[5]
    server_external_IP = argv[6]

    cluster = Cluster()
    cluster.request_GCE_cluster(VMs_num, Docker_num_each_VM, cluster_name)
    print "wait 50 seconds for the cluster to boot ... ..."
    time.sleep(50)
    cluster.run_docker_on_cluster(server_external_IP, server_Weave_IP)

def request_cluster(argv):
    if len(argv) < 5:
        print_help()
        exit(1)

    cluster_name = argv[2]
    VMs_num = int(argv[3])
    Docker_num_each_VM = int(argv[4])

    cluster = Cluster()
    cluster.request_GCE_cluster(VMs_num, Docker_num_each_VM, cluster_name)
    print "Before run Docker on the Cluster, wait at least 50 seconds for the cluster to boot"

def run_cluster(argv):
    if len(argv) < 4:
        print_help()
        exit(1)

    server_Weave_IP = argv[2]
    server_external_IP = argv[3]

    config = Config
    config.load()

    cluster = Cluster()
    cluster.load_cluster_info(config.ATTRIBUTES["cluster_info_file"])
    cluster.run_docker_on_cluster(server_external_IP, server_Weave_IP)

def terminate():
    print "Log into GCE controller to terminate your cluster manually"

def print_help():
    print "usage:"
    print

    print "all", "  ", "request a GCE cluster and run Docker containers with Ambari-agent in all VMs"
    print "\t\t", "<the name of the cluster>"
    print "\t\t", "<number of VMs>"
    print "\t\t", "<number of dockers each VMs>"
    print "\t\t", "<Weave IP of Ambari-server>"
    print "\t\t", "<external IP of Ambari-server>"
    print

    print "request", "  ", "request a cluster from GCE, generate the configuration for the cluster"
    print "\t\t", "<the name of the cluster>"
    print "\t\t", "<number of VMs>"
    print "\t\t", "<number of dockers each VMs>"
    print

    print "run", "  ", "run Docker containers with Ambari-agent in all VMs of the cluster"
    print "\t\t", "<Weave IP of Ambari-server>"
    print "\t\t", "<external IP of Ambari-server>"
    print

    print "terminate", "  ", "terminate the cluster"
    print

    print "help", "  ", "help info"
    print

    print "more options in configuration file config/config.ini"
    print "see more instructions in tips.txt"
    print

def main(argv):
    # the first argument is the python file name
    if len(argv) < 2:
        print_help()
        exit(1)

    command = argv[1]
    if command == "all":
        all(argv)

    elif command == "request":
        request_cluster(argv)

    elif command == "run":
        run_cluster(argv)

    elif command == "terminate":
        terminate()

    elif command == "help":
        print_help()

    else:
        print_help()

main(sys.argv)

# python launcher_cluster.py all test-cluster 2 3 192.168.10.10 104.196.84.248



