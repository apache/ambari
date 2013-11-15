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

var configs = [
  /**********************************************HDFS***************************************/
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["hive_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hivemetastore_host"],
    "foreignKey": ["hive_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["oozie_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["oozieserver_host"],
    "foreignKey": ["oozie_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["webhcat_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hivemetastore_host"],
    "foreignKey": ["webhcat_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "dfs.datanode.address",
    "templateName": ["dfs_datanode_address"],
    "foreignKey": null,
    "value": "0.0.0.0:<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.http.address",
    "templateName": ["dfs_datanode_http_address"],
    "foreignKey": null,
    "value": "0.0.0.0:<templateName[0]>",
    "filename": "hdfs-site.xml"
  },

  /******************************************MAPREDUCE***************************************/
  {
    "name": "mapred.child.java.opts",
    "templateName": ["mapred_child_java_opts_sz"],
    "foreignKey": null,
    "value": "-server -Xmx<templateName[0]>m -Djava.net.preferIPv4Stack=true",
    "filename": "mapred-site.xml"
  },

/**********************************************hbase-site***************************************/
  {
    "name": "hbase.rootdir",
    "templateName": ["namenode_host", "hbase_hdfs_root_dir"],
    "foreignKey": null,
    "value": "hdfs://<templateName[0]>:8020<templateName[1]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.zookeeper.quorum",
    "templateName": ["zookeeperserver_hosts"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },

/**********************************************webhcat-site***************************************/

  {
    "name": "templeton.zookeeper.hosts",
    "templateName": ["zookeeperserver_hosts"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "webhcat-site.xml"
  },

/**********************************************core-site for glusterfs***************************************/
  {
    "name": "fs.glusterfs.impl",
    "templateName": ["fs_glusterfs_impl"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.glusterfs.volname",
    "templateName": ["fs_glusterfs_volname"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.glusterfs.mount",
    "templateName": ["fs_glusterfs_mount"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.glusterfs.server",
    "templateName": ["fs_glusterfs_server"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.glusterfs.automount",
    "templateName": ["fs_glusterfs_automount"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.glusterfs.getfattrcmd",
    "templateName": ["fs_glusterfs_getfattrcmd"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  }
];

/**
 * Configs consists of 2 types: Computed values, which cannot be modified by user
 * and overridable values, which user can modify. We provide interface how to get all of this
 * configs separately
 * @type {Object}
 */
module.exports = {
  all : function(){
    return configs.slice(0);
  },
  overridable: function(){
    return configs.filterProperty('foreignKey');
  },
  computed: function(){
    return configs.filterProperty('foreignKey', null);
  }
};
