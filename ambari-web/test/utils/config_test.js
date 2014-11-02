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
require('config');
require('utils/config');
require('models/service/hdfs');
var setups = require('test/init_model_test');
var modelSetup = setups.configs;

describe('App.config', function () {

  App.supports.capacitySchedulerUi = true;

  var loadServiceSpecificConfigs = function(context, serviceName) {
    context.configGroups = modelSetup.setupConfigGroupsObject(serviceName);
    context.advancedConfigs = modelSetup.setupAdvancedConfigsObject();
    context.tags = modelSetup.setupServiceConfigTagsObject(serviceName);
    context.result = App.config.mergePreDefinedWithLoaded(context.configGroups, context.advancedConfigs, context.tags, App.Service.find().findProperty('id', serviceName).get('serviceName'));
  };

  var loadAllServicesConfigs = function(context, serviceNames) {
    context.configGroups = modelSetup.setupConfigGroupsObject();
  }

  var loadServiceModelsData = function(serviceNames) {
    serviceNames.forEach(function(serviceName) {
      App.store.load(App.Service, {
        id: serviceName,
        service_name: serviceName
      });
    });
  };

  var setupContentForMergeWithStored = function(context) {
    loadServiceModelsData(context.installedServiceNames);
    loadAllServicesConfigs(context);
    setups.setupStackVersion(this, 'HDP-2.1');
    context.result = App.config.mergePreDefinedWithStored(context.storedConfigs, modelSetup.setupAdvancedConfigsObject(), context.installedServiceNames);
  };

  var removeServiceModelData = function(serviceIds) {
    serviceIds.forEach(function(serviceId) {
      var record = App.Service.find(serviceId);
      record.deleteRecord();
      record.get('stateManager').transitionTo('loading');
    });
  };

  describe('#handleSpecialProperties', function () {
    var config = {};
    it('value should be transformed to "1024" from "1024m"', function () {
      config = {
        displayType: 'int',
        value: '1024m',
        defaultValue: '1024m'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal('1024');
      expect(config.defaultValue).to.equal('1024');
    });
    it('value should be transformed to true from "true"', function () {
      config = {
        displayType: 'checkbox',
        value: 'true',
        defaultValue: 'true'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal(true);
      expect(config.defaultValue).to.equal(true);
    });
    it('value should be transformed to false from "false"', function () {
      config = {
        displayType: 'checkbox',
        value: 'false',
        defaultValue: 'false'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal(false);
      expect(config.defaultValue).to.equal(false);
    });
  });

  describe('#capacitySchedulerFilter', function() {
    var testMessage = 'filter should {0} detect `{1}` property';
    describe('Stack version >= 2.0', function() {
      before(function() {
        setups.setupStackVersion(this, 'HDP-2.1');
      });
      var tests = [
        {
          config: {
            name: 'yarn.scheduler.capacity.maximum-am-resource-percent'
          },
          e: false
        },
        {
          config: {
            name: 'yarn.scheduler.capacity.root.capacity'
          },
          e: false
        },
        {
          config: {
            name: 'yarn.scheduler.capacity.root.default.capacity'
          },
          e: true
        }
      ];

      tests.forEach(function(test){
        it(testMessage.format( !!test.e ? '' : 'not', test.config.name), function() {
          expect(App.config.get('capacitySchedulerFilter')(test.config)).to.eql(test.e);
        });
      });
      after(function() {
        setups.restoreStackVersion(this);
      })
    });

    describe('Stack version < 2.0', function() {
      before(function() {
        setups.setupStackVersion(this, 'HDP-1.3');
      });
      var tests = [
        {
          config: {
            name: 'mapred.capacity-scheduler.maximum-system-jobs'
          },
          e: false
        },
        {
          config: {
            name: 'yarn.scheduler.capacity.root.capacity'
          },
          e: false
        },
        {
          config: {
            name: 'mapred.capacity-scheduler.queue.default.capacity'
          },
          e: true
        },
        {
          config: {
            name: 'mapred.queue.default.acl-administer-jobs'
          },
          e: true
        }
      ];

      tests.forEach(function(test){
        it(testMessage.format( !!test.e ? '' : 'not', test.config.name), function() {
          expect(App.config.get('capacitySchedulerFilter')(test.config)).to.eql(test.e);
        });
      });

      after(function() {
        setups.restoreStackVersion(this);
      });
    });
  });

  describe('#fileConfigsIntoTextarea', function () {
    var filename = 'capacity-scheduler.xml';
    var configs = [
      {
        name: 'config1',
        value: 'value1',
        defaultValue: 'value1',
        filename: 'capacity-scheduler.xml'
      },
      {
        name: 'config2',
        value: 'value2',
        defaultValue: 'value2',
        filename: 'capacity-scheduler.xml'
      }
    ];
    it('two configs into textarea', function () {
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('config1=value1\nconfig2=value2\n');
      expect(result[0].defaultValue).to.equal('config1=value1\nconfig2=value2\n');
    });
    it('three config into textarea', function () {
      configs.push({
        name: 'config3',
        value: 'value3',
        defaultValue: 'value3',
        filename: 'capacity-scheduler.xml'
      });
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('config1=value1\nconfig2=value2\nconfig3=value3\n');
      expect(result[0].defaultValue).to.equal('config1=value1\nconfig2=value2\nconfig3=value3\n');
    });
    it('one of three configs has different filename', function () {
      configs[1].filename = 'another filename';
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      //result contains two configs: one with different filename and one textarea config
      expect(result.length).to.equal(2);
      expect(result[1].value).to.equal('config1=value1\nconfig3=value3\n');
      expect(result[1].defaultValue).to.equal('config1=value1\nconfig3=value3\n');
    });
    it('none configs into empty textarea', function () {
      filename = 'capacity-scheduler.xml';
      configs.clear();
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('');
      expect(result[0].defaultValue).to.equal('');
    });

  });

  describe('#textareaIntoFileConfigs', function () {
    var filename = 'capacity-scheduler.xml';
    var testData = [
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1",
          "filename": "capacity-scheduler.xml"
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1\nconfig2=value2\n",
          "filename": "capacity-scheduler.xml"
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1,value2\n",
          "filename": "capacity-scheduler.xml"
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1 config2=value2\n",
          "filename": "capacity-scheduler.xml"
        })]
      }
    ];

    it('config1=value1 to one config', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[0].configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('value1');
      expect(result[0].name).to.equal('config1');
    });
    it('config1=value1\\nconfig2=value2\\n to two configs', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[1].configs, filename);
      expect(result.length).to.equal(2);
      expect(result[0].value).to.equal('value1');
      expect(result[0].name).to.equal('config1');
      expect(result[1].value).to.equal('value2');
      expect(result[1].name).to.equal('config2');
    });
    it('config1=value1,value2\n to one config', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[2].configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('value1,value2');
      expect(result[0].name).to.equal('config1');
    });
    it('config1=value1 config2=value2 to two configs', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[3].configs, filename);
      expect(result.length).to.equal(1);
    });
  });

  describe('#addAvancedConfigs()', function() {
    before(function() {
      this.storedConfigs = modelSetup.setupStoredConfigsObject();
    });

    it('`custom.zoo.cfg` absent in stored configs', function() {
      expect(this.storedConfigs.findProperty('name', 'custom.zoo.cfg')).to.be.undefined;
    });

    it('`custom.zoo.cfg.` from advanced configs should be added to stored configs', function() {
      App.config.addAdvancedConfigs(this.storedConfigs, modelSetup.setupAdvancedConfigsObject(), 'ZOOKEEPER');
      var property = this.storedConfigs.findProperty('name', 'custom.zoo.cfg');
      expect(property).to.be.ok;
      expect(property.category).to.eql('Advanced zoo.cfg');
    });

    it('`capacity-scheduler.xml` property with name `content` should have `displayType` `multiLine`', function() {
      expect(this.storedConfigs.filterProperty('filename', 'capacity-scheduler.xml').findProperty('name','content').displayType).to.eql('multiLine');
    });
  });

  describe('#trimProperty',function() {
    var testMessage = 'displayType `{0}`, value `{1}`{3} should return `{2}`';
    var tests = [
      {
        config: {
          displayType: 'directory',
          value: ' /a /b /c'
        },
        e: '/a,/b,/c'
      },
      {
        config: {
          displayType: 'directories',
          value: ' /a /b '
        },
        e: '/a,/b'
      },
      {
        config: {
          displayType: 'datanodedirs',
          value: ' [DISK]/a [SSD]/b '
        },
        e: '[DISK]/a,[SSD]/b'
      },
      {
        config: {
          displayType: 'host',
          value: ' localhost '
        },
        e: 'localhost'
      },
      {
        config: {
          displayType: 'password',
          value: ' passw ord '
        },
        e: ' passw ord '
      },
      {
        config: {
          displayType: 'advanced',
          value: ' value'
        },
        e: ' value'
      },
      {
        config: {
          displayType: 'advanced',
          value: ' value'
        },
        e: ' value'
      },
      {
        config: {
          displayType: 'advanced',
          value: 'http://localhost ',
          name: 'javax.jdo.option.ConnectionURL'
        },
        e: 'http://localhost'
      },
      {
        config: {
          displayType: 'advanced',
          value: 'http://localhost    ',
          name: 'oozie.service.JPAService.jdbc.url'
        },
        e: 'http://localhost'
      },
      {
        config: {
          displayType: 'custom',
          value: ' custom value '
        },
        e: ' custom value'
      },
      {
        config: {
          displayType: 'masterHosts',
          value: ['host1.com', 'host2.com']
        },
        e: ['host1.com', 'host2.com']
      }
    ];

    tests.forEach(function(test) {
      it(testMessage.format(test.config.displayType, test.config.value, test.e, !!test.config.name ? ', name `' + test.config.name + '`' : ''), function() {
        expect(App.config.trimProperty(test.config)).to.eql(test.e);
        expect(App.config.trimProperty(Em.Object.create(test.config), true)).to.eql(test.e);
      });
    });
  });

  describe('#OnNnHAHideSnn()', function() {
    it('`SNameNode` category present in `ServiceConfig`. It should be removed.', function() {
      App.store.load(App.HDFSService, {
        'id': 'HDFS'
      });
      var ServiceConfig = Em.Object.create({
        configCategories: [ { name: 'SNameNode' } ]
      });
      expect(ServiceConfig.get('configCategories').findProperty('name','SNameNode')).to.ok;
      App.config.OnNnHAHideSnn(ServiceConfig);
      expect(ServiceConfig.get('configCategories').findProperty('name','SNameNode')).to.undefined;
      var record = App.HDFSService.find('HDFS');
      record.deleteRecord();
      record.get('stateManager').transitionTo('loading');
    });
    it('`SNameNode` category absent in `ServiceConfig`. Nothing to do.', function() {
      App.store.load(App.HDFSService, {
        'id': 'HDFS'
      });
      var ServiceConfig = Em.Object.create({
        configCategories: [ { name: 'DataNode' } ]
      });
      App.config.OnNnHAHideSnn(ServiceConfig);
      expect(ServiceConfig.get('configCategories').findProperty('name','DataNode')).to.ok;
      expect(ServiceConfig.get('configCategories.length')).to.eql(1);
    });
  });

  describe('#preDefinedConfigFile', function() {
    before(function() {
      setups.setupStackVersion(this, 'BIGTOP-0.8');
    });

    it('bigtop site properties should be ok.', function() {
      var bigtopSiteProperties = App.config.preDefinedConfigFile('site_properties');
      expect(bigtopSiteProperties).to.be.ok;
    });

    it('a non-existing file should not be ok.', function () {
      var notExistingSiteProperty = App.config.preDefinedConfigFile('notExisting');
      expect(notExistingSiteProperty).to.not.be.ok;
    });

    after(function() {
      setups.restoreStackVersion(this);
    });
  });

  describe('#preDefinedSiteProperties-bigtop', function () {
    before(function() {
      setups.setupStackVersion(this, 'BIGTOP-0.8');
    });

    it('bigtop should use New PostgreSQL Database as its default hive metastore database', function () {
      expect(App.config.get('preDefinedSiteProperties').findProperty('defaultValue', 'New PostgreSQL Database')).to.be.ok;
    });

    after(function() {
      setups.restoreStackVersion(this);
    });
  });

  describe('#preDefinedSiteProperties-hdp2', function () {
    before(function() {
      setups.setupStackVersion(this, 'HDP-2.0');
    });

    it('HDP2 should use New MySQL Database as its default hive metastore database', function () {
      expect(App.config.get('preDefinedSiteProperties').findProperty('defaultValue', 'New MySQL Database')).to.be.ok;
    });

    after(function() {
      setups.restoreStackVersion(this);
    });
  });

  describe('#generateConfigPropertiesByName', function() {
    var tests = [
      {
        names: ['property_1', 'property_2'],
        properties: undefined,
        e: {
          keys: ['name', 'displayName', 'isVisible', 'isReconfigurable']
        },
        m: 'Should generate base property object without additional fields'
      },
      {
        names: ['property_1', 'property_2'],
        properties: { category: 'SomeCat', serviceName: 'SERVICE_NAME' },
        e: {
          keys: ['name', 'displayName', 'isVisible', 'isReconfigurable', 'category', 'serviceName']
        },
        m: 'Should generate base property object without additional fields'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).length).to.eql(test.names.length);
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).map(function(property) {
          return Em.keys(property);
        }).reduce(function(p, c) {
          return p.concat(c);
        }).uniq()).to.eql(test.e.keys);
      });
    });

  });

  describe('#generateConfigPropertiesByName', function() {
    var tests = [
      {
        names: ['property_1', 'property_2'],
        properties: undefined,
        e: {
          keys: ['name', 'displayName', 'isVisible', 'isReconfigurable']
        },
        m: 'Should generate base property object without additional fields'
      },
      {
        names: ['property_1', 'property_2'],
        properties: { category: 'SomeCat', serviceName: 'SERVICE_NAME' },
        e: {
          keys: ['name', 'displayName', 'isVisible', 'isReconfigurable', 'category', 'serviceName']
        },
        m: 'Should generate base property object without additional fields'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).length).to.eql(test.names.length);
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).map(function(property) {
          return Em.keys(property);
        }).reduce(function(p, c) {
          return p.concat(c);
        }).uniq()).to.eql(test.e.keys);
      });
    });

  });

  describe('#generateConfigPropertiesByName', function() {
    var tests = [
      {
        names: ['property_1', 'property_2'],
        properties: undefined,
        e: {
          keys: ['name', 'displayName', 'isVisible', 'isReconfigurable']
        },
        m: 'Should generate base property object without additional fields'
      },
      {
        names: ['property_1', 'property_2'],
        properties: { category: 'SomeCat', serviceName: 'SERVICE_NAME' },
        e: {
          keys: ['name', 'displayName', 'isVisible', 'isReconfigurable', 'category', 'serviceName']
        },
        m: 'Should generate base property object without additional fields'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).length).to.eql(test.names.length);
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).map(function(property) {
          return Em.keys(property);
        }).reduce(function(p, c) {
          return p.concat(c);
        }).uniq()).to.eql(test.e.keys);
      });
    });

  });

  describe('#isManagedMySQLForHiveAllowed', function () {

    var cases = [
      {
        osType: 'redhat5',
        expected: false
      },
      {
        osType: 'redhat6',
        expected: true
      },
      {
        osType: 'sles11',
        expected: false
      }
    ],
      title = 'should be {0} for {1}';

    cases.forEach(function (item) {
      it(title.format(item.expected, item.osType), function () {
        expect(App.config.isManagedMySQLForHiveAllowed(item.osType)).to.equal(item.expected);
      });
    });

  });

});
