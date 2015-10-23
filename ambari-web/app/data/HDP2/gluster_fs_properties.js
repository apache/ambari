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

module.exports = [
/**********************************************GLUSTERFS***************************************/
  {
    "name": "fs.glusterfs.impl",
    "filename": "core-site.xml",
    "serviceName": "GLUSTERFS",
    "category": "General"
  },
  {
    "name": "fs.AbstractFileSystem.glusterfs.impl",
    "filename": "core-site.xml",
    "serviceName": "GLUSTERFS",
    "category": "General"
  },
  {
    "name": "hadoop_heapsize",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop",
    "index": 1
  },
  {
    "name": "hdfs_log_dir_prefix",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "hadoop_pid_dir_prefix",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_heapsize",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_opt_newsize",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_opt_maxnewsize",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_opt_permsize",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_opt_maxpermsize",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "dtnode_heapsize",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "glusterfs_user",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  }
];