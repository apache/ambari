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
  initConfigsLength: 0, // configs length after initialization in order to watch changes
  errorCount: function () {
    var overrideErrors = 0;
    this.get('configs').filterProperty("overrides").forEach(function (e) {
      e.overrides.forEach(function (e) {
        if (e.error) {
          overrideErrors += 1;
        }
      })
    });
    var categoryNames = this.get('configCategories').mapProperty('name');
    var masterErrors = this.get('configs').filter(function (item) {
      return categoryNames.contains(item.get('category'));
    }).filterProperty('isValid', false).filterProperty('isVisible', true).get('length');
    var slaveErrors = 0;
    this.get('configCategories').forEach(function (_category) {
      slaveErrors += _category.get('slaveErrorCount');
    }, this);
    return masterErrors + slaveErrors + overrideErrors;
  }.property('configs.@each.isValid', 'configs.@each.isVisible', 'configCategories.@each.slaveErrorCount', 'configs.@each.overrideErrorTrigger'),

  isPropertiesChanged: function() {
    return this.get('configs').someProperty('isNotDefaultValue') ||
           this.get('configs').someProperty('isOverrideChanged') ||
           this.get('configs.length') !== this.get('initConfigsLength') ||
           (this.get('configs.length') === this.get('initConfigsLength') && this.get('configs').someProperty('defaultValue', null));
  }.property('configs.@each.isNotDefaultValue', 'configs.@each.isOverrideChanged', 'configs.length')
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
  isNotEditable: Ember.computed.not('isEditable'),
  isFinal: false,
  hideFinalIcon: function () {
    return (!this.get('isFinal'))&& this.get('isNotEditable');
  }.property('isFinal', 'isNotEditable'),
  defaultIsFinal: false,
  supportsFinal: false,
  isVisible: true,
  isMock: false, // mock config created created only to displaying
  isRequiredByAgent: true, // Setting it to true implies property will be stored in configuration
  isSecureConfig: false,
  errorMessage: '',
  warnMessage: '',
  serviceConfig: null, // points to the parent App.ServiceConfig object
  filename: '',
  isOriginalSCP : true, // if true, then this is original SCP instance and its value is not overridden value.
  parentSCP: null, // This is the main SCP which is overridden by this. Set only when isOriginalSCP is false.
  selectedHostOptions : null, // contain array of hosts configured with overridden value
  overrides : null,
  overrideValues: [],
  group: null, // Contain group related to this property. Set only when isOriginalSCP is false.
  isUserProperty: null, // This property was added by user. Hence they get removal actions etc.
  isOverridable: true,
  compareConfigs: [],
  isComparison: false,
  hasCompareDiffs: false,
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
  hasInitialValue: false, //if true then property value is defined and saved to server
  /**
   * Usage example see on <code>App.ServiceConfigRadioButtons.handleDBConnectionProperty()</code>
   *
   * @property {Ember.View} additionalView - custom view related to property
   **/
  additionalView: null,

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
  isOverrideChanged: function () {
    if (Em.isNone(this.get('overrides')) && this.get('overrideValues.length') === 0) return false;
    return JSON.stringify(this.get('overrides').mapProperty('isFinal')) !== JSON.stringify(this.get('overrideIsFinalValues'))
      || JSON.stringify(this.get('overrides').mapProperty('value')) !== JSON.stringify(this.get('overrideValues'));
  }.property('isOverridden', 'overrides.@each.isNotDefaultValue'),
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
    var defaultValue = this.get('defaultValue');
    var supportsFinal = this.get('supportsFinal');
    var isFinal = this.get('isFinal');
    var defaultIsFinal = this.get('defaultIsFinal');
    var isEditable = this.get('isEditable');
    return isEditable && ((defaultValue != null && value !== defaultValue) || (supportsFinal && isFinal !== defaultIsFinal));
  }.property('value', 'defaultValue', 'isEditable', 'isFinal', 'defaultIsFinal'),

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
    var hostWithPort = "([\\w|\\.]*)(?=:)";
    var hostWithPrefix = ":\/\/" + hostWithPort;
    switch (this.get('name')) {
      case 'namenode_host':
        this.set('value', masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName'));
        break;
      case 'dfs.http.address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setDefaultValue(hostWithPort,nnHost);
        break;
      case 'dfs.namenode.http-address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setDefaultValue(hostWithPort,nnHost);
        break;
      case 'dfs.https.address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setDefaultValue(hostWithPort,nnHost);
        break;
      case 'dfs.namenode.https-address':
        var nnHost =  masterComponentHostsInDB.findProperty('component', 'NAMENODE').hostName;
        this.setDefaultValue(hostWithPort,nnHost);
        break;
      case 'fs.default.name':
        var nnHost = masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName');
        this.setDefaultValue(hostWithPrefix,'://' + nnHost);
        break;
      case 'fs.defaultFS':
        var nnHost = masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName');
        this.setDefaultValue(hostWithPrefix,'://' + nnHost);
        break;
      case 'hbase.rootdir':
        var nnHost = masterComponentHostsInDB.filterProperty('component', 'NAMENODE').mapProperty('hostName');
        this.setDefaultValue(hostWithPrefix,'://' + nnHost);
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
          this.setDefaultValue(hostWithPort,snnHost.hostName);
        }
        break;
      case 'dfs.namenode.secondary.http-address':
        var snnHost = masterComponentHostsInDB.findProperty('component', 'SECONDARY_NAMENODE');
        if (snnHost) {
          this.setDefaultValue(hostWithPort,snnHost.hostName);
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
        this.setDefaultValue(hostWithPrefix,'://' + hsHost);
        break;
      case 'mapreduce.jobhistory.webapp.address':
        var hsHost = masterComponentHostsInDB.filterProperty('component', 'HISTORYSERVER').mapProperty('hostName');
        this.setDefaultValue(hostWithPort,hsHost);
        break;
      case 'mapreduce.jobhistory.address':
        var hsHost = masterComponentHostsInDB.filterProperty('component', 'HISTORYSERVER').mapProperty('hostName');
        this.setDefaultValue(hostWithPort,hsHost);
        break;
      case 'rm_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName);
        break;
      case 'ats_host':
        var atsHost =  masterComponentHostsInDB.findProperty('component', 'APP_TIMELINE_SERVER');
        if (atsHost)
          this.set('value', atsHost.hostName);
        else
          this.set('value', 'false');
        break;
      case 'yarn.resourcemanager.hostname':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.set('defaultValue',rmHost);
        this.set('value',this.get('defaultValue'));
        break;
      case 'yarn.resourcemanager.resource-tracker.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue(hostWithPort,rmHost);
        break;
      case 'yarn.resourcemanager.webapp.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue(hostWithPort,rmHost);
        break;
      case 'yarn.resourcemanager.scheduler.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue(hostWithPort,rmHost);
        break;
      case 'yarn.resourcemanager.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue(hostWithPort,rmHost);
        break;
      case 'yarn.resourcemanager.admin.address':
        var rmHost = masterComponentHostsInDB.findProperty('component', 'RESOURCEMANAGER').hostName;
        this.setDefaultValue(hostWithPort,rmHost);
        break;
      case 'yarn.timeline-service.webapp.address':
        var atsHost =  masterComponentHostsInDB.findProperty('component', 'APP_TIMELINE_SERVER');
        if (atsHost && atsHost.hostName) {
          this.setDefaultValue(hostWithPort,atsHost.hostName);
        }
        break;
      case 'yarn.timeline-service.webapp.https.address':
        var atsHost =  masterComponentHostsInDB.findProperty('component', 'APP_TIMELINE_SERVER');
        if (atsHost && atsHost.hostName) {
          this.setDefaultValue(hostWithPort,atsHost.hostName);
        }
        break;
      case 'yarn.timeline-service.address':
        var atsHost =  masterComponentHostsInDB.findProperty('component', 'APP_TIMELINE_SERVER');
        if (atsHost && atsHost.hostName) {
          this.setDefaultValue(hostWithPort,atsHost.hostName);
        }
        break;
      case 'nm_hosts':
        this.set('value', slaveComponentHostsInDB.findProperty('componentName', 'NODEMANAGER').hosts.mapProperty('hostName'));
        break;
      case 'jobtracker_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'JOBTRACKER').hostName);
        break;
      case 'mapred.job.tracker':
        var jtHost = masterComponentHostsInDB.findProperty('component', 'JOBTRACKER').hostName;
        this.setDefaultValue(hostWithPort,jtHost);
        break;
      case 'mapred.job.tracker.http.address':
        var jtHost = masterComponentHostsInDB.findProperty('component', 'JOBTRACKER').hostName;
        this.setDefaultValue(hostWithPort,jtHost);
        break;
      case 'mapreduce.history.server.http.address':
        var jtHost = masterComponentHostsInDB.findProperty('component', 'HISTORYSERVER').hostName;
        this.setDefaultValue(hostWithPort,jtHost);
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
        this.setDefaultValue(hostWithPrefix,'://' + hiveHost);
        break;
      case 'hive_ambari_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'HIVE_SERVER').hostName);
        break;
      case 'hive_database':
        var newMySQLDBOption = this.get('options').findProperty('displayName', 'New MySQL Database');
        if (newMySQLDBOption) {
          var isNewMySQLDBOptionHidden = !App.get('supports.alwaysEnableManagedMySQLForHive') && App.get('router.currentState.name') != 'configs' &&
            !App.get('isManagedMySQLForHiveEnabled');
          if (isNewMySQLDBOptionHidden && this.get('value') == 'New MySQL Database') {
            this.set('value', 'Existing MySQL Database');
          }
          Em.set(newMySQLDBOption, 'hidden', isNewMySQLDBOptionHidden);
        }
        break;
      case 'oozieserver_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'OOZIE_SERVER').hostName);
        break;
      case 'oozie.base.url':
        var oozieHost = masterComponentHostsInDB.findProperty('component', 'OOZIE_SERVER').hostName;
        this.setDefaultValue(hostWithPrefix,'://' + oozieHost);
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
      case 'knox_gateway_host':
        this.set('value', masterComponentHostsInDB.findProperty('component', 'KNOX_GATEWAY').hostName);
        break;
      case 'kafka_broker_hosts':
        this.set('value', masterComponentHostsInDB.filterProperty('component', 'KAFKA_BROKER').mapProperty('hostName'));
        break;
      case 'kafka.ganglia.metrics.host':
        var gangliaHost =  masterComponentHostsInDB.findProperty('component', 'GANGLIA_SERVER');
        if (gangliaHost) {
          this.set('value', gangliaHost.hostName);
        }
        break;
      case 'hbase.zookeeper.quorum':
        var zkHosts = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
        this.setDefaultValue("(\\w*)", zkHosts);
        break;
      case 'zookeeper.connect':
      case 'hive.zookeeper.quorum':
      case 'templeton.zookeeper.hosts':
      case 'hadoop.registry.zk.quorum':
      case 'hive.cluster.delegation.token.store.zookeeper.connectString':
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
      case 'templeton.hive.properties':
        var hiveMetaStoreHost = masterComponentHostsInDB.findProperty('component', 'HIVE_METASTORE').hostName;
        if (/\/\/localhost:/g.test(this.get('value'))) {
          this.set('defaultValue', this.get('value') + ', hive.metastore.execute.setugi=true');
          this.setDefaultValue("(localhost)", hiveMetaStoreHost);
        }
        break;
      case 'dfs.name.dir':
      case 'dfs.namenode.name.dir':
      case 'dfs.data.dir':
      case 'dfs.datanode.data.dir':
      case 'yarn.nodemanager.local-dirs':
      case 'yarn.nodemanager.log-dirs':
      case 'mapred.local.dir':
      case 'log.dirs':  // for Kafka Broker
        this.unionAllMountPoints(!isOnlyFirstOneNeeded, localDB);
        break;
      case 'fs.checkpoint.dir':
      case 'dfs.namenode.checkpoint.dir':
      case 'yarn.timeline-service.leveldb-timeline-store.path':
      case 'dataDir':
      case 'oozie_data_dir':
      case 'hbase.tmp.dir':
      case 'storm.local.dir':
      case '*.falcon.graph.storage.directory':
      case '*.falcon.graph.serialize.path':
        this.unionAllMountPoints(isOnlyFirstOneNeeded, localDB);
        break;
      case '*.broker.url':
        var falconServerHost = masterComponentHostsInDB.findProperty('component', 'FALCON_SERVER').hostName;
        this.setDefaultValue('localhost', falconServerHost);
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
    //all hosts should be in local storage without using App.Host model
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
    var components = [];
    switch (this.get('name')) {
      case 'dfs.namenode.name.dir':
      case 'dfs.name.dir':
        components = masterComponentHostsInDB.filterProperty('component', 'NAMENODE');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'fs.checkpoint.dir':
      case 'dfs.namenode.checkpoint.dir':
        components = masterComponentHostsInDB.filterProperty('component', 'SECONDARY_NAMENODE');
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
      case 'yarn.timeline-service.leveldb-timeline-store.path':
        components = masterComponentHostsInDB.filterProperty('component', 'APP_TIMELINE_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'dataDir':
        components = masterComponentHostsInDB.filterProperty('component', 'ZOOKEEPER_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'oozie_data_dir':
        components = masterComponentHostsInDB.filterProperty('component', 'OOZIE_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'hbase.tmp.dir':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'HBASE_REGIONSERVER');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        break;
      case 'storm.local.dir':
        temp = slaveComponentHostsInDB.findProperty('componentName', 'SUPERVISOR');
        temp.hosts.forEach(function (host) {
          setOfHostNames.push(host.hostName);
        }, this);
        components = masterComponentHostsInDB.filterProperty('component', 'NIMBUS');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case '*.falcon.graph.storage.directory':
      case '*.falcon.graph.serialize.path':
        components = masterComponentHostsInDB.filterProperty('component', 'FALCON_SERVER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
        }, this);
        break;
      case 'log.dirs':
        components = masterComponentHostsInDB.filterProperty('component', 'KAFKA_BROKER');
        components.forEach(function (component) {
          setOfHostNames.push(component.hostName);
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
        mPoint = this.get('defaultDirectory');
      } else {
        mPoint = mPoint + this.get('defaultDirectory');
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
      case 'datanodedirs':
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
      case 'label':
        return App.ServiceConfigLabelView;
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
    var supportsFinal = this.get('supportsFinal');
    var isFinal = this.get('isFinal');
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
        case 'datanodedirs':
          if (!validator.isValidDataNodeDir(value)) {
            this.set('errorMessage', 'dir format is wrong, can be "[{storage type}]/{dir name}"');
            isError = true;
          }
          else {
            if (!validator.isAllowedDir(value)) {
              this.set('errorMessage', 'Cannot start with "home(s)"');
              isError = true;
            }
          }
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
          if (value === parentSCP.get('value') && supportsFinal && isFinal === parentSCP.get('isFinal')) {
            this.set('errorMessage', 'Configuration overrides must have different value');
            isError = true;
          } else {
            var overrides = parentSCP.get('overrides');
            if (overrides) {
              overrides.forEach(function (override) {
                if (self != override && value === override.get('value')  && supportsFinal && isFinal === parentSCP.get('isFinal')) {
                  self.set('errorMessage', 'Multiple configuration overrides cannot have same value');
                  isError = true;
                }
              });
            }
          }
        }
      }
    }
    if (!App.get('supports.serverRecommendValidate')) {
      var serviceValidator = this.get('serviceValidator');
      if (serviceValidator!=null) {
        var validationIssue = serviceValidator.validateConfig(this);
        if (validationIssue) {
          this.set('warnMessage', validationIssue);
          isWarn = true;
        }
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
  }.observes('value', 'isFinal', 'retypedPassword')

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
