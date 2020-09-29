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

describe('App.RAHighAvailabilityWizardStep4Controller', function() {
  var controller;

  beforeEach(function() {
    controller = App.RAHighAvailabilityWizardStep4Controller.create({
      content: Em.Object.create({
        raHosts: Em.Object.create()
      })
    });
  });

  describe('#stopAllServices', function() {
    beforeEach(function() {
      sinon.stub(controller, 'stopServices');
    });
    afterEach(function() {
      controller.stopServices.restore();
    });

    it('stopAllServices should be called', function() {
      controller.stopAllServices();
      expect(controller.stopServices.calledWith([], true, true)).to.be.true;
    });
  });

  describe('#installRangerAdmin', function() {
    beforeEach(function() {
      sinon.stub(controller, 'createInstallComponentTask');
    });
    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it('createInstallComponentTask should be called', function() {
      controller.set('content.raHosts.additionalRA', 'host1');
      controller.installRangerAdmin();
      expect(controller.createInstallComponentTask.calledWith('RANGER_ADMIN', 'host1', "RANGER")).to.be.true;
    });
  });

  describe('#reconfigureRanger', function() {
    beforeEach(function() {
      sinon.stub(controller, 'loadConfigsTags');
    });

    afterEach(function() {
      controller.loadConfigsTags.restore();
    });

    it("loadConfigsTags should be called", function() {
      controller.reconfigureRanger();
      expect(controller.loadConfigsTags.calledOnce).to.be.true;
    });
  });

  describe("#loadConfigsTags()", function () {

    it("should return ajax send request", function() {
      controller.loadConfigsTags();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args[0]).to.be.not.empty;
    });
  });

  describe('#onLoadConfigsTags()', function () {
    var dummyData = {
      Clusters: {
        desired_configs: {
          'ranger-site': {
            tag: 1
          }
        }
      }
    };

    it('request is sent', function () {
      controller.set('wizardController', {
        configs: [{siteName: 'ranger-site'}, {siteName: 'hawq-site'}]
      });
      controller.onLoadConfigsTags(dummyData);
      this.args = testHelpers.findAjaxRequest('name', 'reassign.load_configs');
      expect(this.args[0].data.urlParams).to.equal('(type=ranger-site&tag=1)');
    });

  });

  describe("#onLoadConfigs()", function () {
    var data = {
      items: [{
        type: 'ranger-site',
        properties: {}
      }]
    };

    beforeEach(function() {
      sinon.stub(controller, 'reconfigureSites').returns({});
      sinon.stub(App.format, 'role').returns('comp1');
      controller.set('wizardController', {
        configs: [{siteName: 'ranger-site'}, {siteName: 'hawq-site'}]
      });
      controller.set('content', {
        loadBalancerURL: 'https://https://domain-name_0.com/'
      });
    });

    afterEach(function() {
      controller.reconfigureSites.restore();
      App.format.role.restore();
    });

    it("reconfigureSites should be called", function() {
      controller.onLoadConfigs(data);
      expect(controller.reconfigureSites.getCall(0).args).to.be.eql([['ranger-site'], data, 'This configuration is created by Enable comp1 HA wizard']);
    });

    it("App.ajax.send should be called", function() {
      controller.onLoadConfigs(data);
      var args = testHelpers.findAjaxRequest('name', 'common.service.multiConfigurations');
      expect(args[0].data).to.be.eql({
        configs: [
          {
            Clusters: {
              desired_config: {}
            }
          }
        ]
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

  describe('#startAllServices', function() {
    beforeEach(function() {
      sinon.stub(controller, 'startServices');
    });
    afterEach(function() {
      controller.startServices.restore();
    });

    it('startServices should be called', function() {
      controller.startAllServices();
      expect(controller.startServices.calledWith(true)).to.be.true;
    });
  });

});
