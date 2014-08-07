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

require('mixins/wizard/addSecurityConfigs');

describe('App.AddSecurityConfigs', function () {

  var controller = App.AddSecurityConfigs.create({
    content: {},
    enableSubmit: function () {
      this._super()
    },
    secureMapping: [],
    secureProperties: []
  });

  describe('#secureServices', function() {
    it('content.services is correct', function() {
      controller.set('content.services', [{}]);
      expect(controller.get('secureServices')).to.eql([{}]);
      controller.reopen({
        secureServices: []
      })
    });
  });

  describe('#loadUiSideSecureConfigs()', function() {

    beforeEach(function(){
      sinon.stub(controller, 'checkServiceForConfigValue', function() {
        return 'value2';
      });
      sinon.stub(controller, 'setConfigValue', Em.K);
      sinon.stub(controller, 'formatConfigName', Em.K);
    });
    afterEach(function(){
      controller.checkServiceForConfigValue.restore();
      controller.setConfigValue.restore();
      controller.formatConfigName.restore();
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
        foreignKey: null
      }]);

      expect(controller.loadUiSideSecureConfigs()).to.eql([{
        "id": "site property",
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
        dependedServiceName: 'service'
      }]);

      expect(controller.loadUiSideSecureConfigs()).to.eql([{
        "id": "site property",
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
        serviceName: 'service'
      }]);
      sinon.stub(App.Service, 'find', function(){
        return [];
      });

      expect(controller.loadUiSideSecureConfigs()).to.be.empty;
      App.Service.find.restore();
    });
    it('Config has correct serviceName', function() {
      controller.set('secureMapping', [{
        name: 'config1',
        value: 'value1',
        filename: 'file1',
        foreignKey: true,
        serviceName: 'HDFS'
      }]);
      sinon.stub(App.Service, 'find', function(){
        return [{serviceName: 'HDFS'}];
      });

      expect(controller.loadUiSideSecureConfigs()).to.eql([{
        "id": "site property",
        "name": 'config1',
        "value": 'value1',
        "filename": 'file1'
      }]);
      expect(controller.setConfigValue.calledOnce).to.be.true;
      expect(controller.formatConfigName.calledOnce).to.be.true;
      App.Service.find.restore();
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

      expect(controller.setConfigValue(config)).to.be.true;
      expect(config.value).to.be.null;
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
      controller.set('globalProperties', []);
      controller.set('secureProperties', []);
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
      controller.set('globalProperties', [{
        name: 'principal_name'
      }]);
      controller.set('secureProperties', [{
        name: 'principal_name'
      }]);
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
        value: 'value1',
        id: 'puppet var'
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

});



