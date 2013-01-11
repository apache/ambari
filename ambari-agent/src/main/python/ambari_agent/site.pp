# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-hadoop/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-hbase/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-zookeeper/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-oozie/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-pig/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-sqoop/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-templeton/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-hive/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-hcat/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-mysql/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-monitor-webserver/manifests/*.pp'
import '/media/sf_/home/mahadev/workspace/ambari-workspace/ambari-git/ambari-agent/src/main/puppet/modules/hdp-repos/manifests/*.pp'
$NAMENODE= ['h2.hortonworks.com']
$DATANODE= ['h1.hortonworks.com', 'h2.hortonworks.com']
$jdk_location="http://hdp1/downloads"
$jdk_bins= {
"32" => "jdk-6u31-linux-x64.bin",
"64" => "jdk-6u31-linux-x64.bin"
}
$hdfs_user="hdfs"
$java32_home="/usr/jdk64/jdk1.6.0_31"
$java64_home="/usr/jdk64/jdk1.6.0_31"
$configuration =  {
capacity-scheduler=> {
"mapred.capacity-scheduler.queue.default.capacity" => "100",
"mapred.capacity-scheduler.queue.default.supports-priorit" => "false"
},
oozie-site=> {
"oozie.service.ActionService.executor.ext.classes" => "org.apache.oozie.action.hadoop.HiveActionExecutor, org.apache.oozie.action.hadoop.SqoopActionExecutor,org.apache.oozie.action.email.EmailActionExecutor,"
},
mapred-site=> {
"mapred.queue.names" => "hive,pig,default",
"mapred.jobtracker.taskScheduler" => "org.apache.hadoop.mapred.CapacityTaskScheduler"
},
core-site=> {
"fs.default.name" => "hrt8n36.cc1.ygridcore.net"
},
hbase-policy=> {
"security.client.protocol.acl" => "*"
},
hbase-site=> {
"hbase.cluster.distributed" => "true"
},
hdfs-site=> {
"dfs.block.size" => "256000000",
"dfs.replication" => "1"
},
hadoop-policy=> {
"security.client.datanode.protocol.acl" => "*",
"security.client.protocol.acl" => "*"
},
mapred-queue-acls=> {
"mapred.queue.default.acl-submit-job" => "*",
"mapred.queue.default.acl-administer-jobs" => "*"
},
templeton-site=> {
"templeton.override.enabled" => "true"
},
hive-site=> {
"hive.exec.scratchdir" => "/tmp"
},

}
$security_enabled = "true"
$task_bin_exe = "ls"
$hadoop_piddirprefix = "/tmp"
$ganglia_server_host = "localhost"
node /default/ {
 stage{1 :} -> stage{2 :}
class {'hdp': stage => 1}
class {'hdp-hadoop::namenode': stage => 2, service_state => installed_and_configured}
}
