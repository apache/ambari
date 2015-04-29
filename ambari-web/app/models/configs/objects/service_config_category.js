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

var App = require('app');

App.ServiceConfigCategory = Ember.Object.extend({
  name: null,
  /**
   *  We cant have spaces in the name as this is being used as HTML element id while rendering. Hence we introduced 'displayName' where we can have spaces like 'Secondary Name Node' etc.
   */
  displayName: null,
  slaveConfigs: null,
  /**
   * check whether to show custom view in category instead of default
   */
  isCustomView: false,
  customView: null,
  /**
   * Each category might have a site-name associated (hdfs-site, core-site, etc.)
   * and this will be used when determining which category a particular property
   * ends up in, based on its site.
   */
  siteFileName: null,
  /**
   * Can this category add new properties. Used for custom configurations.
   */
  canAddProperty: false,
  nonSlaveErrorCount: 0,
  primaryName: function () {
    switch (this.get('name')) {
      case 'DataNode':
        return 'DATANODE';
        break;
      case 'TaskTracker':
        return 'TASKTRACKER';
        break;
      case 'RegionServer':
        return 'HBASE_REGIONSERVER';
    }
    return null;
  }.property('name'),


  isForMasterComponent: function () {
    var masterServices = [ 'NameNode', 'SNameNode', 'JobTracker', 'HBase Master', 'Oozie Master',
      'Hive Metastore', 'WebHCat Server', 'ZooKeeper Server', 'Ganglia' ];

    return (masterServices.contains(this.get('name')));
  }.property('name'),

  isForSlaveComponent: function () {
    var slaveComponents = ['DataNode', 'TaskTracker', 'RegionServer'];
    return (slaveComponents.contains(this.get('name')));
  }.property('name'),

  slaveErrorCount: function () {
    var length = 0;
    if (this.get('slaveConfigs.groups')) {
      this.get('slaveConfigs.groups').forEach(function (_group) {
        length += _group.get('errorCount');
      }, this);
    }
    return length;
  }.property('slaveConfigs.groups.@each.errorCount'),

  errorCount: function () {
    return this.get('slaveErrorCount') + this.get('nonSlaveErrorCount');
  }.property('slaveErrorCount', 'nonSlaveErrorCount'),

  isAdvanced : function(){
    var name = this.get('name');
    return name.indexOf("Advanced") !== -1 ;
  }.property('name')
});
