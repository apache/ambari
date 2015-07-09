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
from config import Config
from cluster import Cluster

def get_VM(VM_IP, cluster):
    for vm in cluster.VM_list:
        if vm.external_ip == VM_IP:
            return vm

config = Config()
config.load()

cluster = Cluster()
cluster.load_cluster_info(config.ATTRIBUTES["cluster_info_file"])

my_external_IP = sys.argv[1]
server_weave_IP = sys.argv[2]
server_external_IP = sys.argv[3]

vm = get_VM(my_external_IP, cluster)
vm.run_docker(server_weave_IP, server_external_IP, cluster)

