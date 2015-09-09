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
var databaseUtils = require('utils/configs/database');

App.MainServiceInfoConfigsController = Em.Controller.extend(App.ConfigsLoader, App.ServerValidatorMixin, App.EnhancedConfigsMixin, App.ThemesMappingMixin, App.VersionsMappingMixin, App.ConfigsSaverMixin, App.ConfigsComparator, {

  name: 'mainServiceInfoConfigsController',

  isHostsConfigsPage: false,

  forceTransition: false,

  isRecommendedLoaded: true,

  dataIsLoaded: false,

  stepConfigs: [], //contains all field properties that are viewed in this service

  selectedService: null,

  selectedConfigGroup: null,

  selectedServiceNameTrigger: null,

  requestsInProgress: [],

  groupsStore: App.ServiceConfigGroup.find(),

  /**
   * config groups for current service
   * @type {App.ConfigGroup[]}
   */
  configGroups: function() {
    return this.get('groupsStore').filterProperty('serviceName', this.get('content.serviceName'));
  }.property('content.serviceName', 'groupsStore'),

  dependentConfigGroups: function() {
    if (this.get('dependentServiceNames.length') === 0) return [];
    return this.get('groupsStore').filter(function(group) {
      return this.get('dependentServiceNames').contains(group.get('serviceName'));
    }, this);
  }.property('content.serviceName', 'dependentServiceNames', 'groupsStore.length', 'groupStore.@each.name'),

  allConfigs: [],

  /**
   * Determines if save configs is in progress
   * @type {boolean}
   */
  saveInProgress: false,

  isCompareMode: false,

  preSelectedConfigVersion: null,

  /**
   * contain Service Config Property, when user proceed from Select Config Group dialog
   */
  overrideToAdd: null,

  /**
   * version selected to view
   */
  selectedVersion: null,

  /**
   * note passed on configs save
   * @type {string}
   */
  serviceConfigVersionNote: '',

  versionLoaded: false,

  /**
   * Determines when data about config groups is loaded
   * Including recommendations with information about hosts in the each group
   * @type {boolean}
   */
  configGroupsAreLoaded: false,

  dependentServiceNames: [],
  /**
   * defines which service configs need to be loaded to stepConfigs
   * @type {string[]}
   */
  servicesToLoad: function() {
    return [this.get('content.serviceName')].concat(this.get('dependentServiceNames')).uniq();
  }.property('content.serviceName', 'dependentServiceNames.length'),

  /**
   * @type {boolean}
   */
  isCurrentSelected: function () {
    return App.ServiceConfigVersion.find(this.get('content.serviceName') + "_" + this.get('selectedVersion')).get('isCurrent');
  }.property('selectedVersion', 'content.serviceName', 'dataIsLoaded', 'versionLoaded'),

  /**
   * @type {boolean}
   */
  canEdit: function () {
    return (this.get('selectedVersion') == this.get('currentDefaultVersion') || !this.get('selectedConfigGroup.isDefault'))
        && !this.get('isCompareMode') && App.isAccessible('MANAGER') && !this.get('isHostsConfigsPage');
  }.property('selectedVersion', 'isCompareMode', 'currentDefaultVersion'),

  serviceConfigs: function () {
    return App.config.get('preDefinedServiceConfigs');
  }.property('App.config.preDefinedServiceConfigs'),

  showConfigHistoryFeature: true,

  /**
   * Number of errors in the configs in the selected service (only for AdvancedTab if App supports Enhanced Configs)
   * @type {number}
   */
  errorsCount: function () {
    return this.get('selectedService.configs').filter(function (config) {
      return Em.isNone(config.get('widget'));
    }).filter(function(config) {
      return !config.get('isValid') || (config.get('overrides') || []).someProperty('isValid', false);
    }).filterProperty('isVisible').length;
  }.property('selectedService.configs.@each.isValid', 'selectedService.configs.@each.overrideErrorTrigger'),

  /**
   * Determines if Save-button should be disabled
   * Disabled if some configs have invalid values for selected service
   * or save-process currently in progress
   *
   * @type {boolean}
   */
  isSubmitDisabled: function () {
    if (!this.get('selectedService')) return true;
    return this.get('selectedService').get('errorCount') !==  0 || this.get('saveInProgress');
  }.property('selectedService.errorCount', 'saveInProgress'),

  /**
   * Determines if some config value is changed
   * @type {boolean}
   */
  isPropertiesChanged: function(){
    return this.get('stepConfigs').someProperty('isPropertiesChanged', true);
  }.property('stepConfigs.@each.isPropertiesChanged'),

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
      attributeName: 'hasIssues',
      attributeValue: true,
      caption: 'common.combobox.dropdown.issues'
    }
  ],

  /**
   * get array of config properties that are shown in settings tab
   * @type {App.StackConfigProperty[]}
   */
  settingsTabProperties: function() {
    var properties = [];
    App.Tab.find(this.get('content.serviceName') + '_settings').get('sections').forEach(function(s) {
      s.get('subSections').forEach(function(ss) {
        properties = properties.concat(ss.get('configProperties').filterProperty('id'));
      });
    });
    return properties;
  }.property('content.serviceName', 'App.router.clusterController.isStackConfigsLoaded'),

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
    this.get('requestsInProgress').push(request);
  },

  /**
   * clear and set properties to default value
   * @method clearStep
   */
  clearStep: function () {
    this.get('requestsInProgress').forEach(function(r) {
      if (r && r.readyState !== 4) {
        r.abort();
      }
    });
    this.get('requestsInProgress').clear();
    this.clearLoadInfo();
    this.clearSaveInfo();
    this.clearDependentConfigs();
    this.setProperties({
      saveInProgress: false,
      isInit: true,
      hash: null,
      forceTransition: false,
      dataIsLoaded: false,
      versionLoaded: false,
      filter: '',
      serviceConfigVersionNote: '',
      dependentServiceNames: [],
      configGroupsAreLoaded: false
    });
    this.get('filterColumns').setEach('selected', false);
    this.clearConfigs();
  },

  clearConfigs: function() {
    this.get('selectedConfigGroup', null);
    this.get('allConfigs').invoke('destroy');
    this.get('stepConfigs').invoke('destroy');
    this.set('stepConfigs', []);
    this.set('allConfigs', []);
    this.set('selectedService', null);
  },

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
    var serviceName = this.get('content.serviceName');
    this.clearStep();
    this.set('dependentServiceNames', App.StackService.find(serviceName).get('dependentServiceNames'));
    if (App.get('isClusterSupportsEnhancedConfigs')) {
      this.loadConfigTheme(serviceName).always(function() {
        App.themesMapper.generateAdvancedTabs([serviceName]);
      });
    }
    if (!this.get('preSelectedConfigVersion')) {
      this.loadCurrentVersions();
    }
    this.loadServiceConfigVersions();
  },

  /**
   * Generate "finger-print" for current <code>stepConfigs[0]</code>
   * Used to determine, if user has some unsaved changes (comparing with <code>hash</code>)
   * @returns {string|null}
   * @method getHash
   */
  getHash: function () {
    if (!this.get('selectedService.configs.length')) {
      return null;
    }
    var hash = {};
    this.get('selectedService.configs').forEach(function (config) {
      hash[config.get('name')] = {value: App.config.formatPropertyValue(config), overrides: [], isFinal: config.get('isFinal')};
      if (!config.get('overrides')) return;
      if (!config.get('overrides.length')) return;

      config.get('overrides').forEach(function (override) {
        hash[config.get('name')].overrides.push(App.config.formatPropertyValue(override));
      });
    });
    return JSON.stringify(hash);
  },

  parseConfigData: function(data) {
    this.prepareConfigObjects(data, this.get('content.serviceName'));
    var self = this;
    this.loadCompareVersionConfigs(this.get('allConfigs')).done(function() {
      self.addOverrides(data, self.get('allConfigs'));
      self.onLoadOverrides(self.get('allConfigs'));
    });
  },

  prepareConfigObjects: function(data, serviceName) {
    this.get('stepConfigs').clear();

    var configGroups = [];
    data.items.forEach(function (v) {
      if (v.group_name == 'default') {
        v.configurations.forEach(function (c) {
          configGroups.pushObject(c);
        });
      }
    });
    var configs = App.config.mergePredefinedWithSaved(configGroups, serviceName, this.get('selectedConfigGroup'), this.get('canEdit'));
    configs = App.config.sortConfigs(configs);
    /**
     * if property defined in stack but somehow it missed from cluster properties (can be after stack upgrade)
     * ui should add this properties to step configs
     */
    configs = this.mergeWithStackProperties(configs);

    //put properties from capacity-scheduler.xml into one config with textarea view
    if (this.get('content.serviceName') === 'YARN') {
      var configsToSkip = this.get('settingsTabProperties').filterProperty('filename', 'capacity-scheduler.xml');
      configs = App.config.fileConfigsIntoTextarea(configs, 'capacity-scheduler.xml', configsToSkip);
    }

    if (this.get('content.serviceName') === 'KERBEROS') {
      var kdc_type = configs.findProperty('name', 'kdc_type');
      if (kdc_type.get('value') === 'none') {
        configs.findProperty('name', 'kdc_host').set('isRequired', false).set('isVisible', false);
        configs.findProperty('name', 'admin_server_host').set('isRequired', false).set('isVisible', false);
        configs.findProperty('name', 'domains').set('isRequired', false).set('isVisible', false);
      } else if (kdc_type.get('value') === 'active-directory') {
        configs.findProperty('name', 'container_dn').set('isVisible', true);
        configs.findProperty('name', 'ldap_url').set('isVisible', true);
      }
    }

    this.set('allConfigs', configs);
    //add configs as names of host components
    this.addHostNamesToConfig();
    this.addDBProperties(configs);
  },

  /**
   * This method should add UI properties that are market as <code>'isRequiredByAgent': false<code>
   * @param configs
   */
  addDBProperties: function(configs) {
    if (this.get('content.serviceName') === 'HIVE') {
      var propertyToAdd = App.config.get('preDefinedSitePropertiesMap')[App.config.configId('hive_hostname','hive-env')],
        cfg = App.config.createDefaultConfig(propertyToAdd.name, propertyToAdd.serviceName, propertyToAdd.filename, true, propertyToAdd),
        connectionUrl = configs.findProperty('name', 'javax.jdo.option.ConnectionURL');
      if (cfg && connectionUrl) {
        cfg.savedValue = cfg.value = databaseUtils.getDBLocationFromJDBC(connectionUrl.get('value'));
        configs.pushObject(App.ServiceConfigProperty.create(cfg));
      }
    }
  },
  /**
   * adds properties form stack that doesn't belong to cluster
   * to step configs
   * also set recommended value if isn't exists
   *
   * @return {App.ServiceConfigProperty[]}
   * @method mergeWithStackProperties
   */
  mergeWithStackProperties: function (configs) {
    this.get('settingsTabProperties').forEach(function (advanced) {
      if (!configs.someProperty('name', advanced.get('name'))) {
        configs.pushObject(App.ServiceConfigProperty.create({
          name: advanced.get('name'),
          displayName: advanced.get('displayName'),
          value: advanced.get('value'),
          savedValue: null,
          filename: advanced.get('fileName'),
          isUserProperty: false,
          isNotSaved: true,
          recommendedValue: advanced.get('value'),
          isFinal: advanced.get('isFinal'),
          recommendedIsFinal: advanced.get('recommendedIsFinal'),
          serviceName: advanced.get('serviceName'),
          supportsFinal: advanced.get('supportsFinal'),
          category: 'Advanced ' + App.config.getConfigTagFromFileName(advanced.get('fileName')),
          widget: advanced.get('widget')
        }));
      }
    });
    return configs;
  },

  addOverrides: function(data, allConfigs) {
    var self = this;
    data.items.forEach(function(group) {
      if (group.group_name != 'default') {
        var configGroup = App.ServiceConfigGroup.find().filterProperty('serviceName', group.service_name).findProperty('name', group.group_name);
        group.configurations.forEach(function(config) {
          for (var prop in config.properties) {
            var fileName = App.config.getOriginalFileName(config.type);
            var serviceConfig = allConfigs.filterProperty('name', prop).findProperty('filename', fileName);
            var value = App.config.formatOverrideValue(serviceConfig, config.properties[prop]);
            var isFinal = !!(config.properties_attributes && config.properties_attributes.final && config.properties_attributes.final[prop]);

            if (serviceConfig) {
              if (self.get('selectedConfigGroup.isDefault') || configGroup.get('name') == self.get('selectedConfigGroup.name')) {
                var overridePlainObject = {
                  "value": value,
                  "savedValue": value,
                  "isFinal": isFinal,
                  "savedIsFinal": isFinal,
                  "isEditable": self.get('canEdit') && configGroup.get('name') == self.get('selectedConfigGroup.name')
                };
                App.config.createOverride(serviceConfig, overridePlainObject, configGroup);
              }
            } else {
              var isEditable = self.get('canEdit') && configGroup.get('name') == self.get('selectedConfigGroup.name');
              allConfigs.push(App.config.createCustomGroupConfig(prop, config, configGroup, isEditable));
            }
          }
        });
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
    this.get('servicesToLoad').forEach(function(serviceName) {
      var configGroups = serviceName == this.get('content.serviceName') ? this.get('configGroups') : this.get('dependentConfigGroups').filterProperty('serviceName', serviceName);
      var serviceNames = [ serviceName ];
      if(serviceName === 'OOZIE') {
        // For Oozie, also add ELService properties which are marked as FALCON properties.
        serviceNames.push('FALCON')
      }
      var configsByService = this.get('allConfigs').filter(function (c) {
        return serviceNames.contains(c.get('serviceName'));
      });
      //databaseUtils.bootstrapDatabaseProperties(configsByService, serviceName);
      var serviceConfig = App.config.createServiceConfig(serviceName, configGroups, configsByService, configsByService.length);
      if (serviceConfig.get('serviceName') === 'HDFS') {
        if (App.get('isHaEnabled')) {
          var c = serviceConfig.configs,
            removedConfigs = c.filterProperty('category', 'SECONDARY_NAMENODE');
          removedConfigs.setEach('isVisible', false);
          serviceConfig.configs = c;
        }
      }

      this.get('stepConfigs').pushObject(serviceConfig);
    }, this);

    var selectedService = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName'));
    if (this.get('selectedService.serviceName') != selectedService.get('serviceName')) {
      this.propertyDidChange('selectedServiceNameTrigger');
    }
    this.set('selectedService', selectedService);
    this.checkOverrideProperty(selectedService);
    //this.checkDatabaseProperties(selectedService);
    if (!App.Service.find().someProperty('serviceName', 'RANGER')) {
      App.config.removeRangerConfigs(this.get('stepConfigs'));
    } else {
      this.setVisibilityForRangerProperties(selectedService);
    }
    this._onLoadComplete();
    this.getRecommendationsForDependencies(null, true, Em.K, this.get('selectedConfigGroup'));
    App.loadTimer.finish('Service Configs Page');
  },

  /**
   * @method _getRecommendationsForDependenciesCallback
   */
  _onLoadComplete: function () {
    this.get('stepConfigs').forEach(function(serviceConfig){
      serviceConfig.set('initConfigsLength', serviceConfig.get('configs.length'));
    });
    this.setProperties({
      dataIsLoaded: true,
      versionLoaded: true,
      isInit: false,
      hash: this.getHash()
    });
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
   * trigger App.config.createOverride
   * @param {Object[]} stepConfig
   * @private
   * @method checkOverrideProperty
   */
  checkOverrideProperty: function (stepConfig) {
    var overrideToAdd = this.get('overrideToAdd');
    var value = !!this.get('overrideToAdd.widget') ? Em.get(overrideToAdd, 'value') : '';
    if (overrideToAdd) {
      overrideToAdd = stepConfig.configs.filter(function(c){
        return c.name == overrideToAdd.name && c.filename == overrideToAdd.filename;
      });
      if (overrideToAdd[0]) {
        App.config.createOverride(overrideToAdd[0], {"isEditable": true, "value": value}, this.get('selectedConfigGroup'));
        this.set('overrideToAdd', null);
      }
    }
  },

  /**
   * Adds host name of master component to config
   * @private
   * @method addHostNamesToGlobalConfig
   */
  addHostNamesToConfig: function () {
    var serviceName = this.get('content.serviceName');
    var hostComponentMapping = require('data/host_component_mapping');
    //namenode_host is required to derive "fs.default.name" a property of core-site
    try {
      this.setHostForService('HDFS', 'NAMENODE', 'namenode_host', true);
    } catch (err) {
      console.log("No NameNode Host available.  This is expected if you're using GLUSTERFS rather than HDFS.");
    }

    var hostProperties = hostComponentMapping.filter(function (h) {
      return h.serviceUseThis.contains(serviceName) || h.serviceName == serviceName;
    });
    hostProperties.forEach(function (h) {
      this.setHostForService(h.serviceName, h.componentName, h.hostProperty, h.m);
    }, this);
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
      hostConfig.recommendedValue = this.getMasterComponentHostValue(componentName, multiple);
      configs.push(App.ServiceConfigProperty.create(hostConfig));
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
    var components = App.HostComponent.find().filterProperty('componentName', componentName);
  
    if (components.length > 0) {
      return multiple ? components.mapProperty('hostName') : components[0].get('hostName');
    }
    return false;
  },

  /**
   * Trigger loadSelectedVersion
   * @method doCancel
   */
  doCancel: function () {
    this.set('preSelectedConfigVersion', null);
    this.clearDependentConfigs();
    this.loadSelectedVersion(this.get('selectedVersion'), this.get('selectedConfigGroup'));
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

    var isNNAffected = false;
    var restartRequiredHostsAndComponents = this.get('content.restartRequiredHostsAndComponents');
    for (var hostName in restartRequiredHostsAndComponents) {
      restartRequiredHostsAndComponents[hostName].forEach(function (hostComponent) {
        if (hostComponent == 'NameNode')
         isNNAffected = true;
      })
    }
    if (this.get('content.serviceName') == 'HDFS' && isNNAffected &&
      this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
        return App.showConfirmationFeedBackPopup(function (query) {
          var selectedService = self.get('content.id');
          batchUtils.restartAllServiceHostComponents(selectedService, true, query);
        }, bodyMessage);
      });
    } else {
      return App.showConfirmationFeedBackPopup(function (query) {
        var selectedService = self.get('content.id');
        batchUtils.restartAllServiceHostComponents(selectedService, true, query);
      }, bodyMessage);
    }
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
    var restartRequiredHostsAndComponents = event.context.restartRequiredHostsAndComponents;
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
    var restartRequiredHostsAndComponents = event.context.restartRequiredHostsAndComponents;
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
    App.loadTimer.start('Service Configs Page');
    var configGroupVersions = App.ServiceConfigVersion.find().filterProperty('groupId', event.context.get('configGroupId'));
    //check whether config group has config versions
    if (event.context.get('configGroupId') == -1) {
      this.loadCurrentVersions();
    } else if (configGroupVersions.length > 0) {
      this.loadSelectedVersion(configGroupVersions.findProperty('isCurrent').get('version'), event.context);
    } else {
      this.loadSelectedVersion(null, event.context);
    }
  }
});
