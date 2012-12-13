/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server;

//This enumerates all the roles that the server can handle.
//Each component or a job maps to a particular role.
public enum Role {
  ZOOKEEPER_SERVER,
  ZOOKEEPER_CLIENT,
  NAMENODE,
  NAMENODE_SERVICE_CHECK,
  DATANODE,
  HDFS_SERVICE_CHECK,
  SECONDARY_NAMENODE,
  HDFS_CLIENT,
  HBASE_MASTER,
  HBASE_REGIONSERVER,
  HBASE_CLIENT,
  JOBTRACKER,
  TASKTRACKER,
  MAPREDUCE_CLIENT,
  JAVA_JCE,
  KERBEROS_SERVER,
  KERBEROS_CLIENT,
  KERBEROS_ADMIN_CLIENT,
  HADOOP_CLIENT,
  JOBTRACKER_SERVICE_CHECK,
  MAPREDUCE_SERVICE_CHECK,
  ZOOKEEPER_SERVICE_CHECK,
  ZOOKEEPER_QUORUM_SERVICE_CHECK,
  HBASE_SERVICE_CHECK,
  MYSQL_SERVER,
  HIVE_SERVER,
  HIVE_METASTORE,
  HIVE_CLIENT,
  HIVE_SERVICE_CHECK,
  HCAT,
  HCAT_SERVICE_CHECK,
  OOZIE_CLIENT,
  OOZIE_SERVER,
  OOZIE_SERVICE_CHECK,
  PIG,
  PIG_SERVICE_CHECK,
  SQOOP,
  SQOOP_SERVICE_CHECK,
  WEBHCAT_SERVER,
  WEBHCAT_SERVICE_CHECK,
  DASHBOARD,
  DASHBOARD_SERVICE_CHECK,
  NAGIOS_SERVER,
  GANGLIA_SERVER,
  GANGLIA_MONITOR,
  GMOND_SERVICE_CHECK,
  GMETAD_SERVICE_CHECK,
  MONTOR_WEBSERVER,
  DECOMMISSION_DATANODE
}
