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
require('controllers/main/admin/highAvailability/hawq/removeStandby/step3_controller');
var testHelpers = require('test/helpers');

function getController() {
  return App.RemoveHawqStandbyWizardStep3Controller.create({});
}

describe('App.RemoveHawqStandbyWizardStep3Controller', function () {
  var controller;

  beforeEach(function () {
    controller = getController();
  });

  describe("#removeStandby()", function () {

    it("ajax send should be called", function() {
      controller.removeStandby();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#stopRequiredServices()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'stopServices');
    });

    afterEach(function() {
      controller.stopServices.restore();
    });

    it("stopServices should be called", function() {
      controller.stopRequiredServices();
      expect(controller.stopServices.calledWith(['HAWQ'])).to.be.true;
    });
  });

  describe("#reconfigureHAWQ()", function () {

    it("ajax send should be called", function() {
      controller.reconfigureHAWQ();
      expect(App.ajax.send.calledOnce).to.be.true;
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
    });

    afterEach(function() {
      controller.reconfigureSites.restore();
    });

    it("reconfigureSites should be called", function() {
      var data = {
        items: [{
          properties: {}
        }]
      };
      controller.onLoadConfigs(data, {}, {type: 't1'});
      expect(controller.reconfigureSites.getCall(0).args).to.be.eql([['t1'], data, 'This configuration was created by Remove HAWQ Standby wizard']);
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

  describe("#onSaveConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function() {
      controller.onTaskCompleted.restore();
    });

    it("onTaskCompleted should be called", function() {
      controller.onSaveConfigs();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });

  describe("#deleteHawqStandbyComponent()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'deleteComponent');
    });

    afterEach(function() {
      controller.deleteComponent.restore();
    });

    it("deleteComponent should be called", function() {
      controller.deleteHawqStandbyComponent();
      expect(controller.deleteComponent.calledOnce).to.be.true;
    });
  });

  describe("#startRequiredServices()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'startServices');
    });

    afterEach(function() {
      controller.startServices.restore();
    });

    it("startServices should be called", function() {
      controller.startRequiredServices();
      expect(controller.startServices.calledWith(false, ["HAWQ"], true)).to.be.true;
    });
  });

});
