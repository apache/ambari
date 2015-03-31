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
var dataManipulationUtils = require('utils/data_manipulation');
var lazyLoading = require('utils/lazy_loading');

App.MainServiceInfoConfigsController = Em.Controller.extend(App.ServerValidatorMixin, App.EnhancedConfigsMixin, App.ConfigOverridable, App.PreloadRequestsChainMixin, {

  name: 'mainServiceInfoConfigsController',

  isHostsConfigsPage: false,

  forceTransition: false,

  isRecommendedLoaded: true,

  dataIsLoaded: false,

  stepConfigs: [], //contains all field properties that are viewed in this service

  selectedService: null,

  serviceConfigTags: null,

  selectedConfigGroup: null,

  configTypesInfo: {
    items: [],
    supportsFinal: []
  },

  requestInProgress: null,

  selectedServiceConfigTypes: [],

  selectedServiceSupportsFinal: [],

  /**
   * config groups for current service
   */
  configGroups: [],

  allConfigs: [],

  uiConfigs: [],

  saveInProgress: false,

  saveConfigsFlag: true,

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
    var self = this;
    this.clearStep();
    if (App.get('supports.enhancedConfigs')) {
      App.config.loadConfigTheme(this.get('content.serviceName')).always(function() {
        self.setDependentServices(self.get('content.serviceName'));
        App.themesMapper.generateAdvancedTabs([self.get('content.serviceName')]);
        if (self.get('dependentServiceNames.length') > 0) {
          App.config.loadConfigCurrentVersions(self.get('dependentServiceNames'));
        }
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
    var configGroupsWithOverrides = selectedConfigGroup.get('isDefault') && !this.get('isHostsConfigsPage') ? this.get('configGroups') : [selectedConfigGroup];
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
    //STEP 3: Load advanced configs
    var advancedConfigs = this.get('advancedConfigs');
    //STEP 4: Load on-site config by service from server
    App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags')).done(function(configGroups){
      //Merge on-site configs with pre-defined
      var configSet = App.config.mergePreDefinedWithLoaded(configGroups, advancedConfigs, self.get('serviceConfigTags'), serviceName);
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
          App.config.loadServiceConfigGroupOverrides(self.get('allConfigs'), self.get('loadedGroupToOverrideSiteToTagMap'), self.get('configGroups'), self.onLoadOverrides, self);
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
      this.setSupportsFinal(compareObject);
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
    var serviceName = this.get('content.serviceName');
    var advancedConfigs = this.get('advancedConfigs');
    //STEP 10: creation of serviceConfig object which contains configs for current service
    var serviceConfig = App.config.createServiceConfig(serviceName);
    //STEP11: Make SecondaryNameNode invisible on enabling namenode HA
    if (serviceConfig.get('serviceName') === 'HDFS') {
      App.config.OnNnHAHideSnn(serviceConfig);
    }

    serviceConfig = App.config.createServiceConfig(this.get('content.serviceName'));
    this.loadConfigs(this.get('allConfigs'), serviceConfig);
    this.setVisibilityForRangerProperties(serviceConfig);
    this.checkOverrideProperty(serviceConfig);
    this.checkDatabaseProperties(serviceConfig);
    this.get('stepConfigs').pushObject(serviceConfig);
    this.set('selectedService', this.get('stepConfigs').objectAt(0));
    this.checkForSecureConfig(this.get('selectedService'));
    this.setProperties({
      dataIsLoaded: true,
      versionLoaded: true,
      hash: this.getHash(),
      isInit: false
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

    this.setSupportsFinal(serviceConfigProperty);
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
   * set supportsFinal property of config for admin
   * @param {Ember.Object} serviceConfigProperty
   * @private
   * @method setSupportsFinal
   */
  setSupportsFinal: function (serviceConfigProperty) {
    if (serviceConfigProperty.get('isMock')) return;
    var fileName = serviceConfigProperty.get('filename');
    var matchingConfigType = this.get('configTypesInfo').supportsFinal.find(function(configType) {
      return fileName.startsWith(configType);
    });
    serviceConfigProperty.set('supportsFinal', !!matchingConfigType);
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
      if (defaultGroupSelected || (Em.get(override, 'group') && this.get('selectedConfigGroup.name') === Em.get(override, 'group.name'))) {
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
   * tells controller in saving configs was started
   * for now just changes flag <code>saveInProgress<code> to true
   * @private
   * @method startSave
   */
  startSave: function() {
    this.set("saveInProgress", true);
  },

  /**
   * tells controller that save has been finished
   * for now just changes flag <code>saveInProgress<code> to true
   * @private
   * @method completeSave
   */
  completeSave: function() {
    this.set("saveInProgress", false);
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
   * show some warning popups before user save configs
   * @method showWarningPopupsBeforeSave
   * @private
   * @method showWarningPopupsBeforeSave
   */
  showWarningPopupsBeforeSave: function() {
    var self = this;
    if (this.isDirChanged()) {
      App.showConfirmationPopup(function() {
          self.showChangedDependentConfigs(null, function() {
            self.restartServicePopup();
          });
        },
        Em.I18n.t('services.service.config.confirmDirectoryChange').format(self.get('content.displayName')),
        this.completeSave.bind(this)
      );
    } else {
      self.showChangedDependentConfigs(null, function() {
        self.restartServicePopup();
      });
    }
  },

  /**
   * Runs config validation before save
   * @private
   * @method restartServicePopup
   */
  restartServicePopup: function () {
    this.serverSideValidation()
      .done(this.saveConfigs.bind(this))
      .fail(this.completeSave.bind(this));
  },

  /**
   * Define if user has changed some dir properties
   * @return {Boolean}
   * @private
   * @method isDirChanged
   */
  isDirChanged: function () {
    var dirChanged = false;
    var serviceName = this.get('content.serviceName');

    if (serviceName === 'HDFS') {
      var hdfsConfigs = this.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs');
      if ((hdfsConfigs.findProperty('name', 'dfs.namenode.name.dir') && hdfsConfigs.findProperty('name', 'dfs.namenode.name.dir').get('isNotDefaultValue')) ||
        (hdfsConfigs.findProperty('name', 'dfs.namenode.checkpoint.dir') && hdfsConfigs.findProperty('name', 'dfs.namenode.checkpoint.dir').get('isNotDefaultValue')) ||
        (hdfsConfigs.findProperty('name', 'dfs.datanode.data.dir') && hdfsConfigs.findProperty('name', 'dfs.datanode.data.dir').get('isNotDefaultValue'))) {
        dirChanged = true;
      }
    }
    return dirChanged;
  },

  /**
   * Save changed configs and config groups
   * @method saveConfigs
   */
  saveConfigs: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    var configs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    var self = this;

    if (selectedConfigGroup.get('isDefault')) {
      if (this.get('content.serviceName') === 'YARN') {
        configs = App.config.textareaIntoFileConfigs(configs, 'capacity-scheduler.xml');
      }

      /**
       * generates list of properties that was changed
       * @type {Array}
       */
      var modifiedConfigs = configs
        // get only modified and created configs
        .filter(function (config) {
          return config.get('isNotDefaultValue') || config.get('isNotSaved');
        })
        // get file names and add file names that was modified, for example after property removing
        .mapProperty('filename').concat(this.get('modifiedFileNames')).uniq()
        // get configs by filename
        .map(function (fileName) {
          return configs.filterProperty('filename', fileName);
        });

      if (!!modifiedConfigs.length) {
        // concatenate results
        modifiedConfigs = modifiedConfigs.reduce(function (current, prev) {
          return current.concat(prev);
        });
      }
      // save modified original configs that have no group
      this.saveSiteConfigs(modifiedConfigs.filter(function (config) {
        return !config.get('group');
      }));

      /**
       * First we put cluster configurations, which automatically creates /configurations
       * resources. Next we update host level overrides.
       */
      this.doPUTClusterConfigurations();

    } else {
      var overridenConfigs = [];
      var groupHosts = [];
      configs.filterProperty('isOverridden', true).forEach(function (config) {
        overridenConfigs = overridenConfigs.concat(config.get('overrides'));
      });
      // find custom original properties that assigned to selected config group
      overridenConfigs = overridenConfigs.concat(configs.filterProperty('group')
        .filter(function (config) {
          return config.get('group.name') == self.get('selectedConfigGroup.name');
        }));

      this.formatConfigValues(overridenConfigs);
      selectedConfigGroup.get('hosts').forEach(function (hostName) {
        groupHosts.push({"host_name": hostName});
      });

      /**
       * if there are some changes in dependent configs
       * need to save these config to in separate request
       */
      this.saveDependentGroups();

      this.putConfigGroupChanges({
        ConfigGroup: {
          "id": selectedConfigGroup.get('id'),
          "cluster_name": App.get('clusterName'),
          "group_name": selectedConfigGroup.get('name'),
          "tag": selectedConfigGroup.get('service.id'),
          "description": selectedConfigGroup.get('description'),
          "hosts": groupHosts,
          "service_config_version_note": this.get('serviceConfigVersionNote'),
          "desired_configs": this.buildGroupDesiredConfigs(overridenConfigs)
        }
      }, true);
    }
  },

  /**
   * On save configs handler. Open save configs popup with appropriate message
   * @private
   * @method onDoPUTClusterConfigurations
   */
  onDoPUTClusterConfigurations: function () {
    var header, message, messageClass, value, status = 'unknown', urlParams = '',
      result = {
        flag: this.get('saveConfigsFlag'),
        message: null,
        value: null
      },
      extendedModel = App.Service.extendedModel[this.get('content.serviceName')],
      currentService = extendedModel ? App[extendedModel].find(this.get('content.serviceName')) : App.Service.find(this.get('content.serviceName'));

    if (!result.flag) {
      result.message = Em.I18n.t('services.service.config.failSaveConfig');
    }

    App.router.get('clusterController').updateClusterData();
    App.router.get('updateController').updateComponentConfig(function () {
    });
    var flag = result.flag;
    if (result.flag === true) {
      header = Em.I18n.t('services.service.config.saved');
      message = Em.I18n.t('services.service.config.saved.message');
      messageClass = 'alert alert-success';
      // warn the user if any of the components are in UNKNOWN state
      urlParams += ',ServiceComponentInfo/installed_count,ServiceComponentInfo/total_count';
      if (this.get('content.serviceName') === 'HDFS') {
        urlParams += '&ServiceComponentInfo/service_name.in(HDFS)'
      }
    } else {
      header = Em.I18n.t('common.failure');
      message = result.message;
      messageClass = 'alert alert-error';
      value = result.value;
    }
    if(currentService){
      App.QuickViewLinks.proto().set('content', currentService);
      App.QuickViewLinks.proto().loadTags();
    }
    this.showSaveConfigsPopup(header, flag, message, messageClass, value, status, urlParams);
  },

  /**
   * Show save configs popup
   * @return {App.ModalPopup}
   * @private
   * @method showSaveConfigsPopup
   */
  showSaveConfigsPopup: function (header, flag, message, messageClass, value, status, urlParams) {
    var self = this;
    if (flag) {
      this.set('forceTransition', flag);
      self.loadStep();
    }
    return App.ModalPopup.show({
      header: header,
      primary: Em.I18n.t('ok'),
      secondary: null,
      onPrimary: function () {
        this.hide();
        if (!flag) {
          self.completeSave();
        }
      },
      onClose: function () {
        this.hide();
        self.completeSave();
      },
      disablePrimary: true,
      bodyClass: Ember.View.extend({
        flag: flag,
        message: function () {
          return this.get('isLoaded') ? message : Em.I18n.t('services.service.config.saving.message');
        }.property('isLoaded'),
        messageClass: function () {
          return this.get('isLoaded') ? messageClass : 'alert alert-info';
        }.property('isLoaded'),
        setDisablePrimary: function () {
          this.get('parentView').set('disablePrimary', !this.get('isLoaded'));
        }.observes('isLoaded'),
        runningHosts: [],
        runningComponentCount: 0,
        unknownHosts: [],
        unknownComponentCount: 0,
        siteProperties: value,
        isLoaded: false,
        componentsFilterSuccessCallback: function (response) {
          var count = 0,
            view = this,
            lazyLoadHosts = function (dest) {
              lazyLoading.run({
                initSize: 20,
                chunkSize: 50,
                delay: 50,
                destination: dest,
                source: hosts,
                context: view
              });
            },
            /**
             * Map components for their hosts
             * Return format:
             * <code>
             *   {
             *    host1: [component1, component2, ...],
             *    host2: [component3, component4, ...]
             *   }
             * </code>
             * @return {object}
             */
              setComponents = function (item, components) {
              item.host_components.forEach(function (c) {
                var name = c.HostRoles.host_name;
                if (!components[name]) {
                  components[name] = [];
                }
                components[name].push(App.format.role(item.ServiceComponentInfo.component_name));
              });
              return components;
            },
            /**
             * Map result of <code>setComponents</code> to array
             * @return {{name: string, components: string}[]}
             */
              setHosts = function (components) {
              var hosts = [];
              Em.keys(components).forEach(function (key) {
                hosts.push({
                  name: key,
                  components: components[key].join(', ')
                });
              });
              return hosts;
            },
            components = {},
            hosts = [];
          switch (status) {
            case 'unknown':
              response.items.filter(function (item) {
                return (item.ServiceComponentInfo.total_count > item.ServiceComponentInfo.started_count + item.ServiceComponentInfo.installed_count);
              }).forEach(function (item) {
                  var total = item.ServiceComponentInfo.total_count,
                    started = item.ServiceComponentInfo.started_count,
                    installed = item.ServiceComponentInfo.installed_count,
                    unknown = total - started + installed;
                  components = setComponents(item, components);
                  count += unknown;
                });
              hosts = setHosts(components);
              this.set('unknownComponentCount', count);
              lazyLoadHosts(this.get('unknownHosts'));
              break;
            case 'started':
              response.items.filterProperty('ServiceComponentInfo.started_count').forEach(function (item) {
                var started = item.ServiceComponentInfo.started_count;
                components = setComponents(item, components);
                count += started;
                hosts = setHosts(components);
              });
              this.set('runningComponentCount', count);
              lazyLoadHosts(this.get('runningHosts'));
              break;
          }
        },
        componentsFilterErrorCallback: function () {
          this.set('isLoaded', true);
        },
        didInsertElement: function () {
          return App.ajax.send({
            name: 'components.filter_by_status',
            sender: this,
            data: {
              clusterName: App.get('clusterName'),
              urlParams: urlParams
            },
            success: 'componentsFilterSuccessCallback',
            error: 'componentsFilterErrorCallback'
          });
        },
        getDisplayMessage: function () {
          var displayMsg = [];
          var siteProperties = this.get('siteProperties');
          if (siteProperties) {
            siteProperties.forEach(function (_siteProperty) {
              var displayProperty = _siteProperty.siteProperty;
              var displayNames = _siteProperty.displayNames;

              if (displayNames && displayNames.length) {
                if (displayNames.length === 1) {
                  displayMsg.push(displayProperty + Em.I18n.t('as') + displayNames[0]);
                } else {
                  var name;
                  displayNames.forEach(function (_name, index) {
                    if (index === 0) {
                      name = _name;
                    } else if (index === siteProperties.length - 1) {
                      name = name + Em.I18n.t('and') + _name;
                    } else {
                      name = name + ', ' + _name;
                    }
                  }, this);
                  displayMsg.push(displayProperty + Em.I18n.t('as') + name);

                }
              } else {
                displayMsg.push(displayProperty);
              }
            }, this);
          }
          return displayMsg;

        }.property('siteProperties'),

        runningHostsMessage: function () {
          return Em.I18n.t('services.service.config.stopService.runningHostComponents').format(this.get('runningComponentCount'), this.get('runningHosts.length'));
        }.property('runningComponentCount', 'runningHosts.length'),

        unknownHostsMessage: function () {
          return Em.I18n.t('services.service.config.stopService.unknownHostComponents').format(this.get('unknownComponentCount'), this.get('unknownHosts.length'));
        }.property('unknownComponentCount', 'unknownHosts.length'),

        templateName: require('templates/main/service/info/configs_save_popup')
      })
    })
  },

  /**
   * construct desired_configs for config groups from overriden properties
   * @param configs
   * @param timeTag
   * @return {Array}
   * @private
   * @method buildGroupDesiredConfigs
   */
  buildGroupDesiredConfigs: function (configs, timeTag) {
    var sites = [];
    var time = timeTag || (new Date).getTime();
    var siteFileNames = configs.mapProperty('filename').uniq();
    sites = siteFileNames.map(function (filename) {
      return {
        type: filename.replace('.xml', ''),
        tag: 'version' + time,
        properties: []
      };
    });

    configs.forEach(function (config) {
      var type = config.get('filename').replace('.xml', '');
      var site = sites.findProperty('type', type);
      site.properties.push(config);
    });

    return sites.map(function (site) {
      return this.createSiteObj(site.type, site.tag, site.properties);
    }, this);
  },
  /**
   * persist properties of config groups to server
   * show result popup if <code>showPopup</code> is true
   * @param data {Object}
   * @param showPopup {Boolean}
   * @method putConfigGroupChanges
   */
  putConfigGroupChanges: function (data, showPopup) {
    var ajaxOptions = {
      name: 'config_groups.update_config_group',
      sender: this,
      data: {
        id: data.ConfigGroup.id,
        configGroup: data
      }
    };
    if (showPopup) {
      ajaxOptions.success = "putConfigGroupChangesSuccess";
    }
    return App.ajax.send(ajaxOptions);
  },

  /**
   * @private
   * @method putConfigGroupChangesSuccess
   */
  putConfigGroupChangesSuccess: function () {
    this.set('saveConfigsFlag', true);
    this.onDoPUTClusterConfigurations();
  },

  /**
   * set hive hostnames in configs
   * @param configs
   * @private
   * @method setHiveHostName
   */
  setHiveHostName: function (configs) {
    var dbHostPropertyName = null, configsToRemove = [];
    if (configs.someProperty('name', 'hive_database')) {
      var hiveDb = configs.findProperty('name', 'hive_database');

      switch(hiveDb.value) {
        case 'New MySQL Database':
        case 'New PostgreSQL Database':
          dbHostPropertyName = configs.someProperty('name', 'hive_ambari_host') ? 'hive_ambari_host' : dbHostPropertyName;
          configsToRemove = ['hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing MySQL Database':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_mysql_host') ? 'hive_existing_mysql_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing PostgreSQL Database':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_postgresql_host') ? 'hive_existing_postgresql_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing Oracle Database':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_oracle_host') ? 'hive_existing_oracle_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing MSSQL Server database with SQL authentication':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_mssql_server_host') ? 'hive_existing_mssql_server_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'];
          break;
        case 'Existing MSSQL Server database with integrated authentication':
          dbHostPropertyName = configs.someProperty('name', 'hive_existing_mssql_server_2_host') ? 'hive_existing_mssql_server_2_host' : dbHostPropertyName;
          configsToRemove = ['hive_ambari_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database', 'hive_existing_oracle_host', 'hive_existing_oracle_database', 'hive_existing_mssql_server_database', 'hive_existing_mssql_server_host'];
          break;
      }
      configs = dataManipulationUtils.rejectPropertyValues(configs, 'name', configsToRemove);
    }
    if (dbHostPropertyName) {
      var hiveHostNameProperty = App.ServiceConfigProperty.create(App.config.get('preDefinedSiteProperties').findProperty('name', 'hive_hostname'));
      hiveHostNameProperty.set('value', configs.findProperty('name', dbHostPropertyName).get('value'));
      configs.pushObject(hiveHostNameProperty);
    }
    return configs;
  },

  /**
   * set oozie hostnames in configs
   * @param configs
   * @private
   * @method setOozieHostName
   */
  setOozieHostName: function (configs) {
    var dbHostPropertyName = null, configsToRemove = [];
    if (configs.someProperty('name', 'oozie_database')) {
      var oozieDb = configs.findProperty('name', 'oozie_database');
      switch (oozieDb.value) {
        case 'New Derby Database':
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'New MySQL Database':
          var ambariHost = configs.findProperty('name', 'oozie_ambari_host');
          if (ambariHost) {
            ambariHost.name = 'oozie_hostname';
          }
          configsToRemove = ['oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing MySQL Database':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_mysql_host') ? 'oozie_existing_mysql_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing PostgreSQL Database':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_postgresql_host') ? 'oozie_existing_postgresql_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing Oracle Database':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_oracle_host') ? 'oozie_existing_oracle_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_derby_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing MSSQL Server database with SQL authentication':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_mssql_server_host') ? 'oozie_existing_mssql_server_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'];
          break;
        case 'Existing MSSQL Server database with integrated authentication':
          dbHostPropertyName = configs.someProperty('name', 'oozie_existing_mssql_server_2_host') ? 'oozie_existing_mssql_server_2_host' : dbHostPropertyName;
          configsToRemove = ['oozie_ambari_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database', 'oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host'];
          break;
      }
      configs = dataManipulationUtils.rejectPropertyValues(configs, 'name', configsToRemove);
    }

    if (dbHostPropertyName) {
      var oozieHostNameProperty = App.ServiceConfigProperty.create(App.config.get('preDefinedSiteProperties').findProperty('name', 'oozie_hostname'));
      oozieHostNameProperty.set('value', configs.findProperty('name', dbHostPropertyName).get('value'));
      configs.pushObject(oozieHostNameProperty);
    }
    return configs;
  },

  /**
   * save site configs
   * @param configs
   * @private
   * @method saveSiteConfigs
   */
  saveSiteConfigs: function (configs) {
    //storedConfigs contains custom configs as well
    configs = this.setHiveHostName(configs);
    configs = this.setOozieHostName(configs);
    this.formatConfigValues(configs);
    var mappedConfigs = App.config.excludeUnsupportedConfigs(this.get('configMapping').all(), App.Service.find().mapProperty('serviceName'));
    var allUiConfigs = this.loadUiSideConfigs(mappedConfigs);
    this.set('uiConfigs', configs.concat(allUiConfigs));
  },

  /**
   * Reprecent boolean value as string (true => 'true', false => 'false') and trim other values
   * @param serviceConfigProperties
   * @private
   * @method formatConfigValues
   */
  formatConfigValues: function (serviceConfigProperties) {
    serviceConfigProperties.forEach(function (_config) {
      if (typeof _config.get('value') === "boolean") _config.set('value', _config.value.toString());
      _config.set('value', App.config.trimProperty(_config, true));
    });
  },

  /**
   * return configs from the UI side
   * @param configMapping array with configs
   * @return {Array}
   * @private
   * @method loadUiSideConfigs
   */
  loadUiSideConfigs: function (configMapping) {
    var uiConfig = [];
    var configs = configMapping.filterProperty('foreignKey', null);
    this.addDynamicProperties(configs);
    configs.forEach(function (_config) {
      var valueWithOverrides = this.getGlobConfigValueWithOverrides(_config.templateName, _config.value, _config.name);
      if (valueWithOverrides !== null) {
        uiConfig.pushObject({
          "id": "site property",
          "name": _config.name,
          "value": valueWithOverrides.value,
          "filename": _config.filename,
          "overrides": valueWithOverrides.overrides
        });
      }
    }, this);
    return uiConfig;
  },

  /**
   * @param configs
   * @private
   * @method addDynamicProperties
   */
  addDynamicProperties: function (configs) {
    var allConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    var templetonHiveProperty = allConfigs.someProperty('name', 'templeton.hive.properties');
    if (!templetonHiveProperty && this.get('content.serviceName') === 'HIVE') {
      configs.pushObject({
        "name": "templeton.hive.properties",
        "templateName": ["hive.metastore.uris"],
        "foreignKey": null,
        "value": "hive.metastore.local=false,hive.metastore.uris=<templateName[0]>,hive.metastore.sasl.enabled=yes,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse",
        "filename": "webhcat-site.xml"
      });
    }
  },

  /**
   * return config value
   * @param templateName
   * @param expression
   * @param name
   * @return {Object}
   * example: <code>{
   *   value: '...',
   *   overrides: {
   *    'value1': [h1, h2],
   *    'value2': [h3]
   *   }
   * }</code>
   * @private
   * @method getGlobConfigValueWithOverrides
   */
  getGlobConfigValueWithOverrides: function (templateName, expression, name) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    var overrideHostToValue = {};
    if (express != null) {
      express.forEach(function (_express) {
        var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
        var globalObj = this.get('allConfigs').findProperty('name', templateName[index]);
        if (globalObj) {
          var globOverride = globalObj.overrides;
          if (globOverride != null) {
            for (var ov in globOverride) {
              globOverride[ov].forEach(function (host) {
                var replacedVal = (host in overrideHostToValue) ? overrideHostToValue[host] : expression;
                overrideHostToValue[host] = App.config.replaceConfigValues(name, _express, replacedVal, ov);
              }, this);
            }
          }
          value = App.config.replaceConfigValues(name, _express, expression, globalObj.value);
        } else {
          value = null;
        }
      }, this);
    }
    return this.getValueWithOverrides(value, overrideHostToValue)
  },

  /**
   * @param value
   * @param overrideHostToValue
   * @returns {{value: *, overrides: {}}}
   * @private
   * @method getValueWithOverrides
   */
  getValueWithOverrides: function (value, overrideHostToValue) {
    var valueWithOverrides = {
      value: value,
      overrides: {}
    };
    if (!jQuery.isEmptyObject(overrideHostToValue)) {
      for (var host in overrideHostToValue) {
        var hostVal = overrideHostToValue[host];
        if (!(hostVal in valueWithOverrides.overrides)) {
          valueWithOverrides.overrides[hostVal] = [];
        }
        valueWithOverrides.overrides[hostVal].push(host);
      }
    }
    return valueWithOverrides;
  },

  /**
   * Saves cluster level configurations for all necessary sites
   * PUT calls are made to /api/v1/clusters/clusterName for each site
   * @private
   * @method doPUTClusterConfigurations
   */
  doPUTClusterConfigurations: function () {
    this.set('saveConfigsFlag', true);
    var serviceConfigTags = this.get('serviceConfigTags');
    /**
     * adding config tags for dependentConfigs
     */
    for (var i = 0; i < this.get('dependentFileNames.length'); i++) {
      if (!serviceConfigTags.findProperty('siteName', this.get('dependentFileNames')[i])) {
        serviceConfigTags.pushObject({siteName: this.get('dependentFileNames')[i]});
      }
    }
    this.setNewTagNames(serviceConfigTags);
    var siteNameToServerDataMap = {};
    var configsToSave = [];
    serviceConfigTags.forEach(function (_serviceTags) {
      var configs = this.createConfigObject(_serviceTags.siteName, _serviceTags.newTagName);
      if (configs) {
        configsToSave.push(configs);
        siteNameToServerDataMap[_serviceTags.siteName] = configs;
      }
    }, this);
    configsToSave = this.filterChangedConfiguration(configsToSave);
    if (configsToSave.length > 0) {
      var data = [];
      data.pushObject(JSON.stringify({
        Clusters: {
          desired_config: configsToSave
        }
      }));
      if (App.get('supports.enhancedConfigs')) {
        /**
         * adding configs that were changed for dependent services
         * if there are such configs
         */
        this.get('dependentServiceNames').forEach(function(serviceName) {
          var dependentConfigsToSave = this.getDependentConfigObject(serviceName);
          if (dependentConfigsToSave.length > 0) {
            data.pushObject(JSON.stringify({
              Clusters: {
                desired_config: dependentConfigsToSave
              }
            }));
          }
        }, this);
      }
      this.doPUTClusterConfigurationSites(data);
    } else {
      this.onDoPUTClusterConfigurations();
    }
  },

  /**
   * create different config object depending on siteName
   * @param {String} siteName
   * @param {String} tagName
   * @returns {Object|null}
   * @private
   * @method createConfigObject
   */
  createConfigObject: function (siteName, tagName) {
    console.log("TRACE: Inside " + siteName);
    var configObject = {};
    switch (siteName) {
      case 'core-site':
        if (this.get('content.serviceName') === 'HDFS' || this.get('content.serviceName') === 'GLUSTERFS') {
          configObject = this.createCoreSiteObj(tagName);
        } else {
          return null;
        }
        break;
      default:
        var filename = App.config.getOriginalFileName(siteName);
        if (filename === 'mapred-queue-acls.xml') {
          return null;
        }
        configObject = this.createSiteObj(siteName, tagName, this.get('uiConfigs').filterProperty('filename', filename));
        break;
    }
    configObject.service_config_version_note = this.get('serviceConfigVersionNote');
    return configObject;
  },

  /**
   * filter out unchanged configurations
   * @param {Array} configsToSave
   * @private
   * @method filterChangedConfiguration
   */
  filterChangedConfiguration: function (configsToSave) {
    var changedConfigs = [];

    configsToSave.forEach(function (configSite) {
      var oldConfig = App.router.get('configurationController').getConfigsByTags([
        {siteName: configSite.type, tagName: this.loadedClusterSiteToTagMap[configSite.type]}
      ]);
      oldConfig = oldConfig[0] || {};
      var oldProperties = oldConfig.properties || {};
      var oldAttributes = oldConfig["properties_attributes"] || {};
      var newProperties = configSite.properties || {};
      var newAttributes = configSite["properties_attributes"] || {};
      if (this.isAttributesChanged(oldAttributes, newAttributes) || this.isConfigChanged(oldProperties, newProperties) || this.get('modifiedFileNames').contains(App.config.getOriginalFileName(configSite.type))) {
        changedConfigs.push(configSite);
      }
    }, this);
    return changedConfigs;
  },

  /**
   * Compares the loaded config values with the saving config values.
   * @param {Object} loadedConfig -
   * loadedConfig: {
   *      configName1: "configValue1",
   *      configName2: "configValue2"
   *   }
   * @param {Object} savingConfig
   * savingConfig: {
   *      configName1: "configValue1",
   *      configName2: "configValue2"
   *   }
   * @returns {boolean}
   * @private
   * @method isConfigChanged
   */
  isConfigChanged: function (loadedConfig, savingConfig) {
    if (loadedConfig != null && savingConfig != null) {
      var seenLoadKeys = [];
      for (var loadKey in loadedConfig) {
        if (!loadedConfig.hasOwnProperty(loadKey)) continue;
        seenLoadKeys.push(loadKey);
        var loadValue = loadedConfig[loadKey];
        var saveValue = savingConfig[loadKey];
        if ("boolean" == typeof(saveValue)) {
          saveValue = saveValue.toString();
        }
        if (saveValue == null) {
          saveValue = "null";
        }
        if (loadValue !== saveValue) {
          return true;
        }
      }
      for (var saveKey in savingConfig) {
        if (seenLoadKeys.indexOf(saveKey) < 0) {
          return true;
        }
      }
    }
    return false;
  },

  /**
   * Compares the loaded config properties attributes with the saving config properties attributes.
   * @param {Object} oldAttributes -
   * oldAttributes: {
   *   supports: {
   *     final: {
   *       "configValue1" : "true",
   *       "configValue2" : "true"
   *     }
   *   }
   * }
   * @param {Object} newAttributes
   * newAttributes: {
   *   supports: {
   *     final: {
   *       "configValue1" : "true",
   *       "configValue2" : "true"
   *     }
   *   }
   * }
   * @returns {boolean}
   * @private
   * @method isAttributesChanged
   */
  isAttributesChanged: function (oldAttributes, newAttributes) {
    oldAttributes = oldAttributes.final || {};
    newAttributes = newAttributes.final || {};

    var key;
    for (key in oldAttributes) {
      if (oldAttributes.hasOwnProperty(key)
        && (!newAttributes.hasOwnProperty(key) || newAttributes[key] !== oldAttributes[key])) {
        return true;
      }
    }
    for (key in newAttributes) {
      if (newAttributes.hasOwnProperty(key)
        && (!oldAttributes.hasOwnProperty(key) || newAttributes[key] !== oldAttributes[key])) {
        return true;
      }
    }
    return false;
  },

  /**
   * Saves configuration of set of sites. The provided data
   * contains the site name and tag to be used.
   * @param {Object[]} services
   * @return {$.ajax}
   * @method doPUTClusterConfigurationSites
   */
  doPUTClusterConfigurationSites: function (services) {
    return App.ajax.send({
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + services.toString() + ']'
      },
      success: 'doPUTClusterConfigurationSiteSuccessCallback',
      error: 'doPUTClusterConfigurationSiteErrorCallback'
    });
  },

  /**
   * @private
   * @method doPUTClusterConfigurationSiteSuccessCallback
   */
  doPUTClusterConfigurationSiteSuccessCallback: function () {
    this.onDoPUTClusterConfigurations();
  },

  /**
   * @private
   * @method doPUTClusterConfigurationSiteErrorCallback
   */
  doPUTClusterConfigurationSiteErrorCallback: function () {
    this.set('saveConfigsFlag', false);
    this.doPUTClusterConfigurationSiteSuccessCallback();
  },

  /**
   * add newTagName property to each config in serviceConfigs
   * @param serviceConfigs
   * @private
   * @method setNewTagNames
   */
  setNewTagNames: function (serviceConfigs) {
    var time = (new Date).getTime();
    serviceConfigs.forEach(function (_serviceConfigs) {
      _serviceConfigs.newTagName = 'version' + time;
    }, this);
  },

  /**
   * Save "final" attribute for properties
   * @param {Array} properties - array of properties
   * @returns {Object|null}
   * @method getConfigAttributes
   */
  getConfigAttributes: function(properties) {
    var attributes = {
      final: {}
    };
    var finalAttributes = attributes.final;
    var hasAttributes = false;
    properties.forEach(function (property) {
      if (property.isRequiredByAgent !== false && property.isFinal) {
        hasAttributes = true;
        finalAttributes[property.name] = "true";
      }
    });
    if (hasAttributes) {
      return attributes;
    }
    return null;
  },

  /**
   * create core site object
   * @param tagName
   * @return {{type: string, tag: string, properties: object}}
   * @method createCoreSiteObj
   */
  createCoreSiteObj: function (tagName) {
    var coreSiteObj = this.get('uiConfigs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    coreSiteObj.forEach(function (_coreSiteObj) {
      coreSiteProperties[_coreSiteObj.name] = _coreSiteObj.value;
      //this.recordHostOverride(_coreSiteObj, 'core-site', tagName, this);
    }, this);
    var result = {"type": "core-site", "tag": tagName, "properties": coreSiteProperties};
    var attributes = this.getConfigAttributes(coreSiteObj);
    if (attributes) {
      result['properties_attributes'] = attributes;
    }
    return result;
  },

  /**
   * create site object
   * @param {string} siteName
   * @param {string} tagName
   * @param {object[]} siteObj
   * @return {Object}
   * @method createSiteObj
   */
  createSiteObj: function (siteName, tagName, siteObj) {
    var heapsizeException = this.get('heapsizeException');
    var heapsizeRegExp = this.get('heapsizeRegExp');
    var siteProperties = {};
    siteObj.forEach(function (_siteObj) {
      var value = _siteObj.value;
      if (_siteObj.isRequiredByAgent == false) return;
      // site object name follow the format *permsize/*heapsize and the value NOT ends with "m"
      if (heapsizeRegExp.test(_siteObj.name) && !heapsizeException.contains(_siteObj.name) && !(_siteObj.value).endsWith("m")) {
        value += "m";
      }
      siteProperties[_siteObj.name] = value;
      switch (siteName) {
        case 'falcon-startup.properties':
        case 'falcon-runtime.properties':
        case 'pig-properties':
          siteProperties[_siteObj.name] = value;
          break;
        default:
          siteProperties[_siteObj.name] = this.setServerConfigValue(_siteObj.name, value);
      }
    }, this);
    var result = {"type": siteName, "tag": tagName, "properties": siteProperties};
    var attributes = this.getConfigAttributes(siteObj);
    if (attributes) {
      result['properties_attributes'] = attributes;
    }
    return result;
  },

  /**
   * This method will be moved to config's decorators class.
   *
   * For now, provide handling for special properties that need
   * be specified in special format required for server.
   *
   * @param configName {String} - name of config property
   * @param value {*} - value of config property
   *
   * @return {String} - formatted value
   * @method setServerConfigValue
   */
  setServerConfigValue: function (configName, value) {
    switch (configName) {
      case 'storm.zookeeper.servers':
        if( Object.prototype.toString.call( value ) === '[object Array]' ) {
          return JSON.stringify(value).replace(/"/g, "'");
        } else {
          return value;
        }
        break;
      default:
        return value;
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
   * Provides service component name and display-name information for
   * the current selected service.
   * @type {Em.Array} validComponents - array of valid components
   */
  getCurrentServiceComponents: function () {
    var components = this.get('content.hostComponents');
    var validComponents = Em.A([]);
    var seenComponents = {};
    components.forEach(function (component) {
      var cn = component.get('componentName');
      var cdn = component.get('displayName');
      if (!seenComponents[cn]) {
        validComponents.push(Em.Object.create({
          componentName: cn,
          displayName: cdn,
          selected: false
        }));
        seenComponents[cn] = cn;
      }
    });
    return validComponents;
  }.property('content'),

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
   * @method addOverrideProperty
   */
  addOverrideProperty: function (serviceConfigProperty, group, value) {
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
        overrides: null
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
    this.manageConfigurationGroups();
  },

  /**
   * Show popup with config groups
   * User may edit/create/delete them
   * @param controller
   * @returns {App.ModalPopup}
   * @method manageConfigurationGroups
   */
  manageConfigurationGroups: function (controller) {
    var configsController = this;
    var serviceData = (controller && controller.get('selectedService')) || this.get('content');
    var serviceName = serviceData.get('serviceName');
    var displayName = serviceData.get('displayName');
    App.router.get('manageConfigGroupsController').set('isInstaller', !!controller);
    App.router.get('manageConfigGroupsController').set('serviceName', serviceName);
    if (controller) {
      App.router.get('manageConfigGroupsController').set('isAddService', controller.get('content.controllerName') == 'addServiceController');
    }
    return App.ModalPopup.show({
      header: Em.I18n.t('services.service.config_groups_popup.header').format(displayName),
      bodyClass: App.MainServiceManageConfigGroupView.extend({
        serviceName: serviceName,
        displayName: displayName,
        controllerBinding: 'App.router.manageConfigGroupsController'
      }),
      classNames: ['sixty-percent-width-modal', 'manage-configuration-group-popup'],
      primary: Em.I18n.t('common.save'),
      onPrimary: function () {
        var modifiedConfigGroups = this.get('subViewController.hostsModifiedConfigGroups');
        // Save modified config-groups
        if (!!controller) {
          controller.set('selectedService.configGroups', App.router.get('manageConfigGroupsController.configGroups'));
          controller.selectedServiceObserver();
          if (controller.get('name') == "wizardStep7Controller") {
            if (controller.get('selectedService.selected') === false && modifiedConfigGroups.toDelete.length > 0) {
              controller.setGroupsToDelete(modifiedConfigGroups.toDelete);
            }
            configsController.persistConfigGroups();
            this.updateConfigGroupOnServicePage();
          }
          this.hide();
          return;
        }
        console.log("manageConfigurationGroups(): Saving modified config-groups: ", modifiedConfigGroups);
        var self = this;
        var errors = [];
        var deleteQueriesCounter = modifiedConfigGroups.toClearHosts.length + modifiedConfigGroups.toDelete.length;
        var createQueriesCounter = modifiedConfigGroups.toSetHosts.length + modifiedConfigGroups.toCreate.length;
        var deleteQueriesRun = false;
        var createQueriesRun = false;
        var runNextQuery = function () {
          if (!deleteQueriesRun && deleteQueriesCounter > 0) {
            deleteQueriesRun = true;
            modifiedConfigGroups.toClearHosts.forEach(function (cg) {
              configsController.clearConfigurationGroupHosts(cg, finishFunction, finishFunction);
            }, this);
            modifiedConfigGroups.toDelete.forEach(function (cg) {
              configsController.deleteConfigGroup(cg, finishFunction, finishFunction);
            }, this);
          } else if (!createQueriesRun && deleteQueriesCounter < 1) {
            createQueriesRun = true;
            modifiedConfigGroups.toSetHosts.forEach(function (cg) {
              configsController.updateConfigurationGroup(cg, finishFunction, finishFunction);
            }, this);
            modifiedConfigGroups.toCreate.forEach(function (cg) {
              configsController.postNewConfigurationGroup(cg, finishFunction);
            }, this);
          }
        };
        var finishFunction = function (xhr, text, errorThrown) {
          if (xhr && errorThrown) {
            var error = xhr.status + "(" + errorThrown + ") ";
            try {
              var json = $.parseJSON(xhr.responseText);
              error += json.message;
            } catch (err) {
            }
            console.error('Error updating Config Group:', error);
            errors.push(error);
          }
          if (createQueriesRun) {
            createQueriesCounter--;
          } else {
            deleteQueriesCounter--;
          }
          if (deleteQueriesCounter + createQueriesCounter < 1) {
            if (errors.length > 0) {
              console.log(errors);
              self.get('subViewController').set('errorMessage', errors.join(". "));
            } else {
              self.updateConfigGroupOnServicePage();
              self.hide();
            }
          } else {
            runNextQuery();
          }
        };
        runNextQuery();
      },
      subViewController: function () {
        return App.router.get('manageConfigGroupsController');
      }.property('App.router.manageConfigGroupsController'),

      updateConfigGroupOnServicePage: function () {
        var subViewController = this.get('subViewController');
        var selectedConfigGroup = subViewController.get('selectedConfigGroup');
        var managedConfigGroups = subViewController.get('configGroups');
        if (!controller) {
          controller = App.router.get('mainServiceInfoConfigsController');
          controller.set('configGroups', managedConfigGroups);
        } else {
          controller.set('selectedService.configGroups', managedConfigGroups);
        }

        var selectEventObject = {};
        //check whether selectedConfigGroup exists
        if (selectedConfigGroup && controller.get('configGroups').someProperty('name', selectedConfigGroup.get('name'))) {
          selectEventObject.context = selectedConfigGroup;
        } else {
          selectEventObject.context = managedConfigGroups.findProperty('isDefault', true);
        }
        controller.selectConfigGroup(selectEventObject);
      },

      updateButtons: function () {
        var modified = this.get('subViewController.isHostsModified');
        this.set('disablePrimary', !modified);
      }.observes('subViewController.isHostsModified'),
      didInsertElement: Em.K
    });
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
  },

  /**
   * Are some unsaved changes available
   * @returns {boolean}
   * @method hasUnsavedChanges
   */
  hasUnsavedChanges: function () {
    return this.get('hash') != this.getHash();
  },

  /**
   * If some configs are changed and user navigates away or select another config-group, show this popup with propose to save changes
   * @param {String} path
   * @param {object} callback - callback with action to change configs view(change group or version)
   * @return {App.ModalPopup}
   * @method showSavePopup
   */
  showSavePopup: function (path, callback) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/configs/save_configuration'),
        showSaveWarning: true,
        notesArea: Em.TextArea.extend({
          classNames: ['full-width'],
          placeholder: Em.I18n.t('dashboard.configHistory.info-bar.save.popup.placeholder'),
          onChangeValue: function() {
            this.get('parentView.parentView').set('serviceConfigNote', this.get('value'));
          }.observes('value')
        })
      }),
      footerClass: Ember.View.extend({
        templateName: require('templates/main/service/info/save_popup_footer')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onSave: function () {
        self.set('serviceConfigVersionNote', this.get('serviceConfigNote'));
        self.saveStepConfigs();
        this.hide();
      },
      onDiscard: function () {
        self.set('preSelectedConfigVersion', null);
        if (path) {
          self.set('forceTransition', true);
          App.router.route(path);
        } else if (callback) {
          // Prevent multiple popups
          self.set('hash', self.getHash());
          callback();
        }
        this.hide();
      },
      onCancel: function () {
        this.hide();
      }
    });
  }

});
