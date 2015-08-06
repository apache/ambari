<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Ambari-agent Simulator
============
This project provides a tool to create a large Ambari-agent cluster in a convenient way.

## Usage:
Run python launcher_cluster.py to see usage

python launcher_cluster.py request    

    request a cluster from GCE, generate the configuration for the cluster. Parameters:
	<the name of the cluster>, suggestion: {yourname}-group-a)
	<number of VMs>
	<number of dockers each VMs>
	<number of service servers inside VM>
	<number of ambari-server>, either 0 or 1
		
python launcher_cluster.py up
        
    run one cluster, and add to another cluster. Parameters:
	<the name of the cluster>
		
python launcher_cluster.py merge    

    run Docker containers with Ambari-agent in all VMs of the cluster. Parameters:
	<the name of the cluster to be merged>
	<the name of the cluster to be extended>

python launcher_cluster.py merge 

    run Docker containers with Ambari-agent in all VMs of the cluster. Parameters:
    <the name of the cluster to be merged>
    <Weave IP of the Ambari-server>
    <External IP of the Ambari-server>

python launcher_cluster.py list    
        
    list all the cluster
    
python launcher_cluster.py show
    
    show cluster information. Parameters:
    <the name of the cluster>
    
python launcher_cluster.py help    
        
    show help info

