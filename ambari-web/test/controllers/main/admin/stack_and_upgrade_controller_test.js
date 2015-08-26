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
require('utils/string_utils');

describe('App.MainAdminStackAndUpgradeController', function() {

  var controller = App.MainAdminStackAndUpgradeController.create({
    getDBProperty: Em.K,
    setDBProperty: Em.K
  });

  describe("#realRepoUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
    });
    after(function () {
      this.mock.restore();
    });
    it("", function() {
      this.mock.withArgs('apiPrefix').returns('apiPrefix');
      this.mock.withArgs('stackVersionURL').returns('stackVersionURL');
      controller.propertyDidChange('realRepoUrl');
      expect(controller.get('realRepoUrl')).to.equal('apiPrefixstackVersionURL/compatible_repository_versions?fields=*,operating_systems/*,operating_systems/repositories/*');
    });
  });

  describe("#realStackUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
    });
    after(function () {
      this.mock.restore();
    });
    it("", function() {
      this.mock.withArgs('apiPrefix').returns('apiPrefix');
      this.mock.withArgs('clusterName').returns('clusterName');
      controller.propertyDidChange('realStackUrl');
      expect(controller.get('realStackUrl')).to.equal('apiPrefix/clusters/clusterName/stack_versions?fields=*,repository_versions/*,repository_versions/operating_systems/repositories/*');
    });
  });

  describe("#realUpdateUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
    });
    after(function () {
      this.mock.restore();
    });
    it("", function() {
      this.mock.withArgs('apiPrefix').returns('apiPrefix');
      this.mock.withArgs('clusterName').returns('clusterName');
      controller.propertyDidChange('realUpdateUrl');
      expect(controller.get('realUpdateUrl')).to.equal('apiPrefix/clusters/clusterName/stack_versions?fields=ClusterStackVersions/*');
    });
  });

  describe("#load()", function() {
    before(function(){
      sinon.stub(controller, 'loadUpgradeData').returns({
        done: function(callback) {callback();}
      });
      sinon.stub(controller, 'loadStackVersionsToModel').returns({
        done: function(callback) {callback();}
      });
      sinon.stub(controller, 'loadRepoVersionsToModel').returns({
        done: function(callback) {callback();}
      });
      sinon.stub(App.StackVersion, 'find').returns([Em.Object.create({
        state: 'CURRENT',
        repositoryVersion: {
          repositoryVersion: '2.2',
          displayName: 'HDP-2.2'
        }
      })]);
    });
    after(function(){
      controller.loadUpgradeData.restore();
      controller.loadStackVersionsToModel.restore();
      controller.loadRepoVersionsToModel.restore();
      App.StackVersion.find.restore();
    });
    it("", function() {
      controller.load();
      expect(controller.loadUpgradeData.calledWith(true)).to.be.true;
      expect(controller.loadStackVersionsToModel.calledWith(true)).to.be.true;
      expect(controller.loadRepoVersionsToModel.calledOnce).to.be.true;
      expect(controller.get('currentVersion')).to.eql({
        "repository_version": "2.2",
        "repository_name": "HDP-2.2"
      });
    });
  });

  describe("#loadUpgradeData()", function() {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send').returns({
        then: Em.K,
        complete: Em.K
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
    var retryCases = [
      {
        isRetryPendingInitial: true,
        status: 'ABORTED',
        isRetryPending: true,
        requestInProgress: true,
        title: 'retry request not yet applied'
      },
      {
        isRetryPendingInitial: true,
        status: 'UPGRADING',
        isRetryPending: false,
        requestInProgress: false,
        title: 'retry request applied'
      },
      {
        isRetryPendingInitial: false,
        status: 'ABORTED',
        isRetryPending: false,
        requestInProgress: true,
        title: 'no retry request sent'
      },
      {
        isRetryPendingInitial: false,
        status: 'UPGRADING',
        isRetryPending: false,
        requestInProgress: true,
        title: 'upgrade wasn\'t aborted'
      }
    ];
    beforeEach(function () {
      sinon.stub(controller, 'updateUpgradeData', Em.K);
      sinon.stub(controller, 'setDBProperty', Em.K);
    });
    afterEach(function () {
      controller.updateUpgradeData.restore();
      controller.setDBProperty.restore();
    });
    it("correct data", function() {
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
      expect(controller.updateUpgradeData.calledOnce).to.be.true;
      expect(controller.setDBProperty.calledWith('upgradeState', 'UPGRADED')).to.be.true;
    });
    it("data is null", function() {
      var data = null;
      controller.loadUpgradeDataSuccessCallback(data);
      expect(controller.updateUpgradeData.called).to.be.false;
      expect(controller.setDBProperty.called).to.be.false;
    });
    retryCases.forEach(function (item) {
      it(item.title, function () {
        var data = {
          "Upgrade": {
            "request_status": item.status
          }
        };
        controller.setProperties({
          isRetryPending: item.isRetryPendingInitial,
          requestInProgress: true
        });
        controller.loadUpgradeDataSuccessCallback(data);
        expect(controller.getProperties(['isRetryPending', 'requestInProgress'])).to.eql({
          isRetryPending: item.isRetryPending,
          requestInProgress: item.requestInProgress
        });
      });
    });
  });

  describe("#getUpgradeItem()", function() {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it("", function() {
      var item = Em.Object.create({
        request_id: 1,
        group_id: 2,
        stage_id: 3
      });
      controller.getUpgradeItem(item);
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.upgrade.upgrade_item',
        sender: controller,
        data: {
          upgradeId: 1,
          groupId: 2,
          stageId: 3
        },
        success: 'getUpgradeItemSuccessCallback'
      });
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
        success: "runPreUpgradeCheckSuccess",
        error: "runPreUpgradeCheckError"
      });
    });
  });

  describe("#runPreUpgradeCheckSuccess()", function () {
    var cases = [
      {
        check: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "FAIL",
          "reason": "FAIL",
          "failed_on": [],
          "check_type": "SERVICE"
        },
        showClusterCheckPopupCalledCount: 1,
        upgradeCalledCount: 0,
        title: 'popup is displayed if fails are present'
      },
      {
        check: {
          "check": "Configuration Merge Check",
          "status": "WARNING",
          "reason": "Conflict",
          "failed_on": [],
          "failed_detail": [
            {
              type: 't0',
              property: 'p0',
              current: 'c0',
              new_stack_value: 'n0',
              result_value: 'n0'
            },
            {
              type: 't1',
              property: 'p1',
              current: 'c1',
              new_stack_value: null,
              result_value: 'c1'
            },
            {
              type: 't2',
              property: 'p2',
              current: 'c2',
              new_stack_value: null,
              result_value: null
            }
          ],
          "check_type": "CLUSTER",
          "id": "CONFIG_MERGE"
        },
        showClusterCheckPopupCalledCount: 1,
        upgradeCalledCount: 0,
        configs: [
          {
            type: 't0',
            name: 'p0',
            currentValue: 'c0',
            recommendedValue: 'n0',
            resultingValue: 'n0',
            isDeprecated: false,
            willBeRemoved: false
          },
          {
            type: 't1',
            name: 'p1',
            currentValue: 'c1',
            recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
            resultingValue: 'c1',
            isDeprecated: true,
            willBeRemoved: false
          },
          {
            type: 't2',
            name: 'p2',
            currentValue: 'c2',
            recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
            resultingValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved'),
            isDeprecated: true,
            willBeRemoved: true
          }
        ],
        title: 'popup is displayed if warnings are present; configs merge conflicts'
      },
      {
        check: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "PASS",
          "reason": "OK",
          "failed_on": [],
          "check_type": "SERVICE"
        },
        showClusterCheckPopupCalledCount: 0,
        upgradeCalledCount: 1,
        title: 'upgrade is started if fails and warnings are absent'
      }
    ];
    beforeEach(function () {
      sinon.stub(App, 'showClusterCheckPopup', Em.K);
      sinon.stub(controller, 'upgrade', Em.K);
    });
    afterEach(function () {
      App.showClusterCheckPopup.restore();
      controller.upgrade.restore();
    });
    cases.forEach(function (item) {
      it(item.title, function () {
        controller.runPreUpgradeCheckSuccess(
          {
            items: [
              {
                UpgradeChecks: item.check
              }
            ]
          }, null, {
            label: 'name'
          }
        );
        expect(controller.upgrade.callCount).to.equal(item.upgradeCalledCount);
        expect(App.showClusterCheckPopup.callCount).to.equal(item.showClusterCheckPopupCalledCount);
        if (item.check.id == 'CONFIG_MERGE') {
          expect(App.showClusterCheckPopup.firstCall.args[7]).to.eql(item.configs);
        }
      });
    });
  });

  describe("#initDBProperties()", function() {
    before(function () {
      sinon.stub(controller, 'getDBProperties', function (prop) {
        var ret = {};
        prop.forEach(function (k) {
          ret[k] = k;
        });
        return ret;
      });
    });
    after(function () {
      controller.getDBProperties.restore();
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
      expect(App.ajax.send.getCall(0).args[0].data).to.eql({"value": '2.2', "label": 'HDP-2.2'});
      expect(App.ajax.send.getCall(0).args[0].name).to.eql('admin.upgrade.start');
      expect(App.ajax.send.getCall(0).args[0].sender).to.eql(controller);
      expect(App.ajax.send.getCall(0).args[0].success).to.eql('upgradeSuccessCallback');
      expect(App.ajax.send.getCall(0).args[0].callback).to.be.called;
      expect(controller.setDBProperty.calledWith('currentVersion', {
        repository_version: '2.2'
      })).to.be.true;
    });
  });

  describe("#upgradeSuccessCallback()", function() {
    before(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(controller, 'openUpgradeDialog', Em.K);
      sinon.stub(controller, 'setDBProperties', Em.K);
      sinon.stub(controller, 'load', Em.K);
    });
    after(function () {
      App.clusterStatus.setClusterStatus.restore();
      controller.openUpgradeDialog.restore();
      controller.setDBProperties.restore();
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
                stage_id: 1
              })
            ]
          }),
          Em.Object.create({
            group_id: 2,
            upgradeItems: [
              Em.Object.create({
                stage_id: 2
              }),
              Em.Object.create({
                stage_id: 3
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
              progress_percent: 100,
              completed_task_count: 3
            },
            upgrade_items: [
              {
                UpgradeItem: {
                  stage_id: 1,
                  status: 'COMPLETED',
                  progress_percent: 100
                }
              }
            ]
          },
          {
            UpgradeGroup: {
              group_id: 2,
              status: 'ABORTED',
              progress_percent: 50,
              completed_task_count: 1
            },
            upgrade_items: [
              {
                UpgradeItem: {
                  stage_id: 2,
                  status: 'ABORTED',
                  progress_percent: 99
                }
              },
              {
                UpgradeItem: {
                  stage_id: 3,
                  status: 'PENDING',
                  progress_percent: 0
                }
              }
            ]
          }
        ]
      };
      controller.set('upgradeData', oldData);
      controller.updateUpgradeData(newData);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('status')).to.equal('COMPLETED');
      expect(controller.get('upgradeData.upgradeGroups')[0].get('progress_percent')).to.equal(100);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('completed_task_count')).to.equal(3);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('status')).to.equal('COMPLETED');
      expect(controller.get('upgradeData.upgradeGroups')[0].get('upgradeItems')[0].get('progress_percent')).to.equal(100);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('hasExpandableItems')).to.be.true;
      expect(controller.get('upgradeData.upgradeGroups')[1].get('status')).to.equal('ABORTED');
      expect(controller.get('upgradeData.upgradeGroups')[1].get('progress_percent')).to.equal(50);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('completed_task_count')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('upgradeItems')[0].get('status')).to.equal('ABORTED');
      expect(controller.get('upgradeData.upgradeGroups')[1].get('upgradeItems')[1].get('status')).to.equal('PENDING');
      expect(controller.get('upgradeData.upgradeGroups')[1].get('upgradeItems')[0].get('progress_percent')).to.equal(99);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('upgradeItems')[1].get('progress_percent')).to.equal(0);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('hasExpandableItems')).to.be.false;
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
                  stage_id: 1,
                  status: 'IN_PROGRESS'
                }
              },
              {
                UpgradeItem: {
                  stage_id: 2
                }
              }
            ]
          },
          {
            UpgradeGroup: {
              group_id: 2
            },
            upgrade_items: []
          },
          {
            UpgradeGroup: {
              group_id: 3
            },
            upgrade_items: [
              {
                UpgradeItem: {
                  stage_id: 3,
                  status: 'ABORTED'
                }
              },
              {
                UpgradeItem: {
                  stage_id: 4,
                  status: 'PENDING'
                }
              }
            ]
          }
        ]
      };
      controller.initUpgradeData(newData);
      expect(controller.get('upgradeData.Upgrade.request_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('group_id')).to.equal(3);
      expect(controller.get('upgradeData.upgradeGroups')[1].get('group_id')).to.equal(2);
      expect(controller.get('upgradeData.upgradeGroups')[2].get('group_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[2].get('upgradeItems')[0].get('stage_id')).to.equal(2);
      expect(controller.get('upgradeData.upgradeGroups')[2].get('upgradeItems')[1].get('stage_id')).to.equal(1);
      expect(controller.get('upgradeData.upgradeGroups')[0].get('hasExpandableItems')).to.be.false;
      expect(controller.get('upgradeData.upgradeGroups')[1].get('hasExpandableItems')).to.be.false;
      expect(controller.get('upgradeData.upgradeGroups')[2].get('hasExpandableItems')).to.be.true;
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
    it("upgradeState is not COMPLETED", function() {
      App.set('upgradeState', 'UPGRADING');
      controller.finish();
      expect(App.clusterStatus.setClusterStatus.called).to.be.false;
    });
    it("upgradeState is COMPLETED", function() {
      App.set('upgradeState', 'COMPLETED');
      controller.finish();
      expect(controller.setDBProperty.calledWith('upgradeId', undefined)).to.be.true;
      expect(controller.setDBProperty.calledWith('upgradeVersion', undefined)).to.be.true;
      expect(controller.setDBProperty.calledWith('upgradeState', 'INIT')).to.be.true;
      expect(controller.setDBProperty.calledWith('currentVersion', undefined)).to.be.true;
      expect(App.get('upgradeState')).to.equal('INIT');
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#confirmDowngrade()", function() {
    before(function () {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(controller, 'downgrade', Em.K);
    });
    after(function () {
      App.showConfirmationPopup.restore();
      controller.downgrade.restore();
    });
    it("show confirmation popup", function() {
      controller.set('currentVersion', Em.Object.create({
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }));
      var popup = controller.confirmDowngrade();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.downgrade.calledWith(Em.Object.create({
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }))).to.be.true;
    });
  });

  describe("#confirmUpgrade()", function() {
    before(function () {
      sinon.spy(App, 'showConfirmationPopup');
      sinon.stub(controller, 'runPreUpgradeCheck', Em.K);
    });
    after(function () {
      App.showConfirmationPopup.restore();
      controller.runPreUpgradeCheck.restore();
    });
    it("show confirmation popup", function() {
      var version = Em.Object.create({displayName: 'HDP-2.2'});
      var popup = controller.confirmUpgrade(version);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.runPreUpgradeCheck.calledWith(version)).to.be.true;
    });
  });

  describe("#downgrade()", function() {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
      sinon.stub(controller, 'abortUpgrade');
      sinon.stub(App.RepositoryVersion, 'find').returns([
        Em.Object.create({
          displayName: 'HDP-2.3',
          repositoryVersion: '2.3'
        })
      ]);
    });
    after(function () {
      App.ajax.send.restore();
      controller.abortUpgrade.restore();
      App.RepositoryVersion.find.restore();
    });
    it("make ajax call", function() {
      controller.set('upgradeVersion', 'HDP-2.3');
      controller.downgrade(Em.Object.create({
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }), {context: 'context'});
      expect(controller.abortUpgrade.calledOnce).to.be.true;
      expect(App.ajax.send.getCall(0).args[0].data).to.eql({
        value: '2.2',
        label: 'HDP-2.2',
        from: '2.3',
        isDowngrade: true
      });
      expect(App.ajax.send.getCall(0).args[0].name).to.eql('admin.downgrade.start');
      expect(App.ajax.send.getCall(0).args[0].sender).to.eql(controller);
      expect(App.ajax.send.getCall(0).args[0].success).to.eql('upgradeSuccessCallback');
      expect(App.ajax.send.getCall(0).args[0].callback).to.be.called;
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
    var mock = Em.Object.create({
      id: 1,
      defaultStatus: 'INIT',
      stackVersion: {}
    });
    before(function () {
      sinon.spy(mock, 'set');
      sinon.stub(App.db, 'set', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(App.RepositoryVersion, 'find').returns(mock);
    });
    after(function () {
      App.db.set.restore();
      App.clusterStatus.setClusterStatus.restore();
      App.RepositoryVersion.find.restore();
    });
    it("", function() {
      controller.installRepoVersionSuccess({Requests: {id: 1}}, {}, {id: 1});
      expect(App.db.set.calledWith('repoVersionInstall', 'id', [1])).to.be.true;
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
      expect(App.RepositoryVersion.find.calledWith(1)).to.be.true;
      expect(App.RepositoryVersion.find(1).get('defaultStatus')).to.equal('INSTALLING');
      expect(App.RepositoryVersion.find(1).get('stackVersion.state')).to.equal('INSTALLING');
    });
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
      expect(App.ajax.send.getCall(0).args[0].data).to.eql({upgradeId: 1, itemId: 1, groupId: 1, status: 'PENDING'});
      expect(App.ajax.send.getCall(0).args[0].name).to.eql('admin.upgrade.upgradeItem.setState');
      expect(App.ajax.send.getCall(0).args[0].sender).to.eql(controller);
      expect(App.ajax.send.getCall(0).args[0].callback).to.be.called;
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

  describe("#getStackVersionNumber()", function(){
    it("get stack version number", function(){
      var repo = Em.Object.create({
        "stackVersionType": 'HDP',
        "stackVersion": '2.3',
        "repositoryVersion": '2.2.1'
      });
      
      var stackVersion = controller.getStackVersionNumber(repo);
      expect(stackVersion).to.equal('2.3');
    });
    
    it("get default stack version number", function(){
      App.set('currentStackVersion', '1.2.3');
      var repo = Em.Object.create({
        "stackVersionType": 'HDP',
        "repositoryVersion": '2.2.1'
      });
      
      var stackVersion = controller.getStackVersionNumber(repo);
      expect(stackVersion).to.equal('1.2.3');
    });
  });
  
  describe("#saveRepoOS()", function() {
    before(function(){
      this.mock = sinon.stub(controller, 'validateRepoVersions');
      sinon.stub(controller, 'prepareRepoForSaving', Em.K);
      sinon.stub(App.ajax, 'send').returns({success: Em.K});
    });
    after(function(){
      this.mock.restore();
      controller.prepareRepoForSaving.restore();
      App.ajax.send.restore();
    });
    it("validation errors present", function() {
      this.mock.returns({
        done: function(callback) {callback([1]);}
      });
      controller.saveRepoOS(Em.Object.create({repoVersionId: 1}), true);
      expect(controller.validateRepoVersions.calledWith(Em.Object.create({repoVersionId: 1}), true)).to.be.true;
      expect(controller.prepareRepoForSaving.called).to.be.false;
      expect(App.ajax.send.called).to.be.false;
    });
    it("no validation errors", function() {
      this.mock.returns({
        done: function(callback) {callback([]);}
      });
      controller.saveRepoOS(Em.Object.create({repoVersionId: 1}), true);
      expect(controller.validateRepoVersions.calledWith(Em.Object.create({repoVersionId: 1}), true)).to.be.true;
      expect(controller.prepareRepoForSaving.calledWith(Em.Object.create({repoVersionId: 1}))).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#validateRepoVersions()", function () {
    before(function () {
      sinon.stub(App.ajax, 'send').returns({success: Em.K, error: Em.K});
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("skip validation", function () {
      controller.validateRepoVersions(Em.Object.create({repoVersionId: 1}), true);
      expect(App.ajax.send.called).to.be.false;
    });
    it("do validation", function () {
      var repo = Em.Object.create({
        repoVersionId: 1,
        operatingSystems: [
          Em.Object.create({
            isSelected: true,
            repositories: [
              Em.Object.create()
            ]
          })
        ]
      });
      controller.validateRepoVersions(repo, false);
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#showProgressPopup()", function () {
    var mock = {
      initPopup: Em.K
    };
    before(function () {
      sinon.stub(App.router, 'get').withArgs('highAvailabilityProgressPopupController').returns(mock);
      sinon.spy(mock, 'initPopup');
    });
    after(function () {
      App.router.get.restore();
      mock.initPopup.restore();
    });
    it("", function () {
      controller.showProgressPopup(Em.Object.create());
      expect(mock.initPopup.calledOnce).to.be.true;
    });
  });

  describe("#getUrl()", function() {
    beforeEach(function(){
      controller.reopen({
        realStackUrl: 'realStackUrl',
        realRepoUrl: 'realRepoUrl',
        realUpdateUrl: 'realUpdateUrl'
      });
    });
    it("full load is true, stack is null", function() {
      expect(controller.getUrl(null, true)).to.equal('realRepoUrl');
    });
    it("full load is true, stack is valid", function() {
      expect(controller.getUrl({}, true)).to.equal('realStackUrl');
    });
    it("full load is false, stack is valid", function() {
      expect(controller.getUrl({}, false)).to.equal('realUpdateUrl');
    });
  });

  describe("#loadStackVersionsToModel()", function () {
    before(function () {
      sinon.stub(App.HttpClient, 'get');
    });
    after(function () {
      App.HttpClient.get.restore();
    });
    it("", function () {
      controller.loadStackVersionsToModel();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe("#loadRepoVersionsToModel()", function () {
    before(function () {
      sinon.stub(App.HttpClient, 'get');
    });
    after(function () {
      App.HttpClient.get.restore();
    });
    it("", function () {
      controller.loadRepoVersionsToModel();
      expect(App.HttpClient.get.calledOnce).to.be.true;
    });
  });

  describe('#currentVersionObserver()', function () {

    var cases = [
      {
        stackVersionType: 'HDP',
        repoVersion: '2.2.1.1.0-1',
        isStormMetricsSupported: false,
        title: 'HDP < 2.2.2'
      },
      {
        stackVersionType: 'HDP',
        repoVersion: '2.2.2.1.0-1',
        isStormMetricsSupported: true,
        title: 'HDP 2.2.2'
      },
      {
        stackVersionType: 'HDP',
        repoVersion: '2.2.3.1.0-1',
        isStormMetricsSupported: true,
        title: 'HDP > 2.2.2'
      },
      {
        stackVersionType: 'BIGTOP',
        repoVersion: '0.8.1.1.0-1',
        isStormMetricsSupported: true,
        title: 'not HDP'
      }
    ];

    afterEach(function () {
      App.RepositoryVersion.find.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App.RepositoryVersion, 'find').returns([
          Em.Object.create({
            status: 'CURRENT',
            stackVersionType: item.stackVersionType
          })
        ]);
        controller.set('currentVersion', {
          repository_version: item.repoVersion
        });
        expect(App.get('isStormMetricsSupported')).to.equal(item.isStormMetricsSupported);
      });
    });

  });

  describe('#updateFinalize', function () {

    beforeEach(function() {
      sinon.stub($, 'ajax', Em.K);
      controller.set('isFinalizeItem', true);
    });

    afterEach(function () {
      $.ajax.restore();
    });

    it('should do ajax-request', function () {
      sinon.stub(App, 'get').withArgs('upgradeState').returns('HOLDING');
      controller.updateFinalize();
      App.get.restore();
      expect($.ajax.calledOnce).to.be.true;
    });

    it('shouldn\'t do ajax-request', function () {
      sinon.stub(App, 'get').withArgs('upgradeState').returns('HOLDING_TIMEDOUT');
      controller.updateFinalize();
      App.get.restore();
      expect(controller.get('isFinalizeItem')).to.be.false;
      expect($.ajax.calledOnce).to.be.false;
    });

  });

  describe('#updateFinalizeSuccessCallback', function () {

    it('data exists and Finalize should be true', function() {
      var data = {
        upgrade_groups: [
          {
            upgrade_items: [
              {
                UpgradeItem: {
                  context: controller.get('finalizeContext'),
                  status: "HOLDING"
                }
              }
            ]
          }
        ]
      };
      controller.set('isFinalizeItem', false);
      controller.updateFinalizeSuccessCallback(data);
      expect(controller.get('isFinalizeItem')).to.be.true;
    });

    it('data exists and Finalize should be false', function() {
      var data = {
        upgrade_groups: [
          {
            upgrade_items: [
              {
                UpgradeItem: {
                  context: '!@#$%^&',
                  status: "HOLDING"
                }
              }
            ]
          }
        ]
      };
      controller.set('isFinalizeItem', true);
      controller.updateFinalizeSuccessCallback(data);
      expect(controller.get('isFinalizeItem')).to.be.false;
    });

    it('data doesn\'t exist', function() {
      var data = null;
      controller.set('isFinalizeItem', true);
      controller.updateFinalizeSuccessCallback(data);
      expect(controller.get('isFinalizeItem')).to.be.false;
    });

  });

  describe('#updateFinalizeErrorCallback', function () {

    it('should set isFinalizeItem to false', function () {
      controller.set('isFinalizeItem', true);
      controller.updateFinalizeErrorCallback();
      expect(controller.get('isFinalizeItem')).to.be.false;
    });

  });

});
