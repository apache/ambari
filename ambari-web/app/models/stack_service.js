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
require('utils/helper');
require('mixins/models/service_mixin');
require('models/service_config');
//TODO after moving validation/recommendation to BE belove requirements must be deleted
require('utils/configs/defaults_providers/yarn_defaults_provider');
require('utils/configs/defaults_providers/tez_defaults_provider');
require('utils/configs/defaults_providers/hive_defaults_provider');
require('utils/configs/defaults_providers/storm_defaults_provider');
require('utils/configs/defaults_providers/oozie_defaults_provider');
require('utils/configs/validators/yarn_configs_validator');
require('utils/configs/validators/hive_configs_validator');
require('utils/configs/validators/tez_configs_validator');
require('utils/configs/validators/mapreduce2_configs_validator');
require('utils/configs/validators/storm_configs_validator');

/**
 * This model loads all services supported by the stack
 * The model maps to the  http://hostname:8080/api/v1/stacks2/HDP/versions/${versionNumber}/stackServices?fields=StackServices/*,serviceComponents/*
 * @type {*}
 */
App.StackService = DS.Model.extend(App.ServiceModelMixin, {
  comments: DS.attr('string'),
  configTypes: DS.attr('object'),
  serviceVersion: DS.attr('string'),
  serviceCheckSupported: DS.attr('boolean'),
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  isSelected: DS.attr('boolean', {defaultValue: true}),
  isInstalled: DS.attr('boolean', {defaultValue: false}),
  serviceComponents: DS.hasMany('App.StackServiceComponent'),
  configs: DS.attr('array'),

  // Is the service a distributed filesystem
  isDFS: function () {
    var dfsServices = ['HDFS', 'GLUSTERFS'];
    return dfsServices.contains(this.get('serviceName'));
  }.property('serviceName'),

  // Primary DFS. used if there is more than one DFS in a stack.
  // Only one service in the stack should be tagged as primary DFS.
  isPrimaryDFS: function () {
    return this.get('serviceName') === 'HDFS';
  }.property('serviceName'),

  configTypesRendered: function () {
    var configTypes = this.get('configTypes');
    if (this.get('serviceName') == 'HDFS') return configTypes;
    else {
      var renderedConfigTypes = $.extend(true, {}, configTypes);
      delete renderedConfigTypes['core-site'];
      return renderedConfigTypes
    }
  }.property('serviceName', 'configTypes'),

  displayNameOnSelectServicePage: function () {
    var displayName = this.get('displayName');
    var services = this.get('coSelectedServices').slice();
    var serviceDisplayNames = services.map(function (item) {
      return App.format.role(item);
    }, this);
    if (!!serviceDisplayNames.length) {
      serviceDisplayNames.unshift(displayName);
      displayName = serviceDisplayNames.join(" + ");
    }
    return displayName;
  }.property('coSelectedServices', 'serviceName'),

  isHiddenOnSelectServicePage: function () {
    var hiddenServices = ['MAPREDUCE2', 'HCATALOG', 'WEBHCAT'];
    return hiddenServices.contains(this.get('serviceName'));
  }.property('serviceName'),

  dependentServices: function () {
    var serviceName = this.get('serviceName');
    var dependentServices = [];
    if (App.get('isHadoop2Stack')) {
      dependentServices = App.StackService.dependency['HDP-2'][serviceName];
    } else {
      dependentServices = App.StackService.dependency['HDP-1'][serviceName];
    }
    return dependentServices;
  }.property('serviceName'),

  /**
   * other services on which the service is dependent
   */
  serviceDependency: function () {
    var serviceName = this.get('serviceName');
    var serviceDependencyMap, key, serviceDependencies = [];
    if (App.get('isHadoop2Stack')) {
      serviceDependencyMap = App.StackService.dependency['HDP-2'];
    } else {
      serviceDependencyMap = App.StackService.dependency['HDP-1'];
    }
    for (key in serviceDependencyMap) {
      if (serviceDependencyMap[key].contains(serviceName)) serviceDependencies.pushObject(key);
    }
    return  serviceDependencies;
  }.property('serviceName'),

  // Is the service required for monitoring of other hadoop ecosystem services
  isMonitoringService: function () {
    var services = ['NAGIOS', 'GANGLIA'];
    return services.contains(this.get('serviceName'));
  }.property('serviceName'),

  coSelectedServices: function () {
    var coSelectedServices = App.StackService.coSelected[this.get('serviceName')];
    if (!!coSelectedServices) {
      return coSelectedServices;
    } else {
      return [];
    }
  }.property('serviceName'),

  hasClient: function () {
    var serviceComponents = this.get('serviceComponents');
    return serviceComponents.someProperty('isClient');
  }.property('serviceName'),

  hasMaster: function () {
    var serviceComponents = this.get('serviceComponents');
    return serviceComponents.someProperty('isMaster');
  }.property('serviceName'),

  hasSlave: function () {
    var serviceComponents = this.get('serviceComponents');
    return serviceComponents.someProperty('isSlave');
  }.property('serviceName'),

  isClientOnlyService: function () {
    var serviceComponents = this.get('serviceComponents');
    return serviceComponents.everyProperty('isClient');
  }.property('serviceName'),

  isNoConfigTypes: function () {
    var configTypes = this.get('configTypes');
    return !(configTypes && !!Object.keys(configTypes).length);
  }.property('configTypes'),

  customReviewHandler: function () {
    return App.StackService.reviewPageHandlers[this.get('serviceName')];
  }.property('serviceName'),
  //TODO after moving validation/recommendation to BE defaultsProviders must be deleted
  defaultsProviders: function () {
    var defaultConfigsHandler = App.StackService.defaultConfigsHandler[this.get('serviceName')];
    return defaultConfigsHandler && defaultConfigsHandler.defaultsProviders;
  }.property('serviceName'),
  //TODO after moving validation/recommendation to BE configsValidator must be deleted
  configsValidator: function () {
    var defaultConfigsHandler = App.StackService.defaultConfigsHandler[this.get('serviceName')];
    return defaultConfigsHandler && defaultConfigsHandler.configsValidator;
  }.property('serviceName'),

  /**
   * configCategories are fetched from  App.StackService.configCategories.
   * Also configCategories that does not match any serviceComponent of a service and not included in the permissible default pattern are omitted
   */
  configCategories: function () {
    var configCategories = [];
    var serviceName = this.get('serviceName');
    var configTypes = this.get('configTypes');
    var serviceComponents = this.get('serviceComponents');
    if (configTypes && Object.keys(configTypes).length) {
      var pattern = ["General", "CapacityScheduler", "^Advanced", "Env$", "^Custom", "Falcon - Oozie integration", "FalconStartupSite", "FalconRuntimeSite"];
      configCategories = App.StackService.configCategories.call(this).filter(function (_configCategory) {
        var serviceComponentName = _configCategory.get('name');
        var isServiceComponent = serviceComponents.someProperty('componentName', serviceComponentName);
        if (isServiceComponent) return  isServiceComponent;
        var result = false;
        pattern.forEach(function (_pattern) {
          var regex = new RegExp(_pattern);
          if (regex.test(serviceComponentName)) result = true;
        });
        return result;
      });
    }
    return configCategories;
  }.property('serviceName', 'configTypes', 'serviceComponents')
});

