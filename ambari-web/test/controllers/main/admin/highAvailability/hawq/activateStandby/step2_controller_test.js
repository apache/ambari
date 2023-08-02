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
require('controllers/main/admin/highAvailability/hawq/activateStandby/step2_controller');

function getController() {
  return App.ActivateHawqStandbyWizardStep2Controller.create({});
}

describe('App.ActivateHawqStandbyWizardStep2Controller', function () {
  var controller;

  beforeEach(function () {
    controller = getController();
  });

  describe('#isSubmitDisabled', function () {

    var cases = [
      {
        isLoaded: false,
        isSubmitDisabled: true,
        title: 'wizard step content not loaded'
      },
      {
        isLoaded: true,
        isSubmitDisabled: false,
        title: 'wizard step content loaded'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.reopen({
          content: Em.Object.create({})
        });
        controller.set('isLoaded', item.isLoaded);
        expect(controller.get('isSubmitDisabled')).to.equal(item.isSubmitDisabled);
      });
    });

  });

  describe('#setDynamicConfigValues', function () {

    var configs = {
      configs: [
        Em.Object.create({
          name: 'hawq_master_address_host'
        })
      ]
    };

    beforeEach(function () {
      controller.reopen({
        content: Em.Object.create({
          masterComponentHosts: [
            {component: 'HAWQMASTER', hostName: 'h0', isInstalled: true},
            {component: 'HAWQSTANDBY', hostName: 'h1', isInstalled: true}
          ],
          hawqHost: {
            hawqMaster: 'h0',
            hawqStandby: 'h1'
          }
        })
      });
      controller.setDynamicConfigValues(configs);
    });

    it('hawq_master_address_host value', function () {
      expect(configs.configs.findProperty('name', 'hawq_master_address_host').get('value')).to.equal('h1');
    });
    it('hawq_master_address_host recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'hawq_master_address_host').get('recommendedValue')).to.equal('h1');
    });
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'renderConfigs')
    });

    afterEach(function() {
      controller.renderConfigs.restore();
    });

    it("should execute renderConfigs function", function() {
      controller.loadStep();
      expect(controller.renderConfigs.calledOnce).to.be.true;
    });
  });

  describe("#submit()", function () {

    var mock = {
      getKDCSessionState: Em.clb
    };

    beforeEach(function() {
      sinon.spy(mock, 'getKDCSessionState');
      sinon.stub(App, 'get').returns(mock);
      sinon.stub(App.router, 'send');
    });
    afterEach(function() {
      App.get.restore();
      App.router.send.restore();
      mock.getKDCSessionState.restore();
    });

    it('App.router.send should be called', function() {
      controller.set('isLoaded', true);
      controller.submit();
      expect(mock.getKDCSessionState.calledOnce).to.be.true;
      expect(App.router.send.calledOnce).to.be.true;
    });

    it('App.router.send should not be called', function() {
      controller.set('isLoaded', false);
      controller.submit();
      expect(mock.getKDCSessionState.calledOnce).to.be.false;
      expect(App.router.send.calledOnce).to.be.false;
    });
  });

  describe("#renderConfigProperties()", function () {

    beforeEach(function() {
      sinon.stub(App.ServiceConfigProperty, 'create', function(obj) {
        return obj;
      });
    });

    afterEach(function() {
      App.ServiceConfigProperty.create.restore();
    });

    it("config should be added", function() {
      var componentConfig = {
        configs: []
      };
      var _componentConfig = {
        configs: [
          Em.Object.create({
            isReconfigurable: true
          })
        ]
      };
      controller.renderConfigProperties(_componentConfig, componentConfig);
      expect(componentConfig.configs[0].get('isEditable')).to.be.true;
    });
  });

  describe("#renderConfigs()", function () {

    beforeEach(function() {
      newHawqMaster = '';
      sinon.stub(App.ServiceConfigProperty, 'create', function(obj) {
        return obj;
      });
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'HAWQSTANDBY',
          hostName: 'host1'
        })
      ]);
      sinon.stub(controller, 'renderConfigProperties');
      sinon.stub(controller, 'setDynamicConfigValues');
      sinon.stub(controller, 'setProperties');
      this.mock = sinon.stub(App.Service, 'find');
    });

    afterEach(function() {
      App.ServiceConfigProperty.create.restore();
      App.HostComponent.find.restore();
      controller.renderConfigProperties.restore();
      controller.setDynamicConfigValues.restore();
      controller.setProperties.restore();
      this.mock.restore();
    });

    it("configs should be rendered{1}", function() {
      this.mock.returns([
        {serviceName: 'HAWQ'},
        {serviceName: 'YARN'}
      ]);
      controller.renderConfigs();
      expect(controller.renderConfigProperties.calledOnce).to.be.true;
      expect(controller.setDynamicConfigValues.calledOnce).to.be.true;
      expect(controller.setProperties.calledOnce).to.be.true;
    });

    it("configs should be rendered{2}", function() {
      this.mock.returns([
        {serviceName: 'YARN'}
      ]);
      controller.renderConfigs();
      expect(controller.renderConfigProperties.calledOnce).to.be.true;
      expect(controller.setDynamicConfigValues.calledOnce).to.be.true;
      expect(controller.setProperties.calledOnce).to.be.true;
    });
  });

});
