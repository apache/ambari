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

  describe('#identifyCategory', function () {
    var data = {};
    it('should return null if config doesn\'t have category', function () {
      expect(App.config.identifyCategory(data)).to.equal(null);
    });
    it('should return "AdvancedCoreSite" if filename "core-site.xml" and serviceName "HDFS"', function () {
      data = {
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      };
      expect(App.config.identifyCategory(data).name).to.equal('AdvancedCoreSite');
    });
    it('should return "CapacityScheduler" if filename "capacity-scheduler.xml" and serviceName "YARN"', function () {
      data = {
        serviceName: 'YARN',
        filename: 'capacity-scheduler.xml'
      };
      expect(App.config.identifyCategory(data).name).to.equal('CapacityScheduler');
    });
  });

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

  describe('#calculateConfigProperties', function () {
    var config = {};
    var isAdvanced = false;
    var advancedConfigs = [];
    it('isUserProperty should be true if config is custom(site.xml) and not advanced', function () {
      config = {
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      };
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.isUserProperty).to.equal(true);
    });

    it('isUserProperty should be false if config from "capacity-scheduler.xml" or "mapred-queue-acls.xml" ', function () {
      config = {
        name: 'test',
        serviceName: 'MAPREDUCE',
        filename: 'capacity-scheduler.xml',
        isUserProperty: false
      };
      isAdvanced = true;
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.isUserProperty).to.equal(false);
    });

    it('isRequired should be false if config is advanced"', function () {
      config = {
        name: 'test',
        serviceName: 'HDFS',
        filename: 'core-site.xml'
      };
      isAdvanced = true;
      advancedConfigs = [{name:'test', filename: 'core-site.xml'}];
      App.config.calculateConfigProperties(config, isAdvanced, advancedConfigs);
      expect(config.category).to.equal('Advanced');
      expect(config.isRequired).to.equal(true);
      expect(config.filename).to.equal('core-site.xml');
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

  describe('#escapeXMLCharacters', function () {

    var testConfigs = [
      {
        html: '&>"',
        json: '&>"'
      },
      {
        html: '&amp;&gt;&quot;&apos;',
        json: '&>"\''
      },
      {
        html: '&&gt;',
        json: '&>'
      },
      {
        html: '&&&amp;',
        json: '&&&'
      },
      {
        html: 'LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &amp;&gt; /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`',
        json: 'LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &> /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`'
      },
      {
        html: '&&&amp;',
        json: '&amp;&amp;&amp;',
        toXml: true
      }
    ];
    testConfigs.forEach(function(t){
      it('parsing html ' + t.html + ' `toXml` param passed ' + !!t.toXml, function () {
        expect(t.json).to.equal(App.config.escapeXMLCharacters(t.html, t.toXml));
      });
    });
  });

  describe('#mergePreDefinedWithLoaded()', function() {
    before(function() {
      loadServiceModelsData(['HDFS','STORM','ZOOKEEPER']);
      setups.setupStackVersion(this, 'HDP-2.1');
    });

    describe('Load STORM configs: global, storm-site', function() {
      before(function() {
        loadServiceSpecificConfigs(this, "STORM");
      });

      it('site property with `masterHosts` display type should pass value validation', function() {
        var property = this.result.configs.findProperty('name', 'storm.zookeeper.servers');
        expect(property).to.be.ok;
        expect(property.displayType).to.eql('masterHosts');
        expect(property.value).to.eql(["c6401.ambari.apache.org", "c6402.ambari.apache.org"]);
        expect(property.category).to.eql('General')
      });
      it('non-predefined global properties should not be displayed on UI', function() {
        var property = this.result.globalConfigs.findProperty('name', 'nonexistent_property');
        expect(property).to.be.a('object');
        expect(property.isVisible).to.be.false;
      });
      it('non-predefined site properties should have displayType advanced/multiLine', function() {
        var tests = [
          {
            property: 'single_line_property',
            e: 'advanced'
          },
          {
            property: 'multi_line_property',
            e: 'multiLine'
          }
        ];
        tests.forEach(function(test) {
          var property = this.result.configs.findProperty('name', test.property);
          expect(property).to.be.ok;
          expect(property.displayType).to.eql(test.e);
        }, this);

      });
    });

    describe('Load HDFS configs: global, hdfs-site, core-site', function() {
      before(function() {
        loadServiceSpecificConfigs(this, "HDFS");
      });

      it('Data Node, Name Node, SName Node directory properties should have sorted values', function() {
        var tests = [
          {
            property: "dfs.datanode.data.dir",
            e: '/a,/b'
          },
          {
            property: "dfs.namenode.name.dir",
            e: '/a,/b,/c'
          },
          {
            property: "dfs.namenode.checkpoint.dir",
            e: '/a'
          }
        ];
        tests.forEach(function(test) {
          var property = this.result.configs.findProperty('name', test.property);
          expect(property).to.be.ok;
          expect(property.value).to.eql(test.e);
        }, this);
      });
    });

    describe('Load ZOOKEEPER configs: global, zoo.cfg', function() {
      before(function() {
        loadServiceSpecificConfigs(this, "ZOOKEEPER");
      });

      it('zoo.cfg configs should have non xml filename', function() {
        expect(this.result.configs.findProperty('name', 'custom.zoo.cfg').filename).to.eql('zoo.cfg');
      });
    });

    after(function() {
      removeServiceModelData(['HDFS','STORM','ZOOKEEPER']);
      setups.restoreStackVersion(this);
    });
  });

  describe('#syncOrderWithPredefined()', function() {
    before(function() {
      setups.setupStackVersion(this, 'HDP-2.1');
      loadServiceModelsData(['HDFS','STORM','ZOOKEEPER']);
      loadServiceSpecificConfigs(this, 'HDFS');
    });
    it('properties should be ordered according to position in predefined data', function() {
      var result = App.config.syncOrderWithPredefined(this.result);
      expect(result).to.be.a('object');
      expect(result.configs.filterProperty('category','DataNode').mapProperty('name')).to.eql(['dfs.datanode.failed.volumes.tolerated', 'dfs.datanode.data.dir']);
    });
    after(function() {
      removeServiceModelData(['HDFS','STORM','ZOOKEEPER']);
    });
  });

  describe('#mergePreDefinedWithStored()', function() {
    describe('without `storedConfigs` parameter', function() {
      before(function() {
        this.installedServiceNames = ['HDFS','STORM', 'ZOOKEEPER'];
        setupContentForMergeWithStored(this);
      });

      var tests = [
        {
          property: 'dfs.datanode.data.dir',
          e: '/hadoop/hdfs/data'
        },
        {
          property: 'dfs.datanode.failed.volumes.tolerated',
          e: '2'
        }
      ];

      tests.forEach(function(test) {
        it('should set value and defaultValue to ' + test.e + ' for `' + test.property + '`', function() {
          expect(this.result.findProperty('name', test.property).value).to.eql(test.e);
          expect(this.result.findProperty('name', test.property).defaultValue).to.eql(test.e);
        });
      });

      after(function() {
        removeServiceModelData(this.installedServiceNames);
        setups.restoreStackVersion(this);
      });
    });

    describe('with `storedConfigs` parameter', function() {
      before(function() {
        this.installedServiceNames = ['HDFS','STORM','ZOOKEEPER'];
        this.storedConfigs = modelSetup.setupStoredConfigsObject();
        setupContentForMergeWithStored(this);
      });

      var tests = [
        {
          property: 'nonexistent_property',
          stored: true,
          e: {
            value: 'some value',
            isVisible: false,
            category: 'Advanced',
            displayType: 'advanced',
            isRequired: true,
            isOverridable: true
          }
        },
        {
          property: 'content',
          filename: 'hdfs-log4j.xml',
          stored: true,
          predefined: true,
          e: {
            value: 'hdfs log4j content',
            defaultValue: 'hdfs log4j content',
            displayType: 'content'
          }
        },
        {
          property: 'content',
          filename: 'zookeeper-log4j.xml',
          stored: false,
          predefined: true,
          e: {
            value: 'zookeeper log4j.xml content',
            defaultValue: 'zookeeper log4j.xml content',
            displayType: 'content'
          }
        }
      ];

      tests.forEach(function(test) {
        it('`{0}` should pass validation. stored/predefined: {1}/{2}'.format(test.property, !!test.stored, !!test.predefined), function() {
          var property = test.property == 'content' ? this.result.filterProperty('name', 'content').findProperty('filename', test.filename) : this.result.findProperty('name', test.property);
          for (var key in test.e) {
            expect(property[key]).to.be.eql(test.e[key]);
          }
        });
      });

      after(function(){
        removeServiceModelData(this.installedServiceNames);
        setups.restoreStackVersion(this);
      });
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
      expect(property.category).to.eql('Advanced');
    });

    it('`capacity-scheduler.xml` configs related to `YARN` service should have category `CapacityScheduler`', function() {
      App.config.addAdvancedConfigs(this.storedConfigs, modelSetup.setupAdvancedConfigsObject(), 'YARN');
      expect(this.storedConfigs.filterProperty('filename', 'capacity-scheduler.xml').mapProperty('category').uniq()).to.eql(['CapacityScheduler']);
    });
    it('`capacity-scheduler.xml` property with name `content` should have `displayType` `multiLine`', function() {
      expect(this.storedConfigs.filterProperty('filename', 'capacity-scheduler.xml').findProperty('name','content').displayType).to.eql('multiLine');
    });
  });

  describe('#addCustomConfigs()', function() {
    before(function() {
      setups.setupStackVersion(this, 'HDP-2.1');
      this.storedConfigs = modelSetup.setupStoredConfigsObject();
      App.config.addAdvancedConfigs(this.storedConfigs, modelSetup.setupAdvancedConfigsObject(), 'ZOOKEEPER');
      App.config.addAdvancedConfigs(this.storedConfigs, modelSetup.setupAdvancedConfigsObject(), 'YARN');
    });

    it('`yarn.scheduler.capacity.root.default.capacity` should have `isQueue` flag on', function() {
      App.config.addCustomConfigs(this.storedConfigs);
      expect(this.storedConfigs.findProperty('name','yarn.scheduler.capacity.root.default.capacity').isQueue).to.be.ok;
    });

    after(function() {
      setups.restoreStackVersion(this);
    });
  });

  describe('#createServiceConfig()', function() {
    it('should create valid object for `HDFS`', function() {
      var ServiceConfig = App.config.createServiceConfig('HDFS');
      expect(ServiceConfig.configCategories.mapProperty('name')).to.include.members(["NameNode","SNameNode","DataNode"]);
    });
    it('should create valid object for `YARN` with capacity scheduler flag `on`', function() {
      var ServiceConfig = App.config.createServiceConfig('YARN');
      expect(ServiceConfig.configCategories.mapProperty('name')).to.include.members(["ResourceManager","NodeManager"]);
      expect(ServiceConfig.configCategories.findProperty('name', 'CapacityScheduler').customView).to.be.a('Function');
      expect(ServiceConfig.configCategories.findProperty('name', 'CapacityScheduler').isCustomView).to.true;
    });
    it('should create valid object for `YARN` with capacity scheduler flag `off`', function() {
      App.supports.capacitySchedulerUi = false;
      var ServiceConfig = App.config.createServiceConfig('YARN');
      expect(ServiceConfig.configCategories.mapProperty('name')).to.include.members(["ResourceManager","NodeManager"]);
      expect(ServiceConfig.configCategories.findProperty('name', 'CapacityScheduler').isCustomView).to.false;
      App.supports.capacitySchedulerUi = true;
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
});