App.StackService.FIXTURES = [];

App.StackService.displayOrder = [
  'HDFS',
  'GLUSTERFS',
  'MAPREDUCE',
  'MAPREDUCE2',
  'YARN',
  'TEZ',
  'NAGIOS',
  'GANGLIA',
  'HIVE',
  'HCATALOG',
  'WEBHCAT',
  'HBASE',
  'PIG',
  'SQOOP',
  'OOZIE',
  'ZOOKEEPER',
  'HUE',
  'FALCON',
  'STORM',
  'FLUME'
];

App.StackService.dependency = {
  'HDP-1': {
    'MAPREDUCE': ['PIG', 'OOZIE', 'HIVE'],
    'ZOOKEEPER': ['HBASE', 'HIVE', 'WEBHCAT']
  },
  'HDP-2': {
    'YARN': ['PIG', 'OOZIE', 'HIVE', 'TEZ'],
    'TEZ': ['YARN'],
    'OOZIE': ['FALCON'],
    'ZOOKEEPER': ['HDFS', 'HBASE', 'HIVE', 'WEBHCAT', 'STORM']
  }
};

//@TODO: Write unit test for no two keys in the object should have any intersecting elements in their values
App.StackService.coSelected = {
  'YARN': ['MAPREDUCE2'],
  'HIVE': ['HCATALOG', 'WEBHCAT']
};


App.StackService.reviewPageHandlers = {
  'HIVE': {
    'Database': 'loadHiveDbValue'
  },
  'NAGIOS': {
    'Administrator': 'loadNagiosAdminValue'
  },
  'OOZIE': {
    'Database': 'loadOozieDbValue'
  }
};

//TODO after moving validation/recommendation to BE defaultConfigsHandler must be deleted
App.StackService.defaultConfigsHandler = {
  YARN: {defaultsProviders: [App.YARNDefaultsProvider.create()], configsValidator: App.YARNConfigsValidator},
  MAPREDUCE2: {defaultsProviders: [App.YARNDefaultsProvider.create()], configsValidator: App.MapReduce2ConfigsValidator},
  HIVE: {defaultsProviders: [App.HiveDefaultsProvider.create()], configsValidator: App.HiveConfigsValidator},
  STORM: {defaultsProviders: [App.STORMDefaultsProvider.create()], configsValidator: App.STORMConfigsValidator},
  TEZ: {defaultsProviders: [App.TezDefaultsProvider.create()], configsValidator: App.TezConfigsValidator}
};

