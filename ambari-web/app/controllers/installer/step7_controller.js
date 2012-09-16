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
    var mockData = [
      {
        serviceName: 'HDFS',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'}),
          App.ServiceConfigCategory.create({ name: 'NameNode'}),
          App.ServiceConfigCategory.create({ name: 'SNameNode'}),
          App.ServiceConfigCategory.create({ name: 'DataNode'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: [
          {
            name: 'dfs.prop1',
            displayName: 'Prop1',
            value: '',
            defaultValue: '100',
            description: 'This is Prop1',
            displayType: 'digits',
            unit: 'MB',
            category: 'General'
          },
          {
            name: 'dfs.prop2',
            displayName: 'Prop2',
            value: '',
            defaultValue: '0',
            description: 'This is Prop2 (Optional)',
            displayType: 'number',
            isRequired: false,
            category: 'General'
          },
          {
            name: 'dfs.adv.prop1',
            displayName: 'Adv Prop1',
            value: '',
            defaultValue: '100',
            description: 'This is Adv Prop1',
            displayType: 'int',
            isRequired: false,
            category: 'Advanced'
          },
          {
            name: 'dfs.adv.prop2',
            displayName: 'Adv Prop2',
            value: '',
            displayType: 'string',
            defaultValue: 'This is Adv Prop2',
            isRequired: false,
            category: 'Advanced'
          },
          {
            name: 'hdfs-site.xml',
            displayName: 'Custom HDFS Configs',
            value: '',
            defaultValue: '',
            description: 'If you wish to set configuration parameters not exposed through this page, you can specify them here.<br>The text you specify here will be injected into hdfs-site.xml verbatim.',
            displayType: 'custom',
            isRequired: false,
            category: 'Advanced'
          },
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
            name: 'dfs.namenode.dir',
            displayName: 'NameNode directories',
            value: '/grid/0/hadoop/namenode\r\n/grid/1/hadoop/namenode',
            defaultValue: '',
            displayType: 'directories',
            category: 'NameNode'
          },
          {
            name: 'dfs.namenode.prop1',
            displayName: 'NameNode Prop1',
            value: '',
            defaultValue: 'default (nn)',
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
            name: 'fs.checkpoint.dir',
            displayName: 'SNameNode directories',
            value: '',
            defaultValue: '',
            displayType: 'directories',
            category: 'SNameNode'
          },
          {
            name: 'fs.checkpoint.prop1',
            displayName: 'SNameNode Prop1',
            value: '',
            defaultValue: 'default (snn)',
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
          },
          {
            name: 'dfs.data.dir',
            displayName: 'DataNode directories',
            value: '',
            defaultValue: '',
            displayType: 'directories',
            category: 'DataNode'
          },
          {
            name: 'dfs.data.prop1',
            displayName: 'DataNode Prop1',
            value: '',
            defaultValue: 'default (dn)',
            category: 'DataNode'
          }
        ]
      },
      {
        serviceName: 'MapReduce',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'}),
          App.ServiceConfigCategory.create({ name: 'JobTracker'}),
          App.ServiceConfigCategory.create({ name: 'TaskTracker'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: [
          {
            name: 'mapred.prop1',
            displayName: 'Prop1',
            value: '1',
            defaultValue: '0',
            category: 'General'
          },
          {
            name: 'jt.prop1',
            displayName: 'JT Prop1',
            value: '2',
            defaultValue: '128',
            category: 'JobTracker'
          },
          {
            name: 'tt.prop1',
            displayName: 'TT Prop1',
            value: '3',
            defaultValue: '256',
            category: 'TaskTracker'
          },
          {
            name: 'mapred.adv.prop1',
            displayName: 'Adv Prop1',
            value: '',
            defaultValue: '1024',
            category: 'Advanced'
          }
        ]
      },
      {
        serviceName: 'HBase',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'}),
          App.ServiceConfigCategory.create({ name: 'HBaseMaster'}),
          App.ServiceConfigCategory.create({ name: 'RegionServer'}),
          App.ServiceConfigCategory.create({ name: 'Advanced'})
        ],
        configs: []
      },
      {
        serviceName: 'Hive/HCat',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'Hive Metastore'})
        ],
        configs: []
      },
      {
        serviceName: 'ZooKeeper',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'})
        ],
        configs: []
      },
      {
        serviceName: 'Nagios',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'})
        ],
        configs: [
          {
            name: 'nagios_web_login',
            displayName: 'Nagios Admin username',
            value: 'nagiosadmin',
            description: 'Nagios Web UI Admin username'
          },
          {
            name: 'nagios_web_password',
            displayName: 'Nagios Admin password',
            value: '',
            description: 'Nagios Web UI Admin password'
          },
          {
            name: 'nagios_contact',
            displayName: 'Alert email address',
            description: 'Email address to which alert notifications will be sent'
          }
        ]
      },
      {
        serviceName: 'Oozie',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'})
        ],
        configs: []
      },
      {
        serviceName: 'Misc',
        configCategories: [
          App.ServiceConfigCategory.create({ name: 'General'})
        ],
        configs: []
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

