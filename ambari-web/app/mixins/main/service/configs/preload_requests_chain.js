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

/**
 * Mixin with chain of the methods for initial configs loading
 * Used in the service configs controller
 * Entry point - <code>loadClusterEnvSite</code>
 * Chain:
 *    - loadClusterEnvSite
 *      |- (on success callback)
 *         loadServiceConfigVersions
 *           |- (on success callback)
 *              loadSelectedVersion
 *                 |- (on complete callback)
 *                    loadServiceTagsAndGroups
 * @type {Ember.Mixin}
 */
App.PreloadRequestsChainMixin = Em.Mixin.create({

  /**
   * @type {Function}
   */
  trackRequest: Em.required(Function),

  /**
   * @type {Function}
   */
  isVersionDefault: Em.required(Function),

  /**
   * temp stores dependent groups
   */
  dependentConfigGroups: [],
  /**
   * load all tag versions of cluster-env site
   * @returns {$.ajax}
   * @method loadClusterEnvSite
   */
  loadClusterEnvSite: function () {
    return App.ajax.send({
      name: 'config.cluster_env_site',
      sender: this,
      success: 'loadClusterEnvSiteSuccess'
    });
  },

  /**
   * Success-callback for loadClusterEnvSite
   * @param data
   * @private
   * @method loadClusterEnvSiteSuccess
   */
  loadClusterEnvSiteSuccess: function (data) {
    // find the latest tag version
    var maxVersion = Math.max.apply(this, data.items.mapProperty('version'));
    this.set('clusterEnvTagVersion', data.items.findProperty('version', maxVersion).tag);
    this.trackRequest(this.loadServiceConfigVersions());
  },

  /**
   * get service config versions of current service
   * @return {$.ajax}
   * @private
   * @method loadServiceConfigVersions
   */
  loadServiceConfigVersions: function () {
    return App.ajax.send({
      name: 'service.serviceConfigVersions.get',
      data: {
        serviceName: this.get('content.serviceName')
      },
      sender: this,
      success: 'loadServiceConfigVersionsSuccess',
      error: 'loadServiceConfigVersionsError'
    })
  },

  /**
   * success callback for loadServiceConfigVersions
   * load service config versions to model
   * set currentDefaultVersion
   * @param data
   * @param opt
   * @param params
   * @private
   * @method loadServiceConfigVersionsSuccess
   */
  loadServiceConfigVersionsSuccess: function (data, opt, params) {
    App.serviceConfigVersionsMapper.map(data);
    this.set('currentDefaultVersion', data.items.filterProperty('group_id', -1).findProperty('is_current').service_config_version);
    if (this.get('preSelectedConfigVersion')) {
      this.loadSelectedVersion(this.get('preSelectedConfigVersion.version'));
    } else {
      this.loadSelectedVersion();
    }
  },

  /**
   * error callback of loadServiceConfigVersions()
   * override defaultCallback
   * @private
   * @method loadServiceConfigVersionsError
   */
  loadServiceConfigVersionsError: Em.K,

  /**
   * get selected service config version
   * In case selected version is undefined then take currentDefaultVersion
   * @param version
   * @param switchToGroup
   * @method loadSelectedVersion
   */
  loadSelectedVersion: function (version, switchToGroup) {
    var self = this;
    this.set('versionLoaded', false);
    version = version || this.get('currentDefaultVersion');
    //version of non-default group require properties from current version of default group to correctly display page
    var versions = (this.isVersionDefault(version)) ? [version] : [this.get('currentDefaultVersion'), version];
    switchToGroup = (this.isVersionDefault(version) && !switchToGroup) ? this.get('configGroups').findProperty('isDefault') : switchToGroup;

    if (self.get('dataIsLoaded') && switchToGroup) {
      this.set('selectedConfigGroup', switchToGroup);
    }
    var data = {
      serviceName: this.get('content.serviceName'),
      serviceConfigVersions: versions
    };
    if (App.get('isClusterSupportsEnhancedConfigs') && this.get('dependentServiceNames.length')) {
      data.additionalParams = '|service_name.in(' +  this.get('dependentServiceNames') + ')&is_current=true';
    }
    this.trackRequest(App.ajax.send({
      name: 'service.serviceConfigVersions.get.multiple',
      sender: this,
      data: data,
      success: 'loadSelectedVersionSuccess'
    }).complete(function (xhr) {
        if (xhr.statusText === 'abort') return;
        if (self.get('dataIsLoaded')) {
          self.onConfigGroupChange();
        } else {
          self.loadServiceTagsAndGroups();
        }
      }));
  },

  /**
   * load config groups of service
   * and dependent services
   * @private
   * @method loadServiceTagsAndGroups
   */
  loadServiceTagsAndGroups: function () {
    this.trackRequest(App.ajax.send({
      name: 'config.tags_and_groups',
      sender: this,
      data: {
        serviceName: this.get('content.serviceName'),
        urlParams: "&config_groups/ConfigGroup/tag.in(" + this.get('servicesToLoad').join(',') + ')'
      },
      success: 'loadServiceConfigsSuccess'
    }));
  },

  /**
   * set cluster to site tag map
   * @param data
   * @param opt
   * @param params
   * @private
   * @method loadSelectedVersionSuccess
   */
  loadSelectedVersionSuccess: function (data, opt, params) {
    var serviceConfigsDef = this.get('serviceConfigs').filter(function(serviceConfig) {
      return this.get('servicesToLoad').contains(serviceConfig.get('serviceName'));
    }, this);
    var siteToTagMap = {};
    var configTypesRendered = [];
    serviceConfigsDef.forEach(function(s) {
      configTypesRendered = configTypesRendered.concat(Object.keys(s.get('configTypesRendered')));
    });
    var selectedVersion = params.serviceConfigVersions.length > 1 ? params.serviceConfigVersions[1] : params.serviceConfigVersions[0];
    var configurations = [];


    configTypesRendered.forEach(function (siteName) {
      data.items.forEach(function (item) {
        if (item.group_id == -1) {
          configurations = item.configurations;
          if (item.configurations.someProperty('type', siteName)) {
            siteToTagMap[siteName] = item.configurations.findProperty('type', siteName).tag;
          } else if (!siteToTagMap[siteName]) {
            siteToTagMap[siteName] = 'version1';
          }
        } else {
          //set config tags of non-default config group to load overrides from selected version
          this.loadedGroupToOverrideSiteToTagMap[item.group_name] = {};
          item.configurations.forEach(function (config) {
            this.loadedGroupToOverrideSiteToTagMap[item.group_name][config.type] = config.tag;
          }, this)
        }
      }, this)
    }, this);

    App.router.get('configurationController').saveToDB(configurations);

    // add cluster-env tag
    siteToTagMap['cluster-env'] = this.get('clusterEnvTagVersion');

    this.loadedClusterSiteToTagMap = siteToTagMap;
    this.set('selectedVersion', selectedVersion);
    //reset map if selected current version of default group
    if (this.get('isCurrentSelected') && selectedVersion === this.get('currentDefaultVersion')) {
      this.loadedGroupToOverrideSiteToTagMap = {};
    }
  },

  /**
   * Success-callback for loadServiceTagsAndGroups
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @private
   * @method loadServiceConfigsSuccess
   */
  loadServiceConfigsSuccess: function (data, opt, params) {
    this.setConfigGroups(data, opt, params);
  },

  /**
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @private
   * @method setConfigGroups
   */
  setConfigGroups: function (data, opt, params) {
    var serviceName = this.get('content.serviceName');
    var displayName = this.get('content.displayName');
    var selectedConfigGroup;
    var defaultHosts = App.get('allHostNames');

    //parse loaded config groups
    var configGroups = [];
    if (data && data.config_groups && data.config_groups.length) {
      data.config_groups.forEach(function (item) {
        item = item.ConfigGroup;
        if (item.tag === this.get('content.serviceName')) {
          var groupHosts = item.hosts.mapProperty('host_name');
          var newConfigGroup = App.ConfigGroup.create({
            id: item.id,
            name: item.group_name,
            description: item.description,
            isDefault: false,
            parentConfigGroup: null,
            service: App.Service.find().findProperty('serviceName', item.tag),
            hosts: groupHosts,
            configSiteTags: []
          });
          for (var i = 0; i < groupHosts.length; i++) {
            defaultHosts = defaultHosts.without(groupHosts[i]);
          }
          item.desired_configs.forEach(function (config) {
            newConfigGroup.configSiteTags.push(App.ConfigSiteTag.create({
              site: config.type,
              tag: config.tag
            }));
          }, this);
          // select default selected group for hosts page
          if (!selectedConfigGroup && this.get('isHostsConfigsPage') && newConfigGroup.get('hosts').contains(this.get('host.hostName')) && this.get('content.serviceName') === item.tag) {
            selectedConfigGroup = newConfigGroup;
          }
          configGroups.push(newConfigGroup);
        } else if (this.get('dependentServiceNames').contains(item.tag)) {
          /**
           * Load config groups for services that has dependent properties.
           * If user change properties that have dependencies in not default config group
           * user should pick to which config group Ambari should save these properties
           * @type {App.ConfigGroup}
           */
          var newDependentConfigGroup = App.ConfigGroup.create({
            id: item.id,
            name: item.group_name,
            description: item.description,
            isDefault: false,
            parentConfigGroup: null,
            serviceName: item.tag,
            service: App.Service.find().findProperty('serviceName', item.tag),
            hosts: item.hosts.mapProperty('host_name')
          });
          item.desired_configs.forEach(function (config) {
            newDependentConfigGroup.configSiteTags.push(App.ConfigSiteTag.create({
              site: config.type,
              tag: config.tag
            }));
          }, this);
          if (!this.get('dependentConfigGroups').findProperty('name', item.group_name)) {
            this.get('dependentConfigGroups').push(newDependentConfigGroup);
          }
        }
      }, this);
    }
    this.get('dependentServiceNames').forEach(function(serviceName) {
      if (serviceName !== this.get('content.serviceName')) {
        var service = App.Service.find().findProperty('serviceName', serviceName);
        /**
         * default groups for dependent services
         * @type {App.ConfigGroup}
         */
        var defaultConfigGroup = App.ConfigGroup.create({
          name: service.get('displayName') + " Default",
          description: "Default cluster level " + serviceName + " configuration",
          isDefault: true,
          hosts: [],
          parentConfigGroup: null,
          service: service,
          serviceName: serviceName,
          configSiteTags: []
        });
        if (!this.get('dependentConfigGroups').findProperty('name', defaultConfigGroup.get('name'))) {
          this.get('dependentConfigGroups').push(defaultConfigGroup);
        }
      }
    }, this);
    this.set('configGroups', configGroups);
    var defaultConfigGroup = App.ConfigGroup.create({
      name: displayName + " Default",
      description: "Default cluster level " + serviceName + " configuration",
      isDefault: true,
      hosts: defaultHosts,
      parentConfigGroup: null,
      service: this.get('content'),
      serviceName: serviceName,
      configSiteTags: []
    });
    if (!selectedConfigGroup) {
      selectedConfigGroup = configGroups.findProperty('name', this.get('preSelectedConfigVersion.groupName')) || defaultConfigGroup;
    }

    this.get('configGroups').sort(function (configGroupA, configGroupB) {
      return (configGroupA.name > configGroupB.name);
    });
    this.get('configGroups').unshift(defaultConfigGroup);
    this.set('selectedConfigGroup', selectedConfigGroup);
    this.set('preSelectedConfigVersion', null);
  }

});