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

module.exports = {
  setupConfigGroupsObject: function(serviceName) {
    var serviceGroups = this.setupServiceConfigTagsObject(serviceName).mapProperty('siteName');
    var configGroups = [
      {
        "tag":"version1",
        "type":"core-site",
        "properties": {
          "fs.defaultFS" : "hdfs://c6401.ambari.apache.org:8020",
          "fs.trash.interval" : "360"
        }
      },
      {
        "tag":"version1",
        "type":"global",
        "properties":{
          "hadoop_heapsize":"1024",
          "storm_log_dir": "/var/log/storm",
          "stormuiserver_host": "c6401.ambari.apache.org",
          "nonexistent_property": "some value"
        }
      },
      {
        "tag":"version1",
        "type":"hdfs-site",
        "properties": {
          "dfs.datanode.data.dir": "/b,/a",
          "dfs.namenode.name.dir": "/b,/a,/c",
          "dfs.namenode.checkpoint.dir": "/b,/d,/a,/c",
          "dfs.datanode.failed.volumes.tolerated": "2",
          "content": "custom mock property"
        }
      },
      {
        "tag":"version1",
        "type":"hdfs-log4j",
        "properties": {
          "content": "hdfs log4j content"
        }
      },
      {
        "tag":"version1",
        "type":"zoo.cfg",
        "properties": {
          "custom.zoo.cfg": "zoo cfg content"
        }
      },
      {
        "tag":"version1",
        "type":"storm-site",
        "properties": {
          "storm.zookeeper.servers": "['c6401.ambari.apache.org','c6402.ambari.apache.org']",
          "single_line_property": "value",
          "multi_line_property": "value \n value"
        }
      },
      {
        "tag":"version1",
        "type":"zoo.cfg",
        "properties": {
          "custom.zoo.cfg": "value"
        }
      }
    ];
    return configGroups.filter(function(configGroup) {
      return serviceGroups.contains(configGroup.type);
    });
  },
  setupServiceConfigTagsObject: function(serviceName) {
    var configTags = {
      STORM: ['global','storm-site'],
      HDFS: ['global','hdfs-site','core-site','hdfs-log4j'],
      ZOOKEEPER: ['global', 'zoo.cfg']
    };
    var configTagsObject = [];
    if (serviceName) {
      configTags[serviceName].forEach(function(tag) {
        configTagsObject.push({
          siteName: tag,
          tagName: "version1",
          newTagName: null
        });
      });
    } else {
      for (var serviceName in configTags) {
        configTags[serviceName].forEach(function(tag) {
          configTagsObject.push({
            siteName: tag,
            tagName: "version1",
            newTagName: null
          });
        });
      }
    }
    return configTagsObject.uniq();
  },
  setupAdvancedConfigsObject: function() {
    return [
      {
        "serviceName": "HDFS",
        "name": "fs.defaultFS",
        "value": "hdfs://c6401.ambari.apache.org:8020",
        "description": "fs.defaultFS",
        "filename": "core-site.xml"
      },
      {
        "serviceName": "STORM",
        "name": "storm.zookeeper.servers",
        "value": "['localhost']",
        "description": "desc",
        "filename": "storm-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "dfs.datanode.data.dir",
        "value": "/hadoop/hdfs/data",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "dfs.namenode.name.dir",
        "value": "/hadoop/hdfs/namenode",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "dfs.namenode.checkpoint.dir",
        "value": "/hadoop/hdfs/namesecondary",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "dfs.datanode.failed.volumes.tolerated",
        "value": "2",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "content",
        "value": "custom mock property",
        "description": "desc",
        "filename": "hdfs-site.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "content",
        "value": "hdfs log4j content",
        "description": "desc",
        "filename": "hdfs-log4j.xml"
      },
      {
        "serviceName": "HDFS",
        "name": "content",
        "value": "custom hdfs log4j content",
        "description": "desc",
        "filename": "custom-hdfs-log4j.xml"
      },
      {
        "serviceName": "ZOOKEEPER",
        "name": "content",
        "value": "zookeeper log4j.xml content",
        "description": "desc",
        "filename": "zookeeper-log4j.xml"
      },
      {
        "serviceName": "ZOOKEEPER",
        "name": "custom.zoo.cfg",
        "value": "zoo cfg content",
        "description": "zoo.cfg config",
        "filename": "zoo.cfg"
      },
      {
        "serviceName": "YARN",
        "name": "content",
        "value": " value \n value",
        "filename": "capacity-scheduler.xml"
      },
      {
        "serviceName": "YARN",
        "name": "yarn.scheduler.capacity.root.default.capacity",
        "value": "100",
        "filename": "capacity-scheduler.xml"
      }
    ];
  },
  setupStoredConfigsObject: function() {
    return [
      {
        "name":"storm.zookeeper.servers",
        "value":[
          "c6401.ambari.apache.org",
          "c6402.ambari.apache.org"
        ],
        "defaultValue":"['c6401.ambari.apache.org','c6402.ambari.apache.org']",
        "filename":"storm-site.xml",
        "isUserProperty":false,
        "isOverridable":false,
        "showLabel":true,
        "serviceName":"STORM",
        "displayType":"masterHosts",
        "isVisible":true,
        "description":"desc",
        "isSecureConfig":false,
        "category":"General",
        "id":"site property",
        "displayName":"storm.zookeeper.servers"
      },
      {
        "name":"single_line_property",
        "value":"value",
        "defaultValue":"value",
        "filename":"storm-site.xml",
        "isUserProperty":true,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"STORM",
        "id":"site property",
        "displayType":"advanced",
        "displayName":"single_line_property",
        "category":"AdvancedStormSite"
      },
      {
        "name":"multi_line_property",
        "value":"value \n value",
        "defaultValue":"value \n value",
        "filename":"storm-site.xml",
        "isUserProperty":true,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"STORM",
        "id":"site property",
        "displayType":"multiLine",
        "displayName":"multi_line_property",
        "category":"AdvancedStormSite"
      },
      {
        "name":"nonexistent_property",
        "value":"some value",
        "defaultValue":"some value",
        "filename":"global.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"STORM",
        "isVisible":false,
        "id":"puppet var",
        "displayName":null,
        "options":null
      },
      {
        "name":"dfs.datanode.data.dir",
        "value":"/a,/b",
        "defaultValue":"/a,/b",
        "filename":"hdfs-site.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"HDFS",
        "displayType":"directories",
        "isRequired":true,
        "isReconfigurable":true,
        "isVisible":true,
        "description":"desc",
        "index":1,
        "isSecureConfig":false,
        "category":"DataNode",
        "id":"site property",
        "displayName":"DataNode directories"
      },
      {
        "name":"content",
        "value":"custom mock property",
        "defaultValue":"custom mock property",
        "filename":"hdfs-site.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":false,
        "serviceName":"HDFS",
        "displayType":"content",
        "isRequired":true,
        "isRequiredByAgent":true,
        "isReconfigurable":true,
        "isVisible":true,
        "description":"desc",
        "isSecureConfig":false,
        "category":"AdvancedHDFSLog4j",
        "id":"site property",
        "displayName":"content"
      },
      {
        "name":"content",
        "value":"hdfs log4j content",
        "defaultValue":"hdfs log4j content",
        "filename":"hdfs-log4j.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":false,
        "serviceName":"HDFS",
        "displayType":"content",
        "isRequired":true,
        "isRequiredByAgent":true,
        "isReconfigurable":true,
        "isVisible":true,
        "description":"desc",
        "isSecureConfig":false,
        "category":"AdvancedHDFSLog4j",
        "id":"site property",
        "displayName":"content"
      },
      {
        "name":"storm_log_dir",
        "value":"/var/log/storm",
        "defaultValue":"/var/log/storm",
        "filename":"global.xml",
        "isUserProperty":false,
        "isOverridable":true,
        "showLabel":true,
        "serviceName":"STORM",
        "displayType":"directory",
        "isRequired":true,
        "isRequiredByAgent":true,
        "isReconfigurable":true,
        "isVisible":true,
        "description":"Storm log directory",
        "isSecureConfig":false,
        "category":"General",
        "id":"puppet var",
        "displayName":"storm_log_dir"
      }
    ];
  }
}
