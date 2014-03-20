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

App.ServiceConfig = Ember.Object.extend({
  serviceName: '',
  configCategories: [],
  configs: null,
  restartRequired: false,
  restartRequiredMessage: '',
  restartRequiredHostsAndComponents: {},
  configGroups: [],
  errorCount: function () {
    var overrideErrors = 0;
    this.get('configs').filterProperty("overrides").forEach(function (e) {
      e.overrides.forEach(function (e) {
        if (e.error) {
          overrideErrors += 1;
        }
      })
    });
    var masterErrors = this.get('configs').filterProperty('isValid', false).filterProperty('isVisible', true).get('length');
    var slaveErrors = 0;
    this.get('configCategories').forEach(function (_category) {
      slaveErrors += _category.get('slaveErrorCount');
    }, this);
    return masterErrors + slaveErrors + overrideErrors;
  }.property('configs.@each.isValid', 'configs.@each.isVisible', 'configCategories.@each.slaveErrorCount', 'configs.@each.overrideErrorTrigger')
});

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
      'Hive Metastore', 'WebHCat Server', 'ZooKeeper Server', 'Nagios', 'Ganglia' ];

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

  isAdvanced : function(){
    var name = this.get('name');
    return name.indexOf("Advanced") !== -1 ;
  }.property('name')
});


App.SlaveConfigs = Ember.Object.extend({
  componentName: null,
  displayName: null,
  hosts: null,
  groups: null
});

App.Group = Ember.Object.extend({
  name: null,
  hostNames: null,
  properties: null,
  errorCount: function () {
    if (this.get('properties')) {
      return this.get('properties').filterProperty('isValid', false).filterProperty('isVisible', true).get('length');
    }
  }.property('properties.@each.isValid', 'properties.@each.isVisible')
});


