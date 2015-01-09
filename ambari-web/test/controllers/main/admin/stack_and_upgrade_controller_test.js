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
require('controllers/main/admin/stack_and_upgrade_controller');

describe('App.MainAdminStackAndUpgradeController', function() {

  var controller = App.MainAdminStackAndUpgradeController.create({
    getDBProperty: Em.K,
    setDBProperty: Em.K
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
    beforeEach(function(){
      sinon.stub(controller, 'parseVersionsData', function (data) {
        return data;
      });
    });
    afterEach(function(){
      controller.parseVersionsData.restore();
    });
    it("target version installed and higher than current", function() {
      var data = [
        {
          "state": "CURRENT",
          "repository_version": "2.2.0.1-885"
        },
        {
          "state": "INSTALLED",
          "repository_version": "2.2.1.1-885"
        }
      ];
      controller.loadVersionsInfoSuccessCallback(data);
      expect(controller.get('currentVersion')).to.eql({
        "state": "CURRENT",
        "repository_version": "2.2.0.1-885"
      });
      expect(controller.get('targetVersions')).to.eql([{
        "state": "INSTALLED",
        "repository_version": "2.2.1.1-885"
      }]);
    });
    it("target version installed and lower than current", function() {
      var data = [
        {
          "state": "CURRENT",
          "repository_version": "2.2.0.1-885"
        },
        {
          "state": "INSTALLED",
          "repository_version" : "2.2.0.1-885"
        }
      ];
      controller.loadVersionsInfoSuccessCallback(data);
      expect(controller.get('currentVersion')).to.eql({
        "state": "CURRENT",
        "repository_version": "2.2.0.1-885"
      });
      expect(controller.get('targetVersions')).to.be.empty;
    });
    it("target version not installed and lower than current", function() {
      var data = [
        {
          "state": "CURRENT",
          "repository_version": "2.2.0.1-885"
        },
        {
          "state": "INIT",
          "repository_version" : "2.2.0.1-885"
        }
      ];
      controller.loadVersionsInfoSuccessCallback(data);
      expect(controller.get('currentVersion')).to.eql({
        "state": "CURRENT",
        "repository_version": "2.2.0.1-885"
      });
      expect(controller.get('targetVersions')).to.be.empty;
    });
  });

  describe("#loadUpgradeData()", function() {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send').returns({
        then: Em.K
      });
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it("get entire data", function() {
      controller.set('upgradeId', 1);
      controller.loadUpgradeData();
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.upgrade.data',
        sender: controller,
        data: {
          id: 1
        },
        success: 'loadUpgradeDataSuccessCallback'
      })
    });
    it("get only state", function() {
      controller.set('upgradeId', 1);
      controller.loadUpgradeData(true);
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.upgrade.state',
        sender: controller,
        data: {
          id: 1
        },
        success: 'loadUpgradeDataSuccessCallback'
      })
    });
    it("upgrade id is null", function() {
      controller.set('upgradeId', null);
      controller.loadUpgradeData();
      expect(App.ajax.send.called).to.be.false;
    });
  });

  describe("#loadUpgradeDataSuccessCallback()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'updateUpgradeData', Em.K);
    });
    afterEach(function () {
      controller.updateUpgradeData.restore();
    });
    it("", function() {
      var data = {
        "Upgrade": {
          "request_status": "COMPLETED"
        },
        "upgrade_groups": [
          {
            "UpgradeGroup": {
              "id": 1
            },
            "upgrade_items": []
          }
        ]};
      controller.loadUpgradeDataSuccessCallback(data);
      expect(App.get('upgradeState')).to.equal('COMPLETED');
      expect(controller.updateUpgradeData.called).to.be.true;
    });
  });

  describe("#openUpgradeDialog()", function () {
    before(function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
    });
    after(function () {
      App.router.transitionTo.restore();
    });
    it("should open dialog", function () {
      controller.openUpgradeDialog();
      expect(App.router.transitionTo.calledWith('admin.stackUpgrade')).to.be.true;
    });
  });


  describe("#runPreUpgradeCheck()", function() {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("make ajax call", function() {
      controller.runPreUpgradeCheck({
        value: '2.2',
        label: 'HDP-2.2'
      });
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: "admin.rolling_upgrade.pre_upgrade_check",
        sender: controller,
        data: {
          value: '2.2',
          label: 'HDP-2.2'
        },
        success: "runPreUpgradeCheckSuccess"
      });
    });
  });

  describe("#runPreUpgradeCheckSuccess()", function () {
    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show', Em.K);
      sinon.stub(controller, 'upgrade', Em.K);
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
      controller.upgrade.restore();
    });
    it("shows popup", function () {
      var check =  { items: [{
        UpgradeChecks: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "FAIL",
          "reason": "FAIL",
          "failed_on": [],
          "check_type": "SERVICE"
        }
      }]};
      controller.runPreUpgradeCheckSuccess(check,null,{label: "name"});
      expect(controller.upgrade.calledOnce).to.be.false;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
    it("runs upgrade popup", function () {
      var check = { items: [{
        UpgradeChecks: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "PASS",
          "reason": "OK",
          "failed_on": [],
          "check_type": "SERVICE"
        }
      }]};
      controller.runPreUpgradeCheckSuccess(check,null,{label: "name"});
      expect(controller.upgrade.calledOnce).to.be.true;
      expect(App.ModalPopup.show.calledOnce).to.be.false;
    });
  });

  describe("#initDBProperties()", function() {
    before(function () {
      sinon.stub(controller, 'getDBProperty', function (prop) {
        return prop;
      });
    });
    after(function () {
      controller.getDBProperty.restore();
    });
    it("set properties", function () {
      controller.set('wizardStorageProperties', ['prop1']);
      controller.initDBProperties();
      expect(controller.get('prop1')).to.equal('prop1');
    });
  });

  describe("#init()", function() {
    before(function () {
      sinon.stub(controller, 'initDBProperties', Em.K);
    });
    after(function () {
      controller.initDBProperties.restore();
    });
    it("call initDBProperties", function () {
      controller.init();
      expect(controller.initDBProperties.calledOnce).to.be.true;
    });
  });

  describe("#parseVersionsData()", function() {
    it("", function() {
      var data = {
        items: [
          {
            ClusterStackVersions: {},
            repository_versions: [
              {
                RepositoryVersions: {
                  repository_version: '2.2',
                  display_name: 'v1',
                  id: '1'
                }
              }
            ]
          }
        ]
      };
      expect(controller.parseVersionsData(data)).to.eql([
        {
          "repository_name": "v1",
          "repository_id": "1",
          "repository_version": "2.2"
        }
      ]);
    });
  });

  describe("#upgrade()", function() {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
      sinon.stub(controller, 'setDBProperty', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
      controller.setDBProperty.restore();
    });
    it("make ajax call", function() {
      controller.upgrade({
        value: '2.2',
        label: 'HDP-2.2'
      });
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.upgrade.start',
        sender: controller,
        data: {
          version: '2.2'
        },
        success: 'upgradeSuccessCallback'
      });
      expect(controller.get('upgradeVersion')).to.equal('HDP-2.2');
      expect(controller.setDBProperty.calledWith('upgradeVersion', 'HDP-2.2')).to.be.true;
    });
  });

  describe("#upgradeSuccessCallback()", function() {
    before(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(controller, 'openUpgradeDialog', Em.K);
      sinon.stub(controller, 'setDBProperty', Em.K);
    });
    after(function () {
      App.clusterStatus.setClusterStatus.restore();
      controller.openUpgradeDialog.restore();
      controller.setDBProperty.restore();
    });
    it("open upgrade dialog", function() {
      var data = {
        resources: [
          {
            Upgrade: {
              request_id: 1
            }
          }
        ]
      };
      controller.upgradeSuccessCallback(data);
      expect(controller.setDBProperty.calledWith('upgradeId', 1)).to.be.true;
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
      expect(controller.openUpgradeDialog.calledOnce).to.be.true;
    });
  });

  describe("#updateUpgradeData()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'initUpgradeData', Em.K);
    });
    afterEach(function () {
      controller.initUpgradeData.restore();
    });
    it("data loaded first time", function() {
      controller.set('upgradeData', null);
      controller.updateUpgradeData({});
      expect(controller.initUpgradeData.calledWith({})).to.be.true;
    });
    it("update loaded data", function() {
      var oldData = Em.Object.create({
        upgradeGroups: [
          Em.Object.create({
            group_id: 1,
            upgradeItems: [
              Em.Object.create({
                stage_id: 1,
                tasks: [
                  Em.Object.create({
                    id: 1
                  })
                ]
              })
            ]
          })
        ]
      });
      var newData = {
        Upgrade: {
          request_id: 1
        },
        upgrade_groups: [
          {
            UpgradeGroup: {
              group_id: 1,
              status: 'COMPLETED',
              progress_percent: 100
            },
            upgrade_items: [
              {
                UpgradeItem: {
                  stage_id: 1,
                  status: 'COMPLETED',
                  progress_percent: 100
                },
                tasks: [
                  {
                    Tasks: {
                      id: 1,
                      status: 'COMPLETED'
                    }
                  }
                ]
              }
            ]
          }
        ]
      };
      controller.set('upgradeData', oldData);
      controller.updateUpgradeData(newData);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('status')).to.equal('COMPLETED');
      expect(controller.get('upgradeData.upgradeGroups')[0].get('progress_percent')).to.equal(100);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('status')).to.equal('COMPLETED');
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('progress_percent')).to.equal(100);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('tasks')[0].get('status')).to.equal('COMPLETED');
    });
  });

  describe("#initUpgradeData()", function() {
    it("", function() {
      var newData = {
        Upgrade: {
          request_id: 1
        },
        upgrade_groups: [
          {
            UpgradeGroup: {
              group_id: 1
            },
            upgrade_items: [
              {
                UpgradeItem: {
                  stage_id: 1
                },
                tasks: [
                  {
                    Tasks: {
                      id: 1
                    }
                  }
                ]
              }
            ]
          }
        ]
      };
      controller.initUpgradeData(newData);
      expect(controller.get('upgradeData.Upgrade.request_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('group_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('stage_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('tasks')[0].get('id')).to.equal(1);
    });
  });

  describe("#finish()", function() {
    before(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(controller, 'setDBProperty', Em.K);
    });
    after(function () {
      App.clusterStatus.setClusterStatus.restore();
      controller.setDBProperty.restore();
    });
    it("reset upgrade info", function() {
      controller.finish();
      expect(controller.get('upgradeId')).to.be.null;
      expect(controller.setDBProperty.calledWith('upgradeId', undefined)).to.be.true;
      expect(App.get('upgradeState')).to.equal('INIT');
      expect(controller.get('upgradeVersion')).to.be.null;
      expect(controller.setDBProperty.calledWith('upgradeVersion', undefined)).to.be.true;
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });
});
