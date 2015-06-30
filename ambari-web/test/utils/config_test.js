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

  var loadServiceSpecificConfigs = function(context, serviceName) {
    context.configGroups = modelSetup.setupConfigGroupsObject(serviceName);
    context.advancedConfigs = modelSetup.setupAdvancedConfigsObject();
    context.tags = modelSetup.setupServiceConfigTagsObject(serviceName);
    context.result = App.config.mergePreDefinedWithLoaded(context.configGroups, context.advancedConfigs, context.tags, App.Service.find().findProperty('id', serviceName).get('serviceName'));
  };

  var loadAllServicesConfigs = function(context, serviceNames) {
    context.configGroups = modelSetup.setupConfigGroupsObject();
  };

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
        savedValue: '1024m'
      };
      App.config.handleSpecialProperties(config);
      expect(config.value).to.equal('1024');
      expect(config.savedValue).to.equal('1024');
    });
  });

  describe('#fileConfigsIntoTextarea', function () {
    var filename = 'capacity-scheduler.xml';
    var configs = [
      {
        name: 'config1',
        value: 'value1',
        recommendedValue: 'value1',
        filename: 'capacity-scheduler.xml'
      },
      {
        name: 'config2',
        value: 'value2',
        recommendedValue: 'value2',
        filename: 'capacity-scheduler.xml'
      }
    ];
    it('two configs into textarea', function () {
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('config1=value1\nconfig2=value2\n');
      expect(result[0].recommendedValue).to.equal('config1=value1\nconfig2=value2\n');
    });
    it('three config into textarea', function () {
      configs.push({
        name: 'config3',
        value: 'value3',
        recommendedValue: 'value3',
        filename: 'capacity-scheduler.xml'
      });
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('config1=value1\nconfig2=value2\nconfig3=value3\n');
      expect(result[0].recommendedValue).to.equal('config1=value1\nconfig2=value2\nconfig3=value3\n');
    });
    it('one of three configs has different filename', function () {
      configs[1].filename = 'another filename';
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      //result contains two configs: one with different filename and one textarea config
      expect(result.length).to.equal(2);
      expect(result[1].value).to.equal('config1=value1\nconfig3=value3\n');
      expect(result[1].recommendedValue).to.equal('config1=value1\nconfig3=value3\n');
    });
    it('none configs into empty textarea', function () {
      filename = 'capacity-scheduler.xml';
      configs.clear();
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('');
      expect(Em.isNone(result[0].recommendedValue)).to.be.true;
      expect(Em.isNone(result[0].savedValue)).to.be.true;
    });
    it("filename has configs that shouldn't be included in textarea", function () {
      var configs = [
        {
          name: 'config1',
          value: 'value1',
          recommendedValue: 'value1',
          filename: filename
        },
        {
          name: 'config2',
          value: 'value2',
          recommendedValue: 'value2',
          filename: filename
        }
      ];
      var cfg = {
        name: 'config3',
        value: 'value3',
        recommendedValue: 'value3',
        filename: filename
      };
      configs.push(cfg);
      var result = App.config.fileConfigsIntoTextarea.call(App.config, configs, filename, [cfg]);
      expect(result.length).to.equal(2);
      expect(result[1].value).to.equal('config1=value1\nconfig2=value2\n');
      expect(result[1].recommendedValue).to.equal('config1=value1\nconfig2=value2\n');
      expect(configs.findProperty('name', 'config3')).to.eql(cfg);
    });
  });

  describe('#textareaIntoFileConfigs', function () {
    var filename = 'capacity-scheduler.xml';
    var testData = [
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1",
          "filename": "capacity-scheduler.xml",
          "isRequiredByAgent": true
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1\nconfig2=value2\n",
          "filename": "capacity-scheduler.xml",
          "isRequiredByAgent": false
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1,value2\n",
          "filename": "capacity-scheduler.xml",
          "isRequiredByAgent": true
        })]
      },
      {
        configs: [Em.Object.create({
          "name": "capacity-scheduler",
          "value": "config1=value1 config2=value2\n",
          "filename": "capacity-scheduler.xml",
          "isRequiredByAgent": false
        })]
      }
    ];

    it('config1=value1 to one config', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[0].configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('value1');
      expect(result[0].name).to.equal('config1');
      expect(result[0].isRequiredByAgent).to.be.true;
    });
    it('config1=value1\\nconfig2=value2\\n to two configs', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[1].configs, filename);
      expect(result.length).to.equal(2);
      expect(result[0].value).to.equal('value1');
      expect(result[0].name).to.equal('config1');
      expect(result[1].value).to.equal('value2');
      expect(result[1].name).to.equal('config2');
      expect(result[0].isRequiredByAgent).to.be.false;
      expect(result[1].isRequiredByAgent).to.be.false;
    });
    it('config1=value1,value2\n to one config', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[2].configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].value).to.equal('value1,value2');
      expect(result[0].name).to.equal('config1');
      expect(result[0].isRequiredByAgent).to.be.true;
    });
    it('config1=value1 config2=value2 to two configs', function () {
      var result = App.config.textareaIntoFileConfigs.call(App.config, testData[3].configs, filename);
      expect(result.length).to.equal(1);
      expect(result[0].isRequiredByAgent).to.be.false;
    });
  });

  describe('#addAvancedConfigs()', function() {
    beforeEach(function() {
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
      App.config.addAdvancedConfigs(this.storedConfigs, modelSetup.setupAdvancedConfigsObject(), 'YARN');
      expect(this.storedConfigs.filterProperty('filename', 'capacity-scheduler.xml').findProperty('name','content').displayType).to.eql('multiLine');
    });

    it('storing different configs with the same name', function () {
      App.config.addAdvancedConfigs(this.storedConfigs, modelSetup.setupAdvancedConfigsObject(), 'HBASE');
      var properties = this.storedConfigs.filterProperty('name', 'hbase_log_dir');
      var hbaseProperty = properties.findProperty('filename', 'hbase-env.xml');
      var amsProperty = properties.findProperty('filename', 'ams-hbase-env.xml');
      expect(properties).to.have.length(2);
      expect(hbaseProperty.serviceName).to.equal('HBASE');
      expect(hbaseProperty.value).to.equal('/hadoop/hbase');
      expect(amsProperty.serviceName).to.equal('AMBARI_METRICS');
      expect(amsProperty.value).to.equal('/hadoop/ams-hbase');
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
          displayType: 'datanodedirs',
          value: '/a,/b, /c\n/d,\n/e  /f'
        },
        e: '/a,/b,/c,/d,/e,/f'
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
      expect(App.config.get('preDefinedSiteProperties').findProperty('recommendedValue', 'New PostgreSQL Database')).to.be.ok;
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
      expect(App.config.get('preDefinedSiteProperties').findProperty('recommendedValue', 'New MySQL Database')).to.be.ok;
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

  describe('#setPreDefinedServiceConfigs', function() {
    beforeEach(function() {
      sinon.stub(App.StackService, 'find', function() {
        return [
          Em.Object.create({
            id: 'HDFS',
            serviceName: 'HDFS',
            configTypes: {
              'hadoop-env': {},
              'hdfs-site': {}
            }
          }),
          Em.Object.create({
            id: 'OOZIE',
            serviceName: 'OOZIE',
            configTypes: {
              'oozie-env': {},
              'oozie-site': {}
            }
          })
        ];
      });
      App.config.setPreDefinedServiceConfigs(true);
    });
    afterEach(function() {
      App.StackService.find.restore();
    });

    it('should include service MISC', function() {
      expect(App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'MISC')).to.be.ok;
    });

    it('should include -env config types according to stack services', function() {
      var miscCategory = App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'MISC');
      expect(Em.keys(miscCategory.get('configTypes'))).to.eql(['cluster-env', 'hadoop-env', 'oozie-env']);
    });

    it('should not load configs for missed config types', function() {
      var hdfsService = App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'HDFS');
      var rangerRelatedConfigs = hdfsService.get('configs').filterProperty('filename', 'ranger-hdfs-plugin-properties.xml');
      expect(rangerRelatedConfigs.length).to.be.eql(0);
      expect(hdfsService.get('configs.length') > 0).to.be.true;
    });
  });
  
  describe('#isManagedMySQLForHiveAllowed', function () {

    var cases = [
      {
        osFamily: 'redhat5',
        expected: false
      },
      {
        osFamily: 'redhat6',
        expected: true
      },
      {
        osFamily: 'suse11',
        expected: false
      }
    ],
      title = 'should be {0} for {1}';

    cases.forEach(function (item) {
      it(title.format(item.expected, item.osFamily), function () {
        expect(App.config.isManagedMySQLForHiveAllowed(item.osFamily)).to.equal(item.expected);
      });
    });

  });

  describe('#createAdvancedPropertyObject', function() {
    var tests = [
      {
        name: 'proxyuser_group',
        cases: [
          {
            key: 'displayType',
            e: 'user'
          },
          {
            key: 'serviceName',
            e: 'MISC'
          },
          {
            key: 'belongsToService',
            e: ['HIVE', 'OOZIE', 'FALCON']
          }
        ]
      },
      {
        name: 'oozie.service.JPAService.jdbc.password',
        cases: [
          {
            key: 'displayType',
            e: 'password'
          },
          {
            key: 'isVisible',
            e: true
          }
        ]
      },
      {
        name: 'ignore_groupsusers_create',
        cases: [
          {
            key: 'isVisible',
            e: false
          }
        ]
      },
      {
        name: 'user_group',
        cases: [
          {
            key: 'isVisible',
            e: true
          },
          {
            key: 'index',
            e: 30
          },
          {
            key: 'displayName',
            e: 'Hadoop Group'
          }
        ]
      },
      {
        name: 'zk_user',
        cases: [
          {
            key: 'displayName',
            e: 'ZooKeeper User'
          }
        ]
      },
      {
        name: 'mapred_user',
        cases: [
          {
            key: 'displayName',
            e: 'MapReduce User'
          }
        ]
      },
      {
        name: 'smokeuser',
        cases: [
          {
            key: 'displayName',
            e: 'Smoke Test User'
          }
        ]
      }
    ];

    var properties = [];
    modelSetup.advancedConfigs.items.forEach(function(item) {
      properties.push(App.config.createAdvancedPropertyObject(item.StackConfigurations));
    });
    App.config.loadClusterConfigSuccess(modelSetup.advancedClusterConfigs, {url: '/cluster/configurations'}, {callback: function (items) {properties = properties.concat(items)}});

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('isHadoopWindowsStack').returns(false);
    });

    afterEach(function () {
      App.get.restore();
    });

    tests.forEach(function(test) {
      test.cases.forEach(function(testCase) {
        it('config property `{0}` `{1}` key should be`{2}`'.format(test.name, testCase.key, testCase.e), function() {
          var property = properties.findProperty('name', test.name);
          expect(Em.get(property, testCase.key)).to.eql(testCase.e);
        });
      });
    });
  });

  describe('#mergePreDefinedWithLoaded', function() {
    var result;

    before(function() {
      sinon.stub(App.config, 'parseValue', function(value) {return value});
      sinon.stub(App.config, 'getConfigTypesInfoFromService').returns({
        supportsFinal: ['hdfs-site']
      });
      setups.setupStackVersion(this, 'HDP-2.2');
      loadServiceModelsData(['HDFS', 'STORM']);
      App.config.loadAdvancedConfigSuccess(modelSetup.advancedConfigs, { url: '/serviceName/configurations'}, {
        callback: function(advancedConfigs) {
          App.config.loadClusterConfigSuccess(modelSetup.advancedClusterConfigs, { url: '/cluster/configurations'}, {
            callback: function(clusterConfigs) {
              var configCategories = modelSetup.setupConfigGroupsObject();
              var tags = [
                {newTagName: null, tagName: 'version1', siteName: 'hadoop-env'},
                {newTagName: null, tagName: 'version1', siteName: 'hdfs-site'},
                {newTagName: null, tagName: 'version1', siteName: 'cluster-env'},
                {newTagName: null, tagName: 'version1', siteName: 'storm-env'}
              ];
              var serviceName = 'STORM';
              result = App.config.mergePreDefinedWithLoaded(configCategories, advancedConfigs.concat(clusterConfigs), tags, serviceName);
            }
          });
        }
      });
    });

    after(function() {
      App.config.parseValue.restore();
      App.config.getConfigTypesInfoFromService.restore();
      setups.restoreStackVersion(this);
      removeServiceModelData(['HDFS', 'STORM']);
    });
    
    var propertyTests = [
      {
        name: 'hdfs_user',
        cases: [
          {
            key: 'displayType',
            e: 'user'
          },
          {
            key: 'isVisible',
            e: true
          },
          {
            key: 'serviceName',
            e: 'MISC'
          },
          {
            key: 'category',
            e: 'Users and Groups'
          }
        ]
      }
    ];
    propertyTests.forEach(function(test) {
      test.cases.forEach(function(testCase) {
        it('config property `{0}` `{1}` key should be`{2}`'.format(test.name, testCase.key, testCase.e), function() {
          var property = result.configs.findProperty('name', test.name);
          expect(Em.get(property, testCase.key)).to.equal(testCase.e);
        });
      });
    });
  });

  describe('#replaceConfigValues', function () {

    var cases = [
      {
        name: 'name',
        express: '<templateName[0]>',
        value: '<templateName[0]>',
        globValue: 'v',
        expected: 'v',
        title: 'default case'
      },
      {
        name: 'templeton.hive.properties',
        express: '<templateName[0]>',
        value: 'hive.matestore.uris=<templateName[0]>',
        globValue: 'thrift://h0:9933,thrift://h1:9933,thrift://h2:9933',
        expected: 'hive.matestore.uris=thrift://h0:9933\\,thrift://h1:9933\\,thrift://h2:9933',
        title: 'should escape commas for templeton.hive.properties'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(App.config.replaceConfigValues(item.name, item.express, item.value, item.globValue)).to.equal(item.expected);
      });
    });

  });

  describe('#advancedConfigIdentityData', function () {

    var configs = [
      {
        input: {
          property_type: ['USER'],
          property_name: 'hdfs_user'
        },
        output: {
          id: 'puppet var',
          category: 'Users and Groups',
          isVisible: true,
          serviceName: 'MISC',
          isOverridable: false,
          isReconfigurable: false,
          displayName: 'HDFS User',
          displayType: 'user',
          index: 30
        },
        title: 'user, no service name specified, default display name behaviour'
      },
      {
        input: {
          property_type: ['GROUP'],
          property_name: 'knox_group',
          service_name: 'KNOX'
        },
        output: {
          id: 'puppet var',
          category: 'Users and Groups',
          isVisible: true,
          serviceName: 'MISC',
          isOverridable: false,
          isReconfigurable: false,
          displayName: 'Knox Group',
          displayType: 'user',
          index: 0
        },
        title: 'group, service_name = KNOX, default display name behaviour'
      },
      {
        input: {
          property_type: ['USER']
        },
        output: {
          isVisible: false
        },
        isHDPWIN: true,
        title: 'HDPWIN stack'
      },
      {
        input: {
          property_type: ['USER'],
          property_name: 'smokeuser',
          service_name: 'MISC'
        },
        output: {
          displayName: 'Smoke Test User',
          serviceName: 'MISC',
          belongsToService: ['MISC'],
          index: 30
        },
        title: 'smokeuser, service_name = MISC'
      },
      {
        input: {
          property_type: ['GROUP'],
          property_name: 'user_group'
        },
        output: {
          displayName: 'Hadoop Group'
        },
        title: 'user_group'
      },
      {
        input: {
          property_type: ['USER'],
          property_name: 'mapred_user'
        },
        output: {
          displayName: 'MapReduce User'
        },
        title: 'mapred_user'
      },
      {
        input: {
          property_type: ['USER'],
          property_name: 'zk_user'
        },
        output: {
          displayName: 'ZooKeeper User'
        },
        title: 'zk_user'
      },
      {
        input: {
          property_type: ['USER'],
          property_name: 'ignore_groupsusers_create'
        },
        output: {
          displayName: 'Skip group modifications during install',
          displayType: 'checkbox'
        },
        title: 'ignore_groupsusers_create'
      },
      {
        input: {
          property_type: ['GROUP'],
          property_name: 'proxyuser_group'
        },
        output: {
          belongsToService: ['HIVE', 'OOZIE', 'FALCON']
        },
        title: 'proxyuser_group'
      },
      {
        input: {
          property_type: ['PASSWORD'],
          property_name: 'javax.jdo.option.ConnectionPassword'
        },
        output: {
          displayType: 'password'
        },
        title: 'password'
      }
    ];

    before(function () {
      sinon.stub(App.StackService, 'find').returns([
        {
          serviceName: 'KNOX'
        }
      ]);
    });

    afterEach(function () {
      App.get.restore();
    });

    after(function () {
      App.StackService.find.restore();
    });

    configs.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App, 'get').withArgs('isHadoopWindowsStack').returns(Boolean(item.isHDPWIN));
        var propertyData = App.config.advancedConfigIdentityData(item.input);
        Em.keys(item.output).forEach(function (key) {
          expect(propertyData[key]).to.eql(item.output[key]);
        });
      });
    });

  });

  describe('#addUserProperty', function () {

    var cases = [
        {
          stored: {
            id: 0,
            name: 'prop_name0',
            serviceName: 's0',
            value: 'v0',
            savedValue: 'dv0',
            filename: 'fn0.xml',
            overrides: null,
            isVisible: false,
            isFinal: true,
            savedIsFinal: false,
            supportsFinal: true,
            category: 'c0'
          },
          expected: {
            id: 0,
            name: 'prop_name0',
            displayName: 'Prop Name0',
            serviceName: 's0',
            value: 'v0',
            savedValue: 'dv0',
            displayType: 'advanced',
            filename: 'fn0.xml',
            isUserProperty: false,
            hasInitialValue: false,
            isOverridable: true,
            overrides: null,
            isRequired: false,
            isVisible: false,
            isFinal: true,
            savedIsFinal: false,
            supportsFinal: true,
            showLabel: true,
            category: 'c0'
          },
          title: 'default case'
        },
        {
          stored: {
            name: 'n1',
            value: 'multi\nline',
            filename: 'fn1.xml',
            isUserProperty: true,
            hasInitialValue: true,
            showLabel: false
          },
          expected: {
            displayType: 'multiLine',
            isUserProperty: true,
            hasInitialValue: true,
            showLabel: false
          },
          title: 'multiline user property with initial value, label not to be shown'
        },
        {
          stored: {
            name: 'n2',
            filename: 'fn2.xml'
          },
          expected: {
            isUserProperty: false,
            showLabel: true
          },
          title: 'isUserProperty and showLabel not set'
        },
        {
          stored: {
            name: 'ignore_groupsusers_create',
            category: 'Users and Groups',
            filename: 'fn3.xml'
          },
          expected: {
            displayName: 'dn0',
            displayType: 'checkbox',
            index: 0
          }
        },
        {
          stored: {
            name: 'smokeuser',
            category: 'Users and Groups',
            filename: 'fn4.xml'
          },
          expected: {
            displayName: 'dn1',
            index: 1
          }
        },
        {
          stored: {
            name: 'user_group',
            category: 'Users and Groups',
            filename: 'fn5.xml'
          },
          expected: {
            displayName: 'dn1',
            index: 2
          }
        },
        {
          stored: {
            name: 'mapred_user',
            category: 'Users and Groups',
            filename: 'fn6.xml'
          },
          expected: {
            displayName: 'dn1',
            index: 3
          }
        },
        {
          stored: {
            name: 'zk_user',
            category: 'Users and Groups',
            filename: 'fn7.xml'
          },
          expected: {
            displayName: 'dn1',
            index: 4
          }
        }
      ],
      advancedConfigs = [
        {
          name: 'ignore_groupsusers_create',
          displayName: 'dn0',
          displayType: 'checkbox',
          index: 0
        },
        {
          name: 'smokeuser',
          displayName: 'dn1',
          index: 1
        },
        {
          name: 'user_group',
          displayName: 'dn1',
          index: 2
        },
        {
          name: 'mapred_user',
          displayName: 'dn1',
          index: 3
        },
        {
          name: 'zk_user',
          displayName: 'dn1',
          index: 4
        }
      ];

    cases.forEach(function (item) {
      it(item.title || item.stored.name, function () {
        var configData = App.config.addUserProperty(item.stored, true, advancedConfigs);
        Em.keys(item.expected).forEach(function (key) {
          expect(configData[key]).to.equal(item.expected[key]);
        });
      });
    });

  });

  describe('#getOriginalConfigAttribute', function () {

    var stored = {
        name: 'p',
        displayName: 'dn'
      },
      cases = [
        {
          advancedConfigs: [
            {
              name: 'p',
              displayName: 'dn0'
            }
          ],
          expected: 'dn0',
          title: 'should take attribute from advancedConfigs'
        },
        {
          advancedConfigs: [],
          expected: 'dn',
          title: 'property is absent in advancedConfigs'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(App.config.getOriginalConfigAttribute(stored, 'displayName', item.advancedConfigs)).to.equal(item.expected);
      });
    });

  });

  describe('#setConfigValue', function () {

    Em.A([
        {
          mappedConfigs: [
            {
              name: 'falcon_user',
              value: 'fu'
            }
          ],
          allConfigs: [],
          m: 'in mapped, value used',
          e: {
            _name: 'hadoop.proxyuser.fu.groups',
            value: 'fu',
            noMatchSoSkipThisConfig: false
          }
        },
        {
          mappedConfigs: [],
          allConfigs: [
            {
              name: 'falcon_user',
              value: 'fu'
            }
          ],
          m: 'in all, value used',
          e: {
            _name: 'hadoop.proxyuser.fu.groups',
            value: 'fu',
            noMatchSoSkipThisConfig: false
          }
        },
        {
          mappedConfigs: [],
          allConfigs: [
            {
              name: 'falcon_user',
              value: '',
              recommendedValue: 'fu'
            }
          ],
          m: 'in all, default value used',
          e: {
            _name: 'hadoop.proxyuser.fu.groups',
            value: 'fu',
            noMatchSoSkipThisConfig: false
          }
        },
        {
          mappedConfigs: [],
          allConfigs: [],
          m: 'not found',
          e: {
            _name: 'hadoop.proxyuser.<foreignKey[0]>.groups',
            value: '<foreignKey[0]>',
            noMatchSoSkipThisConfig: true
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          var config = {
            name: "hadoop.proxyuser.<foreignKey[0]>.groups",
            templateName: ["proxyuser_group"],
            foreignKey: ["falcon_user"],
            noMatchSoSkipThisConfig: false,
            value: "<foreignKey[0]>"
          };
          App.config.setConfigValue(test.mappedConfigs, test.allConfigs, config);
          expect(config.value).to.equal(test.e.value);
          if(test.e.noMatchSoSkipThisConfig) {
            expect(Em.isNone(config._name)).to.be.true;
          }
          else {
            expect(config._name).to.equal(test.e._name);
          }
          expect(config.noMatchSoSkipThisConfig).to.equal(test.e.noMatchSoSkipThisConfig);
        });

        Em.A([
          {
            mappedConfigs: [],
            allConfigs: [
              {
                name: 'falcon_user',
                value: 'fu'
              },
              {
                name: 'proxyuser_group',
                value: 'pg'
              }
            ],
            m: 'in all, template in all',
            e: {
              _name: 'hadoop.proxyuser.fu.groups',
              value: 'fupg'
            }
          },
            {
              mappedConfigs: [
                {
                  name: 'falcon_user',
                  value: 'fu'
                },
                {
                  name: 'proxyuser_group',
                  value: 'pg'
                }
              ],
              allConfigs: [],
              m: 'in mapped, template in mapped',
              e: {
                _name: 'hadoop.proxyuser.fu.groups',
                value: 'fupg'
              }
            },
            {
              mappedConfigs: [],
              allConfigs: [],
              m: 'not found (template not found too)',
              e: {
                _name: 'hadoop.proxyuser.<foreignKey[0]>.groups',
                value: null
              }
            }
        ]).forEach(function (test) {
            it(test.m, function () {
              var config = {
                name: "hadoop.proxyuser.<foreignKey[0]>.groups",
                templateName: ["proxyuser_group"],
                foreignKey: ["falcon_user"],
                noMatchSoSkipThisConfig: false,
                value: "<foreignKey[0]><templateName[0]>"
              };
              App.config.setConfigValue(test.mappedConfigs, test.allConfigs, config);
            });
          });

    });

  });

  describe('#shouldSupportFinal', function () {

    var cases = [
      {
        shouldSupportFinal: false,
        title: 'no service name specified'
      },
      {
        serviceName: 's0',
        shouldSupportFinal: false,
        title: 'no filename specified'
      },
      {
        serviceName: 'MISC',
        shouldSupportFinal: false,
        title: 'MISC'
      },
      {
        serviceName: 's0',
        filename: 's0-site',
        shouldSupportFinal: true,
        title: 'final attribute supported'
      },
      {
        serviceName: 's0',
        filename: 's0-env',
        shouldSupportFinal: false,
        title: 'final attribute not supported'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.StackService, 'find').returns([
        {
          serviceName: 's0'
        }
      ]);
      sinon.stub(App.config, 'getConfigTypesInfoFromService').returns({
        supportsFinal: ['s0-site']
      });
    });

    afterEach(function () {
      App.StackService.find.restore();
      App.config.getConfigTypesInfoFromService.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(App.config.shouldSupportFinal(item.serviceName, item.filename)).to.equal(item.shouldSupportFinal);
      });
    });

  });

  describe('#removeRangerConfigs', function () {

    it('should remove ranger configs and categories', function () {
      var configs = [
        Em.Object.create({
          configs: [
            Em.Object.create({filename: 'filename'}),
            Em.Object.create({filename: 'ranger-filename'})
          ],
          configCategories: [
            Em.Object.create({name: 'ranger-name'}),
            Em.Object.create({name: 'name'}),
            Em.Object.create({name: 'also-ranger-name'})
          ]
        })
      ];
      App.config.removeRangerConfigs(configs);
      expect(configs).eql(
          [
            Em.Object.create({
              configs: [
                Em.Object.create({filename: 'filename'})
              ],
              configCategories: [
                Em.Object.create({name: 'name'})
              ]
            })
          ]
      );
    });

  });

  describe("#createOverride", function() {
    var template = {
      name: "p1",
      filename: "f1",
      value: "v1",
      recommendedValue: "rv1",
      savedValue: "sv1",
      isFinal: true,
      recommendedIsFinal: false,
      savedIsFinal: true
    };

    var configProperty = App.ServiceConfigProperty.create(template);

    var group = App.ConfigGroup.create({name: "group1"});

    it('creates override with save properties as original config', function() {
      var override = App.config.createOverride(configProperty, {}, group);
      for (var key in template) {
        expect(override.get(key)).to.eql(template[key]);
      }
    });

    it('overrides some values that should be different for override', function() {
      var override = App.config.createOverride(configProperty, {}, group);
      expect(override.get('isOriginalSCP')).to.be.false;
      expect(override.get('overrides')).to.be.null;
      expect(override.get('group')).to.eql(group);
      expect(override.get('parentSCP')).to.eql(configProperty);
    });

    it('overrides some specific values', function() {
      var overridenTemplate = {
        value: "v2",
        recommendedValue: "rv2",
        savedValue: "sv2",
        isFinal: true,
        recommendedIsFinal: false,
        savedIsFinal: true
      };

      var override = App.config.createOverride(configProperty, overridenTemplate, group);
      for (var key in overridenTemplate) {
        expect(override.get(key)).to.eql(overridenTemplate[key]);
      }
    });

    it('throws error due to undefined configGroup', function() {
      expect(App.config.createOverride.bind(App.config, configProperty, {}, null)).to.throw(Error, 'configGroup can\' be null');
    });

    it('throws error due to undefined originalSCP', function() {
      expect(App.config.createOverride.bind(App.config, null, {}, group)).to.throw(Error, 'serviceConfigProperty can\' be null');
    });

    it('updates originalSCP object ', function() {
      configProperty.set('overrides', null);
      configProperty.set('overrideValues', []);
      configProperty.set('overrideIsFinalValues', []);

      var overridenTemplate2 = {
        value: "v12",
        recommendedValue: "rv12",
        savedValue: "sv12",
        isFinal: true,
        recommendedIsFinal: false,
        savedIsFinal: false
      };

      var override = App.config.createOverride(configProperty, overridenTemplate2, group);

      expect(configProperty.get('overrides')[0]).to.be.eql(override);
      expect(configProperty.get('overrideValues')).to.be.eql([overridenTemplate2.value]);
      expect(configProperty.get('overrideIsFinalValues')).to.be.eql([overridenTemplate2.isFinal]);
    });
  });

  describe('#getIsEditable', function() {
    [{
        isDefaultGroup: true,
        isReconfigurable: true,
        canEdit: true,
        res: true,
        m: "isEditable is true"
      },
      {
        isDefaultGroup: false,
        isReconfigurable: true,
        canEdit: true,
        res: false,
        m: "isEditable is false; config group is not default"
      },
      {
        isDefaultGroup: true,
        isReconfigurable: false,
        canEdit: true,
        res: false,
        m: "isEditable is true; config is not reconfigurable"
      },
      {
        isDefaultGroup: true,
        isReconfigurable: true,
        canEdit: false,
        res: false,
        m: "isEditable is true; edition restricted by controller state"
    }].forEach(function(t) {
        it(t.m, function() {
          var configProperty = Ember.Object.create({isReconfigurable: t.isReconfigurable});
          var configGroup = Ember.Object.create({isDefault: t.isDefaultGroup});
          var isEditable = App.config.getIsEditable(configProperty, configGroup, t.canEdit);
          expect(isEditable).to.equal(t.res);
        })
      });
  });

  describe('#getIsSecure', function() {
    var secureConfigs = App.config.get('secureConfigs');
    before(function() {
      App.config.set('secureConfigs', [{name: 'secureConfig'}]);
    });
    after(function() {
      App.config.set('secureConfigs', secureConfigs);
    });

    it('config is secure', function() {
      expect(App.config.getIsSecure('secureConfig')).to.equal(true);
    });
    it('config is not secure', function() {
      expect(App.config.getIsSecure('NotSecureConfig')).to.equal(false);
    });
  });

  describe('#getDefaultCategory', function() {
    it('returns custom category', function() {
      expect(App.config.getDefaultCategory(null, 'filename.xml')).to.equal('Custom filename');
    });
    it('returns advanced category', function() {
      expect(App.config.getDefaultCategory(Em.Object.create, 'filename.xml')).to.equal('Advanced filename');
    });
  });

  describe('#getDefaultDisplayType', function() {
    it('returns singleLine displayTyepe', function() {
      expect(App.config.getDefaultDisplayType('v1')).to.equal('advanced');
    });
    it('returns multiline displayTyepe', function() {
      expect(App.config.getDefaultDisplayType('v1\nv2')).to.equal('multiLine');
    });
  });

  describe('#formatValue', function() {
    it('formatValue for masterHosts', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'masterHosts', value: "['h1','h2']"});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.eql(['h1','h2']);
    });

    it('formatValue for int', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'int', value: '4.0'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('4');
    });

    it('formatValue for int with m', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'int', value: '4m'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('4');
    });

    it('formatValue for float', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'float', value: '0.40'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('0.4');
    });

    it('formatValue for kdc_type', function () {
      var serviceConfigProperty = Em.Object.create({'name': 'kdc_type', value: 'mit-kdc'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal(Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'));
    });

    it('don\'t format value', function () {
      var serviceConfigProperty = Em.Object.create({'name': 'any', displayType: 'any', value: 'any'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('any');
    });
  });

  describe('#getPropertyIfExists', function() {
    [
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someProperty: '1' },
        secondObject: { someProperty: '2' },
        res: '1',
        m: 'use value from first object'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someOtherProperty: '1' },
        secondObject: { someProperty: '2' },
        res: '2',
        m: 'use value from second object'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someOtherProperty: '1' },
        secondObject: { someOtherProperty: '2' },
        res: 'default',
        m: 'use default value'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        res: 'default',
        m: 'use default value'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: false },
        secondObject: { someProperty: true },
        res: false,
        m: 'use value from first object, check booleans'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: 0 },
        secondObject: { someProperty: 1 },
        res: 0,
        m: 'use value from first object, check 0'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: '' },
        secondObject: { someProperty: '1' },
        res: '',
        m: 'use value from first object, check empty string'
      }
    ].forEach(function (t) {
        it(t.m, function () {
          expect(App.config.getPropertyIfExists(t.propertyName, t.defaultValue, t.firstObject, t.secondObject)).to.equal(t.res);
        })
      });
  });

  describe('#mergeStackConfigsWithUI', function() {
    beforeEach(function() {
      sinon.stub(App.config, 'getPropertyIfExists', function(propertyName, defaultValue) {return defaultValue});
      sinon.stub(App.config, 'getDefaultCategory', function(stackConfigProperty, filename) {return 'Advanced ' + filename});
      sinon.stub(App.config, 'getIsSecure', function() {return false});
      sinon.stub(App.config, 'getDefaultDisplayType', function() {return 'string'});
    });

    afterEach(function() {
      App.config.getPropertyIfExists.restore();
      App.config.getDefaultCategory.restore();
      App.config.getIsSecure.restore();
      App.config.getDefaultDisplayType.restore();
    });
    var service = Em.Object.create({ serviceName:'sName' });
    var template =  {
        name: 'pName',
        filename: 'pFileName',
        value: 'v',
        savedValue: 'v',
        isFinal: true,
        savedIsFinal: true,
        recommendedValue: null,
        recommendedIsFinal: null,
        displayName: 'pName',
        description: null,
        displayType: 'string',
        category: 'Advanced pFileName',
        isSecureConfig: false,
        serviceName: 'sName',
        isVisible: true,
        isUserProperty: false,
        isRequired: true,
        id: 'site property',
        supportsFinal: false,
        isRequiredByAgent: true,
        isReconfigurable: true,
        unit: null,
        isOverridable: true,
        index: null,
        showLabel: true,
        dependentConfigPattern: null,
        options: null,
        radioName: null,
        belongsToService: []
    };
    it('performs merge UI and Stack properties', function() {
      for (var key in template){
        expect(App.config.mergeStackConfigsWithUI('pName', 'pFileName', 'v', true, service, Em.Object.create({}), Em.Object.create({}))[key]).to.eql(template[key]);
      }
    });

    it('performs merge UI and Stack properties (without stackProperty)', function() {
      template.isUserProperty = true;
      template.isRequired = false;
      for (var key in template){
        expect(App.config.mergeStackConfigsWithUI('pName', 'pFileName', 'v', true, service, null, Em.Object.create({}))[key]).to.eql(template[key]);
      }
    });

    it('performs merge UI and Stack properties (unknown service)', function () {
      template.serviceName = 'MISC';
      template.isUserProperty = false;
      template.isRequired = true;
      template.isVisible = false;
      for (var key in template) {
        expect(App.config.mergeStackConfigsWithUI('pName', 'pFileName', 'v', true, null, Em.Object.create({}), Em.Object.create({}))[key]).to.eql(template[key]);
      }
    });

    it('called getPropertyIfExists couple of times', function () {
      App.config.mergeStackConfigsWithUI('pName', 'pFileName', 'v', true, null, Em.Object.create({}), Em.Object.create({}));
      expect(App.config.getPropertyIfExists.callCount).to.equal(23);
    });

    it('called getPropertyIfExists with args', function () {
      var stackConfigProperty = Em.Object.create({stackProperty: true});
      var UIConfigProperty = Em.Object.create({UIProperty: true});
      App.config.mergeStackConfigsWithUI('pName', 'pFileName', 'v', true, service, stackConfigProperty, UIConfigProperty);

      expect(App.config.getPropertyIfExists.calledWith('recommendedValue', null, stackConfigProperty, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('recommendedIsFinal', null, stackConfigProperty, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('displayName', 'pName', stackConfigProperty, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('description', null, stackConfigProperty, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('supportsFinal', false, stackConfigProperty, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('id', 'site property', UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('isRequiredByAgent', true, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('isReconfigurable', true, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('unit', null, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('isOverridable', true, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('index', null, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('showLabel', true, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('dependentConfigPattern', null, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('options', null, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('belongsToService', [], UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('radioName', null, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('displayType', 'string', UIConfigProperty, stackConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('category', 'Advanced pFileName', UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('isSecureConfig', false, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('serviceName', 'sName', stackConfigProperty, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('isVisible', true, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('isUserProperty', false, UIConfigProperty)).to.be.true;
      expect(App.config.getPropertyIfExists.calledWith('isRequired', true, UIConfigProperty)).to.be.true;
    });
  });

});
