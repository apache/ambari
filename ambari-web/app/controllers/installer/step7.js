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

  submit: function () {
    // validate all fields
    this.get('content');
  },

  init: function () {
    var mockData = [
      {
        serviceName: 'HDFS',
        configCategories: [ 'General', 'NameNode', 'SNameNode', 'DataNode', 'Advanced' ],
        configs: [
          {
            name: 'dfs.prop1',
            displayName: 'Prop1',
            value: '',
            defaultValue: '100',
            description: 'This is Prop1',
            displayType: 'string',
            unit: 'MB',
            category: 'General',
            errorMessage: 'Prop1 validation error'
          },
          {
            name: 'dfs.prop2',
            displayName: 'Prop2',
            value: '',
            defaultValue: '0',
            description: 'This is Prop2',
            displayType: 'int',
            category: 'General'
          },
          {
            name: 'dfs.adv.prop1',
            displayName: 'Adv Prop1',
            value: '',
            defaultValue: '100',
            description: 'This is Adv Prop1',
            displayType: 'int',
            category: 'Advanced'
          },
          {
            name: 'dfs.adv.prop2',
            displayName: 'Adv Prop2',
            value: '',
            displayType: 'string',
            defaultValue: 'This is Adv Prop2',
            category: 'Advanced'
          },
          {
            name: 'hdfs-site.xml',
            displayName: 'hdfs-site.xml',
            value: '',
            defaultValue: '',
            description: 'Custom configurations that you want to put in hdfs-site.xml.<br>The text you specify here will be injected into hdfs-site.xml verbatim.',
            displayType: 'custom',
            category: 'Advanced'
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
        configCategories: [ 'General', 'JobTracker', 'TaskTracker', 'Advanced' ],
        configs: [
          {
            name: 'mapred.prop1',
            displayName: 'Prop1',
            value: '',
            defaultValue: '0',
            category: 'General'
          },
          {
            name: 'jt.prop1',
            displayName: 'JT Prop1',
            value: '',
            defaultValue: '128',
            category: 'JobTracker'
          },
          {
            name: 'tt.prop1',
            displayName: 'TT Prop1',
            value: '',
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
        serviceConfig.configs.pushObject(serviceConfigProperty);
      });

      console.log('pushing ' + serviceConfig.serviceName);
      self.content.pushObject(serviceConfig);
    });

    this.set('selectedService', this.objectAt(0));
  }

})
;
