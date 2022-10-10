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
var testHelpers = require('test/helpers');
require('controllers/main/admin/highAvailability/hawq/activateStandby/step3_controller');

function getController() {
  return App.ActivateHawqStandbyWizardStep3Controller.create({});
}

describe('App.ActivateHawqStandbyWizardStep3Controller', function () {
  var controller;

  beforeEach(function () {
    controller = getController();
  });

  describe("#stopRequiredServices()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'stopServices');
    });

    afterEach(function() {
      controller.stopServices.restore();
    });

    it("should stop services", function() {
      controller.stopRequiredServices();
      expect(controller.stopServices.calledOnce).to.be.true;
    });
  });

  describe("#startRequiredServices()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'startServices');
    });

    afterEach(function() {
      controller.startServices.restore();
    });

    it("should start services", function() {
      controller.startRequiredServices();
      expect(controller.startServices.calledOnce).to.be.true;
    });
  });

  describe("#onSaveConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function() {
      controller.onTaskCompleted.restore();
    });

    it("should execute onTaskCompleted function", function() {
      controller.onSaveConfigs();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });

  describe("#installHawqMaster()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
    });

    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it("should execute createInstallComponentTask function", function() {
      controller.reopen({
        content: {
          hawqHosts: {
            hawqMaster: 'host1',
            hawqStandby: 'host1'
          }
        }
      });
      controller.installHawqMaster();
      expect(controller.createInstallComponentTask.calledOnce).to.be.true;
    });
  });

  describe("#deleteOldHawqMaster()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'deleteComponent');
    });

    afterEach(function() {
      controller.deleteComponent.restore();
    });

    it("should execute deleteComponent function", function() {
      controller.reopen({
        content: {
          hawqHosts: {
            hawqMaster: 'host1',
            hawqStandby: 'host1'
          }
        }
      });
      controller.deleteOldHawqMaster();
      expect(controller.deleteComponent.calledOnce).to.be.true;
    });
  });

  describe("#deleteHawqStandby()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'deleteComponent');
    });

    afterEach(function() {
      controller.deleteComponent.restore();
    });

    it("should execute deleteComponent function", function() {
      controller.reopen({
        content: {
          hawqHosts: {
            hawqMaster: 'host1',
            hawqStandby: 'host1'
          }
        }
      });
      controller.deleteHawqStandby();
      expect(controller.deleteComponent.calledOnce).to.be.true;
    });
  });

  describe("#onLoadConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'reconfigureSites').returns({});
      controller.set('content.configs', [
        {
          filename: 't1',
          name: 'p1',
          value: 'val1'
        }
      ]);
      sinon.stub(App.format, 'role').returns('comp1');
    });

    afterEach(function() {
      controller.reconfigureSites.restore();
      App.format.role.restore();
    });

    it("reconfigureSites should be called", function() {
      var data = {
        items: [{
          properties: {}
        }]
      };
      var str = Em.I18n.t('admin.activateHawqStandby.step4.save.configuration.note').format('comp1');
      controller.onLoadConfigs(data, {}, {type: 't1'});
      expect(controller.reconfigureSites.getCall(0).args).to.be.eql([['t1'], data, str]);
    });

    it("App.ajax.send should be called", function() {
      var data = {
        items: [{
          properties: {}
        }]
      };
      controller.onLoadConfigs(data, {}, {type: 't1'});
      var args = testHelpers.findAjaxRequest('name', 'common.service.configurations');
      expect(args[0]).to.be.eql({
        name: 'common.service.configurations',
        sender: controller,
        data: {
          desired_config: {}
        },
        success: 'onSaveConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe("#onLoadHawqConfigsTags()", function () {

    it("App.ajax.send should be called", function() {
      var data = {
        Clusters: {
          desired_configs: {
            'hawq-site': {
              tag: 1
            }
          }
        }
      };
      controller.onLoadHawqConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(args[0]).to.be.eql({
        name: 'reassign.load_configs',
        sender: controller,
        data: {
          urlParams: '(type=hawq-site&tag=1)',
          type: 'hawq-site'
        },
        success: 'onLoadConfigs',
        error: 'onTaskError'
      });
    });
  });

  describe("#reconfigureHAWQ()", function () {

    it("should send ajax request", function() {
      controller.reconfigureHAWQ();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#activateStandby()", function () {

    it("should send ajax request", function() {
      controller.activateStandby();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

});
