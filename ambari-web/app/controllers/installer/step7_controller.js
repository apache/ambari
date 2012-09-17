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

App.InstallerStep7Controller = Em.ArrayController.extend({

  name: 'installerStep7Controller',

  content: [],

  selectedService: null,

  selectedSlaveHosts: null,

  isSubmitDisabled: function() {
    return !this.everyProperty('errorCount', 0);
  }.property('@each.errorCount'),

  init: function () {
    // TODO: get selected services from previous step

    var selectedServices = [ 'HDFS', 'MapReduce', 'Ganglia', 'Nagios', 'HBase', 'Pig', 'Sqoop', 'Oozie', 'Hive', 'Templeton', 'ZooKeeper'];

    var configProperties = App.ConfigProperties.create();
    var mockData = [
      {
        serviceName: 'Nagios',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'})
        ],
        configs: configProperties.filterProperty('serviceName', 'NAGIOS')
      },
      {
        serviceName: 'Hive/HCat',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'Hive Metastore'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: configProperties.filterProperty('serviceName', 'HIVE')
      },
      {
        serviceName: 'HDFS',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'NameNode'}),
          App.ServiceConfigCategory.create({ name: 'SNameNode'}),
          App.ServiceConfigCategory.create({ name: 'DataNode'}),
          App.ServiceConfigCategory.create({ name: 'General'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: configProperties.filterProperty('serviceName', 'HDFS')
        /*
        [
          {
            name: 'ambari.namenode.host',
            displayName: 'NameNode host',
            value: 'host0001.com.com',
            defaultValue: '',
            description: 'The host that has been assigned to run NameNode',
            displayType: 'masterHost',
            category: 'NameNode'
          },
          {
            name: 'ambari.snamenode.host',
            displayName: 'SNameNode host',
            value: 'host0002.com.com',
            defaultValue: '',
            description: 'The host that has been assigned to run Secondary NameNode',
            displayType: 'masterHost',
            category: 'SNameNode'
          },
          {
            name: 'ambari.datanode.hosts',
            displayName: 'DataNode hosts',
            value: [ 'host0003.com.com', 'host0004.com.com', 'host0005.com.com' ],
            defaultValue: '',
            description: 'The hosts that have been assigned to run DataNodes',
            displayType: 'slaveHosts',
            category: 'DataNode'
          }
        ]
        */
      },
      {
        serviceName: 'MapReduce',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'JobTracker'}),
          App.ServiceConfigCategory.create({ name: 'TaskTracker'}),
          App.ServiceConfigCategory.create({ name: 'General'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: configProperties.filterProperty('serviceName', 'MAPREDUCE')
      },
      {
        serviceName: 'HBase',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'HBase Master'}),
          App.ServiceConfigCategory.create({ name: 'RegionServer'}),
          App.ServiceConfigCategory.create({ name: 'General'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: configProperties.filterProperty('serviceName', 'HBASE')
      },
      {
        serviceName: 'ZooKeeper',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'ZooKeeper Server'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: configProperties.filterProperty('serviceName', 'ZOOKEEPER')
      },
      {
        serviceName: 'Oozie',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'Oozie Server'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: configProperties.filterProperty('serviceName', 'OOZIE')
      },
      {
        serviceName: 'Templeton',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'Templeton Server'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: configProperties.filterProperty('serviceName', 'TEMPLETON')
      },
      {
        serviceName: 'Misc',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: configProperties.filterProperty('serviceName', 'MISC')
      }

    ];

    var self = this;

    mockData.forEach(function(_serviceConfig) {
      var serviceConfig = App.ServiceConfig.create({
        serviceName: _serviceConfig.serviceName,
        configCategories: _serviceConfig.configCategories,
        configs: []
      });
      _serviceConfig.configs.forEach(function(_serviceConfigProperty) {
        var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
        serviceConfigProperty.serviceConfig = serviceConfig;
        serviceConfig.configs.pushObject(serviceConfigProperty);
        serviceConfigProperty.validate();
      });

      console.log('pushing ' + serviceConfig.serviceName);
      self.content.pushObject(serviceConfig);
    });

    this.set('selectedService', this.objectAt(0));
  },

  submit: function() {
    if (!this.get('isSubmitDisabled')) {
      App.get('router').transitionTo('step8');
    }
  },

  showSlaveHosts: function(event) {
    this.set('selectedSlaveHosts', event.context);
    App.ModalPopup.show({
      header: 'Slave Hosts',
      bodyClass: Ember.View.extend({
        templateName: require('templates/installer/slaveHostsMatrix')
      })
    });
  },

  addSlaveComponentGroup: function(event) {
    App.ModalPopup.show({
      header: 'Add a ' + event.context + ' Group',
      bodyClass: Ember.View.extend({
        templateName: require('templates/installer/slaveHostsMatrix')
      })
    });
  }

});

