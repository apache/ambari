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

App.MainServiceInfoConfigsController = Em.Controller.extend({
  name: 'mainServiceInfoConfigsController',
  dataIsLoaded: false,
  stepConfigs: [], //contains all field properties that are viewed in this service
  selectedService: null,
  serviceConfigTags: null,
  globalConfigs: [],
  uiConfigs: [],
  customConfig: [],
  isApplyingChanges: false,
  serviceConfigs: require('data/service_configs'),
  configs: require('data/config_properties').configProperties,
  configMapping: require('data/config_mapping'),
  customConfigs: require('data/custom_configs'),
  
  /**
   * During page load time, we get the host overrides from the server.
   * The current host -> site:tag map is stored below. This will be 
   * useful during save, so that removals can also be determined.
   * 
   * Example:
   * {
   *  'hostname1':{
   *    'global': 'version1',
   *    'core-site': 'version1',
   *    'hdfs-site', 'tag3187261938'
   *  }
   * } 
   *  
   * @see savedHostToOverrideSiteToTagMap
   */
  loadedHostToOverrideSiteToTagMap: {},

  /**
   * During page load time the cluster level site to tag
   * mapping is stored here.
   *
   * Example:
   * {
   *  'global': 'version1',
   *  'hdfs-site': 'version1',
   *  'core-site': 'version1'
   * }
   */
  loadedClusterSiteToTagMap: {},

  /**
   * During page save time, we set the host overrides to the server.
   * The new host -> site:tag map is stored below. This will be 
   * useful during save, to update the host's host components. Also,
   * it will be useful in deletion of overrides.
   * 
   * Example:
   * {
   *  'hostname1': {
   *    'global': {
   *      'tagName': 'tag3187261938_hostname1',
   *      'map': {
   *        'hadoop_heapsize': '2048m'
   *      }
   *    }
   *  }
   * }
   * 
   * @see loadedHostToOverrideSiteToTagMap
   */
  savedHostToOverrideSiteToTagMap: {},

  /**
   * Holds the actual base service-config server data uploaded.
   * This is used by the host-override mechanism to update host
   * specific values.
   */
  savedSiteNameToServerServiceConfigDataMap: {},
  
  isSubmitDisabled: function () {
    return (!(this.stepConfigs.everyProperty('errorCount', 0)) || this.get('isApplyingChanges'));
  }.property('stepConfigs.@each.errorCount', 'isApplyingChanges'),

  slaveComponentGroups: null,

  /**
   * Filter text will be located here
   */
  filter: '',

  /**
   * Dropdown menu items in filter compbobox
   */
  filterColumns: function(){
    var result = [];
    for(var i = 1; i<4; i++){
      result.push(Ember.Object.create({
        name: this.t('common.combobox.dropdown.' + i),
        selected: false
      }));
    }
    return result;
  }.property(),

  /**
   * clear and set properties to default value
   */
  clearStep: function () {
    this.set('dataIsLoaded', false);
    this.set('filter', '');
    this.get('filterColumns').setEach('selected', false);
    this.get('stepConfigs').clear();
    this.get('globalConfigs').clear();
    this.get('uiConfigs').clear();
    this.get('customConfig').clear();
    this.set('loadedHostToOverrideSiteToTagMap', {});
    this.set('savedHostToOverrideSiteToTagMap', {});
    this.set('savedSiteNameToServerServiceConfigDataMap', {});
    if (this.get('serviceConfigTags')) {
      this.set('serviceConfigTags', null);
    }
  },

  serviceConfigProperties: function () {
    return App.db.getServiceConfigProperties();
  }.property('content'),

  /**
   * On load function
   */
  loadStep: function () {
    console.log("TRACE: Loading configure for service");
    this.clearStep();
    this.loadServiceConfigs();
  },

  /**
   * Loads the actual configuration of all host components. 
   * This helps in determining which services need a restart, and also
   * in showing which properties are actually applied or not. 
   * This method also compares the actual_configs with the desired_configs
   * and builds a diff structure.
   * 
   * Internall it calculates an array of host-components which need restart.
   * Example:
   * [
   *  {
   *    componentName: 'DATANODE',
   *    serviceName: 'HDFS',
   *    host: 'host.name',
   *    type: 'core-site',
   *    desiredConfigTags: {tag:'version1'},
   *    actualConfigTags: {tag:'version4'. host_override:'version2'}
   *  },
   *  ...
   * ]
   * 
   * From there it return the following restart-data for this service.
   * It represents the hosts, whose components need restart, and the
   * properties which require restart.
   * 
   * {
   *  hostAndHostComponents: {
   *   'hostname1': {
   *     'DATANODE': {
   *       'property1': 'value1',
   *       'property2': 'value2'
   *     },
   *     'TASKTRACKER': {
   *       'prop1': 'val1'
   *     }
   *    },
   *    'hostname6': {
   *     'ZOOKEEPER': {
   *       'property1': 'value3'
   *     }
   *    }
   *  },
   *  propertyToHostAndComponent: {
   *    'property1': {
   *      'hostname1': ['DATANODE'],
   *      'hostname6': ['ZOOKEEPER']
   *    },
   *    'property2': {
   *      'hostname1': ['DATANODE']
   *    },
   *    'prop1': {
   *      'hostname1': ['TASKTRACKER']
   *    }
   *  }
   * }
   */
  loadActualConfigsAndCalculateRestarts: function () {
    var currentService = this.get('content.serviceName');
    var restartData = {
        hostAndHostComponents: {},
        propertyToHostAndComponent: {}
    };
    var self = this;
    var actualConfigsUrl = this.getUrl('/data/services/host_component_actual_configs.json', '/services?fields=components/host_components/HostRoles/actual_configs');
    $.ajax({
      type: 'GET',
      url: actualConfigsUrl,
      async: false,
      timeout: 10000,
      dataType: 'json',
      success: function (data) {
        var diffHostComponents = [];
        console.debug("loadActualConfigs(" + actualConfigsUrl + "): Data=", data);
        var configsToDownload = [];
        data.items.forEach(function (service) {
          // For current service, do any of the host_components differ in
          // configuration?
          if (currentService === service.ServiceInfo.service_name) {
            service.components.forEach(function (serviceComponent) {
              serviceComponent.host_components.forEach(function (hostComponent) {
                if (hostComponent.HostRoles.actual_configs) {
                  for ( var site in hostComponent.HostRoles.actual_configs) {
                    var actualConfigsTags = hostComponent.HostRoles.actual_configs[site];
                    var desiredConfigTags = self.getDesiredConfigTag(site, hostComponent.HostRoles.host_name);
                    if (desiredConfigTags.tag !== actualConfigsTags.tag || 
                        (desiredConfigTags.host_override != null && 
                            actualConfigsTags.host_override != null && 
                            desiredConfigTags.host_override !== actualConfigsTags.host_override)) {
                      // Restart may be necessary for this host-component
                      diffHostComponents.push({
                        componentName: hostComponent.HostRoles.component_name,
                        serviceName: serviceComponent.ServiceComponentInfo.service_name,
                        host: hostComponent.HostRoles.host_name,
                        type: site,
                        desiredConfigTags: desiredConfigTags,
                        actualConfigTags: actualConfigsTags
                      });
                      self.addConfigDownloadParam(site, actualConfigsTags.tag, configsToDownload);
                      self.addConfigDownloadParam(site, actualConfigsTags.host_override, configsToDownload);
                      self.addConfigDownloadParam(site, desiredConfigTags.tag, configsToDownload);
                      self.addConfigDownloadParam(site, desiredConfigTags.host_override, configsToDownload);
                    }
                  }
                }
              });
            });
          }
        });
        if (configsToDownload.length > 0) {
          var url = self.getUrl('/data/configurations/cluster_level_actual_configs.json?' + configsToDownload.join('|'), '/configurations?' + configsToDownload.join('|'));
          $.ajax({
            type: 'GET',
            url: url,
            async: false,
            timeout: 10000,
            dataType: 'json',
            success: function (data) {
              console.log("configsToDownload(): In success for ", url);
              if (data.items) {
                data.items.forEach(function (item) {
                  App.config.loadedConfigurationsCache[item.type + "_" + item.tag] = item.properties;
                });
              }
            },
            error: function (request, ajaxOptions, error) {
              console.log("TRACE: In error function for the configsToDownload call");
              console.log("TRACE: value of the url is: " + url);
              console.log("TRACE: error code status is: " + request.status);
            },
            statusCode: require('data/statusCodes')
          });
        }
        // Now all the configurations are loaded.
        // Find the diff in properties
        if (diffHostComponents.length > 0) {
          diffHostComponents.forEach(function (diffHostComponent) {
            var actualConfigs = App.config.loadedConfigurationsCache[diffHostComponent.type + "_" + diffHostComponent.actualConfigTags.tag];
            var desiredConfigs = App.config.loadedConfigurationsCache[diffHostComponent.type + "_" + diffHostComponent.desiredConfigTags.tag];
            var diffs = self.getConfigDifferences(actualConfigs, desiredConfigs);
            if (!jQuery.isEmptyObject(diffs)) {
              var skip = false;
              if (diffHostComponent.type == 'global') {
                if(!App.config.isServiceEffectedByGlobalChange(
                    diffHostComponent.serviceName, 
                    diffHostComponent.desiredConfigTags.tag, 
                    diffHostComponent.actualConfigTags.tag)){
                  skip = true;
                }
              }
              if(!skip){
                // Populate restartData.hostAndHostComponents
                if (!(diffHostComponent.host in restartData.hostAndHostComponents)) {
                  restartData.hostAndHostComponents[diffHostComponent.host] = {};
                }
                if (!(diffHostComponent.componentName in restartData.hostAndHostComponents[diffHostComponent.host])) {
                  restartData.hostAndHostComponents[diffHostComponent.host][diffHostComponent.componentName] = {};
                }
                jQuery.extend(restartData.hostAndHostComponents[diffHostComponent.host][diffHostComponent.componentName], diffs);

                // Populate restartData.propertyToHostAndComponent
                for ( var diff in diffs) {
                  if (!(diff in restartData.propertyToHostAndComponent)) {
                    restartData.propertyToHostAndComponent[diff] = {};
                  }
                  if (!(diffHostComponent.host in restartData.propertyToHostAndComponent[diff])) {
                    restartData.propertyToHostAndComponent[diff][diffHostComponent.host] = [];
                  }
                  if (!(restartData.propertyToHostAndComponent[diff][diffHostComponent.host].contains(diffHostComponent.componentName))) {
                    restartData.propertyToHostAndComponent[diff][diffHostComponent.host].push(diffHostComponent.componentName);
                  }
                }
              }
            }
          });
        }
        console.log("loadActualConfigs(): Finished loading. Restart host components = ", diffHostComponents);
      },
      error: function (request, ajaxOptions, error) {
        console.log("loadActualConfigs(): URL:" + actualConfigsUrl + ". Status:", request.status, ", Error:", error);
      },
      statusCode: require('data/statusCodes')
    });
    console.log("loadActualConfigsAndCalculateRestarts(): Restart data = ", restartData);
    return restartData;
  },
  
  /**
   * Determines the differences between desired and actual configs and returns
   * them as an object. The key is the property, and value is actual_config.
   */
  getConfigDifferences: function (actualConfigs, desiredConfigs) {
    var differences = {};
    if (actualConfigs != null && desiredConfigs != null) {
      for(var desiredProp in desiredConfigs){
        if(desiredConfigs[desiredProp] !== actualConfigs[desiredProp]){
          differences[desiredProp] = actualConfigs[desiredProp];
        }
      }
    }
    return differences;
  },
  
  addConfigDownloadParam: function(site, tag, configsToDownload){
    if(tag!=null && !(site+"_"+tag in App.config.loadedConfigurationsCache)){
      var configParam = "(type="+site+"&tag="+tag+")";
      if(!configsToDownload.contains(configParam)){
        configsToDownload.push(configParam);
      }
    }
  },
  
  getDesiredConfigTag: function(site, hostName){
    var tag = {tag: this.loadedClusterSiteToTagMap[site], host_override: null};
    if(hostName in this.loadedHostToOverrideSiteToTagMap){
      var map = this.loadedHostToOverrideSiteToTagMap[hostName];
      if(site in map){
        tag.host_overrides = map[site];
      }
    }
    return tag;
  },

  /**
   * Loads service configurations
   */
  loadServiceConfigs: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        serviceName: this.get('content.serviceName'),
        serviceConfigsDef: this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName'))
      },
      success: 'loadServiceTagsSuccess'
    });
  },

  loadServiceTagsSuccess: function(data, opt, params){
    var serviceConfigsDef = params.serviceConfigsDef;
    var serviceName = this.get('content.serviceName');
    console.debug("loadServiceConfigs(): data=", data);

    this.loadedClusterSiteToTagMap = {};
    //STEP 1: handle tags from JSON data
    for ( var site in data.Clusters.desired_configs) {
      if (serviceConfigsDef.sites.indexOf(site) > -1) {
        this.loadedClusterSiteToTagMap[site] = data.Clusters.desired_configs[site]['tag'];
        var overrides = data.Clusters.desired_configs[site].host_overrides;
        if (overrides) {
          overrides.forEach(function (override) {
            var hostname = override.host_name;
            var tag = override.tag;
            if(!this.loadedHostToOverrideSiteToTagMap[hostname]){
              this.loadedHostToOverrideSiteToTagMap[hostname] = {};
            }
            this.loadedHostToOverrideSiteToTagMap[hostname][site] = tag;
          }, this);
        }
      }
    }
    //STEP 2: Create an array of objects defining tag names to be polled and new tag names to be set after submit
    this.setServiceConfigTags(this.loadedClusterSiteToTagMap);
    //STEP 3: Load advanced configs from server
    var advancedConfigs = App.config.loadAdvancedConfig(serviceName) || [];
    //STEP 4: Load on-site config by service from server
    var configGroups = App.config.loadConfigsByTags(this.get('serviceConfigTags'));
    //STEP 5: Merge global and on-site configs with pre-defined
    var configSet = App.config.mergePreDefinedWithLoaded(configGroups, advancedConfigs, this.get('serviceConfigTags'), serviceName);

    //var serviceConfigs = this.getSitesConfigProperties(advancedConfigs);
    var configs = configSet.configs;
    //put global configs into globalConfigs to save them separately
    this.set('globalConfigs', configSet.globalConfigs);

    //STEP 6: add advanced configs
    App.config.addAdvancedConfigs(configs, advancedConfigs, serviceName);
    //STEP 7: add custom configs
    App.config.addCustomConfigs(configs);
    //STEP 8: add configs as names of host components
    this.addHostNamesToGlobalConfig();

    var allConfigs = this.get('globalConfigs').concat(configs);
    //this.loadServiceConfigHostsOverrides(serviceConfigs, this.loadedHostToOverrideSiteToTagMap);
    //STEP 9: Load and add host override configs
    App.config.loadServiceConfigHostsOverrides(allConfigs, this.loadedHostToOverrideSiteToTagMap);
    var restartData = this.loadActualConfigsAndCalculateRestarts();
    //STEP 10: creation of serviceConfig object which contains configs for current service
    var serviceConfig = App.config.createServiceConfig(serviceName);
    this.checkForRestart(serviceConfig, restartData);
    if (serviceName || serviceConfig.serviceName === 'MISC') {
    //STEP 11: render configs and wrap each in ServiceConfigProperty object
      this.loadComponentConfigs(allConfigs, serviceConfig, restartData);
      this.get('stepConfigs').pushObject(serviceConfig);
    }
    this.set('selectedService', this.get('stepConfigs').objectAt(0));
    this.set('dataIsLoaded', true);
  },
  /**
   * Changes format from Object to Array
   * 
   * {
   *  'core-site': 'version1',
   *  'hdfs-site': 'version1',
   *  'global': 'version2',
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
   * check whether host component must be restarted
   * @param serviceConfig
   * @param restartData
   */
  checkForRestart: function(serviceConfig, restartData){
    var hostsCount = 0;
    var hostComponentCount = 0;
    var restartHosts = Ember.A([]);
    if(restartData != null && restartData.hostAndHostComponents != null && !jQuery.isEmptyObject(restartData.hostAndHostComponents)){
      serviceConfig.set('restartRequired', true);
      for(var host in restartData.hostAndHostComponents){
        hostsCount++;
        var componentsArray = Ember.A([]);
        for(var component in restartData.hostAndHostComponents[host]){
          componentsArray.push(Ember.Object.create({name: App.format.role(component)}));
          hostComponentCount++;
        }
        var hostObj = App.Host.find(host);
        restartHosts.push(Ember.Object.create({hostData: hostObj, components: componentsArray}))
      }
      serviceConfig.set('restartRequiredHostsAndComponents', restartHosts);
      serviceConfig.set('restartRequiredMessage', 'Service needs '+hostComponentCount+' components on ' + hostsCount +' hosts to be restarted.')
    }
  },

  /**
   * Load child components to service config object
   * @param configs
   * @param componentConfig
   * @param restartData
   */
  loadComponentConfigs: function (configs, componentConfig, restartData) {
    configs.forEach(function (_serviceConfigProperty) {
      console.log("config", _serviceConfigProperty);
      if (!_serviceConfigProperty) return;
      var overrides = _serviceConfigProperty.overrides;
      // we will populate the override properties below
      _serviceConfigProperty.overrides = null;

      if (_serviceConfigProperty.isOverridable === undefined) {
        _serviceConfigProperty.isOverridable = true;
      }
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      var propertyName = serviceConfigProperty.get('name');
      if (restartData != null && propertyName in restartData.propertyToHostAndComponent) {
        serviceConfigProperty.set('isRestartRequired', true);
        var message = '<ul>';
        for(var host in restartData.propertyToHostAndComponent[propertyName]){
          var appHost = App.Host.find(host);
          message += "<li>"+appHost.get('publicHostName');
          message += "<ul>";
          restartData.propertyToHostAndComponent[propertyName][host].forEach(function(comp){
            message += "<li>"+App.format.role(comp)+"</li>"
          });
          message += "</ul></li>";
        }
        message += "</ul>";
        serviceConfigProperty.set('restartRequiredMessage', message);
      }
      if (serviceConfigProperty.get('serviceName') === this.get('content.serviceName')) {
        // serviceConfigProperty.serviceConfig = componentConfig;
        if (App.get('isAdmin')) {
          serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
        } else {
          serviceConfigProperty.set('isEditable', false);
        }

        console.log("config result", serviceConfigProperty);
      } else {
        serviceConfigProperty.set('isVisible', false);
      }
      if (overrides != null) {
        for(var overridenValue in overrides){
          var hostsArray = overrides[overridenValue];
          var newSCP = App.ServiceConfigProperty.create(_serviceConfigProperty);
          newSCP.set('value', overridenValue);
          newSCP.set('isOriginalSCP', false); // indicated this is overridden value,
          newSCP.set('parentSCP', serviceConfigProperty);
          newSCP.set('selectedHostOptions', Ember.A(hostsArray));
          var parentOverridesArray = serviceConfigProperty.get('overrides');
          if(parentOverridesArray == null){
            parentOverridesArray = Ember.A([]);
            serviceConfigProperty.set('overrides', parentOverridesArray);
          }
          parentOverridesArray.pushObject(newSCP);
          console.debug("createOverrideProperty(): Added:", newSCP, " to main-property:", serviceConfigProperty)
        }
      }
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
  },

  /**
   * Determines which host components are running on each host.
   * @return Returned in the following format:
   * {
   *  runningHosts: {
   *    'hostname1': 'NameNode, DataNode, JobTracker',
   *    'hostname2': 'DataNode',
   *  },
   *  runningComponentCount: 5
   * }
   */
  getRunningHostComponents: function (services) {
    var runningHosts = [];
    var runningComponentCount = 0;
    var hostToIndexMap = {};
    services.forEach(function (service) {
      var runningHostComponents = service.get('runningHostComponents');
      if (runningHostComponents != null) {
        runningHostComponents.forEach(function (hc) {
          var hostName = hc.get('host.publicHostName');
          var componentName = hc.get('displayName');
          runningComponentCount++;
          if (!(hostName in hostToIndexMap)) {
            runningHosts.push({
              name: hostName,
              components: ""
            });
            hostToIndexMap[hostName] = runningHosts.length - 1;
          }
          var hostObj = runningHosts[hostToIndexMap[hostName]];
          if (hostObj.components.length > 0)
            hostObj.components += ", " + componentName;
          else
            hostObj.components += componentName;
        });
        runningHosts.sort(function (a, b) {
          return a.name.localeCompare(b.name);
        });
      }
    });
    return {
      runningHosts: runningHosts,
      runningComponentCount: runningComponentCount
    };
  },
  
  /**
   * open popup with appropriate message
   */
  restartServicePopup: function (event) {
    if(this.get("isSubmitDisabled")){
      return;
    }
    var header;
    var message;
    var messageClass;
    var value;
    var flag = false;
    var runningHosts = null;
    var runningComponentCount = 0;

    var dfd = $.Deferred();
    var self = this;
    var serviceName = this.get('content.serviceName');
    var displayName = this.get('content.displayName');

    if (App.supports.hostOverrides || 
        (serviceName !== 'HDFS' && this.get('content.isStopped') === true) ||
        ((serviceName === 'HDFS') && this.get('content.isStopped') === true && (!App.Service.find().someProperty('id', 'MAPREDUCE') || App.Service.find('MAPREDUCE').get('isStopped')))) {

      var dirChanged = false;

      if (serviceName === 'HDFS') {
        var hdfsConfigs = self.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs');
        if (
          hdfsConfigs.findProperty('name', 'dfs_name_dir').get('isNotDefaultValue') ||
          hdfsConfigs.findProperty('name', 'fs_checkpoint_dir').get('isNotDefaultValue') ||
          hdfsConfigs.findProperty('name', 'dfs_data_dir').get('isNotDefaultValue')
        ) {
          dirChanged = true;
        }
      } else if (serviceName === 'MAPREDUCE') {
        var mapredConfigs = self.get('stepConfigs').findProperty('serviceName', 'MAPREDUCE').get('configs');
        if (
          mapredConfigs.findProperty('name', 'mapred_local_dir').get('isNotDefaultValue') ||
          mapredConfigs.findProperty('name', 'mapred_system_dir').get('isNotDefaultValue')
        ) {
          dirChanged = true;
        }
      }

      if (dirChanged) {
        App.showConfirmationPopup(function() {
          dfd.resolve();
        }, Em.I18n.t('services.service.config.confirmDirectoryChange').format(displayName));
      } else {
        dfd.resolve();
      }

      dfd.done(function() {
        var result = self.saveServiceConfigProperties();
        App.router.get('clusterController').updateClusterData();
        flag = result.flag;
        if (result.flag === true) {
          header = Em.I18n.t('services.service.config.saved');
          message = Em.I18n.t('services.service.config.saved.message');
          messageClass = 'alert alert-success';
        } else {
          header = Em.I18n.t('common.failure');
          message = result.message;
          messageClass = 'alert alert-error';
          value = result.value;
        }
      });
    } else {
      var rhc;
      if (this.get('content.serviceName') !== 'HDFS' || (this.get('content.serviceName') === 'HDFS' && !App.Service.find().someProperty('id', 'MAPREDUCE'))) {
        rhc = this.getRunningHostComponents([this.get('content')]);
        header = Em.I18n.t('services.service.config.notSaved');
        message = Em.I18n.t('services.service.config.msgServiceStop');
      } else {
        rhc = this.getRunningHostComponents([this.get('content'), App.Service.find('MAPREDUCE')]);
        header = Em.I18n.t('services.service.config.notSaved');
        message = Em.I18n.t('services.service.config.msgHDFSMapRServiceStop');
      }
      messageClass = 'alert alert-error';
      runningHosts = rhc.runningHosts;
      runningComponentCount = rhc.runningComponentCount;
      dfd.resolve();
    }

    dfd.done(function () {
      App.ModalPopup.show({
        header: header,
        primary: Em.I18n.t('ok'),
        secondary: null,
        onPrimary: function () {
          this.hide();
          if (flag) {
            self.loadStep();
          }
        },
        bodyClass: Ember.View.extend({
          flag: flag,
          message: message,
          messageClass: messageClass,
          runningHosts: runningHosts,
          runningComponentCount: runningComponentCount,
          siteProperties: value,
          getDisplayMessage: function () {
            var displayMsg = [];
            var siteProperties = this.get('siteProperties');
            if (siteProperties) {
              siteProperties.forEach(function (_siteProperty) {
                var displayProperty = _siteProperty.siteProperty;
                var displayNames = _siteProperty.displayNames;
                /////////
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
          getRunningHostsMessage: function () {
            return Em.I18n.t('services.service.config.stopService.runningHostComponents').format(this.get('runningComponentCount'), this.get('runningHosts.length'));
          }.property('runningComponentCount', 'runningHosts.length'),
          template: Ember.Handlebars.compile([
            '<div class="{{unbound view.messageClass}}" style="margin-bottom:0">{{view.message}}</div>',
            '{{#unless view.flag}}',
            ' <br/>',
            ' <div class="pre-scrollable" style="max-height: 250px;">',
            '   <ul>',
            '   {{#each val in view.getDisplayMessage}}',
            '     <li>',
            '       {{val}}',
            '     </li>',
            '   {{/each}}',
            '   </ul>',
            ' </div>',
            '{{/unless}}',
            '{{#if view.runningHosts}}',
            ' <i class="icon-warning-sign"></i>  {{view.getRunningHostsMessage}}',
            ' <table class="table-striped running-host-components-table">',
            '   <tr><th>{{t common.host}}</th><th>{{t common.components}}</th></tr>',
            '   {{#each host in view.runningHosts}}',
            '     <tr><td>{{host.name}}</td><td>{{host.components}}</td></tr>',
            '   {{/each}}',
            ' </table>',
            '{{/if}}'
          ].join('\n'))
        })
      })
    });
  },

  /**
   * Save config properties
   */
  saveServiceConfigProperties: function () {
    var result = {
      flag: false,
      message: null,
      value: null
    };
    this.savedHostToOverrideSiteToTagMap = {};
    var configs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    this.saveGlobalConfigs(configs);
    this.saveSiteConfigs(configs);

    /**
     * First we put cluster configurations, which automatically creates /configurations
     * resources. Next we update host level overrides.
     */
    result.flag = this.doPUTClusterConfigurations();
    if (!result.flag) {
      result.message = Em.I18n.t('services.service.config.failSaveConfig');
    }else{
      result.flag = result.flag && this.doPUTHostOverridesConfigurationSites();
      if (!result.flag) {
        result.message = Em.I18n.t('services.service.config.failSaveConfigHostExceptions');
      }
    }
    console.log("The result from applyCreatdConfToService is: " + result);
    return result;
  },

  /**
   * save new or change exist configs in global configs
   * @param configs
   */
  saveGlobalConfigs: function (configs) {
    var globalConfigs = this.get('globalConfigs');
    configs.filterProperty('id', 'puppet var').forEach(function (uiConfigProperty) {
      if (globalConfigs.someProperty('name', uiConfigProperty.name)) {
        var modelGlobalConfig = globalConfigs.findProperty('name', uiConfigProperty.name);
        modelGlobalConfig.value = uiConfigProperty.value;
        var uiOverrides = uiConfigProperty.get('overrides');
        if(uiOverrides!=null && uiOverrides.get('length')>0){
          modelGlobalConfig.overrides = {};
          uiOverrides.forEach(function(uiOverride){
            var value = uiOverride.get('value');
            modelGlobalConfig.overrides[value] = [];
            uiOverride.get('selectedHostOptions').forEach(function(host){
              modelGlobalConfig.overrides[value].push(host);
            });
          });
        }
      } else {
        globalConfigs.pushObject({
          name: uiConfigProperty.name,
          value: uiConfigProperty.value
        });
      }
    }, this);

    this.setHiveHostName(globalConfigs);
    this.setOozieHostName(globalConfigs);
    this.set('globalConfigs', globalConfigs);
  },

  /**
   * set hive hostnames in global configs
   * @param globals
   */
  setHiveHostName: function (globals) {
    if (globals.someProperty('name', 'hive_database')) {
      var hiveDb = globals.findProperty('name', 'hive_database');
      if (hiveDb.value === 'New MySQL Database') {
        var ambariHost = globals.findProperty('name', 'hive_ambari_host');
        if (ambariHost) {
          ambariHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_database'));
      } else if (hiveDb.value === 'Existing MySQL Database'){
        var existingMySqlHost = globals.findProperty('name', 'hive_existing_mysql_host');
        if (existingMySqlHost) {
          existingMySqlHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_database'));
      } else { //existing oracle database
        var existingOracleHost = globals.findProperty('name', 'hive_existing_oracle_host');
        if (existingOracleHost) {
          existingOracleHost.name = 'hive_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_database'));
      }

    }
  },

  /**
   * set oozie hostnames in global configs
   * @param globals
   */
  setOozieHostName: function (globals) {
    if (globals.someProperty('name', 'oozie_database')) {
      var oozieDb = globals.findProperty('name', 'oozie_database');
      if (oozieDb.value === 'New Derby Database'){
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
      } else if (oozieDb.value === 'New MySQL Database') {
        var ambariHost = globals.findProperty('name', 'oozie_ambari_host');
        if (ambariHost) {
          ambariHost.name = 'oozie_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_derby_database'));

      } else if (oozieDb.value === 'Existing MySQL Database') {
        var existingMySqlHost = globals.findProperty('name', 'oozie_existing_mysql_host');
        if (existingMySqlHost) {
          existingMySqlHost.name = 'oozie_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_derby_database'));
      } else{ //existing oracle database
        var existingOracleHost = globals.findProperty('name', 'oozie_existing_oracle_host');
        if (existingOracleHost) {
          existingOracleHost.name = 'oozie_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_derby_database'));
      }

    }
  },

  /**
   * save site configs
   * @param configs
   */
  saveSiteConfigs: function (configs) {
    //storedConfigs contains custom configs as well
    var serviceConfigProperties = configs.filterProperty('id', 'site property');
    serviceConfigProperties.forEach(function(_config){
      if(typeof _config.get('value') === "boolean") _config.set('value', _config.value.toString());
    });
    var storedConfigs = serviceConfigProperties.filterProperty('value');
    var allUiConfigs = this.loadUiSideConfigs(this.get('configMapping').all());
    this.set('uiConfigs', storedConfigs.concat(allUiConfigs));
  },

  /**
   * return configs from the UI side
   * @param configMapping array with configs
   * @return {Array}
   */
  loadUiSideConfigs: function (configMapping) {
    var uiConfig = [];
    var configs = configMapping.filterProperty('foreignKey', null);
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
   * return global config value
   * @param templateName
   * @param expression
   * @param name
   * @return {
   *   value: '...',
   *   overrides: {
   *    'value1': [h1, h2],
   *    'value2': [h3]
   *   }
   * }
   */
  getGlobConfigValueWithOverrides: function (templateName, expression, name) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (express == null) {
      return { value : expression, overrides: {}};      // if site property do not map any global property then return the value
    }
    var overrideHostToValue = {};
    express.forEach(function (_express) {
      //console.log("The value of template is: " + _express);
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('globalConfigs').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globalObj = this.get('globalConfigs').findProperty('name', templateName[index]);
        var globValue = globalObj.value;
        // Hack for templeton.zookeeper.hosts
        var preReplaceValue = null;
        if (value !== null) {   // if the property depends on more than one template name like <templateName[0]>/<templateName[1]> then don't proceed to the next if the prior is null or not found in the global configs
          preReplaceValue = value;
          value = this._replaceConfigValues(name, _express, value, globValue);
        }
        if(globalObj.overrides!=null){
          for(ov in globalObj.overrides){
            var hostsArray = globalObj.overrides[ov];
            hostsArray.forEach(function(host){
              if(!(host in overrideHostToValue)){
                overrideHostToValue[host] = this._replaceConfigValues(name, _express, preReplaceValue, ov);
              }else{
                overrideHostToValue[host] = this._replaceConfigValues(name, _express, overrideHostToValue[host], ov);
              }
            }, this);
          }
        }
      } else {
        /*
         console.log("ERROR: The variable name is: " + templateName[index]);
         console.log("ERROR: mapped config from configMapping file has no corresponding variable in " +
         "content.serviceConfigProperties. Two possible reasons for the error could be: 1) The service is not selected. " +
         "and/OR 2) The service_config metadata file has no corresponding global var for the site property variable");
         */
        value = null;
      }
    }, this);

    var valueWithOverrides = {
        value: value,
        overrides: {}
    };
    if(!jQuery.isEmptyObject(overrideHostToValue)){
      for(var host in overrideHostToValue){
        var hostVal = overrideHostToValue[host];
        if(!(hostVal in valueWithOverrides.overrides)){
          valueWithOverrides.overrides[hostVal] = [];
        }
        valueWithOverrides.overrides[hostVal].push(host);
      }
    }
    return valueWithOverrides;
  },
  
  _replaceConfigValues: function (name, express, value, globValue) {
    if (name === "templeton.zookeeper.hosts" || name === 'hbase.zookeeper.quorum') {
      var zooKeeperPort = '2181';
      if (typeof globValue === 'string') {
        var temp = [];
        temp.push(globValue);
        globValue = temp;
      }
      if (name === "templeton.zookeeper.hosts") {
        var temp = [];
        globValue.forEach(function (_host, index) {
          temp.push(globValue[index] + ':' + zooKeeperPort);
        }, this);
        globValue = temp;
      }
      value = value.replace(express, globValue.toString());
    } else {
      value = value.replace(express, globValue);
    }
    return value;
  },

  /**
   * Saves cluster level configurations for all necessary sites.
   * PUT calls are made to /api/v1/clusters/clusterName for each site.
   * 
   * @return {Boolean}
   */
  doPUTClusterConfigurations: function () {
    var result = true;
    var serviceConfigTags = this.get('serviceConfigTags');
    this.setNewTagNames(serviceConfigTags);
    var siteNameToServerDataMap = {};
    
    serviceConfigTags.forEach(function (_serviceTags) {
      if (_serviceTags.siteName === 'global') {
        console.log("TRACE: Inside global");
        var serverGlobalConfigs = this.createGlobalSiteObj(_serviceTags.newTagName);
        siteNameToServerDataMap['global'] = serverGlobalConfigs;
        if(this.isConfigChanged(App.config.loadedConfigurationsCache['global_'+this.loadedClusterSiteToTagMap['global']], serverGlobalConfigs.properties)){
          result = result && this.doPUTClusterConfigurationSite(serverGlobalConfigs);
        }
      } else if (_serviceTags.siteName === 'core-site') {
        console.log("TRACE: Inside core-site");
        if (this.get('content.serviceName') === 'HDFS') {
          var coreSiteConfigs = this.createCoreSiteObj(_serviceTags.newTagName);
          siteNameToServerDataMap['core-site'] = coreSiteConfigs;
          if(this.isConfigChanged(App.config.loadedConfigurationsCache['core-site_'+this.loadedClusterSiteToTagMap['core-site']], coreSiteConfigs.properties)){
            result = result && this.doPUTClusterConfigurationSite(coreSiteConfigs);
          }
        }
      } else {
        var serverConfigs = this.createSiteObj(_serviceTags.siteName, _serviceTags.newTagName);
        siteNameToServerDataMap[_serviceTags.siteName] = serverConfigs;
        if(this.isConfigChanged(App.config.loadedConfigurationsCache[_serviceTags.siteName+'_'+this.loadedClusterSiteToTagMap[_serviceTags.siteName]], serverConfigs.properties)){
          result = result && this.doPUTClusterConfigurationSite(serverConfigs);
        }
     }
    }, this);
    this.savedSiteNameToServerServiceConfigDataMap = siteNameToServerDataMap;
    return result;
  },
  

  /**
   * Compares the loaded config values with the saving config values.
   */
  isConfigChanged: function (loadedConfig, savingConfig) {
    var changed = false;
    if (loadedConfig != null && savingConfig != null) {
      var seenLoadKeys = [];
      for ( var loadKey in loadedConfig) {
        seenLoadKeys.push(loadKey);
        var loadValue = loadedConfig[loadKey];
        var saveValue = savingConfig[loadKey];
        if("boolean" == typeof(saveValue)){
          saveValue = saveValue.toString();
        }
        if(saveValue==null){
          saveValue = "null";
        }
        if (loadValue !== saveValue) {
          changed = true;
          break;
        }
      }
      for ( var saveKey in savingConfig) {
        if (seenLoadKeys.indexOf(saveKey) < 0) {
          changed = true;
          break;
        }
      }
    }
    return changed;
  },
  
  /**
   * Saves configuration of a particular site. The provided data
   * contains the site name and tag to be used.
   */
  doPUTClusterConfigurationSite: function (data) {
    var result;
    var url = this.getUrl('', '');
    var clusterData = {
        Clusters: {
          desired_config: data
        }
    };
    console.log("applyClusterConfigurationToSite(): PUTting data:", clusterData);
    $.ajax({
      type: 'PUT',
      url: url,
      async: false,
      dataType: 'text',
      data: JSON.stringify(clusterData),
      timeout: 5000,
      success: function (data) {
        console.log("applyClusterConfigurationToSite(): In success for data:", data);
        result = true;
      },
      error: function (request, ajaxOptions, error) {
        console.log('applyClusterConfigurationToSite(): ERROR:', request.responseText, ", error=", error);
        result = false;
      },
      statusCode: require('data/statusCodes')
    });
    console.log("applyClusterConfigurationToSite(): Exiting with result=" + result);
    return result;
  },


  /**
   * Creates host level overrides for service configuration.
   * 
   */
  doPUTHostOverridesConfigurationSites: function(){
    var singlePUTHostData = [];
    var savedHostSiteArray = [];
    for ( var host in this.savedHostToOverrideSiteToTagMap) {
      for ( var siteName in this.savedHostToOverrideSiteToTagMap[host]) {
        var tagName = this.savedHostToOverrideSiteToTagMap[host][siteName].tagName;
        var map = this.savedHostToOverrideSiteToTagMap[host][siteName].map;
        savedHostSiteArray.push(host+"///"+siteName);
        singlePUTHostData.push({
          RequestInfo: {
            query: 'Hosts/host_name='+host
          },
          Body: {
            Hosts: {
              desired_config: {
                type: siteName,
                tag: tagName,
                properties: map
              }
            }
          }
        });
      }
    }
    // Now cleanup removed overrides
    for ( var loadedHost in this.loadedHostToOverrideSiteToTagMap) {
      for ( var loadedSiteName in this.loadedHostToOverrideSiteToTagMap[loadedHost]) {
        if (!(savedHostSiteArray.contains(loadedHost + "///" + loadedSiteName))) {
          // This host-site combination was loaded, but not saved.
          // Meaning it is not needed anymore. Hence send a DELETE command.
          singlePUTHostData.push({
            RequestInfo: {
              query: 'Hosts/host_name='+loadedHost
            },
            Body: {
              Hosts: {
                desired_config: {
                  type: loadedSiteName,
                  tag: this.loadedHostToOverrideSiteToTagMap[loadedHost][loadedSiteName],
                  selected: false
                }
              }
            }
          });
        }
      }
    }
    console.debug("createHostOverrideConfigSites(): PUTting host-overrides. Data=",singlePUTHostData);
    if(singlePUTHostData.length>0){
      var url = this.getUrl('', '/hosts');
      var hostOverrideResult = true;
      $.ajax({
        type: 'PUT',
        url: url,
        data: JSON.stringify(singlePUTHostData),
        async: false,
        dataType: 'text',
        timeout: 5000,
        success: function (data) {
          var jsonData = jQuery.parseJSON(data);
          hostOverrideResult = true;
          console.log("createHostOverrideConfigSites(): SUCCESS:", url, ". RESPONSE:",jsonData);
        },
        error: function (request, ajaxOptions, error) {
          hostOverrideResult = false;
          console.log("createHostOverrideConfigSites(): ERROR:", url, ". RESPONSE:",request.responseText);
        },
        statusCode: require('data/statusCodes')
      });
      return hostOverrideResult;
    }
    return true;
  },
   
  /**
   * add newTagName property to each config in serviceConfigs
   * @param serviceConfigs
   */
  setNewTagNames: function (serviceConfigs) {
    var time = (new Date).getTime();
    serviceConfigs.forEach(function (_serviceConfigs) {
      _serviceConfigs.newTagName = 'version' + time;
    }, this);
  },

  /**
   * create global site object
   * @param tagName
   * @return {Object}
   */
  createGlobalSiteObj: function (tagName) {
    var globalSiteProperties = {};
    this.get('globalConfigs').forEach(function (_globalSiteObj) {
      // do not pass any globalConfigs whose name ends with _host or _hosts
      if (!/_hosts?$/.test(_globalSiteObj.name)) {
        // append "m" to JVM memory options except for hadoop_heapsize
        if (/_heapsize|_newsize|_maxnewsize$/.test(_globalSiteObj.name) && _globalSiteObj.name !== 'hadoop_heapsize') {
          _globalSiteObj.value += "m";
        }
        globalSiteProperties[_globalSiteObj.name] = _globalSiteObj.value;
        this.recordHostOverride(_globalSiteObj, 'global', tagName, this);
        //console.log("TRACE: name of the global property is: " + _globalSiteObj.name);
        //console.log("TRACE: value of the global property is: " + _globalSiteObj.value);
      }
    }, this);
    return {"type": "global", "tag": tagName, "properties": globalSiteProperties};
  },
  
  recordHostOverride: function(serviceConfigObj, siteName, tagName, self){
    if('get' in serviceConfigObj){
      return this._recordHostOverrideFromEmberObj(serviceConfigObj, siteName, tagName, self);
    }else{
      return this._recordHostOverrideFromObj(serviceConfigObj, siteName, tagName, self);
    }
  },
  
  /**
   * Records all the host overrides per site/tag
   */
  _recordHostOverrideFromObj: function(serviceConfigObj, siteName, tagName, self){
    var overrides = serviceConfigObj.overrides;
    if(overrides){
      for(var value in overrides){
        overrides[value].forEach(function(host){
          if(!(host in self.savedHostToOverrideSiteToTagMap)){
            self.savedHostToOverrideSiteToTagMap[host] = {};
          }
          if(!(siteName in self.savedHostToOverrideSiteToTagMap[host])){
            self.savedHostToOverrideSiteToTagMap[host][siteName] = {};
            self.savedHostToOverrideSiteToTagMap[host][siteName].map = {};
          }
          var finalTag = tagName + '_' + host;
          console.log("recordHostOverride(): Saving host override for host="+host+", site="+siteName+", tag="+finalTag+", (key,value)=("+serviceConfigObj.name+","+value+")");
          self.savedHostToOverrideSiteToTagMap[host][siteName].tagName = finalTag;
          self.savedHostToOverrideSiteToTagMap[host][siteName].map[serviceConfigObj.name] = value;
        });
      }
    }
  },

  /**
   * Records all the host overrides per site/tag
   */
  _recordHostOverrideFromEmberObj: function(serviceConfigObj, siteName, tagName, self){
    var overrides = serviceConfigObj.get('overrides');
    if(overrides){
      overrides.forEach(function(override){
        override.get('selectedHostOptions').forEach(function(host){
          if(!(host in self.savedHostToOverrideSiteToTagMap)){
            self.savedHostToOverrideSiteToTagMap[host] = {};
          }
          if(!(siteName in self.savedHostToOverrideSiteToTagMap[host])){
            self.savedHostToOverrideSiteToTagMap[host][siteName] = {};
            self.savedHostToOverrideSiteToTagMap[host][siteName].map = {};
          }
          var finalTag = tagName + '_' + host;
          console.log("recordHostOverride(): Saving host override for host="+host+", site="+siteName+", tag="+finalTag+", (key,value)=("+serviceConfigObj.name+","+override.get('value')+")");
          self.savedHostToOverrideSiteToTagMap[host][siteName].tagName = finalTag;
          self.savedHostToOverrideSiteToTagMap[host][siteName].map[serviceConfigObj.name] = override.get('value');
        });
      });
    }
  },

  /**
   * create core site object
   * @param tagName
   * @return {Object}
   */
  createCoreSiteObj: function (tagName) {
    var coreSiteObj = this.get('uiConfigs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    // hadoop.proxyuser.oozie.hosts needs to be skipped if oozie is not selected
    var isOozieSelected = App.Service.find().someProperty('serviceName', 'OOZIE');
    var oozieUser = this.get('globalConfigs').someProperty('name', 'oozie_user') ? this.get('globalConfigs').findProperty('name', 'oozie_user').value : null;
    var isHiveSelected = App.Service.find().someProperty('serviceName', 'HIVE');
    var hiveUser = this.get('globalConfigs').someProperty('name', 'hive_user') ? this.get('globalConfigs').findProperty('name', 'hive_user').value : null;
    var isHcatSelected = App.Service.find().someProperty('serviceName', 'WEBHCAT');
    var hcatUser = this.get('globalConfigs').someProperty('name', 'hcat_user') ? this.get('globalConfigs').findProperty('name', 'hcat_user').value : null;
    coreSiteObj.forEach(function (_coreSiteObj) {
      if ((isOozieSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + oozieUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + oozieUser + '.groups')) && (isHiveSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + hiveUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + hiveUser + '.groups')) && (isHcatSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + hcatUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + hcatUser + '.groups'))) {
        coreSiteProperties[_coreSiteObj.name] = _coreSiteObj.value;
        this.recordHostOverride(_coreSiteObj, 'core-site', tagName, this);
      }
    }, this);
    return {"type": "core-site", "tag": tagName, "properties": coreSiteProperties};
  },

  /**
   * create site object
   * @param siteName
   * @param tagName
   * @return {Object}
   */
  createSiteObj: function (siteName, tagName) {
    var siteObj = this.get('uiConfigs').filterProperty('filename', siteName + '.xml');
    var siteProperties = {};
    siteObj.forEach(function (_siteObj) {
      siteProperties[_siteObj.name] = _siteObj.value;
      this.recordHostOverride(_siteObj, siteName, tagName, this);
    }, this);
    return {"type": siteName, "tag": tagName, "properties": siteProperties};
  },

  /**
   * Set display names of the property tfrom he puppet/global names
   * @param: displayNames: a field to be set with displayNames
   * @param names: array of property puppet/global names
   */
  setPropertyDisplayNames: function (displayNames, names) {
    var stepConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).configs;
    names.forEach(function (_name, index) {
      if (stepConfigs.someProperty('name', _name)) {
        displayNames.push(stepConfigs.findProperty('name', _name).displayName);
      }
    }, this);
  },

  /**
   * Set property of the site variable
   */
  setSiteProperty: function (key, value, filename) {
    if (filename === 'core-site.xml' && this.get('uiConfigs').filterProperty('filename', 'core-site.xml').someProperty('name', key)) {
      this.get('uiConfigs').filterProperty('filename', 'core-site.xml').findProperty('name', key).value = value;
      return;
    }
    this.get('uiConfigs').pushObject({
      "id": "site property",
      "name": key,
      "value": value,
      "filename": filename
    });
  },

  /**
   * return either specific url for request if testMode is false or testUrl
   * @param testUrl
   * @param url
   * @return {*}
   */
  getUrl: function (testUrl, url) {
    return (App.testMode) ? testUrl : App.apiPrefix + '/clusters/' + App.router.getClusterName() + url;
  },

  /**
   * Adds host name of master component to global config;
   */
  addHostNamesToGlobalConfig: function () {
    var serviceName = this.get('content.serviceName');
    var globalConfigs = this.get('globalConfigs');
    var serviceConfigs = this.get('serviceConfigs').findProperty('serviceName', serviceName).configs;
    //namenode_host is required to derive "fs.default.name" a property of core-site
    var nameNodeHost = this.get('serviceConfigs').findProperty('serviceName', 'HDFS').configs.findProperty('name', 'namenode_host');
    nameNodeHost.defaultValue = App.Service.find('HDFS').get('hostComponents').findProperty('componentName', 'NAMENODE').get('host.hostName');
    globalConfigs.push(nameNodeHost);

    //zooKeeperserver_host
    var zooKeperHost = this.get('serviceConfigs').findProperty('serviceName', 'ZOOKEEPER').configs.findProperty('name', 'zookeeperserver_hosts');
    if (serviceName === 'ZOOKEEPER' || serviceName === 'HBASE' || serviceName === 'WEBHCAT') {
      zooKeperHost.defaultValue = App.Service.find('ZOOKEEPER').get('hostComponents').filterProperty('componentName', 'ZOOKEEPER_SERVER').mapProperty('host.hostName');
      globalConfigs.push(zooKeperHost);
    }

    switch (serviceName) {
      case 'HDFS':
        var sNameNodeHost = serviceConfigs.findProperty('name', 'snamenode_host');
        sNameNodeHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'SECONDARY_NAMENODE').get('host.hostName');
        globalConfigs.push(sNameNodeHost);
        break;
      case 'MAPREDUCE':
        var jobTrackerHost = serviceConfigs.findProperty('name', 'jobtracker_host');
        jobTrackerHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'JOBTRACKER').get('host.hostName');
        globalConfigs.push(jobTrackerHost);
        break;
      case 'HIVE':
        var hiveMetastoreHost = serviceConfigs.findProperty('name', 'hivemetastore_host');
        hiveMetastoreHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'HIVE_SERVER').get('host.hostName');
        globalConfigs.push(hiveMetastoreHost);
        break;
      case 'OOZIE':
        var oozieServerHost = serviceConfigs.findProperty('name', 'oozieserver_host');
        oozieServerHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'OOZIE_SERVER').get('host.hostName');
        globalConfigs.push(oozieServerHost);
        break;
      case 'HBASE':
        var hbaseMasterHost = serviceConfigs.findProperty('name', 'hbasemaster_host');
        hbaseMasterHost.defaultValue = this.get('content.hostComponents').filterProperty('componentName', 'HBASE_MASTER').mapProperty('host.hostName');
        globalConfigs.push(hbaseMasterHost);
        break;
      case 'HUE':
        var hueServerHost = serviceConfigs.findProperty('name', 'hueserver_host');
        hueServerHost.defaultValue = this.get('content.hostComponents').findProperty('componentName', 'HUE_SERVER').get('host.hostName');
        globalConfigs.push(hueServerHost);
        break;
      case 'WEBHCAT':
        var webhcatMasterHost = serviceConfigs.findProperty('name', 'webhcatserver_host');
        webhcatMasterHost.defaultValue = this.get('content.hostComponents').filterProperty('componentName', 'WEBHCAT_SERVER').mapProperty('host.hostName');
        globalConfigs.push(webhcatMasterHost);
        break;
    }
  },
  
  /**
   * Provides service component name and display-name information for 
   * the current selected service. 
   */
  getCurrentServiceComponents: function () {
    var service = this.get('content');
    var components = service.get('hostComponents');
    var validComponents = Ember.A([]);
    var seenComponents = {};
    components.forEach(function(component){
      var cn = component.get('componentName');
      var cdn = component.get('displayName');
      if(!seenComponents[cn]){
        validComponents.push(Ember.Object.create({
          componentName: cn,
          displayName: cdn,
          selected: false
        }));
        seenComponents[cn] = cn;
      }
    });
    return validComponents;
  }.property('content'),
  
  getAllHosts: function () {
    return App.router.get('mainHostController.content');
  }.property('App.router.mainHostController.content'),
  
  doCancel: function () {
    this.loadStep();
  }
});