App.ServiceConfigProperty = Ember.Object.extend({

  id: '', //either 'puppet var' or 'site property'
  name: '',
  displayName: '',
  value: '',
  retypedPassword: '',
  defaultValue: '',
  defaultDirectory: '',
  description: '',
  displayType: 'string', // string, digits, number, directories, custom
  unit: '',
  category: 'General',
  isRequired: true, // by default a config property is required
  isReconfigurable: true, // by default a config property is reconfigurable
  isEditable: true, // by default a config property is editable
  isVisible: true,
  isRequiredByAgent: true, // Setting it to true implies property will be stored in global configuration
  isSecureConfig: false,
  errorMessage: '',
  warnMessage: '',
  serviceConfig: null, // points to the parent App.ServiceConfig object
  filename: '',
  isOriginalSCP : true, // if true, then this is original SCP instance and its value is not overridden value.
  parentSCP: null, // This is the main SCP which is overridden by this. Set only when isOriginalSCP is false.
  selectedHostOptions : null, // contain array of hosts configured with overridden value
  overrides : null,
  group: null, // Contain group related to this property. Set only when isOriginalSCP is false.
  isUserProperty: null, // This property was added by user. Hence they get removal actions etc.
  isOverridable: true,
  showLabel: true,
  error: false,
  warn: false,
  overrideErrorTrigger: 0, //Trigger for overrridable property error
  isRestartRequired: false,
  restartRequiredMessage: 'Restart required',
  index: null, //sequence number in category
  editDone: false, //Text field: on focusOut: true, on focusIn: false
  serviceValidator: null,
  isNotSaved: false, // user property was added but not saved

  /**
   * On Overridable property error message, change overrideErrorTrigger value to recount number of errors service have
   */
  observeErrors: function () {
    this.set("overrideErrorTrigger", this.get("overrideErrorTrigger") + 1);
  }.observes("overrides.@each.errorMessage"),
  /**
   * No override capabilities for fields which are not edtiable
   * and fields which represent master hosts.
   */
  isPropertyOverridable: function () {
    var overrideable = this.get('isOverridable');
    var editable = this.get('isEditable');
    var overrides = this.get('overrides');
    var dt = this.get('displayType');
    return overrideable && (editable || !overrides || !overrides.length) && ("masterHost" != dt);
  }.property('isEditable', 'displayType', 'isOverridable', 'overrides.length'),
  isOverridden: function() {
    var overrides = this.get('overrides');
    return (overrides != null && overrides.get('length')>0) || !this.get('isOriginalSCP');
  }.property('overrides', 'overrides.length', 'isOriginalSCP'),
  isRemovable: function() {
    var isOriginalSCP = this.get('isOriginalSCP');
    var isUserProperty = this.get('isUserProperty');
    var isEditable = this.get('isEditable');
    var hasOverrides = this.get('overrides.length') > 0;
    // Removable when this is a user property, or it is not an original property and it is editable
    return isEditable && !hasOverrides && (isUserProperty || !isOriginalSCP);
  }.property('isUserProperty', 'isOriginalSCP', 'overrides.length'),
  init: function () {
    if(this.get("displayType")=="password"){
      this.set('retypedPassword', this.get('value'));
    }
    if ((this.get('id') === 'puppet var') && this.get('value') == '') {
      this.set('value', this.get('defaultValue'));
    }
    // TODO: remove mock data
  },

  /**
   * Indicates when value is not the default value.
   * Returns false when there is no default value.
   */
  isNotDefaultValue: function () {
    var value = this.get('value');
    var dValue = this.get('defaultValue');
    var isEditable = this.get('isEditable');
    return isEditable && dValue != null && value !== dValue;
  }.property('value', 'defaultValue', 'isEditable'),

  /**
   * Don't show "Undo" for hosts on Installer Step7
   */
  cantBeUndone: function() {
    var types = ["masterHost", "slaveHosts", "masterHosts", "slaveHost","radio button"];
    var displayType = this.get('displayType');
    var result = false;
    types.forEach(function(type) {
      if (type === displayType) {
        result = true;
      }
    });
    return result;
  }.property('displayType'),

  initialValue: function (localDB) {
    var masterComponentHostsInDB = localDB.masterComponentHosts;
    //console.log("value in initialvalue: " + JSON.stringify(masterComponentHostsInDB));
    var hostsInfo = localDB.hosts; // which we are setting in installerController in step3.
    var slaveComponentHostsInDB = localDB.slaveComponentHosts;
    var isOnlyFirstOneNeeded = true;
    switch (this.get('name')) {
      case 'namenode_host':
        this.set('value', masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName'));
        break;
      case 'dfs.http.address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setDefaultValue("(\\w*)(?=:)",nnHost);
        break;
      case 'dfs.namenode.http-address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setDefaultValue("(\\w*)(?=:)",nnHost);
        break;
      case 'dfs.https.address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setDefaultValue("(\\w*)(?=:)",nnHost);
        break;
      case 'dfs.namenode.https-address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setDefaultValue("(\\w*)(?=:)",nnHost);
        break;
      case 'fs.default.name':
        var nnHost = masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName');
        this.setDefaultValue(":\/\/(\\w*)(?=:)",'://' + nnHost);
        break;
      case 'fs.defaultFS':
        var nnHost = masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName');
        this.setDefaultValue(":\/\/(\\w*)(?=:)",'://' + nnHost);
        break;
      case 'hbase.rootdir':
        var nnHost = masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName');
        this.setDefaultValue(":\/\/(\\w*)(?=:)",'://' + nnHost);
        break;
      case 'snamenode_host':
        // Secondary NameNode does not exist when NameNode HA is enabled
        var snn = masterComponentHostsInDB.findProperty('component', 'SECONDARY_NAMENODE');
        if (snn) {
          this.set('value', snn.hostName);
        }
        break;
      case 'dfs.secondary.http.address':
        var snnHost = masterComponentHostsInDB.findProperty('component', 'SECONDARY_NAMENODE');
        if (snnHost) {
          this.setDefaultValue("(\\w*)(?=:)",snnHost.hostName);
        }
        break;
      case 'dfs.namenode.secondary.http-address':
        var snnHost = masterComponentHostsInDB.findProperty('component', 'SECONDARY_NAMENODE');
        if (snnHost) {
          this.setDefaultValue("(\\w*)(?=:)",snnHost.hostName);
        }
        break;
      case 'datanode_hosts':
        this.set('value', slaveComponentHostsInDB.findProperty('componentName', 'DATANODE').hosts.mapProperty('hostName'));
        break;
      case 'hs_host':
        this.set('value', masterComponentHostsInDB.filterProperty('component', 'HISTORYSERVER').mapProperty('hostName'));
        break;
      case 'yarn.log.server.url':
        var hsHost = masterComponentHostsInDB.filterProperty('component', 'HISTORYSERVER').mapProperty('hostName');
        this.setDefaultValue(":\/\/(\\w*)(?=:)",'://' + hsHost);
        break;
      case 'mapreduce.jobhistory.webapp.address':
        var hsHost = masterComponentHostsInDB.filterProperty('component', 'HISTORYSERVER').mapProperty('hostName');
        this.setDefaultValue("(\\w*)(?=:)",hsHost);
        break;
      case 'mapreduce.jobhistory.address':
        var hsHost = masterComponentHostsInDB.filterProperty('component', 'HISTORYSERVER').mapProperty('hostName');
        this.setDefaultValue("(\\w*)(?=:)",hsHost);
        break;
      case 'rm_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName);
        break;
      case 'ats_host':
        if (!App.supports.appTimelineServer) return; // @todo remove test mode check after App Timeline service integration
        this.set('value', masterComponentHostsInDB.findProperty('component', 'APP_TIMELINE_SERVER').hostName);
        break;
      case 'yarn.resourcemanager.hostname':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.set('defaultValue',rmHost);
        this.set('value',this.get('defaultValue'));
        break;
      case 'yarn.resourcemanager.resource-tracker.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue("(\\w*)(?=:)",rmHost);
        break;
      case 'yarn.resourcemanager.webapp.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue("(\\w*)(?=:)",rmHost);
        break;
      case 'yarn.resourcemanager.scheduler.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue("(\\w*)(?=:)",rmHost);
        break;
      case 'yarn.resourcemanager.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue("(\\w*)(?=:)",rmHost);
        break;
      case 'yarn.resourcemanager.admin.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue("(\\w*)(?=:)",rmHost);
        break;
      case 'yarn.timeline-service.webapp.address':
        var hsHost = masterComponentHostsInDB.findProperty('component', 'APP_TIMELINE_SERVER').hostName;
        this.setDefaultValue("(0.0.0.0)(?=:)", hsHost);
        break;
      case 'yarn.timeline-service.webapp.https.address':
        var hsHost = masterComponentHostsInDB.findProperty('component', 'APP_TIMELINE_SERVER').hostName;
        this.setDefaultValue("(0.0.0.0)(?=:)", hsHost);
        break;
      case 'nm_hosts':
        this.set('value', slaveComponentHostsInDB.findProperty('componentName', 'NODEMANAGER').hosts.mapProperty('hostName'));
        break;
      case 'jobtracker_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'JOBTRACKER').hostName);
        break;
      case 'mapred.job.tracker':
        var jtHost = masterComponentHostsInDB.findProperty('component', 'JOBTRACKER').hostName;
        this.setDefaultValue("(\\w*)(?=:)",jtHost);
        break;
      case 'mapred.job.tracker.http.address':
        var jtHost = masterComponentHostsInDB.findProperty('component', 'JOBTRACKER').hostName;
        this.setDefaultValue("(\\w*)(?=:)",jtHost);
        break;
      case 'mapreduce.history.server.http.address':
        var jtHost = masterComponentHostsInDB.findProperty('component', 'HISTORYSERVER').hostName;
        this.setDefaultValue("(\\w*)(?=:)",jtHost);
        break;
      case 'tasktracker_hosts':
        this.set('value', slaveComponentHostsInDB.findProperty('componentName', 'TASKTRACKER').hosts.mapProperty('hostName'));
        break;
      case 'hbasemaster_host':
        this.set('value', masterComponentHostsInDB.filterProperty('component', 'HBASE_MASTER').mapProperty('hostName'));
        break;
      case 'regionserver_hosts':
        this.set('value', slaveComponentHostsInDB.findProperty('componentName', 'HBASE_REGIONSERVER').hosts.mapProperty('hostName'));
        break;
      case 'hivemetastore_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'HIVE_SERVER').hostName);
        break;
      case 'hive.metastore.uris':
        var hiveHost = masterComponentHostsInDB.findProperty('component', 'HIVE_SERVER').hostName;
        this.setDefaultValue(":\/\/(\\w*)(?=:)",'://' + hiveHost);
        break;
      case 'hive_ambari_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'HIVE_SERVER').hostName);
        break;
      case 'oozieserver_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'OOZIE_SERVER').hostName);
        break;
      case 'oozie.base.url':
        var oozieHost = masterComponentHostsInDB.findProperty('component', 'OOZIE_SERVER').hostName;
        this.setDefaultValue(":\/\/(\\w*)(?=:)",'://' + oozieHost);
        break;
      case 'webhcatserver_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'WEBHCAT_SERVER').hostName);
        break;
      case 'hueserver_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'HUE_SERVER').hostName);
        break;
      case 'oozie_ambari_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'OOZIE_SERVER').hostName);
        break;
      case 'storm.zookeeper.servers':
      case 'zookeeperserver_hosts':
        this.set('value', masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName'));
        break;
      case 'nimbus.host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'NIMBUS').hostName);
        break;
      case 'falconserver_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'FALCON_SERVER').hostName);
        break;
      case 'drpcserver_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'DRPC_SERVER').hostName);
        break;
      case 'stormuiserver_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'STORM_UI_SERVER').hostName);
        break;
      case 'storm_rest_api_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'STORM_REST_API').hostName);
        break;
      case 'supervisor_hosts':
        this.set('value', slaveComponentHostsInDB.findProperty('componentName', 'SUPERVISOR').hosts.mapProperty('hostName'));
        break;
      case 'hbase.zookeeper.quorum':
        var zkHosts = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
        this.setDefaultValue("(\\w*)", zkHosts);
        break;
      case 'templeton.zookeeper.hosts':
        var zkHosts = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
        var zkHostPort = zkHosts;
        var regex = "\\w*:(\\d+)";   //regex to fetch the port
        var portValue = this.get('defaultValue').match(new RegExp(regex));
        if (!portValue) return;
        if (portValue[1]) {
          for ( var i = 0; i < zkHosts.length; i++ ) {
            zkHostPort[i] = zkHosts[i] + ":" + portValue[1];
          }
        }
        this.setDefaultValue("(.*)", zkHostPort);
        break;
      case 'dfs.name.dir':
      case 'dfs.namenode.name.dir':
      case 'dfs.data.dir':
      case 'dfs.datanode.data.dir':
      case 'yarn.nodemanager.local-dirs':
      case 'yarn.nodemanager.log-dirs':
      case 'mapred.local.dir':
        this.unionAllMountPoints(!isOnlyFirstOneNeeded, localDB);
        break;
      case 'fs.checkpoint.dir':
      case 'dfs.namenode.checkpoint.dir':
      case 'zk_data_dir':
      case 'oozie_data_dir':
      case 'hbase.tmp.dir':
        this.unionAllMountPoints(isOnlyFirstOneNeeded, localDB);
        break;
    }
  },

  /**
   * @param regex : String
   * @param replaceWith : String
   */
  setDefaultValue: function(regex,replaceWith) {
    var defaultValue = this.get('defaultValue');
    var re = new RegExp(regex);
    defaultValue = defaultValue.replace(re,replaceWith);
    this.set('defaultValue',defaultValue);
    this.set('value',this.get('defaultValue'));
  },

  unionAllMountPoints: function (isOnlyFirstOneNeeded, localDB) {
    var hostname = '';
    var mountPointsPerHost = [];
    var mountPointAsRoot;
    var masterComponentHostsInDB = localDB.masterComponentHosts;
    var slaveComponentHostsInDB = localDB.slaveComponentHosts;
    var hostsInfo = localDB.hosts; // which we are setting in installerController in step3.
    App.Host.find().forEach(function(item){
      if(!hostsInfo[item.get('id')]){
        hostsInfo[item.get('id')] = {
          name: item.get('id'),
          cpu: item.get('cpu'),
          memory: item.get('memory'),
          disk_info: item.get('diskInfo'),
          bootStatus: "REGISTERED",
          isInstalled: true
        };
      }
    });
    var temp = '';
    var setOfHostNames = [];
    switch (this.get('name')) {
      case 'dfs.namenode.name.dir':
      case 'dfs.name.dir':
        var components = masterComponentHostsInDB.filterProperty('component', 'NAMENODE');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'fs.checkpoint.dir':
      case 'dfs.namenode.checkpoint.dir':
        var components = masterComponentHostsInDB.filterProperty('component', 'SECONDARY_NAMENODE');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'dfs.data.dir':
      case 'dfs.datanode.data.dir':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'DATANODE');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        break;
      case 'mapred.local.dir':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'TASKTRACKER') || slaveComponentHostsInDB.findProperty('componentName', 'NODEMANAGER');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        break;
      case 'yarn.nodemanager.log-dirs':
      case 'yarn.nodemanager.local-dirs':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'NODEMANAGER');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        break;
      case 'zk_data_dir':
        var components = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'oozie_data_dir':
        var components = masterComponentHostsInDB.filterProperty('component', 'OOZIE_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'hbase.tmp.dir':
        var temp = slaveComponentHostsInDB.findProperty('componentName', 'HBASE_REGIONSERVER');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        break;
    }

    // In Add Host Wizard, if we did not select this slave component for any host, then we don't process any further.
    if (setOfHostNames.length === 0) {
      return;
    }

    var allMountPoints = [];
    for (var i = 0; i < setOfHostNames.length; i++) {
      hostname = setOfHostNames[i];

      mountPointsPerHost = hostsInfo[hostname].disk_info;

      mountPointAsRoot = mountPointsPerHost.findProperty('mountpoint', '/');

      // If Server does not send any host details information then atleast one mountpoint should be presumed as root
      // This happens in a single container Linux Docker environment.
      if (!mountPointAsRoot) {
        mountPointAsRoot = {mountpoint: '/'};
      }

      mountPointsPerHost = mountPointsPerHost.filter(function (mPoint) {
        return !(['/', '/home', '/boot'].contains(mPoint.mountpoint) || ['devtmpfs', 'tmpfs', 'vboxsf'].contains(mPoint.type));
      });

      mountPointsPerHost.forEach(function (mPoint) {
        if( !allMountPoints.findProperty("mountpoint", mPoint.mountpoint)) {
          allMountPoints.push(mPoint);
        }
      }, this);
    }
    if (allMountPoints.length == 0) {
      allMountPoints.push(mountPointAsRoot);
    }
    this.set('value', '');
    if (!isOnlyFirstOneNeeded) {
      allMountPoints.forEach(function (eachDrive) {
        var mPoint = this.get('value');
        if (!mPoint) {
          mPoint = "";
        }
        if (eachDrive.mountpoint === "/") {
          mPoint += this.get('defaultDirectory') + "\n";
        } else {
          mPoint += eachDrive.mountpoint + this.get('defaultDirectory') + "\n";
        }
        this.set('value', mPoint);
        this.set('defaultValue', mPoint);
      }, this);
    } else {
      var mPoint = allMountPoints[0].mountpoint;
      if (mPoint === "/") {
        mPoint = this.get('defaultDirectory') + "\n";
      } else {
        mPoint = mPoint + this.get('defaultDirectory') + "\n";
      }
      this.set('value', mPoint);
      this.set('defaultValue', mPoint);
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
      case 'content':
        return App.ServiceConfigTextAreaContent;
        break;
      case 'multiLine':
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
    var valueRange = this.get('valueRange');
    var values = [];//value split by "," to check UNIX users, groups list

    var isError = false;
    var isWarn = false;

    if (typeof value === 'string' && value.length === 0) {
      if (this.get('isRequired')) {
        this.set('errorMessage', 'This is required');
        isError = true;
      } else {
        return;
      }
    }

    if (!isError) {
      switch (this.get('displayType')) {
        case 'int':
          if (!validator.isValidInt(value)) {
            this.set('errorMessage', 'Must contain digits only');
            isError = true;
          } else {
            if(valueRange){
              if(value < valueRange[0] || value > valueRange[1]){
                this.set('errorMessage', 'Must match the range');
                isError = true;
              }
            }
          }
          break;
        case 'float':
          if (!validator.isValidFloat(value)) {
            this.set('errorMessage', 'Must be a valid number');
            isError = true;
          }
          break;
        case 'UNIXList':
          if(value != '*'){
            values = value.split(',');
            for(var i = 0, l = values.length; i < l; i++){
              if(!validator.isValidUNIXUser(values[i])){
                if(this.get('type') == 'USERS'){
                  this.set('errorMessage', 'Must be a valid list of user names');
                } else {
                  this.set('errorMessage', 'Must be a valid list of group names');
                }
                isError = true;
              }
            }
          }
          break;
        case 'checkbox':
          break;
        case 'directories':
        case 'directory':
          if (!validator.isValidDir(value)) {
            this.set('errorMessage', 'Must be a slash at the start');
            isError = true;
          }
          else {
            if (!validator.isAllowedDir(value)) {
              this.set('errorMessage', 'Can\'t start with "home(s)"');
              isError = true;
            }
          }
          break;
        case 'custom':
          break;
        case 'user':
          if (!validator.isValidUserName(value)) {
            this.set('errorMessage', Em.I18n.t('users.userName.validationFail'));
            isError = true;
          }
          break;
        case 'email':
          if (!validator.isValidEmail(value)) {
            this.set('errorMessage', 'Must be a valid email address');
            isError = true;
          }
          break;
        case 'host':
          var hiveOozieHostNames = ['hive_hostname','hive_existing_mysql_host','hive_existing_oracle_host','hive_ambari_host',
          'oozie_hostname','oozie_existing_mysql_host','oozie_existing_oracle_host','oozie_ambari_host'];
          if(hiveOozieHostNames.contains(this.get('name'))) {
            if (validator.hasSpaces(value)) {
              this.set('errorMessage', Em.I18n.t('host.spacesValidation'));
              isError = true;
            }
          } else {
            if (validator.isNotTrimmed(value)) {
              this.set('errorMessage', Em.I18n.t('host.trimspacesValidation'));
              isError = true;
            }
          }
          break;
        case 'advanced':
          if(this.get('name')=='javax.jdo.option.ConnectionURL' || this.get('name')=='oozie.service.JPAService.jdbc.url') {
            if (validator.isNotTrimmed(value)) {
              this.set('errorMessage', Em.I18n.t('host.trimspacesValidation'));
              isError = true;
            }
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
      // Check if this value is already in any of the overrides
      var self = this;
      var isOriginalSCP = this.get('isOriginalSCP');
      var parentSCP = this.get('parentSCP');
      if (!isOriginalSCP) {
        if (!isError && parentSCP != null) {
          if (value === parentSCP.get('value')) {
            this.set('errorMessage', 'Configuration overrides must have different value');
            isError = true;
          } else {
            var overrides = parentSCP.get('overrides');
            overrides.forEach(function (override) {
              if (self != override && value === override.get('value')) {
                self.set('errorMessage', 'Multiple configuration overrides cannot have same value');
                isError = true;
              }
            });
          }
        }
      }
    }
    
    var serviceValidator = this.get('serviceValidator');
    if (serviceValidator!=null) {
      var validationIssue = serviceValidator.validateConfig(this);
      if (validationIssue) {
    	this.set('warnMessage', validationIssue);
    	isWarn = true;
      }
    }

    if (!isWarn || isError) { // Errors get priority
        this.set('warnMessage', '');
        this.set('warn', false);
    } else {
        this.set('warn', true);
    }
    
    if (!isError) {
        this.set('errorMessage', '');
        this.set('error', false);
      } else {
        this.set('error', true);
      }
  }.observes('value', 'retypedPassword')

});

App.ConfigSiteTag = Ember.Object.extend({
  site: DS.attr('string'),
  tag: DS.attr('string'),
  /**
   * Object map of hostname->override-tag for overrides.
   * <b>Creators should set new object here.<b>
   */
  hostOverrides: null
});