App.StackService.configCategories = function () {
  var serviceConfigCategories = [];
  switch (this.get('serviceName')) {
    case 'HDFS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'NAMENODE', displayName: 'NameNode'}),
        App.ServiceConfigCategory.create({ name: 'SECONDARY_NAMENODE', displayName: 'Secondary NameNode'}),
        App.ServiceConfigCategory.create({ name: 'DATANODE', displayName: 'DataNode'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'GLUSTERFS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'MAPREDUCE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HISTORYSERVER', displayName: 'History Server'}),
        App.ServiceConfigCategory.create({ name: 'JOBTRACKER', displayName: 'JobTracker'}),
        App.ServiceConfigCategory.create({ name: 'TASKTRACKER', displayName: 'TaskTracker'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'YARN':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'RESOURCEMANAGER', displayName: 'Resource Manager'}),
        App.ServiceConfigCategory.create({ name: 'NODEMANAGER', displayName: 'Node Manager'}),
        App.ServiceConfigCategory.create({ name: 'APP_TIMELINE_SERVER', displayName: 'Application Timeline Server'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'CapacityScheduler', displayName: 'Scheduler', isCustomView: true, siteFileName: 'capacity-scheduler.xml'})
      ]);
      break;
    case 'MAPREDUCE2':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HISTORYSERVER', displayName: 'History Server'})
      ]);
      break;
    case 'HIVE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HIVE_METASTORE', displayName: 'Hive Metastore'})
      ]);
      break;
    case 'WEBHCAT':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'WEBHCAT_SERVER', displayName: 'WebHCat Server'})
      ]);
      break;
    case 'HBASE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HBASE_MASTER', displayName: 'HBase Master'}),
        App.ServiceConfigCategory.create({ name: 'HBASE_REGIONSERVER', displayName: 'RegionServer'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'ZOOKEEPER':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'ZOOKEEPER_SERVER', displayName: 'ZooKeeper Server'})
      ]);
      break;
    case 'OOZIE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'OOZIE_SERVER', displayName: 'Oozie Server'})
      ]);
      break;
    case 'FALCON':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'FALCON_SERVER', displayName: 'Falcon Server'}),
        App.ServiceConfigCategory.create({ name: 'Falcon - Oozie integration', displayName: 'Falcon - Oozie integration'}),
        App.ServiceConfigCategory.create({ name: 'FalconStartupSite', displayName: 'Falcon startup.properties'}),
        App.ServiceConfigCategory.create({ name: 'FalconRuntimeSite', displayName: 'Falcon runtime.properties'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'STORM':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'NIMBUS', displayName: 'Nimbus'}),
        App.ServiceConfigCategory.create({ name: 'SUPERVISOR', displayName: 'Supervisor'}),
        App.ServiceConfigCategory.create({ name: 'STORM_UI_SERVER', displayName: 'Storm UI Server'}),
        App.ServiceConfigCategory.create({ name: 'STORM_REST_API', displayName: 'Storm REST API Server'}),
        App.ServiceConfigCategory.create({ name: 'DRPC_SERVER', displayName: 'DRPC Server'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'TEZ':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'FLUME':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'FLUME_HANDLER', displayName: 'flume.conf', siteFileName: 'flume-conf', canAddProperty: false})
      ]);
      break;
    case 'PIG':
      break;
    case 'SQOOP':
      break;
    case 'HCATALOG':
      break;
    default:
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
  }
  serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({ name: 'Advanced', displayName: 'Advanced'}));

  var configTypes = Object.keys(this.get('configTypes'));
  if (this.get('serviceName') !== 'HDFS') {
    configTypes = configTypes.without('core-site');
  }
  //Falcon has dependency on oozie-site but oozie-site advanced/custom section should not be shown on Falcon page
  if (this.get('serviceName') !== 'OOZIE') {
    configTypes = configTypes.without('oozie-site');
  }

  //Hive has dependency on tez-site but tez-site advanced/custom section should not be shown on Hive page
  if (this.get('serviceName') !== 'TEZ') {
    configTypes = configTypes.without('tez-site');
  }

  //oozie has dependency on yarn-site but yarn-site advanced/custom section should not be shown on Oozie page
  if (this.get('serviceName') !== 'YARN') {
    configTypes = configTypes.without('yarn-site');
  }


  // Add Advanced section for every configType to all the services
  configTypes.forEach(function (type) {
    var displayName = 'Advanced ' + type;
    var canAddProperty = false;
    var advancedSection;
    advancedSection = App.ServiceConfigCategory.create({ name: 'Advanced ' + type, displayName: displayName,
      canAddProperty: canAddProperty});
    serviceConfigCategories.pushObject(advancedSection);
  }, this);

  // Add custom section for every configType to all the services
  configTypes.forEach(function (type) {
    var displayName = 'Custom ' + type;
    var canAddProperty = true;
    var customSection;
    var siteFileName = type + '.xml';
    var configTypesWithNoCustomSection = ['capacity-scheduler','mapred-queue-acls','flume-conf', 'pig-properties'];
    if (type.endsWith('-env') || type.endsWith('-log4j') || configTypesWithNoCustomSection.contains(type)) {
      return;
    }
    customSection = App.ServiceConfigCategory.create({ name: displayName, displayName: displayName, siteFileName: siteFileName,
      canAddProperty: canAddProperty});
    serviceConfigCategories.pushObject(customSection);
  }, this);
  return serviceConfigCategories;
};
