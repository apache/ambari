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
require('models/configs/objects/service_config_category');

/**
 * This model loads all services supported by the stack
 * The model maps to the  http://hostname:8080/api/v1/stacks/HDP/versions/${versionNumber}/services?fields=StackServices/*,serviceComponents/*
 * @type {*}
 */
App.StackService = DS.Model.extend({
  serviceName: DS.attr('string'),
  serviceType: DS.attr('string'),
  displayName: DS.attr('string'),
  comments: DS.attr('string'),
  configTypes: DS.attr('object'),
  serviceVersion: DS.attr('string'),
  serviceCheckSupported: DS.attr('boolean'),
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  isSelected: DS.attr('boolean', {defaultValue: true}),
  isInstalled: DS.attr('boolean', {defaultValue: false}),
  isInstallable: DS.attr('boolean', {defaultValue: true}),
  isServiceWithWidgets: DS.attr('boolean', {defaultValue: false}),
  stack: DS.belongsTo('App.Stack'),
  serviceComponents: DS.hasMany('App.StackServiceComponent'),
  configs: DS.attr('array'),
  requiredServices: DS.attr('array'),

  /**
   * contains array of serviceNames that have configs that
   * depends on configs from current service
   * @type {String[]}
   */
  dependentServiceNames: DS.attr('array', {defaultValue: []}),

  // Is the service a distributed filesystem
  isDFS: function () {
    var dfsServices = ['HDFS', 'GLUSTERFS'];
    if( this.get('serviceType') == 'HCFS' || dfsServices.contains(this.get('serviceName')) )
    	return true;
    else
    	return false;
  }.property('serviceName'),

  // Primary DFS. used if there is more than one DFS in a stack.
  // Only one service in the stack should be tagged as primary DFS.
  isPrimaryDFS: function () {
    return this.get('serviceName') === 'HDFS';
  }.property('serviceName'),

  configTypesRendered: function () {
    var configTypes = this.get('configTypes');
    var renderedConfigTypes = $.extend(true, {}, configTypes);
    if (this.get('serviceName') == 'FALCON') {
      delete renderedConfigTypes['oozie-site'];
    }
    return renderedConfigTypes
  }.property('serviceName', 'configTypes'),

  displayNameOnSelectServicePage: function () {
    var displayName = this.get('displayName');
    console.info("displayName = " + displayName);
    var services = this.get('coSelectedServices').slice();
    var serviceDisplayNames = services.map(function (item) {
      return App.format.role(item, true);
    }, this);
    if (!!serviceDisplayNames.length) {
      serviceDisplayNames.unshift(displayName);
      displayName = serviceDisplayNames.join(" + ");
    }
    return displayName;
  }.property('coSelectedServices', 'serviceName'),

  isHiddenOnSelectServicePage: function () {
    var hiddenServices = ['MAPREDUCE2'];
    return hiddenServices.contains(this.get('serviceName')) || !this.get('isInstallable') || this.get('doNotShowAndInstall');
  }.property('serviceName', 'isInstallable'),

  doNotShowAndInstall: function () {
    var skipServices = [];
    if(!App.supports.installGanglia) {
      skipServices.push('GANGLIA');
    }
    if(App.router.get('clusterInstallCompleted') != true){
      skipServices.push('RANGER', 'RANGER_KMS');
    }
    return skipServices.contains(this.get('serviceName'));
  }.property('serviceName'),

  // Is the service required for monitoring of other hadoop ecosystem services
  isMonitoringService: function () {
    var services = ['GANGLIA'];
    return services.contains(this.get('serviceName'));
  }.property('serviceName'),

  // Is the service required for reporting host metrics
  isHostMetricsService: function () {
      var services = ['GANGLIA', 'AMBARI_METRICS'];
      return services.contains(this.get('serviceName'));
  }.property('serviceName'),

  // Is the service required for reporting hadoop service metrics
  isServiceMetricsService: function () {
      var services = ['GANGLIA'];
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

  hasNonMastersWithCustomAssignment: function () {
    var serviceComponents = this.get('serviceComponents');
    return serviceComponents.rejectProperty('isMaster').rejectProperty('cardinality', 'ALL').length > 0;
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

  hasHeatmapSection: function() {
    return ['HDFS', 'YARN', 'HBASE'].contains(this.get('serviceName'));
  }.property('serviceName'),

  /**
   * configCategories are fetched from  App.StackService.configCategories.
   * Also configCategories that does not match any serviceComponent of a service and not included in the permissible default pattern are omitted
   */
  configCategories: function () {
    var configCategories = [];
    var configTypes = this.get('configTypes');
    var serviceComponents = this.get('serviceComponents');
    if (configTypes && Object.keys(configTypes).length) {
      var pattern = ["General", "CapacityScheduler", "FaultTolerance", "Isolation", "Performance", "HIVE_SERVER2", "KDC", "Kadmin","^Advanced", "Env$", "^Custom", "Falcon - Oozie integration", "FalconStartupSite", "FalconRuntimeSite", "MetricCollector", "Settings$", "AdvancedHawqCheck"];
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
  'MAPREDUCE2',
  'YARN',
  'TEZ',
  'GANGLIA',
  'HIVE',
  'HAWQ',
  'PXF',
  'HBASE',
  'PIG',
  'SQOOP',
  'OOZIE',
  'ZOOKEEPER',
  'FALCON',
  'STORM',
  'FLUME'
];

App.StackService.componentsOrderForService = {
  'HAWQ': ['HAWQMASTER', 'HAWQSTANDBY']
};

//@TODO: Write unit test for no two keys in the object should have any intersecting elements in their values
App.StackService.coSelected = {
  'YARN': ['MAPREDUCE2']
};

App.StackService.reviewPageHandlers = {
  'HIVE': {
    'Database': 'loadHiveDbValue'
  },
  'OOZIE': {
    'Database': 'loadOozieDbValue'
  }
};

App.StackService.configCategories = function () {
  var serviceConfigCategories = [];
  switch (this.get('serviceName')) {
    case 'HDFS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'NAMENODE', displayName: 'NameNode', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'SECONDARY_NAMENODE', displayName: 'Secondary NameNode', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'DATANODE', displayName: 'DataNode', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'NFS_GATEWAY', displayName: 'NFS Gateway', showHost: true})
      ]);
      break;
    case 'GLUSTERFS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'YARN':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'RESOURCEMANAGER', displayName: 'Resource Manager', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'NODEMANAGER', displayName: 'Node Manager', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'APP_TIMELINE_SERVER', displayName: 'Application Timeline Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'FaultTolerance', displayName: 'Fault Tolerance'}),
        App.ServiceConfigCategory.create({ name: 'Isolation', displayName: 'Isolation'}),
        App.ServiceConfigCategory.create({ name: 'CapacityScheduler', displayName: 'Scheduler', siteFileName: 'capacity-scheduler.xml'})
      ]);
      break;
    case 'MAPREDUCE2':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HISTORYSERVER', displayName: 'History Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'HIVE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HIVE_METASTORE', displayName: 'Hive Metastore', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'WEBHCAT_SERVER', displayName: 'WebHCat Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'Performance', displayName: 'Performance'}),
        App.ServiceConfigCategory.create({ name: 'HIVE_SERVER2', displayName: 'Hive Server2'}),
        App.ServiceConfigCategory.create({ name: 'HIVE_CLIENT', displayName: 'Hive Client'})
      ]);
      break;
    case 'HBASE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'HBASE_MASTER', displayName: 'HBase Master', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'HBASE_REGIONSERVER', displayName: 'RegionServer', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'ZOOKEEPER':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'ZOOKEEPER_SERVER', displayName: 'ZooKeeper Server', showHost: true})
      ]);
      break;
    case 'OOZIE':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'OOZIE_SERVER', displayName: 'Oozie Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'Falcon - Oozie integration', displayName: 'Falcon - Oozie integration'})
      ]);
      break;
    case 'FALCON':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'FALCON_SERVER', displayName: 'Falcon Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'Falcon - Oozie integration', displayName: 'Falcon - Oozie integration'}),
        App.ServiceConfigCategory.create({ name: 'FalconStartupSite', displayName: 'Falcon startup.properties'}),
        App.ServiceConfigCategory.create({ name: 'FalconRuntimeSite', displayName: 'Falcon runtime.properties'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'STORM':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'NIMBUS', displayName: 'Nimbus', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'SUPERVISOR', displayName: 'Supervisor', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'STORM_UI_SERVER', displayName: 'Storm UI Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'STORM_REST_API', displayName: 'Storm REST API Server', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'DRPC_SERVER', displayName: 'DRPC Server', showHost: true}),
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
    case 'KNOX':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'KNOX_GATEWAY', displayName: 'Knox Gateway', showHost: true})
      ]);
      break;
    case 'KAFKA':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'KAFKA_BROKER', displayName: 'Kafka Broker', showHost: true})
      ]);
      break;
    case 'KERBEROS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'KDC', displayName: 'KDC'}),
        App.ServiceConfigCategory.create({ name: 'Kadmin', displayName: 'Kadmin'}),
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'AMBARI_METRICS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'MetricCollector', displayName: 'Metric Collector'})
      ]);
      break;
    case 'RANGER':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'RANGER_ADMIN', displayName: 'Admin Settings', showHost: true}),
        App.ServiceConfigCategory.create({ name: 'DBSettings', displayName: 'DB Settings'}),
        App.ServiceConfigCategory.create({ name: 'RangerSettings', displayName: 'Ranger Settings'}),
        App.ServiceConfigCategory.create({ name: 'UnixAuthenticationSettings', displayName: 'Unix Authentication Settings'}),
        App.ServiceConfigCategory.create({ name: 'ADSettings', displayName: 'AD Settings'}),
        App.ServiceConfigCategory.create({ name: 'LDAPSettings', displayName: 'LDAP Settings'}),
        App.ServiceConfigCategory.create({ name: 'KnoxSSOSettings', displayName: 'Knox SSO Settings'})
      ]);
      break;
    case 'RANGER_KMS':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'RANGER_KMS_SERVER', displayName: 'Ranger KMS Server', showHost: true})
      ]);
      break;
    case 'ACCUMULO':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
      break;
    case 'PIG':
      break;
    case 'SQOOP':
      break;
    case 'HAWQ':
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'}),
        App.ServiceConfigCategory.create({ name: 'AdvancedHawqCheck', displayName: 'Advanced Hawq Check'})
      ]);
      break;
    default:
      serviceConfigCategories.pushObjects([
        App.ServiceConfigCategory.create({ name: 'General', displayName: 'General'})
      ]);
  }
  serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({ name: 'Advanced', displayName: 'Advanced'}));

  var configTypes = Object.keys(this.get('configTypes'));

  //Falcon has dependency on oozie-site but oozie-site advanced/custom section should not be shown on Falcon page
  if (this.get('serviceName') !== 'OOZIE') {
    configTypes = configTypes.without('oozie-site');
  }

  // Add Advanced section for every configType to all the services
  configTypes.forEach(function (type) {
    serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({
      name: 'Advanced ' + type,
      displayName: Em.I18n.t('common.advanced') + " " + type,
      canAddProperty: false
    }));
  }, this);

  // Add custom section for every configType to all the services
  configTypes.forEach(function (type) {
    var configTypesWithNoCustomSection = ['capacity-scheduler','mapred-queue-acls','flume-conf', 'pig-properties','topology','users-ldif', 'pxf-profiles', 'pxf-public-classpath'];
    if (type.endsWith('-env') || type.endsWith('-log4j') || configTypesWithNoCustomSection.contains(type)) {
      return;
    }
    serviceConfigCategories.pushObject(App.ServiceConfigCategory.create({
      name: 'Custom ' + type,
      displayName: Em.I18n.t('common.custom') + " " + type,
      siteFileName: type + '.xml',
      canAddProperty: true
    }));
  }, this);
  return serviceConfigCategories;
};
