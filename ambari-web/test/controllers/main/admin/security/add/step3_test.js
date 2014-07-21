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

require('controllers/main/admin/security/add/step3');
var stringUtils = require('utils/string_utils');
var modelSetup = require('test/init_model_test');

describe('App.MainAdminSecurityAddStep3Controller', function () {

  var controller = App.MainAdminSecurityAddStep3Controller.create({
    content: {}
  });

  describe('#openInfoInNewTab()', function() {
    it('Correct data', function() {
      var mock = {
        document: {
          write: function(){}
        },
        focus: function(){}
      };
      sinon.stub(window, 'open', function () {
        return mock;
      });
      sinon.stub(stringUtils, 'arrayToCSV', function () {
        return 'CSV_CONTENT';
      });
      sinon.spy(mock.document, 'write');
      sinon.spy(mock, 'focus');
      controller.set('hostComponents', ['comp1']);

      controller.openInfoInNewTab();
      expect(window.open.calledWith('')).to.be.true;
      expect(stringUtils.arrayToCSV.calledWith(['comp1'])).to.be.true;
      expect(mock.document.write.calledWith('CSV_CONTENT')).to.be.true;
      expect(mock.focus.calledOnce).to.be.true;
      window.open.restore();
      stringUtils.arrayToCSV.restore();
    });
  });

  describe('#loadStep()', function() {

    beforeEach(function(){
      sinon.stub(controller, 'getSecurityUsers', function () {
        return [{
          name: 'user_group',
          value: 'value1'
        }];
      });
    });
    afterEach(function(){
      controller.getSecurityUsers.restore();
    });

    it('No hosts installed', function() {
      controller.set('hosts', []);
      controller.loadStep();
      expect(controller.get('hostComponents')).to.be.empty;
    });
    it('One host installed', function () {
      controller.set('hosts', [Em.Object.create({hostName: 'host1'})]);
      sinon.stub(controller, 'setMandatoryConfigs', function (result) {
        return result.push('setMandatoryConfigs');
      });
      sinon.stub(controller, 'setComponentsConfig', function (result) {
        return result.push('setComponentsConfig');
      });
      sinon.stub(controller, 'setHostComponentsSecureValue', function (result) {
        return result.push('setHostComponentsSecureValue');
      });

      controller.loadStep();
      expect(controller.setMandatoryConfigs.calledOnce).to.be.true;
      expect(controller.setComponentsConfig.calledOnce).to.be.true;
      expect(controller.setHostComponentsSecureValue.calledOnce).to.be.true;
      expect(controller.get('hostComponents')).to.eql(["setMandatoryConfigs", "setComponentsConfig", "setHostComponentsSecureValue"]);

      controller.setMandatoryConfigs.restore();
      controller.setComponentsConfig.restore();
      controller.setHostComponentsSecureValue.restore();
    });
  });

  describe('#buildComponentToOwnerMap()', function() {
    it('componentToUserMap is empty', function() {
      controller.set('componentToUserMap', {});
      expect(controller.buildComponentToOwnerMap([])).to.eql({});
    });
    it('componentToUserMap has properties', function() {
      controller.set('componentToUserMap', {'COMP1': 'config1'});
      var securityUsers = [{
        name: 'config1',
        value: 'value1'
      }];
      expect(controller.buildComponentToOwnerMap(securityUsers)).to.eql({'COMP1': 'value1'});
    });
  });

  describe('#setComponentsConfig()', function() {

    beforeEach(function(){
      modelSetup.setupStackServiceComponent();
      controller.set('content.serviceConfigProperties', [
        {
          serviceName: 'HDFS',
          name: 'principal1',
          value: '_HOST'
        },
        {
          serviceName: 'HDFS',
          name: 'keytab1',
          value: 'value1'
        }
      ]);
    });

    afterEach(function() {
      modelSetup.cleanStackServiceComponent();
    });

    it('componentToConfigMap is empty', function() {
      controller.set('componentToConfigMap', []);
      var result = [];
      controller.setComponentsConfig(result, Em.Object.create(), 'hadoopGroupId');
      expect(result).to.be.empty;
    });
    it('isHadoop2Stack = false, when component from stack2', function() {
      sinon.stub(App, 'get', function () {
        return false;
      });
      controller.set('componentToConfigMap', [{
        componentName: 'DATANODE',
        principal: 'principal1',
        keytab: 'keytab1',
        displayName: 'displayName1',
        isHadoop2Stack: true
      }]);
      var host = Em.Object.create({
        hostComponents: [{componentName: 'DATANODE'}],
        hostName: 'host1'
      });
      var result = [];
      controller.setComponentsConfig(result, host, 'hadoopGroupId');
      expect(result).to.be.empty;
      App.get.restore();
    });
    it('isHadoop2Stack = true, when component from stack2', function() {
      sinon.stub(App, 'get', function () {
        return true;
      });
      controller.set('componentToConfigMap', [{
        componentName: 'DATANODE',
        principal: 'principal1',
        keytab: 'keytab1',
        displayName: 'displayName1',
        isHadoop2Stack: true
      }]);
      var host = Em.Object.create({
        hostComponents: [{componentName: 'DATANODE'}],
        hostName: 'host1'
      });
      var result = [];
      controller.setComponentsConfig(result, host, 'hadoopGroupId');
      expect(result.length).to.equal(1);
      App.get.restore();
    });
    it('Component does not match host-component', function() {
      controller.set('componentToConfigMap', [{
        componentName: 'DATANODE',
        principal: 'principal1',
        keytab: 'keytab1',
        displayName: 'displayName1'
      }]);
      var host = Em.Object.create({
        hostComponents: [{componentName: 'DATANODE1'}],
        hostName: 'host1'
      });
      var result = [];
      controller.setComponentsConfig(result, host, 'hadoopGroupId');
      expect(result).to.be.empty;
    });
    it('Component matches host-component', function() {
      controller.set('componentToConfigMap', [{
        componentName: 'DATANODE',
        principal: 'principal1',
        keytab: 'keytab1',
        displayName: 'displayName1'
      }]);
      var host = Em.Object.create({
        hostComponents: [{componentName: 'DATANODE'}],
        hostName: 'host1'
      });
      var result = [];
      controller.setComponentsConfig(result, host, 'hadoopGroupId');
      expect(result.length).to.equal(1);
    });
  });

  describe('#setMandatoryConfigs()', function() {

    beforeEach(function () {
      sinon.stub(App.Service, 'find', function () {
        return [
          {serviceName: 'SERVICE1'}
        ];
      });
      controller.set('content.serviceConfigProperties', [
        {
          serviceName: 'GENERAL',
          name: 'kerberos_domain',
          value: 'realm1'
        }
      ]);
    });
    afterEach(function () {
      App.Service.find.restore();
    });

    it('mandatoryConfigs is empty', function() {
      var result = [];
      controller.set('mandatoryConfigs', []);

      controller.setMandatoryConfigs(result, [], '', '');
      expect(result).to.be.empty;
    });
    it('config has unknown service to check', function() {
      var result = [];
      controller.set('mandatoryConfigs', [{
        userConfig: 'kerberos_domain',
        keytab: 'kerberos_domain',
        displayName: '',
        checkService: 'HBASE'
      }]);

      controller.setMandatoryConfigs(result, [], '', '');
      expect(result).to.be.empty;
    });
    it('config should be added', function() {
      var result = [];
      controller.set('mandatoryConfigs', [{
        userConfig: 'userConfig1',
        keytab: 'kerberos_domain',
        displayName: ''
      }]);
      var securityUsers = [{
        name: 'userConfig1',
        value: 'value1'
      }];

      controller.setMandatoryConfigs(result, securityUsers, '', '');
      expect(result.length).to.equal(1);
    });
  });

  describe('#setHostComponentsSecureValue()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'buildComponentToOwnerMap', Em.K);
      sinon.stub(controller, 'changeDisplayName', Em.K);
      sinon.stub(controller, 'getSecureProperties', function(){
        return {principal: '', keytab: ''};
      });
    });
    afterEach(function () {
      controller.buildComponentToOwnerMap.restore();
      controller.changeDisplayName.restore();
      controller.getSecureProperties.restore();
    });

    it('host.hostComponents is empty', function() {
      var result = [];
      var host = Em.Object.create({
        hostComponents: []
      });

      controller.setHostComponentsSecureValue(result, host);
      expect(result).to.be.empty;
    });
    it('host-component does not match component to display', function() {
      var result = [];
      var host = Em.Object.create({
        hostComponents: [Em.Object.create({
          componentName: 'UNKNOWN'
        })]
      });

      controller.setHostComponentsSecureValue(result, host);
      expect(result).to.be.empty;
    });
    it('host-component matches component to display', function() {
      var result = [];
      var host = Em.Object.create({
        hostComponents: [Em.Object.create({
          componentName: 'DATANODE'
        })]
      });

      controller.setHostComponentsSecureValue(result, host, {}, [], '');
      expect(result.length).to.equal(1);
    });
    it('addedPrincipalsHost already contain such config', function() {
      var result = [];
      var host = Em.Object.create({
        hostName: 'host1',
        hostComponents: [Em.Object.create({
          componentName: 'DATANODE'
        })]
      });

      controller.setHostComponentsSecureValue(result, host, {'host1--': true}, [], '');
      expect(result.length).to.be.empty;
    });
  });

  describe('#getSecureProperties()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'getPrincipal', function () {
        return 'principal';
      });
    });
    afterEach(function () {
      controller.getPrincipal.restore();
    });

    var testCases = [
      {
        title: 'serviceConfigs is empty',
        content: {
          serviceConfigs: [],
          componentName: ''
        },
        result: {}
      },
      {
        title: 'Config has component that does not match component name',
        content: {
          serviceConfigs: [{
            component: 'comp1'
          }],
          componentName: 'comp2'
        },
        result: {}
      },
      {
        title: 'Config has components that does not match component name',
        content: {
          serviceConfigs: [{
            components: ['comp1']
          }],
          componentName: 'comp2'
        },
        result: {}
      },
      {
        title: 'Config has component that matches component name',
        content: {
          serviceConfigs: [{
            name: 'C_principal_name',
            component: 'comp1',
            value: 'value1'
          }],
          componentName: 'comp1'
        },
        result: {
          principal: 'principal'
        }
      },
      {
        title: 'Config has components that matches component name',
        content: {
          serviceConfigs: [{
            name: 'C_principal_name',
            components: ['comp1'],
            value: 'value1'
          }],
          componentName: 'comp1'
        },
        result: {
          principal: 'principal'
        }
      },
      {
        title: 'Config name without correct postfix',
        content: {
          serviceConfigs: [{
            name: 'config1',
            component: 'comp1',
            value: 'value1'
          }],
          componentName: 'comp1'
        },
        result: {}
      },
      {
        title: 'Config name with "_keytab" postfix',
        content: {
          serviceConfigs: [{
            name: 'c_keytab',
            component: 'comp1',
            value: 'value1'
          }],
          componentName: 'comp1'
        },
        result: {
          keytab: 'value1'
        }
      },
      {
        title: 'Config name with "_keytab_path" postfix',
        content: {
          serviceConfigs: [{
            name: 'c_keytab_path',
            component: 'comp1',
            value: 'value1'
          }],
          componentName: 'comp1'
        },
        result: {
          keytab: 'value1'
        }
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.getSecureProperties(test.content.serviceConfigs, test.content.componentName, '')).to.eql(test.result);
      });
    });
  });

  describe('#getPrincipal()', function () {

    var testCases = [
      {
        title: 'Config value missing "_HOST" string, unit is empty',
        content: {
          config: {
            value: 'value1',
            unit: ''
          },
          hostName: ''
        },
        result: 'value1'
      },
      {
        title: 'Config value missing "_HOST" string, unit is correct',
        content: {
          config: {
            value: 'value1',
            unit: 'unit1'
          },
          hostName: ''
        },
        result: 'value1unit1'
      },
      {
        title: 'Config value contains "_HOST" string, host name in lowercase',
        content: {
          config: {
            value: '_HOST',
            unit: 'unit1'
          },
          hostName: 'host1'
        },
        result: 'host1unit1'
      },
      {
        title: 'Config value contains "_HOST" string, host name in uppercase',
        content: {
          config: {
            value: '_HOST',
            unit: 'unit1'
          },
          hostName: 'HOST1'
        },
        result: 'host1unit1'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.getPrincipal(test.content.config, test.content.hostName)).to.equal(test.result);
      });
    });
  });

  describe('#changeDisplayName()', function() {
    it('name is HiveServer2', function() {
      expect(controller.changeDisplayName('HiveServer2')).to.equal('Hive Metastore and HiveServer2');
    });
    it('name is not HiveServer2', function() {
      expect(controller.changeDisplayName('something')).to.equal('something');
    });
  });

  describe('#getSecurityUsers()', function () {
    it('testMode is true, testModeUsers is empty', function () {
      controller.set('testModeUsers', []);
      App.testMode = true;
      expect(controller.getSecurityUsers()).to.eql([]);
    });
    it('testMode is true, testModeUsers is correct', function () {
      controller.set('testModeUsers', [
        {
          name: 'user1',
          value: 'value1'
        }
      ]);
      App.testMode = true;
      expect(controller.getSecurityUsers()).to.eql([
        {
          id: 'puppet var',
          name: 'user1',
          value: 'value1'
        }
      ]);
    });
    it('testMode is false', function () {
      sinon.stub(App.db, 'getSecureUserInfo', function () {
        return [{}];
      });
      App.testMode = false;
      expect(controller.getSecurityUsers()).to.eql([{}]);
      expect(App.db.getSecureUserInfo.calledOnce).to.be.true;
      App.db.getSecureUserInfo.restore();
    });
  });
});