## Introduction to Weave Networking
[Weave](https://github.com/weaveworks/weave) is a tool to connect Docker containers distributed across different hosts. 
This project use Weave to assign each Docker container (with Ambari-agent) a unique internal IP and a domain name.
In each VM, a special Docker container will be launched by Weave to act as a Router, 
and to connect all the Docker containers in this VM with other Docker containers in other VMs.
Also, another special Docker container will be launched by Weave to act as a DNS for this VM.
Each Docker container can connect with each other by using the internal IP, host name or domain name.

All the Weave internal IP should be configured by the user. 
In this following document, we use subnet 192.168.#.#/16, and use the IP within this subnet to configure Weave. 
Actually, You can use any IP as you wish, even public IP, in which case, 
it will replace the connection to the real outside connection, and redirect the connection to the internal Docker container.

## Quick Start
With the following 6 steps, you can create a cluster with Ambari-agents, and connect them to your Ambari-server. 
Among all the steps, the step 3 is to configure this program, the step 4 is the one which really matters, 
and other steps act as a one-time configuration or suggestion for your Ambari-server.

You can start with this guide by downloading the code to your own computer or any computers which can access the GCE controller.

* Step 1: Mark down IP of the GCE VM which installed Ambari-server, Ambari_Server_IP=104.196.81.81
* Step 2: Copy example/config.ini to config/config.ini
* Step 3: Modify the following values in config/config.ini
    * Output_folder
    * GCE_controller_key_file
    * VM_key_file
    * cluster_info_file, use {yourname}-group-a
* Step 4: Use the command line to run 3 VMs, each VM has 5 Ambari-agents.

        python launcher_cluster.py all {yourname}-group-a 3 5 192.168.255.1 Ambari_Server_IP

* Step 5: Log into your Ambari-server machine, run the following command line to set up Weave internal network 

        cd agent-simulator/network
        ./set_ambari_server_network.sh 192.168.255.1 192.168.255.2 16

* Step 6: Operate on Ambari-server web GUI
    * Add all agents: docker-[0-14]-{yourname}-group-a.weave.local
    * Choose manual registration
    * Choose to install HDFS, YARN, HBase, Zookeeper, Ambari-Metrics
    

#### More on Quick Start
* Step 7: Add one more cluster to your Ambari-server
    * Modify cluster_info_file in config/config.ini, use {yourname}-group-b
    * Modify Docker_IP_base in config.config.ini, use 192.168.2.1
    * Use the command line to run 2 VMs, each VM has 10 Ambari-agents:

                python launcher_cluster.py all {yourname}-group-b 2 10 192.168.255.1 Ambari_Server_IP

    * On Ambari-server web GUI, add all agents: docker-[0-19]-{yourname}-group-b.weave.local
* Step 8: Add one VM with Ambari-agent installed to your Ambari-server
    * Log into your VM, set Ambari_Server_IP as the value of server hostname in the file /etc/ambari-agent/conf/ambari-agent.ini
    * On the VM, Run the following command set up Weave internal network

                cd agent-simulator/network
                ./set_host_network.sh 192.168.254.1 192.168.254.2 16 Ambari_Server_IP


## Detail Work Flow:
* Step 1: Install Ambari-server
    * Use existing Ambari-server, or, install and launch a new one
    * agent-simulator/server/ambari_server_install.sh: this shell might help you install the Ambari-server
    * agent-simulator/server/ambari_server-_reset_data.sh: this shell might help you set Ambari-server to initial state
        
* Step 2: Decide IP in your mind
    * Mark down the IP of the Ambari-server, say {IP of Ambari-server = 104.196.81.81}
    * Come up a subnet say, {subnet = 192.168.#.#/16} {Docker_IP_mask = 16}
    * Pick one address closer to the END of the subnet as the Weave INTERNAL IP of Ambari-server, 
    and another one as the Weave DNS IP of Ambari-server, 
    say {Weave IP of Ambari-server = 192.168.255.1} {Weave DNS IP of Ambari-server = 192.168.255.2}
    * Pick one address closer to the START of the subnet as the Weave INTERNAL IP of the FIRST Ambari-agent, 
    say {Docker_IP_base = 192.168.1.1}
    * Other Weave INTERNAL IP of Amari-agent will be automatically assigned based on the FIRST one (increasingly).
    
* Step 3: First time set up Ambari-server       
    * Copy all the agent-simulator code base to Ambari-server
    * cd agent-simulator/network
    * Run set_ambari_server_network.sh
    * In this example, use parameters: {Weave IP of Ambari-server = 192.168.255.1} {Weave DNS IP of Ambari-server = 192.168.255.2} {Docker_IP_mask = 16}
    
* Step 4: Modify config.ini
    * Modify attributes: Output_folder, GCE_controller_key_file, GCE_VM_key_file, cluster_info_file
    * Change Docker_IP_base and Docker_IP_mask, in this example {Docker_IP_base = 192.168.1.1} {Docker_IP_mask = 16}
    
* Step 5: Request Ambari-agent cluster
    * Run python launcher_cluster.py request
    * Use {your name}-group-a as the cluster name. In case you wanna add more cluster to your Ambari-server, change the last letter
    
* Step 6: Modify Cluster Information File
    * A TXT file will appear under directory ./config within 1 minutes, which has the information about the cluster
    * Typically, you would like NameNode, RegionServer, ResourceManager, etc.. to dominate one VM. 
    * In this example, change the configuration of the first and the second VM, make each of them only have one Docker. 
    You can install different server services only into these two Docker containers later on the Ambari-server web GUI.

* Step 7: Run Ambari-agent Cluster
    Run python launcher_cluster.py run
    In this exmaple, use parameters: {Weave IP of Ambari-server = 192.168.255.1} {IP of Ambari-server = 104.196.81.81}
    
* Step 8: Operate on Ambari-server web GUI


## Expand Cluster With This Script
Be careful if you wanna use this script to add more Ambari-agents AGAIN to your Ambari-server

* Use different Cluster Name when providing parameters to launcher_cluster.py
* In config.ini, use the same Docker_IP_mask, make sure the same subnet
* Change config.ini to use different Docker_IP_base, make sure that all new IPs never overlap with the existing IPs
* Change config.ini to use different cluster_info_file, make sure the existing cluster information is not overwritten
   
## Expand Cluster By Adding other Hosts/VMs
   
## Naming Convention
Cluster Name: the name of the cluster must be unique to make sure every VM on GCE has its unique name. The suggestion is using {your name}-group-{a-z}

VM Name: each VM has a domain name assigned by GCE, its host name is {cluster name}-{index}

Docker Container Name: the domain name of Docker container is docker-{index}-{cluster name}.weave.local, 
the prefix "docker" can be configured by value Container_hostname_fix in config/config.ini, 
you can find out which VM has which Docker container in the cluster information file.


## Image for Docker Container

## Use Different Partition for Docker Container

## The IP assign mechanism.
Basically, you don't have to worry about IP. The maximum number of IP is limited by weave_ip_base and weave_ip_mask.
By fault, the subnet is 192.168.#.#/16. Once you have already created 256*256 agents (the real number is smaller, 
since the DNS on the VM also uses IP address), some address might fall out side of subnet and some fall inside,
which causes a connection issue. The function related is cluster._increase_ip(). You might want to wrap the IP around, 
but corner cases are always there, there is no silver bullet.


## Issues
* This tool do NOT support parallel usage
* If GCE has no enough resource, the cluster returned to you will have a smaller number of VM
* Don't merge your cluster into someone else's cluster. Actually you can do it, but you have to dig into the network, and
make sure the IP configuration is right.

## Suggestions:
* Make sure your cluster name is unique, or you might cause trouble to other people's VM
* Use CTRL + P, then CTRL + Q to exit Docker container. Use "exit" will terminate the container.
* Remove ~/.ssh/know_hosts files, especially if you run a large cluster. 
You might get a warning from SSH, because the new GCE VM assigned to you might have the same IP with the VMs you saved in know_hosts file. 
Remove .ssh/know_hosts before run this script.
* Ambari-agent and Ambari-server have to be the same version to successfully register. 
The command used to install Ambari-agent is in the Dockerfile
    