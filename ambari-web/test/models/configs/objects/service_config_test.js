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
var configPropertyHelper = require('utils/configs/config_property_helper');

require('models/configs/objects/service_config');

var serviceConfig,
  group,
  configsData = [
    Ember.Object.create({
      category: 'c0',
      overrides: [
        {
          error: true,
          errorMessage: 'error'
        },
        {
          error: true
        },
        {}
      ]
    }),
    Ember.Object.create({
      category: 'c1',
      isValid: false,
      isVisible: true
    }),
    Ember.Object.create({
      category: 'c0',
      isValid: true,
      isVisible: true
    }),
    Ember.Object.create({
      category: 'c1',
      isValid: false,
      isVisible: false
    })
  ],
  configCategoriesData = [
    Em.Object.create({
      name: 'c0',
      slaveErrorCount: 1
    }),
    Em.Object.create({
      name: 'c1',
      slaveErrorCount: 2
    })
  ],
  components = [
    {
      name: 'NameNode',
      master: true
    },
    {
      name: 'SNameNode',
      master: true
    },
    {
      name: 'JobTracker',
      master: true
    },
    {
      name: 'HBase Master',
      master: true
    },
    {
      name: 'Oozie Master',
      master: true
    },
    {
      name: 'Hive Metastore',
      master: true
    },
    {
      name: 'WebHCat Server',
      master: true
    },
    {
      name: 'ZooKeeper Server',
      master: true
    },
    {
      name: 'Ganglia',
      master: true
    },
    {
      name: 'DataNode',
      slave: true
    },
    {
      name: 'TaskTracker',
      slave: true
    },
    {
      name: 'RegionServer',
      slave: true
    }
  ],
  masters = components.filterProperty('master'),
  slaves = components.filterProperty('slave'),
  groupNoErrorsData = [].concat(configsData.slice(2)),
  groupErrorsData = [configsData[1]];

describe('App.ServiceConfig', function () {

  beforeEach(function () {
    serviceConfig = App.ServiceConfig.create();
  });

  describe('#errorCount', function () {
    it('should be 0', function () {
      serviceConfig.setProperties({
        configs: [],
        configCategories: []
      });
      expect(serviceConfig.get('errorCount')).to.equal(0);
    });
    it('should sum counts of all errors', function () {
      serviceConfig.setProperties({
        configs: configsData,
        configCategories: configCategoriesData
      });
      expect(serviceConfig.get('errorCount')).to.equal(6);
      expect(serviceConfig.get('configCategories').findProperty('name', 'c0').get('nonSlaveErrorCount')).to.equal(2);
      expect(serviceConfig.get('configCategories').findProperty('name', 'c1').get('nonSlaveErrorCount')).to.equal(1);
    });
    it('should include invalid properties with widgets', function() {
      serviceConfig.setProperties({
        configs: [
          Em.Object.create({
            isValid: false,
            widgetType: 'type',
            isVisible: true,
            category: 'some1'
          }),
          Em.Object.create({
            isValid: false,
            widgetType: 'type',
            isVisible: true,
            category: 'some2'
          }),
          Em.Object.create({
            isValid: false,
            widgetType: null,
            isVisible: true,
            category: 'some2'
          }),
          Em.Object.create({
            isValid: false,
            widgetType: 'type',
            isVisible: true
          })
        ],
        configCategories: [
          Em.Object.create({ name: 'some1', slaveErrorCount: 0}),
          Em.Object.create({ name: 'some2', slaveErrorCount: 0})
        ]
      });
      expect(serviceConfig.get('errorCount')).to.equal(4);
    });
  });

});

describe('App.Group', function () {

  beforeEach(function () {
    group = App.Group.create();
  });

  describe('#errorCount', function () {
    it('should be 0', function () {
      group.set('properties', groupNoErrorsData);
      expect(group.get('errorCount')).to.equal(0);
    });
    it('should be 1', function () {
      group.set('properties', groupErrorsData);
      expect(group.get('errorCount')).to.equal(1);
    });
  });

});
