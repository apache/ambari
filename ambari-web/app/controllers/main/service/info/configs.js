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
require('controllers/wizard/slave_component_groups_controller');
var batchUtils = require('utils/batch_scheduled_requests');

App.MainServiceInfoConfigsController = Em.Controller.extend(App.ServerValidatorMixin, App.EnhancedConfigsMixin, App.PreloadRequestsChainMixin, App.ThemesMappingMixin, App.VersionsMappingMixin, App.ConfigsSaverMixin, {

  name: 'mainServiceInfoConfigsController',

  isHostsConfigsPage: false,

  forceTransition: false,

  isRecommendedLoaded: true,

  dataIsLoaded: false,

  stepConfigs: [], //contains all field properties that are viewed in this service

  selectedService: null,

  serviceConfigTags: null,

  selectedConfigGroup: null,

  requestInProgress: null,

  selectedServiceConfigTypes: [],

  selectedServiceSupportsFinal: [],

  /**
   * config groups for current service
   * @type {App.ConfigGroup[]}
   */
  configGroups: [],

  allConfigs: [],

  uiConfigs: [],

  /**
   * Determines if save configs is in progress
   * @type {boolean}
   */
  saveInProgress: false,

  isCompareMode: false,

  compareServiceVersion: null,

  preSelectedConfigVersion: null,

  /**
   * contain Service Config Property, when user proceed from Select Config Group dialog
   */
  overrideToAdd: null,

  /**
   * version of default config group, configs of which currently applied
   */
  currentDefaultVersion: null,

  /**
   * version selected to view
   */
  selectedVersion: null,

  /**
   * file names of changed configs
   * @type {string[]}
   */
  modifiedFileNames: [],

  /**
   * note passed on configs save
   * @type {string}
   */
  serviceConfigVersionNote: '',

  versionLoaded: false,

  /**
   * current cluster-env version
   * @type {string}
   */
  clusterEnvTagVersion: '',

  /**
   * defines which service configs need to be loaded to stepConfigs
   * @type {string[]}
   */
  servicesToLoad: function() {
    return this.get('dependentServiceNames').concat([this.get('content.serviceName')]).uniq();
  }.property('content.serviceName', 'dependentServiceNames'),

  /**
   * defines which config groups need to be loaded
   * @type {object[]}
   */
  configGroupsToLoad: function() {
    return this.get('configGroups').concat(this.get('dependentConfigGroups')).uniq();
  }.property('content.serviceName', 'dependentServiceNames'),

  /**
   * configs from stack for dependent services
   * @type {App.StackConfigProperty[]}
   */
  advancedConfigs: function() {
    return App.StackConfigProperty.find().filter(function(scp) {
      return this.get('servicesToLoad').contains(scp.get('serviceName'));
    }, this);
  }.property('content.serviceName'),

  /**
   * @type {boolean}
   */
  isCurrentSelected: function () {
    return App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + this.get('selectedVersion')).get('isCurrent');
  }.property('selectedVersion', 'content.serviceName', 'dataIsLoaded'),

  /**
   * @type {boolean}
   */
  canEdit: function () {
    return this.get('isCurrentSelected') && !this.get('isCompareMode');
  }.property('isCurrentSelected', 'isCompareMode'),

  serviceConfigs: function () {
    return App.config.get('preDefinedServiceConfigs');
  }.property('App.config.preDefinedServiceConfigs'),

  configMapping: function () {
    return App.config.get('configMapping');
  }.property('App.config.configMapping'),

  configs: function () {
    return  App.config.get('preDefinedSiteProperties');
  }.property('App.config.preDefinedSiteProperties'),

  secureConfigs: require('data/HDP2/secure_mapping'),

  showConfigHistoryFeature: true,

  /**
   * Map, which contains relation between group and site
   * to upload overridden properties
   * @type {object}
   */
  loadedGroupToOverrideSiteToTagMap: {},

  /**
   * During page load time the cluster level site to tag
   * mapping is stored here.
   *
   * Example:
   * {
   *  'hdfs-site': 'version1',
   *  'core-site': 'version1'
   * }
   */
  loadedClusterSiteToTagMap: {},

  /**
   * Determines if Save-button should be disabled
   * Disabled if some configs have invalid values or save-process currently in progress
   * @type {boolean}
   */
  isSubmitDisabled: function () {
    return (!(this.get('stepConfigs').everyProperty('errorCount', 0)) || this.get('saveInProgress'));
  }.property('stepConfigs.@each.errorCount', 'saveInProgress'),

  /**
   * Determines if some config value is changed
   * @type {boolean}
   */
  isPropertiesChanged: function(){
    return this.get('stepConfigs').someProperty('isPropertiesChanged', true);
  }.property('stepConfigs.@each.isPropertiesChanged'),

  slaveComponentGroups: null,

  /**
   * Filter text will be located here
   * @type {string}
   */
  filter: '',

  /**
   * List of filters for config properties to populate filter combobox
   * @type {{attributeName: string, attributeValue: boolean, caption: string}[]}
   */
  propertyFilters: [
    {
      attributeName: 'isOverridden',
      attributeValue: true,
      caption: 'common.combobox.dropdown.overridden'
    },
    {
      attributeName: 'isFinal',
      attributeValue: true,
      caption: 'common.combobox.dropdown.final'
    },
    {
      attributeName: 'hasCompareDiffs',
      attributeValue: true,
      caption: 'common.combobox.dropdown.changed',
      dependentOn: 'isCompareMode'
    },
    {
      attributeName: 'isValid',
      attributeValue: false,
      caption: 'common.combobox.dropdown.issues'
    },
    {
      attributeName: 'warn',
      attributeValue: true,
      caption: 'common.combobox.dropdown.warnings'
    }
  ],

  /**
   * List of heapsize properties not to be parsed
   * @type {string[]}
   */
  heapsizeException: ['hadoop_heapsize', 'yarn_heapsize', 'nodemanager_heapsize', 'resourcemanager_heapsize', 'apptimelineserver_heapsize', 'jobhistory_heapsize'],

  /**
   * Regular expression for heapsize properties detection
   * @type {regexp}
   */
  heapsizeRegExp: /_heapsize|_newsize|_maxnewsize|_permsize|_maxpermsize$/,

  /**
   * Dropdown menu items in filter combobox
   * @type {{attributeName: string, attributeValue: string, name: string, selected: boolean}[]}
   */
  filterColumns: function () {
    var filterColumns = [];

    this.get('propertyFilters').forEach(function(filter) {
      if (Em.isNone(filter.dependentOn) || this.get(filter.dependentOn)) {
        filterColumns.push(Ember.Object.create({
          attributeName: filter.attributeName,
          attributeValue: filter.attributeValue,
          name: this.t(filter.caption),
          selected: filter.dependentOn ? this.get(filter.dependentOn) : false
        }));
      }
    }, this);
    return filterColumns;
  }.property('propertyFilters', 'isCompareMode'),

  /**
   * indicate whether service config version belongs to default config group
   * @param {object} version
   * @return {Boolean}
   * @private
   * @method isVersionDefault
   */
  isVersionDefault: function(version) {
    return (App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + version).get('groupId') == -1);
  },

  /**
   * register request to view to track his progress
   * @param {$.ajax} request
   * @method trackRequest
   */
  trackRequest: function (request) {
    this.set('requestInProgress', request);
  },

  /**
   * clear and set properties to default value
   * @method clearStep
   */
  clearStep: function () {
    if (this.get('requestInProgress') && this.get('requestInProgress').readyState !== 4) {
      this.get('requestInProgress').abort();
      this.set('requestInProgress', null);
    }
    this.setProperties({
      saveInProgress: false,
      modifiedFileNames: [],
      isInit: true,
      hash: null,
      forceTransition: false,
      dataIsLoaded: false,
      versionLoaded: false,
      filter: '',
      loadedGroupToOverrideSiteToTagMap: {},
      serviceConfigVersionNote: ''
    });
    this.get('filterColumns').setEach('selected', false);
    this.get('stepConfigs').clear();
    this.get('allConfigs').clear();
    this.get('uiConfigs').clear();
    if (this.get('serviceConfigTags')) {
      this.set('serviceConfigTags', null);
    }
  },

  /**
   * @type {object[]}
   */
  serviceConfigProperties: function () {
    return App.db.getServiceConfigProperties();
  }.property('content'),

  /**
   * "Finger-print" of the <code>stepConfigs</code>. Filled after first configGroup selecting
   * Used to determine if some changes were made (when user navigates away from this page)
   * @type {String|null}
   */
  hash: null,

  /**
   * Is this initial config group changing
   * @type {Boolean}
   */
  isInit: true,

  /**
   * On load function
   * @method loadStep
   */
  loadStep: function () {
    console.log("TRACE: Loading configure for service");
    var serviceName = this.get('content.serviceName');
    this.clearStep();
    if (App.get('supports.enhancedConfigs')) {
      this.setDependentServices(serviceName);
      this.loadConfigTheme(serviceName).always(function() {
        App.themesMapper.generateAdvancedTabs([serviceName]);
      });
    }
    this.loadClusterEnvSite();
  },

  /**
   * Generate "finger-print" for current <code>stepConfigs[0]</code>
   * Used to determine, if user has some unsaved changes (comparing with <code>hash</code>)
   * @returns {string|null}
   * @method getHash
   */
  getHash: function () {
    if (!this.get('stepConfigs')[0]) {
      return null;
    }
    var hash = {};
    this.get('stepConfigs')[0].configs.forEach(function (config) {
      hash[config.get('name')] = {value: config.get('value'), overrides: [], isFinal: config.get('isFinal')};
      if (!config.get('overrides')) return;
      if (!config.get('overrides.length')) return;

      config.get('overrides').forEach(function (override) {
        hash[config.get('name')].overrides.push(override.get('value'));
      });
    });
    return JSON.stringify(hash);
  },

  /**
   * Update configs on the page after <code>selectedConfigGroup</code> is changed
   * @method onConfigGroupChange
   */
  onConfigGroupChange: function () {
    var self = this;
    this.get('stepConfigs').clear();
    var selectedConfigGroup = this.get('selectedConfigGroup');
    var serviceName = this.get('content.serviceName');
    //STEP 1: handle tags from JSON data for host overrides
    var configGroupsWithOverrides = selectedConfigGroup.get('isDefault') && !this.get('isHostsConfigsPage') ? this.get('configGroupsToLoad') : [selectedConfigGroup].concat(this.get('dependentConfigGroups'));
    configGroupsWithOverrides.forEach(function (item) {
      var groupName = item.get('name');
      if (Em.isNone(this.loadedGroupToOverrideSiteToTagMap[groupName])) {
        this.loadedGroupToOverrideSiteToTagMap[groupName] = {};
        item.get('configSiteTags').forEach(function (siteTag) {
          var site = siteTag.get('site');
          this.loadedGroupToOverrideSiteToTagMap[groupName][site] = siteTag.get('tag');
        }, this);
      }
    }, this);
    //STEP 2: Create an array of objects defining tag names to be polled and new tag names to be set after submit
    this.setServiceConfigTags(this.loadedClusterSiteToTagMap);
    //STEP 4: Load on-site config by service from server
    App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags')).done(function(configGroups){
      //Merge on-site configs with pre-defined
      var configSet = App.config.mergePreDefinedWithLoaded(configGroups, self.get('advancedConfigs'), self.get('serviceConfigTags'), serviceName);
      configSet = App.config.syncOrderWithPredefined(configSet);

      var configs = configSet.configs;
      //put properties from capacity-scheduler.xml into one config with textarea view
      if (self.get('content.serviceName') === 'YARN') {
        configs = App.config.fileConfigsIntoTextarea(configs, 'capacity-scheduler.xml');
      }
      self.set('allConfigs', configs);
      //add configs as names of host components
      self.addHostNamesToConfig();
      //load configs of version being compared against
      self.loadCompareVersionConfigs(self.get('allConfigs')).done(function (isComparison) {
        //Load and add overriden configs of group
        if (!isComparison && (!self.get('selectedConfigGroup').get('isDefault') || self.get('isCurrentSelected'))) {
          App.config.loadServiceConfigGroupOverrides(self.get('allConfigs'), self.get('loadedGroupToOverrideSiteToTagMap'), self.get('configGroupsToLoad'), self.onLoadOverrides, self);
        } else {
          self.onLoadOverrides(self.get('allConfigs'));
        }
      });
    });
  }.observes('selectedConfigGroup'),

  /**
   * load version configs for comparison
   * @param allConfigs
   * @return {object}
   * @private
   * @method loadCompareVersionConfigs
   */
  loadCompareVersionConfigs: function (allConfigs) {
    var dfd = $.Deferred();
    var self = this;
    var compareServiceVersions = [];

    if (this.get('compareServiceVersion')) {
      if (!this.isVersionDefault(this.get('compareServiceVersion').get('version'))) {
        compareServiceVersions = [this.get('compareServiceVersion').get('version'), this.get('selectedVersion')];
      } else {
        compareServiceVersions = [this.get('compareServiceVersion').get('version')];
      }

      this.getCompareVersionConfigs(compareServiceVersions).done(function (json) {
        self.initCompareConfig(allConfigs, json);
        self.set('compareServiceVersion', null);
        self.set('isCompareMode', true);
        dfd.resolve(true);
      }).fail(function () {
          self.set('compareServiceVersion', null);
          dfd.resolve(true);
        });
    } else {
      self.set('isCompareMode', false);
      allConfigs.setEach('isComparison', false);
      dfd.resolve(false);
    }
    return dfd.promise();
  },

  /**
   * attach analogical config to each property for comparison
   * @param allConfigs
   * @param json
   * @private
   * @method initCompareConfig
   */
  initCompareConfig: function(allConfigs, json) {
    var serviceVersionMap = {};
    var configNamesMap = {};
    var serviceName = this.get('content.serviceName');
    var compareVersionNumber = this.get('compareServiceVersion').get('version');
    //indicate whether compared versions are from non-default group
    var compareNonDefaultVersions = (json.items.length > 1);

    serviceVersionMap[compareVersionNumber] = {};
    if (compareNonDefaultVersions) {
      serviceVersionMap[this.get('selectedVersion')] = {};
    }
    allConfigs.mapProperty('name').forEach(function(name) {
      configNamesMap[name] = true;
    });

    json.items.forEach(function (item) {
      item.configurations.forEach(function (configuration) {
        if (serviceName == 'YARN' && configuration.type == 'capacity-scheduler') {
          // put all properties in a single textarea for capacity-scheduler
          var value = '';
          for (var prop in configuration.properties) {
            value += prop + '=' + configuration.properties[prop] + '\n';
          }
          serviceVersionMap[item.service_config_version][configuration.type + '-' + configuration.type] = {
            name: configuration.type,
            value: value,
            type: configuration.type,
            tag: configuration.tag,
            version: configuration.version,
            service_config_version: item.service_config_version
          };
        } else {
          for (var prop in configuration.properties) {
            serviceVersionMap[item.service_config_version][prop + '-' + configuration.type] = {
              name: prop,
              value: configuration.properties[prop],
              type: configuration.type,
              tag: configuration.tag,
              version: configuration.version,
              service_config_version: item.service_config_version
            };
            if (Em.isNone(configNamesMap[prop])) {
              allConfigs.push(this.getMockConfig(prop, serviceName, App.config.getOriginalFileName(configuration.type)));
            }
          }
        }
        if (configuration.properties_attributes && configuration.properties_attributes.final) {
          for (var final in configuration.properties_attributes.final) {
            serviceVersionMap[item.service_config_version][final + '-' + configuration.type].isFinal = (configuration.properties_attributes.final[final] === 'true');
          }
        }
      }, this);
    }, this);

    if (compareNonDefaultVersions) {
      allConfigs.forEach(function (serviceConfig) {
        this.setCompareConfigs(serviceConfig, serviceVersionMap, compareVersionNumber, this.get('selectedVersion'));
      }, this);
    } else {
      allConfigs.forEach(function (serviceConfig) {
        var serviceCfgVersionMap = serviceVersionMap[this.get('compareServiceVersion').get('version')];
        var compareConfig = serviceCfgVersionMap[serviceConfig.name + '-' + App.config.getConfigTagFromFileName(serviceConfig.filename)]
        this.setCompareDefaultGroupConfig(serviceConfig, compareConfig);
      }, this);
    }
  },

  /**
   * set compare properties to service config of non-default group
   * @param serviceConfig
   * @param serviceVersionMap
   * @param compareVersion
   * @param selectedVersion
   * @private
   * @method setCompareConfigs
   */
  setCompareConfigs: function (serviceConfig, serviceVersionMap, compareVersion, selectedVersion) {
    var compareConfig = serviceVersionMap[compareVersion][serviceConfig.name + '-' + App.config.getConfigTagFromFileName(serviceConfig.filename)];
    var selectedConfig = serviceVersionMap[selectedVersion][serviceConfig.name + '-' + App.config.getConfigTagFromFileName(serviceConfig.filename)];

    serviceConfig.compareConfigs = [];
    serviceConfig.isComparison = true;

    if (compareConfig && selectedConfig) {
      serviceConfig.compareConfigs.push(this.getComparisonConfig(serviceConfig, compareConfig));
      serviceConfig.compareConfigs.push(this.getComparisonConfig(serviceConfig, selectedConfig));
      serviceConfig.hasCompareDiffs = this.hasCompareDiffs(serviceConfig.compareConfigs[0], serviceConfig.compareConfigs[1]);
    } else if (compareConfig && !selectedConfig) {
      serviceConfig.compareConfigs.push(this.getComparisonConfig(serviceConfig, compareConfig));
      serviceConfig.compareConfigs.push(this.getMockComparisonConfig(selectedConfig, selectedVersion));
      serviceConfig.hasCompareDiffs = true;
    } else if (!compareConfig && selectedConfig) {
      serviceConfig.compareConfigs.push(this.getMockComparisonConfig(selectedConfig, compareVersion));
      serviceConfig.compareConfigs.push(this.getComparisonConfig(serviceConfig, selectedConfig));
      serviceConfig.hasCompareDiffs = true;
    }
  },

  /**
   * init attributes and wrap mock compare config into App.ServiceConfigProperty
   * @param serviceConfig
   * @param compareServiceVersion
   * @return {object}
   * @private
   * @method getMockComparisonConfig
   */
  getMockComparisonConfig: function (serviceConfig, compareServiceVersion) {
    var compareObject = $.extend(true, {isComparison: false},  serviceConfig);
    compareObject.isEditable = false;

    compareObject.serviceVersion = App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + compareServiceVersion);
    compareObject.isMock = true;
    compareObject.displayType = 'label';
    compareObject = App.ServiceConfigProperty.create(compareObject);
    compareObject.set('value', Em.I18n.t('common.property.undefined'));
    return compareObject;
  },

  /**
   * init attributes and wrap compare config into App.ServiceConfigProperty
   * @param serviceConfig
   * @param compareConfig
   * @return {object}
   * @private
   * @method getComparisonConfig
   */
  getComparisonConfig: function (serviceConfig, compareConfig) {
    var compareObject = $.extend(true, {isComparison: false, isOriginalSCP: false},  serviceConfig);
    compareObject.isEditable = false;

    if (compareConfig) {
      if (serviceConfig.isMock) {
        compareObject.displayType = 'string';
        compareObject.isMock = false;
      }
      compareObject.serviceVersion = App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + compareConfig.service_config_version);
      compareObject = App.ServiceConfigProperty.create(compareObject);
      compareObject.set('isFinal', compareConfig.isFinal);
      compareObject.set('value', App.config.formatOverrideValue(serviceConfig, compareConfig.value));
      compareObject.set('compareConfigs', null);
    }
    return compareObject;
  },

  /**
   * set compare properties to service config of default group
   * @param serviceConfig
   * @param compareConfig
   * @private
   * @method setCompareDefaultGroupConfig
   */
  setCompareDefaultGroupConfig: function (serviceConfig, compareConfig) {
    var compareObject = {};

    serviceConfig.compareConfigs = [];
    serviceConfig.isComparison = true;

    //if config isn't reconfigurable then it can't have changed value to compare
    if (compareConfig && (serviceConfig.isReconfigurable || serviceConfig.isUserProperty)) {
      compareObject = this.getComparisonConfig(serviceConfig, compareConfig);
      serviceConfig.hasCompareDiffs = serviceConfig.isMock || this.hasCompareDiffs(serviceConfig, compareObject);
      serviceConfig.compareConfigs.push(compareObject);
    } else if (serviceConfig.isUserProperty) {
      serviceConfig.compareConfigs.push(this.getMockComparisonConfig(serviceConfig, this.get('compareServiceVersion.version')));
      serviceConfig.hasCompareDiffs = true;
    }
    return serviceConfig;
  },

  /**
   * check value and final attribute of original and compare config for differencies
   * @param originalConfig
   * @param compareConfig
   * @return {Boolean}
   * @private
   * @method hasCompareDiffs
   */
  hasCompareDiffs: function (originalConfig, compareConfig) {
    return (originalConfig.value !== compareConfig.value) || (!!originalConfig.isFinal !== (compareConfig.isFinal == true));
  },

  /**
   * generate mock config object
   * @param name
   * @param serviceName
   * @param filename
   * @return {Object}
   * @private
   * @method getMockConfig
   */
  getMockConfig: function (name, serviceName, filename) {
    var undefinedConfig = {
      description: name,
      displayName: name,
      id: "site property",
      isOverridable: false,
      isReconfigurable: false,
      isRequired: false,
      isRequiredByAgent: false,
      isSecureConfig: false,
      isUserProperty: true,
      isVisible: true,
      name: name,
      filename: filename,
      serviceName: serviceName,
      value: Em.I18n.t('common.property.undefined'),
      isMock: true,
      displayType: 'label'
    };
    var category = App.config.identifyCategory(undefinedConfig);
    undefinedConfig.category = category && category.name;
    return undefinedConfig;
  },

  /**
   * get configs of chosen version from server to compare
   * @param compareServiceVersions
   * @return {$.ajax}
   * @private
   * @method getCompareVersionConfigs
   */
  getCompareVersionConfigs: function (compareServiceVersions) {
    this.set('versionLoaded', false);

    return App.ajax.send({
      name: 'service.serviceConfigVersions.get.multiple',
      sender: this,
      data: {
        serviceName: this.get('content.serviceName'),
        serviceConfigVersions: compareServiceVersions
      }
    });
  },

  /**
   * @param serviceConfig
   * @private
   * @method checkDatabaseProperties
   */
  checkDatabaseProperties: function (serviceConfig) {
    this.hideHiveDatabaseProperties(serviceConfig.configs);
    this.hideOozieDatabaseProperties(serviceConfig.configs);
  },

  /**
   * @param configs
   * @private
   * @method hideHiveDatabaseProperties
   */
  hideHiveDatabaseProperties: function (configs) {
    if (!['HIVE'].contains(this.get('content.serviceName'))) return;
    var property = configs.findProperty('name', 'hive_hostname');
    if (property) property.set('isVisible', false);

    if (configs.someProperty('name', 'hive_database')) {
      var hiveDb = configs.findProperty('name', 'hive_database');
      if (hiveDb.value === 'Existing MSSQL Server database with integrated authentication') {
        configs.findProperty('name', 'javax.jdo.option.ConnectionUserName').setProperties({
          isVisible: false,
          isRequired: false
        });
        configs.findProperty('name', 'javax.jdo.option.ConnectionPassword').setProperties({
          isVisible: false,
          isRequired: false
        });
      }
    }
  },

  /**
   * @param configs
   * @private
   * @method hideOozieDatabaseProperties
   */
  hideOozieDatabaseProperties: function (configs) {
    if (!['OOZIE'].contains(this.get('content.serviceName'))) return;
    var property = configs.findProperty('name', 'oozie_hostname');
    if (property) property.set('isVisible', false);

    if (configs.someProperty('name', 'oozie_database')) {
      var oozieDb = configs.findProperty('name', 'oozie_database');
      if (oozieDb.value === 'Existing MSSQL Server database with integrated authentication') {
        configs.findProperty('name', 'oozie.service.JPAService.jdbc.username').setProperties({
          isVisible: false,
          isRequired: false
        });
        configs.findProperty('name', 'oozie.service.JPAService.jdbc.password').setProperties({
          isVisible: false,
          isRequired: false
        });
      }
    }
  },

  /**
   * @param allConfigs
   * @private
   * @method onLoadOverrides
   */
  onLoadOverrides: function (allConfigs) {
    var self = this;
    var serviceNames = this.get('servicesToLoad');
    serviceNames.forEach(function(serviceName) {
      var serviceConfig = App.config.createServiceConfig(serviceName);
      //Make SecondaryNameNode invisible on enabling namenode HA
      if (serviceConfig.get('serviceName') === 'HDFS') {
        App.config.OnNnHAHideSnn(serviceConfig);
      }
      var configsByService = this.get('allConfigs').filterProperty('serviceName', serviceName);
      this.loadConfigs(configsByService, serviceConfig);

      this.get('stepConfigs').pushObject(serviceConfig);
    }, this);

    var selectedService = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName'));
    this.set('selectedService', selectedService);
    this.setVisibilityForRangerProperties(selectedService);
    this.checkOverrideProperty(selectedService);
    this.checkDatabaseProperties(selectedService);
    this.checkForSecureConfig(this.get('selectedService'));
    this.getRecommendationsForDependencies(null, true, function() {
      self.setProperties({
        dataIsLoaded: true,
        versionLoaded: true,
        hash: self.getHash(),
        isInit: false
      });
    });
  },

  /**
   * Changes format from Object to Array
   *
   * {
   *  'core-site': 'version1',
   *  'hdfs-site': 'version1',
   *  ...
   * }
   *
   * to
   *
   * [
   *  {
   *    siteName: 'core-site',
   *    tagName: 'version1',
   *    newTageName: null
   *  },
   *  ...
   * ]
   *
   * set tagnames for configuration of the *-site.xml
   * @private
   * @method setServiceConfigTags
   */
  setServiceConfigTags: function (desiredConfigsSiteTags) {
    console.debug("setServiceConfigTags(): Trying to set ", desiredConfigsSiteTags);
    var newServiceConfigTags = [];
    for (var index in desiredConfigsSiteTags) {
      newServiceConfigTags.pushObject({
        siteName: index,
        tagName: desiredConfigsSiteTags[index],
        newTagName: null
      }, this);
    }
    console.debug("setServiceConfigTags(): Setting 'serviceConfigTags' to ", newServiceConfigTags);
    this.set('serviceConfigTags', newServiceConfigTags);
  },

  /**
   * check whether the config property is a security related knob
   * @param serviceConfig
   * @private
   * @method checkForSecureConfig
   */
  checkForSecureConfig: function (serviceConfig) {
    serviceConfig.get('configs').forEach(function (_config) {
      this.get('secureConfigs').forEach(function (_secureConfig) {
        if (_config.get('name') === _secureConfig.name) {
          _config.set('isSecureConfig', true);
        }
      }, this)
    }, this)
  },

  /**
   * Load child components to service config object
   * @param {Array} configs - array of configs
   * @param {Object} componentConfig - component config object
   * @method loadConfigs
   */
  loadConfigs: function (configs, componentConfig) {
    var defaultGroupSelected = this.get('selectedConfigGroup.isDefault');
    configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = this.createConfigProperty(_serviceConfigProperty, defaultGroupSelected);
      componentConfig.get('configs').pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
    componentConfig.set('initConfigsLength', componentConfig.get('configs.length'));
  },

  /**
   * create {Em.Object} service_cfg_property based on {Object}_serviceConfigProperty and additional info
   * @param {Object} _serviceConfigProperty - config object
   * @param {Boolean} defaultGroupSelected - true if selected cfg group is default
   * @returns {Ember.Object|null}
   * @private
   * @method createConfigProperty
   */
  createConfigProperty: function (_serviceConfigProperty, defaultGroupSelected) {
    if (!_serviceConfigProperty) return null;

    var overrides = Em.get(_serviceConfigProperty, 'overrides');
    // we will populate the override properties below
    Em.set(_serviceConfigProperty, 'overrides', null);
    Em.set(_serviceConfigProperty, 'isOverridable', Em.isNone(Em.get(_serviceConfigProperty, 'isOverridable')) ? true : Em.get(_serviceConfigProperty, 'isOverridable'));

    var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);

    this.setValuesForOverrides(overrides, _serviceConfigProperty, serviceConfigProperty, defaultGroupSelected);
    this.setEditability(serviceConfigProperty, defaultGroupSelected);

    return serviceConfigProperty;
  },

  /**
   * hide properties from Advanced ranger category that match pattern
   * if property with dependentConfigPattern is false otherwise don't hide
   * @param serviceConfig
   * @private
   * @method setVisibilityForRangerProperties
   */
  setVisibilityForRangerProperties: function(serviceConfig) {
    var category = "Advanced ranger-{0}-plugin-properties".format(this.get('content.serviceName').toLowerCase());
    if (serviceConfig.configCategories.findProperty('name', category)) {
      var patternConfig = serviceConfig.configs.findProperty('dependentConfigPattern');
      if (patternConfig) {
        var value = patternConfig.get('value') === true || ["yes", "true"].contains(patternConfig.get('value').toLowerCase());

        serviceConfig.configs.filter(function(c) {
          if (c.get('category') === category && c.get('name').match(patternConfig.get('dependentConfigPattern')) && c.get('name') != patternConfig.get('name'))
            c.set('isVisible', value);
        });
      }
    }
  },

  /**
   * trigger addOverrideProperty
   * @param {Object} componentConfig
   * @private
   * @method checkOverrideProperty
   */
  checkOverrideProperty: function (componentConfig) {
    var overrideToAdd = this.get('overrideToAdd');
    if (overrideToAdd) {
      overrideToAdd = componentConfig.configs.filter(function(c){
        return c.name == overrideToAdd.name && c.filename == overrideToAdd.filename;
      });
      if (overrideToAdd[0]) {
        this.addOverrideProperty(overrideToAdd[0], this.get('selectedConfigGroup'));
        this.set('overrideToAdd', null);
      }
    }
  },

  /**
   * set isEditable property of config for admin
   * if default cfg group and not on the host config page
   * @param {Ember.Object} serviceConfigProperty
   * @param {Boolean} defaultGroupSelected
   * @private
   * @method setEditability
   */
  setEditability: function (serviceConfigProperty, defaultGroupSelected) {
    serviceConfigProperty.set('isEditable', false);
    if (serviceConfigProperty.get('isComparison')) return;
    if (App.isAccessible('ADMIN') && defaultGroupSelected && !this.get('isHostsConfigsPage') && !serviceConfigProperty.get('group')) {
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
    } else if (serviceConfigProperty.get('group') && this.get('selectedConfigGroup.name') === serviceConfigProperty.get('group.name')) {
      serviceConfigProperty.set('isEditable', true);
    }
  },

  /**
   * set override values
   * @param overrides
   * @param _serviceConfigProperty
   * @param serviceConfigProperty
   * @param defaultGroupSelected
   * @private
   * @method setValuesForOverrides
   */
  setValuesForOverrides: function (overrides, _serviceConfigProperty, serviceConfigProperty, defaultGroupSelected) {
    if (Em.isNone(overrides)) return;
    overrides.forEach(function (override) {
      if (defaultGroupSelected || (Em.get(override, 'group') && this.get('selectedConfigGroup.name') === Em.get(override, 'group.name'))
        || serviceConfigProperty.get('serviceName') !== this.get('content.serviceName')) {
        var newSCP = this.createNewSCP(override, _serviceConfigProperty, serviceConfigProperty, defaultGroupSelected);
        var parentOverridesArray = serviceConfigProperty.get('overrides');
        if (parentOverridesArray == null) {
          parentOverridesArray = Em.A([]);
          serviceConfigProperty.set('overrides', parentOverridesArray);
        }
        parentOverridesArray.pushObject(newSCP);
        serviceConfigProperty.set('overrideValues', parentOverridesArray.mapProperty('value'));
        serviceConfigProperty.set('overrideIsFinalValues', parentOverridesArray.mapProperty('isFinal'));
        console.debug("createOverrideProperty(): Added override to main-property:", serviceConfigProperty.get('name'));
      }
    }, this);
  },

  /**
   * create new overridden property and set appropriate fields
   * @param override
   * @param _serviceConfigProperty
   * @param serviceConfigProperty
   * @param defaultGroupSelected
   * @returns {*}
   * @private
   * @method createNewSCP
   */
  createNewSCP: function (override, _serviceConfigProperty, serviceConfigProperty, defaultGroupSelected) {
    var newSCP = App.ServiceConfigProperty.create(_serviceConfigProperty, {
      value: Em.get(override, 'value'),
      isFinal: Em.get(override, 'isFinal'),
      group: Em.get(override, 'group'),
      supportsFinal: serviceConfigProperty.get('supportsFinal'),
      isOriginalSCP: false,
      parentSCP: serviceConfigProperty,
      overrides: null
    });
    if (defaultGroupSelected) {
      newSCP.set('isEditable', false);
    }
    return newSCP;
  },

  /**
   * method to run saving configs
   * @method saveStepConfigs
   */
  saveStepConfigs: function() {
    if (!this.get("isSubmitDisabled")) {
      this.startSave();
      this.showWarningPopupsBeforeSave();
    }
  },
  /**
   * Array of Objects
   * {
   *  hostProperty - hostName property name for current component
   *  componentName - master componentName
   *  serviceName - serviceName of component
   *  serviceUseThis - services that use hostname property of component(componentName)
   *  m(multiple) - true if can be more than one components installed on cluster
   * }
   */

  hostComponentsmapping: [
    {
      hostProperty: 'snamenode_host',
      componentName: 'SECONDARY_NAMENODE',
      serviceName: 'HDFS',
      serviceUseThis: []
    },
    {
      hostProperty: 'jobtracker_host',
      componentName: 'JOBTRACKER',
      serviceName: 'MAPREDUCE2',
      serviceUseThis: []
    },
    {
      hostProperty: 'hs_host',
      componentName: 'HISTORYSERVER',
      serviceName: 'MAPREDUCE2',
      serviceUseThis: ['YARN']
    },
    {
      hostProperty: 'ats_host',
      componentName: 'APP_TIMELINE_SERVER',
      serviceName: 'YARN',
      serviceUseThis: []
    },
    {
      hostProperty: 'rm_host',
      componentName: 'RESOURCEMANAGER',
      serviceName: 'YARN',
      serviceUseThis: []
    },
    {
      hostProperty: 'hivemetastore_host',
      componentName: 'HIVE_METASTORE',
      serviceName: 'HIVE',
      serviceUseThis: ['HIVE'],
      m: true
    },
    {
      hostProperty: 'hive_ambari_host',
      componentName: 'HIVE_SERVER',
      serviceName: 'HIVE',
      serviceUseThis: []
    },
    {
      hostProperty: 'oozieserver_host',
      componentName: 'OOZIE_SERVER',
      serviceName: 'OOZIE',
      serviceUseThis: [],
      m: true
    },
    {
      hostProperty: 'oozie_ambari_host',
      componentName: 'OOZIE_SERVER',
      serviceName: 'OOZIE',
      serviceUseThis: []
    },
    {
      hostProperty: 'hbasemaster_host',
      componentName: 'HBASE_MASTER',
      serviceName: 'HBASE',
      serviceUseThis: [],
      m: true
    },
    {
      hostProperty: 'webhcatserver_host',
      componentName: 'WEBHCAT_SERVER',
      serviceName: 'HIVE',
      serviceUseThis: [],
      m: true
    },
    {
      hostProperty: 'zookeeperserver_hosts',
      componentName: 'ZOOKEEPER_SERVER',
      serviceName: 'ZOOKEEPER',
      serviceUseThis: ['HBASE', 'HIVE'],
      m: true
    },
    {
      hostProperty: 'stormuiserver_host',
      componentName: 'STORM_UI_SERVER',
      serviceName: 'STORM',
      serviceUseThis: []
    },
    {
      hostProperty: 'drpcserver_host',
      componentName: 'DRPC_SERVER',
      serviceName: 'STORM',
      serviceUseThis: []
    },
    {
      hostProperty: 'storm_rest_api_host',
      componentName: 'STORM_REST_API',
      serviceName: 'STORM',
      serviceUseThis: []
    },
    {
      hostProperty: 'supervisor_hosts',
      componentName: 'SUPERVISOR',
      serviceName: 'STORM',
      serviceUseThis: [],
      m: true
    }
  ],

  /**
   * Adds host name of master component to config
   * @private
   * @method addHostNamesToGlobalConfig
   */
  addHostNamesToConfig: function () {
    var serviceName = this.get('content.serviceName');
    var configs = this.get('allConfigs');
    //namenode_host is required to derive "fs.default.name" a property of core-site
    try {
      this.setHostForService('HDFS', 'NAMENODE', 'namenode_host', true);
    } catch (err) {
      console.log("No NameNode Host available.  This is expected if you're using GLUSTERFS rather than HDFS.");
    }

    var hostProperties = this.get('hostComponentsmapping').filter(function (h) {
      return h.serviceUseThis.contains(serviceName) || h.serviceName == serviceName;
    });
    hostProperties.forEach(function (h) {
      this.setHostForService(h.serviceName, h.componentName, h.hostProperty, h.m);
    }, this);

    if (serviceName === 'HIVE') {
      var hiveDb = configs.findProperty('name', 'hive_database').value;
      if (['Existing MySQL Database', 'Existing Oracle Database', 'Existing PostgreSQL Database', 'Existing MSSQL Server database with SQL authentication', 'Existing MSSQL Server database with integrated authentication'].contains(hiveDb)) {
        configs.findProperty('name', 'hive_hostname').isVisible = true;
      }
    }
    if (serviceName === 'OOZIE') {
      var oozieDb = configs.findProperty('name', 'oozie_database').value;
      if (['Existing MySQL Database', 'Existing Oracle Database', 'Existing PostgreSQL Database', 'Existing MSSQL Server database with SQL authentication', 'Existing MSSQL Server database with integrated authentication'].contains(oozieDb)) {
        configs.findProperty('name', 'oozie_hostname').isVisible = true;
      }
    }
  },

  /**
   * set host name(s) property for component
   * @param {String} serviceName - service name of component
   * @param {String} componentName - component name which host we want to know
   * @param {String} hostProperty - name of host property for current component
   * @param {Boolean} multiple - true if can be more than one component
   * @private
   * @method setHostForService
   */
  setHostForService: function (serviceName, componentName, hostProperty, multiple) {
    var configs = this.get('allConfigs');
    var serviceConfigs = this.get('serviceConfigs').findProperty('serviceName', serviceName).get('configs');
    var hostConfig = serviceConfigs.findProperty('name', hostProperty);
    if (hostConfig) {
      hostConfig.defaultValue = this.getMasterComponentHostValue(componentName, multiple);
      configs.push(hostConfig);
    }
  },

  /**
   * get hostName of component
   * @param {String} componentName
   * @param {Boolean} multiple - true if can be more than one component installed on cluster
   * @return {String|Array|Boolean} hostName|hostNames|false if missing component
   * @private
   * @method getMasterComponentHostValue
   */
  getMasterComponentHostValue: function (componentName, multiple) {
    var components = this.get('content.hostComponents').filterProperty('componentName', componentName);
    if (components.length > 0) {
      return multiple ? components.mapProperty('hostName') : components[0].get('hostName');
    }
    return false;
  },

  /**
   * Trigger loadStep
   * @method loadStep
   */
  doCancel: function () {
    this.set('preSelectedConfigVersion', null);
    Em.run.once(this, 'onConfigGroupChange');
  },

  /**
   * trigger restartAllServiceHostComponents(batchUtils) if confirmed in popup
   * @method restartAllStaleConfigComponents
   * @return App.showConfirmationFeedBackPopup
   */
  restartAllStaleConfigComponents: function () {
    var self = this;
    var serviceDisplayName = this.get('content.displayName');
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('services.service.restartAll.confirmMsg').format(serviceDisplayName),
      confirmButton: Em.I18n.t('services.service.restartAll.confirmButton'),
      additionalWarningMsg: this.get('content.passiveState') === 'OFF' ? Em.I18n.t('services.service.restartAll.warningMsg.turnOnMM').format(serviceDisplayName) : null
    });
    return App.showConfirmationFeedBackPopup(function (query) {
      var selectedService = self.get('content.id');
      batchUtils.restartAllServiceHostComponents(selectedService, true, query);
    }, bodyMessage);
  },

  /**
   * trigger launchHostComponentRollingRestart(batchUtils)
   * @method rollingRestartStaleConfigSlaveComponents
   */
  rollingRestartStaleConfigSlaveComponents: function (componentName) {
    batchUtils.launchHostComponentRollingRestart(componentName.context, this.get('content.displayName'), this.get('content.passiveState') === "ON", true);
  },

  /**
   * trigger showItemsShouldBeRestarted popup with hosts that requires restart
   * @param {{context: object}} event
   * @method showHostsShouldBeRestarted
   */
  showHostsShouldBeRestarted: function (event) {
    var restartRequiredHostsAndComponents = event.context;
    var hosts = [];
    for (var hostName in restartRequiredHostsAndComponents) {
      hosts.push(hostName);
    }
    var hostsText = hosts.length == 1 ? Em.I18n.t('common.host') : Em.I18n.t('common.hosts');
    hosts = hosts.join(', ');
    this.showItemsShouldBeRestarted(hosts, Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(hostsText));
  },

  /**
   * trigger showItemsShouldBeRestarted popup with components that requires restart
   * @param {{context: object}} event
   * @method showComponentsShouldBeRestarted
   */
  showComponentsShouldBeRestarted: function (event) {
    var restartRequiredHostsAndComponents = event.context;
    var hostsComponets = [];
    var componentsObject = {};
    for (var hostName in restartRequiredHostsAndComponents) {
      restartRequiredHostsAndComponents[hostName].forEach(function (hostComponent) {
        hostsComponets.push(hostComponent);
        if (componentsObject[hostComponent] != undefined) {
          componentsObject[hostComponent]++;
        } else {
          componentsObject[hostComponent] = 1;
        }
      })
    }
    var componentsList = [];
    for (var obj in componentsObject) {
      var componentDisplayName = (componentsObject[obj] > 1) ? obj + 's' : obj;
      componentsList.push(componentsObject[obj] + ' ' + componentDisplayName);
    }
    var componentsText = componentsList.length == 1 ? Em.I18n.t('common.component') : Em.I18n.t('common.components');
    hostsComponets = componentsList.join(', ');
    this.showItemsShouldBeRestarted(hostsComponets, Em.I18n.t('service.service.config.restartService.shouldBeRestarted').format(componentsText));
  },

  /**
   * Show popup with selectable (@see App.SelectablePopupBodyView) list of items
   * @param {string} content string with comma-separated list of hostNames or componentNames
   * @param {string} header popup header
   * @returns {App.ModalPopup}
   * @method showItemsShouldBeRestarted
   */
  showItemsShouldBeRestarted: function (content, header) {
    return App.ModalPopup.show({
      content: content,
      header: header,
      bodyClass: App.SelectablePopupBodyView,
      secondary: null
    });
  },

  /**
   * add new overridden property to config property object
   * @param {object} serviceConfigProperty - config property object
   * @param {App.ConfigGroup} group - config group for new property
   * @param {String} value
   * @param {boolean} isNotSaved TODO
   * @method addOverrideProperty
   */
  addOverrideProperty: function (serviceConfigProperty, group, value, isNotSaved) {
    if (serviceConfigProperty.get('isOriginalSCP')) {
      var overrides = serviceConfigProperty.get('overrides');
      if (!overrides) {
        overrides = [];
        serviceConfigProperty.set('overrides', overrides);
      }
      // create new override with new value
      var newSCP = App.ServiceConfigProperty.create(serviceConfigProperty, {
        value: value || '',
        isOriginalSCP: false,
        parentSCP: serviceConfigProperty,
        isEditable: true,
        group: group,
        overrides: null,
        isNotSaved: isNotSaved
      });
      console.debug("createOverrideProperty(): Added:", newSCP, " to main-property:", serviceConfigProperty);
      overrides.pushObject(newSCP);
    }
  },

  /**
   * trigger manageConfigurationGroups
   * @method manageConfigurationGroup
   */
  manageConfigurationGroup: function () {
    App.router.get('manageConfigGroupsController').manageConfigurationGroups(null, this.get('content'));
  },

  /**
   * If user changes cfg group if some configs was changed popup with propose to save changes must be shown
   * @param {object} event - triggered event for selecting another config-group
   * @method selectConfigGroup
   */
  selectConfigGroup: function (event) {
    var self = this;

    function callback() {
      self.doSelectConfigGroup(event);
    }

    if (!this.get('isInit')) {
      if (this.hasUnsavedChanges()) {
        this.showSavePopup(null, callback);
        return;
      }
    }
    callback();
  },

  /**
   * switch view to selected group
   * @param event
   * @method selectConfigGroup
   */
  doSelectConfigGroup: function (event) {
    //clean when switch config group
    this.loadedGroupToOverrideSiteToTagMap = {};
    var configGroupVersions = App.ServiceConfigVersion.find().filterProperty('groupId', event.context.get('id'));
    //check whether config group has config versions
    if (configGroupVersions.length > 0) {
      this.loadSelectedVersion(configGroupVersions.findProperty('isCurrent').get('version'), event.context);
    } else {
      this.loadSelectedVersion(null, event.context);
    }
  }
});
