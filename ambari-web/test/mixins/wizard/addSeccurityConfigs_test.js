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

var stackDescriptorData = require('test/mock_data_setup/stack_descriptors');
var stackDescriptor = stackDescriptorData.artifact_data;
var controller;
require('mixins/wizard/addSecurityConfigs');

describe('App.AddSecurityConfigs', function () {

  beforeEach(function () {
    controller = Em.Object.create(App.AddSecurityConfigs, {
      content: {
        services: []
      },
      enableSubmit: function () {
        this._super();
      },
      configs: [],
      secureMapping: [],
      secureProperties: []
    });
  });

  describe('#secureServices', function() {
    it('content.services is correct', function() {
      controller.set('content.services', [{}]);
      expect(controller.get('secureServices')).to.eql([{}]);
    });
  });

  describe('#loadUiSideSecureConfigs()', function() {

    beforeEach(function(){
      sinon.stub(controller, 'checkServiceForConfigValue', function() {
        return 'value2';
      });
      sinon.stub(controller, 'setConfigValue', Em.K);
      sinon.stub(controller, 'formatConfigName', Em.K);
      sinon.stub(App.Service, 'find').returns([{serviceName: 'SOME_SERVICE'}]);
    });

    afterEach(function(){
      controller.checkServiceForConfigValue.restore();
      controller.setConfigValue.restore();
      controller.formatConfigName.restore();
      App.Service.find.restore();
    });

    it('secureMapping is empty', function() {
      controller.set('secureMapping', []);

      expect(controller.loadUiSideSecureConfigs()).to.be.empty;
    });

    it('Config does not have dependedServiceName', function() {
      controller.set('secureMapping', [{
        name: 'config1',
        value: 'value1',
        filename: 'file1',
        serviceName: 'SOME_SERVICE',
        foreignKey: null
      }]);

      expect(controller.loadUiSideSecureConfigs()).to.eql([{
        "name": 'config1',
        "value": 'value1',
        "filename": 'file1'
      }]);
    });

    it('Config has dependedServiceName', function() {
      controller.set('secureMapping', [{
        name: 'config1',
        value: 'value1',
        filename: 'file1',
        foreignKey: null,
        serviceName: 'SOME_SERVICE',
        dependedServiceName: 'SOME_SERVICE'
      }]);

      expect(controller.loadUiSideSecureConfigs()).to.eql([{
        "name": 'config1',
        "value": 'value2',
        "filename": 'file1'
      }]);
    });

    it('Config has non-existent serviceName', function() {
      controller.set('secureMapping', [{
        name: 'config1',
        value: 'value1',
        filename: 'file1',
        foreignKey: true,
        serviceName: 'NO_SERVICE'
      }]);

      expect(controller.loadUiSideSecureConfigs()).to.be.empty;
    });

    it('Config has correct serviceName', function() {
      controller.set('secureMapping', [{
        name: 'config1',
        value: 'value1',
        filename: 'file1',
        foreignKey: true,
        serviceName: 'SOME_SERVICE'
      }]);

      expect(controller.loadUiSideSecureConfigs()).to.eql([{
        "name": 'config1',
        "value": 'value1',
        "filename": 'file1'
      }]);
      expect(controller.setConfigValue.calledOnce).to.be.true;
      expect(controller.formatConfigName.calledOnce).to.be.true;
    });

  });

  describe('#checkServiceForConfigValue()', function() {

    it('services is empty', function() {
      var services = [];

      expect(controller.checkServiceForConfigValue('value1', services)).to.equal('value1');
    });

    it('Service is loaded', function() {
      var services = [{}];
      sinon.stub(App.Service, 'find', function () {
        return Em.Object.create({isLoaded: false});
      });

      expect(controller.checkServiceForConfigValue('value1', services)).to.equal('value1');

      App.Service.find.restore();
    });

    it('Service is not loaded', function() {
      var services = [{
        replace: 'val'
      }];
      sinon.stub(App.Service, 'find', function () {
        return Em.Object.create({isLoaded: false});
      });

      expect(controller.checkServiceForConfigValue('value1', services)).to.equal('ue1');

      App.Service.find.restore();
    });

  });

  describe('#formatConfigName()', function() {

    it('config.value is null', function() {
      var config = {
        value: null
      };

      expect(controller.formatConfigName([], config)).to.be.false;
    });

    it('config.name does not contain foreignKey', function() {
      var config = {
        value: 'value1',
        name: 'config1'
      };

      expect(controller.formatConfigName([], config)).to.be.false;
    });

    it('globalProperties is empty, use uiConfig', function() {
      var config = {
        value: 'value1',
        name: '<foreignKey[0]>',
        foreignKey: ['key1']
      };
      controller.set('globalProperties', []);
      var uiConfig = [{
        name: 'key1',
        value: 'globalValue1'
      }];

      expect(controller.formatConfigName(uiConfig, config)).to.be.true;
      expect(config._name).to.equal('globalValue1');
    });

  });

  describe('#setConfigValue()', function() {

    it('config.value is null', function() {
      var config = {
        value: null
      };

      expect(controller.setConfigValue(config)).to.be.false;
    });

    it('config.value does not match "templateName"', function() {
      var config = {
        value: ''
      };

      expect(controller.setConfigValue(config)).to.be.false;
    });

    it('No such property in global configs', function() {
      var config = {
        value: '<templateName[0]>',
        templateName: ['config1']
      };
      controller.set('globalProperties', []);
      controller.set('configs', []);

      expect(controller.setConfigValue(config)).to.be.true;
      expect(config.value).to.be.null;
    });

    it('Hive Metastore hostname array is converted to string', function () {
      var config = {
        value: '<templateName[0]>',
        templateName: ['hive_metastore']
      };
      controller.set('globalProperties', []);
      controller.set('configs', [
        {
          name: 'hive_metastore',
          value: ['h0', 'h1', 'h2']
        }
      ]);

      expect(controller.setConfigValue(config)).to.be.true;
      expect(config.value).to.equal('h0,h1,h2');
    });

  });

  describe('#addHostConfig()', function() {

    afterEach(function () {
      App.Service.find.restore();
    });

    it('No such service loaded', function() {
      sinon.stub(App.Service, 'find', function(){
        return Em.Object.create({isLoaded: false});
      });

      expect(controller.addHostConfig('service1', 'comp1', 'config1')).to.be.false;
    });

    it('No such service in secureServices', function() {
      sinon.stub(App.Service, 'find', function(){
        return Em.Object.create({isLoaded: true});
      });
      controller.set('secureServices', []);

      expect(controller.addHostConfig('service1', 'comp1', 'config1')).to.be.false;
    });

    it('Service does not have such host-component', function() {
      sinon.stub(App.Service, 'find', function(){
        return Em.Object.create({
          isLoaded: true,
          hostComponents: []
        });
      });
      controller.set('secureServices', [{
        serviceName: 'service1'
      }]);

      expect(controller.addHostConfig('service1', 'comp1', 'config1')).to.be.false;
    });
  });

  describe('#getPrincipalNames()', function() {

    beforeEach(function () {
      controller.setProperties({
        globalProperties: [],
        secureProperties: []
      });
    });

    it('globalProperties and secureProperties are empty', function() {
      expect(controller.getPrincipalNames()).to.be.empty;
    });

    it('global property name does not match "principal_name"', function() {
      controller.set('globalProperties', [{
        name: 'config1'
      }]);
      expect(controller.getPrincipalNames()).to.be.empty;
    });

    it('secure property name does not match "principal_name"', function() {
      controller.set('secureProperties', [{
        name: 'config1'
      }]);
      expect(controller.getPrincipalNames()).to.be.empty;
    });

    it('property with such name already exists', function() {
      controller.setProperties({
        globalProperties: [{
          name: 'principal_name'
        }],
        secureProperties: [{
          name: 'principal_name'
        }]
      });
      expect(controller.getPrincipalNames().mapProperty('name')).to.eql(['principal_name']);
    });

  });

  describe('#loadUsersFromServer()', function() {

    it('testMode = true', function() {
      controller.set('testModeUsers', [{
        name: 'user1',
        value: 'value1'
      }]);
      controller.set('serviceUsers', []);
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return true;
        return Em.get(App, k);
      });

      controller.loadUsersFromServer();
      expect(controller.get('serviceUsers')).to.eql([{
        name: 'user1',
        value: 'value1'
      }]);
      App.get.restore();
    });

    it('testMode = false', function() {
      sinon.stub(App.router, 'set', Em.K);
      sinon.stub(App.db, 'getSecureUserInfo', function(){
        return [];
      });
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return false;
        return Em.get(App, k);
      });

      controller.loadUsersFromServer();
      expect(App.db.getSecureUserInfo.calledOnce).to.be.true;
      expect(App.router.set.calledWith('mainAdminSecurityController.serviceUsers', [])).to.be.true;

      App.router.set.restore();
      App.get.restore();
      App.db.getSecureUserInfo.restore();
    });

  });

  describe('#createServicesStackDescriptorConfigs', function() {

    var result;
    beforeEach(function() {
      result = controller.createServicesStackDescriptorConfigs(stackDescriptorData);
    });

    Em.A([
      {
        property: 'spnego_keytab',
        e: [
          { key: 'value', value: '${keytab_dir}/spnego.service.keytab' },
          { key: 'serviceName', value: 'Cluster' }
        ]
      },
      // principal name inherited from /spnego with predefined value
      {
        property: 'oozie.authentication.kerberos.principal',
        e: [
          { key: 'value', value: 'HTTP/${host}@${realm}' },
          { key: 'isEditable', value: true }
        ]
      },
      // keytab inherited from /spnego without predefined file value
      {
        property: 'oozie.authentication.kerberos.keytab',
        e: [
          { key: 'value', value: null },
          { key: 'isEditable', value: false },
          { key: 'referenceProperty', value: 'spnego:keytab' },
          { key: 'observesValueFrom', value: 'spnego_keytab' }
        ]
      }
    ]).forEach(function(test) {
      it('property {0} should be created'.format(test.property), function() {
        expect(result.findProperty('name', test.property)).to.be.ok;
      });
      test.e.forEach(function(expected) {
        it('property `{0}` should have `{1}` with value `{2}`'.format(test.property, expected.key, expected.value), function() {
          expect(result.findProperty('name', test.property)).to.have.deep.property(expected.key, expected.value);
        });
      });
    });
  });

  describe('#expandKerberosStackDescriptorProps', function() {
    var serviceName = 'Cluster';
    var result;
    beforeEach(function() {
      result = controller.expandKerberosStackDescriptorProps(stackDescriptor.properties, serviceName);
    });
    Em.A([
      {
        property: 'realm',
        e: [
          { key: 'isEditable', value: false },
          { key: 'serviceName', value: 'Cluster' }
        ]
      },
      {
        property: 'keytab_dir',
        e: [
          { key: 'isEditable', value: true },
          { key: 'serviceName', value: 'Cluster' }
        ]
      }
    ]).forEach(function(test) {
      it('property {0} should be created'.format(test.property), function() {
        expect(result.findProperty('name', test.property)).to.be.ok;
      });
      test.e.forEach(function(expected) {
        it('property `{0}` should have `{1}` with value `{2}`'.format(test.property, expected.key, expected.value), function() {
          expect(result.findProperty('name', test.property)).to.have.deep.property(expected.key, expected.value);
        });
      });
    });
  });

  describe('#createConfigsByIdentity', function() {
    var identitiesData = stackDescriptor.services[0].components[0].identities;
    var properties;

    beforeEach(function () {
      properties = controller.createConfigsByIdentities(identitiesData, 'HDFS');
    });

    Em.A([
      {
        property: 'dfs.namenode.kerberos.principal',
        e: [
          { key: 'value', value: 'nn/_HOST@${realm}' }
        ]
      },
      {
        property: 'dfs.web.authentication.kerberos.principal',
        e: [
          { key: 'referenceProperty', value: 'spnego:principal' },
          { key: 'isEditable', value: false }
        ]
      }
    ]).forEach(function(test) {
      it('property {0} should be created'.format(test.property), function() {
        expect(properties.findProperty('name', test.property)).to.be.ok;
      });
      test.e.forEach(function(expected) {
        it('property `{0}` should have `{1}` with value `{2}`'.format(test.property, expected.key, expected.value), function() {
          expect(properties.findProperty('name', test.property)).to.have.deep.property(expected.key, expected.value);
        });
      });
    });
  });

  describe('#parseIdentityObject', function() {
    var testCases = [
      {
        identity: stackDescriptor.services[0].components[0].identities[0],
        tests: [
          {
            property: 'dfs.namenode.kerberos.principal',
            e: [
              { key: 'filename', value: 'hdfs-site' }
            ]
          },
          {
            property: 'dfs.namenode.keytab.file',
            e: [
              { key: 'value', value: '${keytab_dir}/nn.service.keytab' }
            ]
          }
        ]
      },
      {
        identity: stackDescriptor.services[0].components[0].identities[1],
        tests: [
          {
            property: 'dfs.namenode.kerberos.https.principal',
            e: [
              { key: 'filename', value: 'hdfs-site' }
            ]
          }
        ]
      },
      {
        identity: stackDescriptor.identities[0],
        tests: [
          {
            property: 'spnego_principal',
            e: [
              { key: 'displayName', value: 'Spnego Principal' },
              { key: 'filename', value: 'cluster-env' }
            ]
          }
        ]
      },
      {
        identity: stackDescriptor.identities[0],
        tests: [
          {
            property: 'spnego_keytab',
            e: [
              { key: 'displayName', value: 'Spnego Keytab' },
              { key: 'filename', value: 'cluster-env' }
            ]
          }
        ]
      }
    ];
    
    testCases.forEach(function(testCase) {
      testCase.tests.forEach(function(test) {
        it('property `{0}` should be present'.format(test.property), function() {
          var result = controller.parseIdentityObject(testCase.identity);
          expect(result.findProperty('name', test.property)).to.be.ok;
        });
        test.e.forEach(function(expected) {
          it('property `{0}` should have `{1}` with value `{2}`'.format(test.property, expected.key, expected.value), function() {
            var result = controller.parseIdentityObject(testCase.identity);
            expect(result.findProperty('name', test.property)).to.have.deep.property(expected.key, expected.value);
          });
        });
      });
    });
  });

  describe('#processConfigReferences', function() {

    var generateProperty = function(name, reference) {
      return Em.Object.create({ name: name, referenceProperty: reference});
    };
    var descriptor = {
      identities: [
        { name: 'spnego', principal: { value: 'spnego_value' }, keytab: { file: 'spnego_file'} },
        { name: 'hdfs',
          principal: { value: 'hdfs_value', configuration: "hadoop-env/hdfs_user_principal_name" },
          keytab: { file: 'hdfs_file', configuration: "hadoop-env/hdfs_user_keytab"} }
      ],
      services: [
        {
          name: 'SERVICE',
          identities: [
            { name: '/spnego' },
            { name: '/hdfs' }
          ]
        },
        {
          name: 'SERVICE2',
          components: [
            {
              name: 'COMPONENT',
              identities: [
                {
                  name: 'component_prop1',
                  keytab: { configuration: 'service2-site/component.keytab' },
                  principal: { configuration: null }
                },
                {
                  name: 'component_prop2',
                  keytab: { configuration: 'service2-site/component2.keytab' },
                  principal: { configuration: 'service2-site/component2.principal' }
                }
              ]
            }
          ]
        }
      ]
    };
    var configs = Em.A([
      generateProperty('spnego_inherited_keytab', 'spnego:keytab'),
      generateProperty('spnego_inherited_principal', 'spnego:principal'),
      generateProperty('hdfs_inherited_keytab', 'hdfs:keytab'),
      generateProperty('hdfs_inherited_principal', 'hdfs:principal'),
      generateProperty('component_prop1_inherited_principal', 'component_prop1:principal'),
      generateProperty('component_prop1_inherited_keytab', 'component_prop1:keytab'),
      generateProperty('component_prop2_inherited_keytab', 'component_prop2:keytab'),
      generateProperty('component_prop2_inherited_principal', 'component_prop2:principal')
    ]);
    var tests = [
      { name: 'spnego_inherited_keytab', e: 'spnego_keytab' },
      { name: 'spnego_inherited_principal', e: 'spnego_principal' },
      { name: 'hdfs_inherited_keytab', e: 'hdfs_user_keytab' },
      { name: 'hdfs_inherited_principal', e: 'hdfs_user_principal_name' },
      { name: 'component_prop1_inherited_keytab', e: 'component.keytab' },
      { name: 'component_prop1_inherited_principal', e: 'component_prop1_principal' },
      { name: 'component_prop2_inherited_keytab', e: 'component2.keytab' },
      { name: 'component_prop2_inherited_principal', e: 'component2.principal' }
    ];

    before(function() {
      controller.processConfigReferences(descriptor, configs);
    });
    
    tests.forEach(function(test) {
      it('`{0}` should observe value from `{1}` property'.format(test.name, test.e), function() {
        expect(configs.findProperty('name', test.name).get('observesValueFrom')).to.be.eql(test.e); 
      });
    });
  });

  describe('#_getDisplayNameForConfig', function () {

    beforeEach(function () {
      controller.set('secureProperties', require('data/HDP2/secure_properties').configProperties);
    });

    it('config from `cluster-env`', function() {
      var config = {
        fileName: 'cluster-env',
        name: 'someCoolName'
      };
      var displayName = controller._getDisplayNameForConfig(config.name, config.fileName);
      expect(displayName).to.equal(App.format.normalizeName(config.name));
    });

    it('config does not exist in the secure_properties', function() {
      var config = {
        fileName: '',
        name: 'someCoolFakeName'
      };
      var displayName = controller._getDisplayNameForConfig(config.name, config.fileName);
      expect(displayName).to.equal(config.name);
    });

    it('config exists in the secure_properties', function() {
      var config = {
        fileName: '',
        name: 'storm_ui_keytab'
      };
      var displayName = controller._getDisplayNameForConfig(config.name, config.fileName);
      expect(displayName).to.equal(controller.get('secureProperties').findProperty('name', config.name).displayName);
    });

  })

});
