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
var controller;
require('controllers/main/admin/stack_and_upgrade_controller');

describe('App.MainAdminStackAndUpgradeController', function() {

  beforeEach(function() {
    controller = App.MainAdminStackAndUpgradeController.create({});
  });

  describe("#services", function () {
    before(function () {
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({serviceName: 'S1', isInstalled: false}),
        Em.Object.create({serviceName: 'S2', isInstalled: false})
      ]);
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({serviceName: 'S1'})
      ]);
    });
    after(function () {
      App.StackService.find.restore();
      App.Service.find.restore();
    });
    it("", function () {
      controller.propertyDidChange('services');
      expect(controller.get('services')).to.eql([
        Em.Object.create({serviceName: 'S1', isInstalled: true}),
        Em.Object.create({serviceName: 'S2', isInstalled: false})
      ])
    });
  });

  describe("#goToAddService()" , function() {
    beforeEach(function() {
      sinon.stub(App.get('router'), 'transitionTo', Em.K);
    });
    afterEach(function() {
     App.get('router').transitionTo.restore();
    });
    it("routes to Add Service Wizard", function() {
      controller.goToAddService({context: "serviceName"});
      expect(App.get('router').transitionTo.calledOnce).to.be.true;
      expect(controller.get('serviceToInstall')).to.be.equal("serviceName");
    });
  });

  describe("#loadVersionsInfo()", function() {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("make ajax call", function() {
      controller.loadVersionsInfo();
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.stack_versions.all',
        sender: controller,
        data: {},
        success: 'loadVersionsInfoSuccessCallback'
      })
    });
  });

  describe("#loadVersionsInfoSuccessCallback()", function() {
    it("", function() {
      var data = {"items": [
        {
          "ClusterStackVersions": {
            "state": "CURRENT"
          }
        },
        {
          "ClusterStackVersions": {
            "state": "INSTALLED"
          }
        }
      ]};
      controller.loadVersionsInfoSuccessCallback(data);
      expect(controller.get('currentVersion')).to.eql({
        "state": "CURRENT"
      });
      expect(controller.get('targetVersions')).to.eql([{
        "state": "INSTALLED"
      }]);
    });
  });

  describe("#loadUpgradeTasks()", function() {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("make ajax call", function() {
      controller.loadUpgradeTasks();
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.upgrade.tasks',
        sender: controller,
        data: {
          id: 1
        },
        success: 'loadUpgradeTasksSuccessCallback'
      })
    });
  });

  describe("#loadUpgradeTasksSuccessCallback()", function() {
    it("", function() {
      var data = {"items": [
        {
          "UpgradeItem": {
            "id": 1
          }
        }
      ]};
      controller.loadUpgradeTasksSuccessCallback(data);
      expect(controller.get('upgradeTasks')).to.eql([
        {
          "id": 1
        }
      ]);
    });
  });

  describe("#openUpgradeDialog()", function () {
    before(function () {
      sinon.stub(App.ModalPopup, 'show', Em.K);
    });
    after(function () {
      App.ModalPopup.show.restore();
    });
    it("should open dialog", function () {
      controller.openUpgradeDialog();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });
});
