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
          "request_status": "UPGRADED"
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
      expect(App.get('upgradeState')).to.equal('UPGRADED');
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
      controller.runPreUpgradeCheck(Em.Object.create({
        repositoryVersion: '2.2',
        displayName: 'HDP-2.2'
      }));
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
      sinon.stub(App, 'showClusterCheckPopup', Em.K);
      sinon.stub(controller, 'upgrade', Em.K);
    });
    afterEach(function () {
      App.showClusterCheckPopup.restore();
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
      expect(controller.upgrade.called).to.be.false;
      expect(App.showClusterCheckPopup.called).to.be.true;
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
      expect(controller.upgrade.called).to.be.true;
      expect(App.showClusterCheckPopup.called).to.be.false;
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
      controller.set('currentVersion', {
        repository_version: '2.2'
      });
      controller.upgrade({
        value: '2.2',
        label: 'HDP-2.2'
      });
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.upgrade.start',
        sender: controller,
        data: {
          value: '2.2',
          label: 'HDP-2.2'
        },
        success: 'upgradeSuccessCallback'
      });
      expect(controller.setDBProperty.calledWith('currentVersion', {
        repository_version: '2.2'
      })).to.be.true;
    });
  });

  describe("#upgradeSuccessCallback()", function() {
    before(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(controller, 'openUpgradeDialog', Em.K);
      sinon.stub(controller, 'setDBProperty', Em.K);
      sinon.stub(controller, 'load', Em.K);
    });
    after(function () {
      App.clusterStatus.setClusterStatus.restore();
      controller.openUpgradeDialog.restore();
      controller.setDBProperty.restore();
      controller.load.restore();
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
      controller.upgradeSuccessCallback(data, {}, {label: 'HDP-2.2.1', isDowngrade: true});
      expect(controller.setDBProperty.calledWith('upgradeId', 1)).to.be.true;
      expect(controller.setDBProperty.calledWith('upgradeVersion', 'HDP-2.2.1')).to.be.true;
      expect(controller.setDBProperty.calledWith('isDowngrade', true)).to.be.true;
      expect(controller.load.calledOnce).to.be.true;
      expect(controller.get('upgradeVersion')).to.equal('HDP-2.2.1');
      expect(controller.get('upgradeData')).to.be.null;
      expect(controller.get('isDowngrade')).to.be.true;
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
              },
              {
                UpgradeItem: {
                  stage_id: 2
                },
                tasks: [
                  {
                    Tasks: {
                      id: 2
                    }
                  }
                ]
              }
            ]
          },
          {
            UpgradeGroup: {
              group_id: 2
            },
            upgrade_items: [

            ]
          }
        ]
      };
      controller.initUpgradeData(newData);
      expect(controller.get('upgradeData.Upgrade.request_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('group_id')).to.equal(2);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('group_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('upgradeItems')[0].get('stage_id')).to.equal(2);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('upgradeItems')[1].get('stage_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('upgradeItems')[0].get('tasks')[0].get('id')).to.equal(2);
    });
  });

  describe.skip("#finish()", function() {
    before(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(controller, 'setDBProperty', Em.K);
    });
    after(function () {
      App.clusterStatus.setClusterStatus.restore();
      controller.setDBProperty.restore();
    });
    it("upgradeState is COMPLETED", function() {
      App.set('upgradeState', 'COMPLETED');
      controller.finish();
      expect(controller.setDBProperty.calledWith('upgradeId', undefined)).to.be.true;
      expect(controller.setDBProperty.calledWith('upgradeVersion', undefined)).to.be.true;
      expect(controller.setDBProperty.calledWith('upgradeState', 'INIT')).to.be.true;
      expect(controller.setDBProperty.calledWith('currentVersion', undefined)).to.be.true;
      expect(App.get('upgradeState')).to.equal('INIT');
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.false;
    });
    it("upgradeState is not COMPLETED", function() {
      App.set('upgradeState', 'UPGRADING');
      controller.finish();
      expect(App.clusterStatus.setClusterStatus.called).to.be.false;
    });
  });

  describe("#confirmDowngrade()", function() {
    before(function () {
      sinon.stub(App, 'showConfirmationPopup', Em.K);
    });
    after(function () {
      App.showConfirmationPopup.restore();
    });
    it("show confirmation popup", function() {
      controller.set('currentVersion', Em.Object.create({
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }));
      controller.confirmDowngrade();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  });

  describe("#downgrade()", function() {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
      sinon.stub(controller, 'setUpgradeItemStatus');
    });
    after(function () {
      App.ajax.send.restore();
      controller.setUpgradeItemStatus.restore();
    });
    it("make ajax call", function() {
      controller.downgrade(Em.Object.create({
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }), {context: 'context'});
      expect(controller.setUpgradeItemStatus.calledWith('context', 'FAILED')).to.be.true;
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.downgrade.start',
        sender: controller,
        data: {
          value: '2.2',
          label: 'HDP-2.2',
          isDowngrade: true
        },
        success: 'upgradeSuccessCallback'
      });
    });
  });

  describe("#installRepoVersionConfirmation()", function () {
    before(function () {
      sinon.stub(controller, 'installRepoVersion', Em.K);
    });
    after(function () {
      controller.installRepoVersion.restore();
    });
    it("show popup", function () {
      var repo = Em.Object.create({'displayName': 'HDP-2.2'});
      var popup = controller.installRepoVersionConfirmation(repo);
      popup.onPrimary();
      expect(controller.installRepoVersion.calledWith(repo)).to.be.true;
    });
  });

  describe("#installRepoVersion()", function () {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("make ajax call", function () {
      var repo = Em.Object.create({
        stackVersionType: 'HDP',
        stackVersionNumber: '2.2',
        repositoryVersion: '2.2.1',
        repoId: 1
      });
      controller.installRepoVersion(repo);
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#installRepoVersionSuccess()", function() {
    var mock = {set: Em.K};
    before(function () {
      sinon.spy(mock, 'set');
      sinon.stub(App.db, 'set', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(App.RepositoryVersion, 'find').returns(mock);
    });
    after(function () {
      mock.set.restore();
      App.db.set.restore();
      App.clusterStatus.setClusterStatus.restore();
      App.RepositoryVersion.find.restore();
    });
    it("", function() {
      controller.installRepoVersionSuccess({Requests: {id: 1}}, {}, {id: 1});
      expect(App.db.set.calledWith('repoVersionInstall', 'id', [1])).to.be.true;
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
      expect(App.RepositoryVersion.find.calledWith(1)).to.be.true;
      expect(mock.set.calledWith('defaultStatus', 'INSTALLING')).to.be.true;    });
  });

  describe("#setUpgradeItemStatus()", function () {
    before(function () {
      sinon.stub(App.ajax, 'send', function () {
        return {
          done: function (callback) {
            callback();
          }
        }
      });
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("", function () {
      var item = Em.Object.create({
        request_id: 1,
        stage_id: 1,
        group_id: 1
      });
      controller.setUpgradeItemStatus(item, 'PENDING');
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.upgrade.upgradeItem.setState',
        sender: controller,
        data: {
          upgradeId: 1,
          itemId: 1,
          groupId: 1,
          status: 'PENDING'
        }
      });
      expect(item.get('status')).to.equal('PENDING');
    });
  });

  describe("#prepareRepoForSaving()", function () {
    it("prepare date for saving", function () {
      var repo = Em.Object.create({
        operatingSystems: [
          Em.Object.create({
            osType: "redhat6",
            isDisabled: Ember.computed.not('isSelected'),
            repositories: [Em.Object.create({
                "baseUrl": "111121",
                "repoId": "HDP-2.2",
                "repoName": "HDP",
                hasError: false
            }),
              Em.Object.create({
                "baseUrl": "1",
                "repoId": "HDP-UTILS-1.1.0.20",
                "repoName": "HDP-UTILS",
                hasError: false
              })]
           })
        ]
      });
      var result = {
        "operating_systems": [
          {
            "OperatingSystems": {
              "os_type": "redhat6"
            },
            "repositories": [
              {
                "Repositories": {
                  "base_url": "111121",
                  "repo_id": "HDP-2.2",
                  "repo_name": "HDP"
                }
              },
              {
                "Repositories": {
                  "base_url": "1",
                  "repo_id": "HDP-UTILS-1.1.0.20",
                  "repo_name": "HDP-UTILS"
                }
              }
            ]
          }
        ]};
      expect(controller.prepareRepoForSaving(repo)).to.eql(result);
    });
  });
});
