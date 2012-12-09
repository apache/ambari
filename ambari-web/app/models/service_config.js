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
var validator = require('utils/validator');

App.ConfigProperties = Ember.ArrayProxy.extend({
  content: require('data/config_properties').configProperties
});

App.ServiceConfig = Ember.Object.extend({
  serviceName: '',
  configCategories: [],
  configs: null,

  errorCount: function () {
    return this.get('configs').filterProperty('isValid', false).filterProperty('isVisible', true).get('length');
  }.property('configs.@each.isValid', 'configs.@each.isVisible')
});

App.ServiceConfigCategory = Ember.Object.extend({
  name: null,

  isForMasterComponent: function () {
    var masterServices = [ 'NameNode', 'SNameNode', 'JobTracker', 'HBase Master', 'Oozie Master',
      'Hive Metastore', 'Templeton Server', 'ZooKeeper Server', 'Nagios', 'Ganglia' ];

    return (masterServices.contains(this.get('name')));
  }.property('name'),

  isForSlaveComponent: function () {
    return this.get('name') === 'DataNode' || this.get('name') === 'TaskTracker' ||
      this.get('name') === 'RegionServer';
  }.property('name')
});

App.ServiceConfigProperty = Ember.Object.extend({

  id: '', //either 'puppet var' or 'site property'
  name: '',
  displayName: '',
  value: '',
  defaultValue: '',
  description: '',
  displayType: 'string', // string, digits, number, directories, custom
  unit: '',
  category: 'General',
  isRequired: true, // by default a config property is required
  isReconfigurable: true, // by default a config property is reconfigurable
  isEditable: true, // by default a config property is editable
  errorMessage: '',
  serviceConfig: null, // points to the parent App.ServiceConfig object
  filename: '',

  init: function () {
    if (this.get('id') === 'puppet var') {
      this.set('value', this.get('defaultValue'));
    }
    // TODO: remove mock data
  },

  initialValue: function () {
    var masterComponentHostsInDB = App.db.getMasterComponentHosts();
    //console.log("value in initialvalue: " + JSON.stringify(masterComponentHostsInDB));
    switch (this.get('name')) {
      case 'namenode_host':
        var temp = masterComponentHostsInDB.findProperty('component', 'NAMENODE');
        console.log("********The value of namemenode host is: " + temp.hostName);
        this.set('value', temp.hostName);
        break;
      case 'snamenode_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'SECONDARY_NAMENODE').hostName);
        break;
      case 'jobtracker.host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'JOBTRACKER').hostName);
        break;
      case 'hbasemaster.host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'HBASE_MASTER').hostName);
        break;
      case 'zookeeperserver.hosts':
        this.set('value', masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName'));
        break;
      case 'hivemetastore.host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'HIVE_SERVER').hostName);
        break;
      case 'hive_ambari_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'HIVE_SERVER').hostName);
        break;
      case 'oozieserver.host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'OOZIE_SERVER').hostName);
        break;
      case 'oozie_ambari_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'OOZIE_SERVER').hostName);
        break;
    }
  },

  isValid: function () {
    return this.get('errorMessage') === '';
  }.property('errorMessage'),

  viewClass: function () {
    switch (this.get('displayType')) {
      case 'checkbox':
        return App.ServiceConfigCheckbox;
      case 'password':
        return App.ServiceConfigPasswordField;
      case 'combobox':
        return App.ServiceConfigComboBox;
      case 'radio button':
        return App.ServiceConfigRadioButtons;
        break;
      case 'directories':
        return App.ServiceConfigTextArea;
        break;
      case 'custom':
        return App.ServiceConfigBigTextArea;
      case 'masterHost':
        return App.ServiceConfigMasterHostView;
      case 'masterHosts':
        return App.ServiceConfigMasterHostsView;
      case 'slaveHosts':
        return App.ServiceConfigSlaveHostsView;
      default:
        if (this.get('unit')) {
          return App.ServiceConfigTextFieldWithUnit;
        } else {
          return App.ServiceConfigTextField;
        }
    }
  }.property('displayType'),

  validate: function () {

    var value = this.get('value');

    var isError = false;

    if (this.get('isRequired')) {
      if (typeof value === 'string' && value.trim().length === 0) {
        this.set('errorMessage', 'This is required');
        isError = true;
      }
    }

    if (!isError) {
      switch (this.get('displayType')) {
        case 'int':
          if (!validator.isValidInt(value)) {
            this.set('errorMessage', 'Must contain digits only');
            isError = true;
          }
          break;
        case 'float':
          if (!validator.isValidFloat(value)) {
            this.set('errorMessage', 'Must be a valid number');
            isError = true;
          }
          break;
        case 'checkbox':
          break;
        case 'directories':
          break;
        case 'custom':
          break;
        case 'email':
          if (!validator.isValidEmail(value)) {
            this.set('errorMessage', 'Must be a valid email address');
            isError = true;
          }
          break;
        case 'password':
          // retypedPassword is set by the retypePasswordView child view of App.ServiceConfigPasswordField
          if (value !== this.get('retypedPassword')) {
            this.set('errorMessage', 'Passwords do not match');
            isError = true;
          }
      }
    }
    if (!isError) {
      this.set('errorMessage', '');
    }
  }.observes('value', 'retypedPassword')

});

