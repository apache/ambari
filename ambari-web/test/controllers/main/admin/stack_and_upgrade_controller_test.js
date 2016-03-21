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
var testHelpers = require('test/helpers');

describe('App.MainAdminStackAndUpgradeController', function() {

  var controller = App.MainAdminStackAndUpgradeController.create({
    getDBProperty: Em.K,
    setDBProperty: Em.K
  });

  before(function () {
    sinon.stub(App.router, 'get').withArgs('clusterController.isLoaded').returns(false);
  });

  after(function () {
    App.router.get.restore();
    controller.destroy();
  });

  describe("#realRepoUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
      this.mock.withArgs('apiPrefix').returns('apiPrefix')
        .withArgs('stackVersionURL').returns('stackVersionURL');
    });
    after(function () {
      this.mock.restore();
    });
    it("should be valid", function() {
      controller.propertyDidChange('realRepoUrl');
      expect(controller.get('realRepoUrl')).to.equal('apiPrefixstackVersionURL/repository_versions?fields=*,RepositoryVersions/*,operating_systems/*,operating_systems/repositories/*');
    });
  });

  describe("#realStackUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
      this.mock.withArgs('apiPrefix').returns('apiPrefix')
        .withArgs('clusterName').returns('clusterName');
    });
    after(function () {
      this.mock.restore();
    });
    it("should be valid", function() {
      controller.propertyDidChange('realStackUrl');
      expect(controller.get('realStackUrl')).to.equal('apiPrefix/clusters/clusterName/stack_versions?fields=*,repository_versions/*,repository_versions/operating_systems/repositories/*');
    });
  });

  describe("#realUpdateUrl", function() {
    before(function () {
      this.mock = sinon.stub(App, 'get');
      this.mock.withArgs('apiPrefix').returns('apiPrefix')
        .withArgs('clusterName').returns('clusterName');
    });
    after(function () {
      this.mock.restore();
    });
    it("realUpdateUrl is valid", function() {
      controller.propertyDidChange('realUpdateUrl');
      expect(controller.get('realUpdateUrl')).to.equal('apiPrefix/clusters/clusterName/stack_versions?fields=ClusterStackVersions/*');
    });
  });

  describe("#requestStatus", function() {
    it("state ABORTED", function() {
      controller.set('upgradeData', { Upgrade: {request_status: 'ABORTED'}});
      controller.propertyDidChange('requestStatus');
      expect(controller.get('requestStatus')).to.equal('SUSPENDED');
    });
    it("state not ABORTED", function() {
      controller.set('upgradeData', { Upgrade: {request_status: 'INIT'}});
      controller.propertyDidChange('requestStatus');
      expect(controller.get('requestStatus')).to.equal('INIT');
    });
  });

  describe("#load()", function() {
    beforeEach(function(){
      sinon.stub(controller, 'loadUpgradeData').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'loadStackVersionsToModel').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'loadRepoVersionsToModel').returns({
        done: Em.clb
      });
      sinon.stub(App.StackVersion, 'find').returns([Em.Object.create({
        state: 'CURRENT',
        repositoryVersion: {
          repositoryVersion: '2.2',
          displayName: 'HDP-2.2'
        }
      })]);
      controller.load();
    });
    afterEach(function(){
      controller.loadUpgradeData.restore();
      controller.loadStackVersionsToModel.restore();
      controller.loadRepoVersionsToModel.restore();
      App.StackVersion.find.restore();
    });
    it("loadUpgradeData called with valid arguments", function() {
      expect(controller.loadUpgradeData.calledWith(true)).to.be.true;
    });
    it('loadStackVersionsToModel called with valid arguments', function () {
      expect(controller.loadStackVersionsToModel.calledWith(true)).to.be.true;
    });
    it('loadRepoVersionsToModel called once', function () {
      expect(controller.loadRepoVersionsToModel.calledOnce).to.be.true;
    });
    it('currentVersion is corrent', function () {
      expect(controller.get('currentVersion')).to.eql({
        "repository_version": "2.2",
        "repository_name": "HDP-2.2"
      });
    });
  });

  describe("#loadUpgradeData()", function() {

    it("get entire data", function() {
      controller.set('upgradeId', 1);
      controller.loadUpgradeData();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.data');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        id: 1
      });
    });
    it("get only state", function() {
      controller.set('upgradeId', 1);
      controller.loadUpgradeData(true);
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.state');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        id: 1
      });
    });
    it("upgrade id is null", function() {
      controller.set('upgradeId', null);
      controller.loadUpgradeData();
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.state');
      var args2 = testHelpers.findAjaxRequest('name', 'admin.upgrade.data');
      expect(args).to.not.exists;
      expect(args2).to.not.exists;
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

    it("default callback", function() {
      var item = Em.Object.create({
        request_id: 1,
        group_id: 2,
        stage_id: 3
      });
      controller.getUpgradeItem(item);
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.upgrade_item');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].success).to.be.equal('getUpgradeItemSuccessCallback');
      expect(args[0].data).to.be.eql({
        upgradeId: 1,
        groupId: 2,
        stageId: 3
      });
    });
    it("custom callback", function() {
      var item = Em.Object.create({
        request_id: 1,
        group_id: 2,
        stage_id: 3
      });
      controller.getUpgradeItem(item, 'customCallback');
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.upgrade_item');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].success).to.be.equal('customCallback');
      expect(args[0].data).to.be.eql({
        upgradeId: 1,
        groupId: 2,
        stageId: 3
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
    it("make ajax call", function() {
      controller.runPreUpgradeCheck(Em.Object.create({
        repositoryVersion: '2.2',
        displayName: 'HDP-2.2',
        upgradeType: 'ROLLING',
        skipComponentFailures: false,
        skipSCFailures: false
      }));
      var args = testHelpers.findAjaxRequest('name', 'admin.upgrade.pre_upgrade_check');
      expect(args[0]).to.exists;
      expect(args[0].sender).to.be.eql(controller);
      expect(args[0].data).to.be.eql({
        value: '2.2',
        label: 'HDP-2.2',
        type: 'ROLLING',
        skipComponentFailures: 'false',
        skipSCFailures: 'false'
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
        if (item.check.id === 'CONFIG_MERGE') {
          expect(App.showClusterCheckPopup.firstCall.args[2]).to.eql(item.configs);
        }
      });
    });
  });

  describe("#initDBProperties()", function() {
    before(function () {
      this.mock = sinon.stub(controller, 'getDBProperties');
    });
    after(function () {
      this.mock.restore();
    });
    it("set string properties", function () {
      this.mock.returns({prop: 'string'});
      controller.initDBProperties();
      expect(controller.get('prop')).to.equal('string');
    });
    it("set number properties", function () {
      this.mock.returns({prop: 0});
      controller.initDBProperties();
      expect(controller.get('prop')).to.equal(0);
    });
    it("set boolean properties", function () {
      this.mock.returns({prop: false});
      controller.initDBProperties();
      expect(controller.get('prop')).to.be.false;
    });
    it("set undefined properties", function () {
      this.mock.returns({prop: undefined});
      controller.set('prop', 'value');
      controller.initDBProperties();
      expect(controller.get('prop')).to.equal('value');
    });
    it("set null properties", function () {
      this.mock.returns({prop: null});
      controller.set('prop', 'value');
      controller.initDBProperties();
      expect(controller.get('prop')).to.equal('value');
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
    var callArgs;

    beforeEach(function () {
      sinon.stub(controller, 'setDBProperty', Em.K);
      controller.set('currentVersion', {
        repository_version: '2.2'
      });
      controller.upgrade({
        value: '2.2',
        label: 'HDP-2.2'
      });
      callArgs = testHelpers.findAjaxRequest('name', 'admin.upgrade.start')[0];
    });

    afterEach(function () {
      controller.setDBProperty.restore();
    });

    it("request-data is valid", function() {
      expect(callArgs.data).to.eql({"value": '2.2', "label": 'HDP-2.2'});
    });
    it('request-name is valid', function () {
      expect(callArgs.name).to.equal('admin.upgrade.start');
    });
    it('request-sender is valid', function () {
      expect(callArgs.sender).to.eql(controller);
    });
    it('callback is valid', function () {
      expect(callArgs.success).to.equal('upgradeSuccessCallback');
    });
    it('callback is called', function () {
      expect(callArgs.callback).to.be.called;
    });
    it('setDBProperty is called with valid data', function () {
      expect(controller.setDBProperty.calledWith('currentVersion', {
        repository_version: '2.2'
      })).to.be.true;
    });
  });

  describe("#upgradeSuccessCallback()", function() {

    beforeEach(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(controller, 'openUpgradeDialog', Em.K);
      sinon.stub(controller, 'setDBProperties', Em.K);
      sinon.stub(controller, 'load', Em.K);
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
    });

    afterEach(function () {
      App.clusterStatus.setClusterStatus.restore();
      controller.openUpgradeDialog.restore();
      controller.setDBProperties.restore();
      controller.load.restore();
    });

    it('load is called ocne', function() {
      expect(controller.load.calledOnce).to.be.true;
    });
    it('upgradeVersion is HDP-2.2.1', function() {
      expect(controller.get('upgradeVersion')).to.equal('HDP-2.2.1');
    });
    it('upgradeData is null', function() {
      expect(controller.get('upgradeData')).to.be.null;
    });
    it('isDowngrade is true', function() {
      expect(controller.get('isDowngrade')).to.be.true;
    });
    it('App.clusterStatus.setClusterStatus is called once', function() {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
    it('controller.openUpgradeDialog is called once', function() {
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

    describe('upgradeData exists', function () {

      var groups;

      beforeEach(function() {
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
                display_status: 'COMPLETED',
                progress_percent: 100,
                completed_task_count: 3
              },
              upgrade_items: [
                {
                  UpgradeItem: {
                    stage_id: 1,
                    status: 'COMPLETED',
                    display_status: 'COMPLETED',
                    progress_percent: 100
                  }
                }
              ]
            },
            {
              UpgradeGroup: {
                group_id: 2,
                status: 'ABORTED',
                display_status: 'ABORTED',
                progress_percent: 50,
                completed_task_count: 1
              },
              upgrade_items: [
                {
                  UpgradeItem: {
                    stage_id: 2,
                    status: 'ABORTED',
                    display_status: 'ABORTED',
                    progress_percent: 99
                  }
                },
                {
                  UpgradeItem: {
                    stage_id: 3,
                    status: 'PENDING',
                    display_status: 'PENDING',
                    progress_percent: 0
                  }
                }
              ]
            }
          ]
        };
        controller.set('upgradeData', oldData);
        controller.updateUpgradeData(newData);
        groups = controller.get('upgradeData.upgradeGroups');
      });

      describe("checking 1st group", function() {
        it('status is COMPLETED', function () {
          expect(groups[0].get('status')).to.equal('COMPLETED');
        });
        it('display_status is COMPLETED', function () {
          expect(groups[0].get('display_status')).to.equal('COMPLETED');
        });
        it('progress_percent is 100', function () {
          expect(groups[0].get('progress_percent')).to.equal(100);
        });
        it('completed_task_count = 3', function () {
          expect(groups[0].get('completed_task_count')).to.equal(3);
        });
        it('upgradeItems.0.status is COMPLETED', function () {
          expect(groups[0].get('upgradeItems')[0].get('status')).to.equal('COMPLETED');
        });
        it('upgradeItems.0.display_status is COMPLETED', function () {
          expect(groups[0].get('upgradeItems')[0].get('display_status')).to.equal('COMPLETED');
        });
        it('upgradeItems.0.progress_percent is 100', function () {
          expect(groups[0].get('upgradeItems')[0].get('progress_percent')).to.equal(100);
        });
        it('hasExpandableItems is true', function () {
          expect(groups[0].get('hasExpandableItems')).to.be.true;
        });
      });

      describe('checking 2nd group', function () {
        it('status is ABORTED', function () {
          expect(groups[1].get('status')).to.equal('ABORTED');
        });
        it('display_status is ABORTED', function () {
          expect(groups[1].get('display_status')).to.equal('ABORTED');
        });
        it('progress_percent is 50', function () {
          expect(groups[1].get('progress_percent')).to.equal(50);
        });
        it('completed_task_count = 1', function () {
          expect(groups[1].get('completed_task_count')).to.equal(1);
        });
        it('upgradeItems.[].status = ["ABORTED", "PENDING"]', function () {
          expect(groups[1].get('upgradeItems').mapProperty('status')).to.eql(['ABORTED', 'PENDING']);
        });
        it('upgradeItems.[].display_status = ["ABORTED", "PENDING"]', function () {
          expect(groups[1].get('upgradeItems').mapProperty('display_status')).to.eql(['ABORTED', 'PENDING']);
        });
        it('upgradeItems.[].progress_percent = [99, 0]', function () {
          expect(groups[1].get('upgradeItems').mapProperty('progress_percent')).to.eql([99, 0]);
        });
        it('hasExpandableItems is false', function () {
          expect(groups[1].get('hasExpandableItems')).to.be.false;
        });
      });

    });
  });

  describe("#initUpgradeData()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'setDBProperty');
      var newData = {
        Upgrade: {
          request_id: 1,
          downgrade_allowed: false
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
    });
    afterEach(function () {
      controller.setDBProperty.restore();
    });
    it("setDBProperty called with valid arguments", function() {
      expect(controller.setDBProperty.calledWith('downgradeAllowed', false)).to.be.true;
    });
    it('downgradeAllowed is false', function () {
      expect(controller.get('downgradeAllowed')).to.be.false;
    });
    it('upgradeData.Upgrade.request_id is 1', function () {
      expect(controller.get('upgradeData.Upgrade.request_id')).to.equal(1);
    });
    it('upgradeData.upgradeGroups contain valid data', function () {
      var groups = controller.get('upgradeData.upgradeGroups');
      expect(groups.mapProperty('group_id')).to.eql([3,2,1]);
      expect(groups[2].get('upgradeItems').mapProperty('stage_id')).to.eql([2,1]);
      expect(groups.mapProperty('hasExpandableItems')).to.eql([false, false, true]);
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

  describe("#upgradeOptions()", function() {
    var version = Em.Object.create({displayName: 'HDP-2.2'});
    beforeEach(function () {
      sinon.spy(App.ModalPopup, 'show');
      sinon.spy(App, 'showConfirmationFeedBackPopup');
      sinon.stub(controller, 'getSupportedUpgradeTypes').returns({
        done: function (callback) {
          callback([1]);
          return {
            always: function (alwaysCallback) {
              alwaysCallback();
              return {};
            }
          };
        }
      });
      sinon.stub(controller, 'runPreUpgradeCheck', Em.K);
      sinon.stub(App.RepositoryVersion, 'find').returns([
        Em.Object.create({
          status: 'CURRENT'
        })
      ]);
      controller.get('runningCheckRequests').clear();
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
      App.showConfirmationFeedBackPopup.restore();
      controller.runPreUpgradeCheck.restore();
      controller.getSupportedUpgradeTypes.restore();
      controller.get('upgradeMethods').setEach('selected', false);
      App.RepositoryVersion.find.restore();
    });

    describe("show confirmation popup", function() {

      beforeEach(function () {
        controller.set('isDowngrade', false);
        this.popup = controller.upgradeOptions(false, version);
      });

      it('popup is shown', function () {
        expect(App.ModalPopup.show.calledOnce).to.be.true;
      });

      it('all upgradeMethods have isCheckRequestInProgress = true', function () {
        expect(controller.get('upgradeMethods').everyProperty('isCheckRequestInProgress')).to.be.true;
      });

      it('upgradeMethods no one is selected', function () {
        expect(controller.get('upgradeMethods').someProperty('selected')).to.be.false;
      });

      describe('#popup.onPrimary', function () {

        beforeEach(function () {
          controller.get('upgradeMethods')[0].set('selected', true);
          this.confirmPopup = this.popup.onPrimary();
        });

        it('showConfirmationFeedBackPopup is called once', function () {
          expect(App.showConfirmationFeedBackPopup.calledOnce).to.be.true;
        });

        describe('#confirmPopup.onPrimary', function () {
          beforeEach(function () {
            this.confirmPopup.onPrimary();
          });

          it('runPreUpgradeCheck is called with correct version', function () {
            expect(controller.runPreUpgradeCheck.calledWith(version)).to.be.true;
          });

          it('runningCheckRequests has 1 item', function () {
            expect(controller.get('runningCheckRequests')).to.have.length(1);
          });

        });

      });

    });

    describe("NOT show confirmation popup on Downgrade", function() {
      beforeEach(function () {
        controller.set('isDowngrade', true);
        controller.upgradeOptions(false, version);
      });

      it('runningCheckRequests has 1 item', function () {
        expect( controller.get('runningCheckRequests')).to.have.length(1);
      });

    });
  });

  describe("#confirmUpgrade()", function() {
    before(function () {
      sinon.stub(controller, 'upgradeOptions', Em.K);
    });
    after(function () {
      controller.upgradeOptions.restore();
    });
    it("show show upgrade options popup window", function() {
      var version = Em.Object.create({displayName: 'HDP-2.2'});
      controller.confirmUpgrade(version);
      expect(controller.upgradeOptions.calledWith(false, version)).to.be.true;
    });
  });

  describe("#downgrade()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'abortUpgrade');
      sinon.stub(App.RepositoryVersion, 'find').returns([
        Em.Object.create({
          displayName: 'HDP-2.3',
          repositoryVersion: '2.3'
        })
      ]);
      controller.set('upgradeVersion', 'HDP-2.3');
      controller.set('upgradeType', 'NON_ROLLING');
      controller.downgrade(Em.Object.create({
        repository_version: '2.2',
        repository_name: 'HDP-2.2'
      }), {context: 'context'});
      this.callArgs = testHelpers.findAjaxRequest('name', 'admin.downgrade.start')[0];
    });

    afterEach(function () {
      controller.abortUpgrade.restore();
      App.RepositoryVersion.find.restore();
    });

    it('abortUpgrade is called once', function() {
      expect(controller.abortUpgrade.calledOnce).to.be.true;
    });
    it('request-data is valid', function () {
      expect(this.callArgs.data).to.eql({
        from: '2.3',
        value: '2.2',
        label: 'HDP-2.2',
        isDowngrade: true,
        upgradeType: "NON_ROLLING"
      });
    });
    it('request-name is valid', function () {
      expect(this.callArgs.name).to.be.equal('admin.downgrade.start');
    });
    it('request-sender is valid', function () {
      expect(this.callArgs.sender).to.be.eql(controller);
    });
    it('callback is valid', function () {
      expect(this.callArgs.success).to.be.equal('upgradeSuccessCallback');
    });
    it('callback is called', function () {
      expect(this.callArgs.callback).to.be.called;
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

    it("make ajax call", function () {
      var repo = Em.Object.create({
        stackVersionType: 'HDP',
        stackVersionNumber: '2.2',
        repositoryVersion: '2.2.1',
        repoId: 1
      });
      controller.installRepoVersion(repo);
      var args = testHelpers.findAjaxRequest('name', 'admin.stack_version.install.repo_version');
      expect(args).to.exists;
    });
  });

  describe("#installRepoVersionSuccess()", function() {
    var mock = Em.Object.create({
      id: 1,
      defaultStatus: 'INIT',
      stackVersion: {}
    });
    beforeEach(function () {
      sinon.spy(mock, 'set');
      sinon.stub(App.db, 'set', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(App.RepositoryVersion, 'find').returns(mock);
      controller.installRepoVersionSuccess({Requests: {id: 1}}, {}, {id: 1});
    });
    afterEach(function () {
      App.db.set.restore();
      App.clusterStatus.setClusterStatus.restore();
      App.RepositoryVersion.find.restore();
      mock.set.restore();
    });
    it("data sdtored to the local db", function() {
      expect(App.db.set.calledWith('repoVersionInstall', 'id', [1])).to.be.true;
    });
    it('clusterStatus is updated', function () {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
    it('App.RepositoryVersion models have valid states', function () {
      expect(App.RepositoryVersion.find.calledWith(1)).to.be.true;
      expect(App.RepositoryVersion.find(1).get('defaultStatus')).to.equal('INSTALLING');
      expect(App.RepositoryVersion.find(1).get('stackVersion.state')).to.equal('INSTALLING');
    });
  });

  describe("#setUpgradeItemStatus()", function () {
    var item;
    beforeEach(function () {
      item = Em.Object.create({
        request_id: 1,
        stage_id: 1,
        group_id: 1
      });
      controller.setUpgradeItemStatus(item, 'PENDING');
      this.callArgs = testHelpers.findAjaxRequest('name', 'admin.upgrade.upgradeItem.setState')[0];
    });

    it('request-data is valid', function () {
      expect(this.callArgs.data).to.be.eql({upgradeId: 1, itemId: 1, groupId: 1, status: 'PENDING'});
    });
    it('request-name is valid', function () {
      expect(this.callArgs.name).to.be.equal('admin.upgrade.upgradeItem.setState');
    });
    it('request-sendeer is valid', function () {
      expect(this.callArgs.sender).to.be.eql(controller);
    });
    it('callback is called', function () {
      expect(this.callArgs.callback).to.be.called;
    });
    it('item.status is PENDING', function () {
      expect(item.get('status')).to.equal('PENDING');
    });
  });

  describe("#prepareRepoForSaving()", function () {
    it("prepare date for saving", function () {
      var repo = Em.Object.create({
        operatingSystems: [
          Em.Object.create({
            osType: "redhat6",
            isDisabled: Em.computed.not('isSelected'),
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
    });
    after(function(){
      this.mock.restore();
      controller.prepareRepoForSaving.restore();
    });
    describe("validation errors present", function() {

      beforeEach(function () {
        this.mock.returns({
          done: function(callback) {callback([1]);}
        });
        controller.saveRepoOS(Em.Object.create({repoVersionId: 1}), true);
        this.args = testHelpers.findAjaxRequest('name', 'admin.stack_versions.edit.repo');
      });

      it('validateRepoVersions is called with valid arguments', function () {
        expect(controller.validateRepoVersions.calledWith(Em.Object.create({repoVersionId: 1}), true)).to.be.true;
      });

      it('prepareRepoForSaving is not called', function () {
        expect(controller.prepareRepoForSaving.called).to.be.false;
      });

      it('no requests are sent', function () {
        expect(this.args).to.not.exists;
      });
    });

    describe("no validation errors", function() {

      beforeEach(function () {
        this.mock.returns({
          done: function(callback) {callback([]);}
        });
        controller.saveRepoOS(Em.Object.create({repoVersionId: 1}), true);
      });
      it('validateRepoVersions is called with valid arguments', function () {
        expect(controller.validateRepoVersions.calledWith(Em.Object.create({repoVersionId: 1}), true)).to.be.true;
      });
      it('prepareRepoForSaving is called with valid arguments', function () {
        expect(controller.prepareRepoForSaving.calledWith(Em.Object.create({repoVersionId: 1}))).to.be.true;
      });
    });
  });

  describe("#validateRepoVersions()", function () {

    it("skip validation", function () {
      controller.validateRepoVersions(Em.Object.create({repoVersionId: 1}), true);
      var args = testHelpers.findAjaxRequest('name', 'admin.stack_versions.validate.repo');
      expect(args).to.not.exists;
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
      var args = testHelpers.findAjaxRequest('name', 'admin.stack_versions.validate.repo');
      expect(args[0]).to.exists;
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
    it("HttpClient did get-request", function () {
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
    it("HttpClient did get-request", function () {
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
      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(App.RepositoryVersion, 'find').returns([
            Em.Object.create({
              status: 'CURRENT',
              stackVersionType: item.stackVersionType
            })
          ]);
          controller.set('currentVersion', {
            repository_version: item.repoVersion
          });
        });

        it('isStormMetricsSupported is ' + (item.isStormMetricsSupported ? '' : 'not') + ' supported', function () {
          expect(App.get('isStormMetricsSupported')).to.equal(item.isStormMetricsSupported);
        });

      });
    });

  });

  describe('#updateFinalize', function () {

    beforeEach(function() {
      controller.set('isFinalizeItem', true);
      this.stub = sinon.stub(App, 'get');
    });

    afterEach(function () {
      this.stub.restore();
    });

    describe('should do ajax-request', function () {

      beforeEach(function () {
        this.stub.withArgs('upgradeState').returns('HOLDING');
        controller.updateFinalize();
        this.args = testHelpers.findAjaxRequest('name', 'admin.upgrade.finalizeContext');
      });

      it('request is sent', function () {
        expect(this.args[0]).to.exists;
      });

    });

    describe('shouldn\'t do ajax-request', function () {

      beforeEach(function () {
        this.stub.withArgs('upgradeState').returns('HOLDING_TIMEDOUT');
        controller.updateFinalize();
        this.args = testHelpers.findAjaxRequest('name', 'admin.upgrade.finalizeContext');
      });

      it('request is not sent', function () {
        expect(this.args).to.not.exists;
      });

      it('isFinalizeItem is false', function () {
        expect(controller.get('isFinalizeItem')).to.be.false;
      });

    });

  });

  describe('#updateFinalizeSuccessCallback', function () {

    it('data exists and Finalize should be true', function() {
      var data = {
        items: [
          {
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

  describe("#suspendUpgrade()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'abortUpgradeWithSuspend').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'setDBProperty', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      controller.suspendUpgrade();
    });
    afterEach(function () {
      controller.abortUpgradeWithSuspend.restore();
      controller.setDBProperty.restore();
      App.clusterStatus.setClusterStatus.restore();
    });
    it("upgrade aborted", function() {
      expect(controller.abortUpgradeWithSuspend.calledOnce).to.be.true;
    });
    it('App.upgradeState is ABORTED', function () {
      expect(App.get('upgradeState')).to.equal('ABORTED');
    });
    it('new upgradeState is saved to the localDB', function () {
      expect(controller.setDBProperty.calledWith('upgradeState', 'ABORTED')).to.be.true;
    });
    it('clusterStatus is updated', function () {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#resumeUpgrade()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'retryUpgrade').returns({
        done: Em.clb
      });
      sinon.stub(controller, 'setDBProperty', Em.K);
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      controller.resumeUpgrade();
    });
    afterEach(function () {
      controller.retryUpgrade.restore();
      controller.setDBProperty.restore();
      App.clusterStatus.setClusterStatus.restore();
    });
    it("Upgrade is retrying", function() {
      expect(controller.retryUpgrade.calledOnce).to.be.true;
    });
    it('App.upgradeState is PENDING', function () {
      expect(App.get('upgradeState')).to.equal('PENDING');
    });
    it('new upgradeState is saved to the localDB', function () {
      expect(controller.setDBProperty.calledWith('upgradeState', 'PENDING')).to.be.true;
    });
    it('clusterStatus is updated', function () {
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#runUpgradeMethodChecks()", function() {
    beforeEach(function () {
      sinon.stub(controller, 'runPreUpgradeCheckOnly');
    });
    afterEach(function () {
      controller.runPreUpgradeCheckOnly.restore();
      controller.get('upgradeMethods').setEach('allowed', true);
    });
    it("no allowed upgrade methods", function () {
      controller.get('upgradeMethods').setEach('allowed', false);
      controller.runUpgradeMethodChecks();
      expect(controller.runPreUpgradeCheckOnly.called).to.be.false;
    });
    it("Rolling method allowed", function () {
      controller.get('upgradeMethods').setEach('allowed', true);
      controller.runUpgradeMethodChecks(Em.Object.create({
        repositoryVersion: 'v1',
        displayName: 'V1'
      }));
      expect(controller.runPreUpgradeCheckOnly.calledWith({
        value: 'v1',
        label: 'V1',
        type: 'ROLLING'
      })).to.be.true;
    });
  });

  describe("#restoreLastUpgrade()", function () {

    var data = {
      Upgrade: {
        request_id: 1,
        direction: 'UPGRADE',
        request_status: 'PENDING',
        upgrade_type: 'ROLLING',
        downgrade_allowed: true,
        skip_failures: true,
        skip_service_check_failures: true,
        to_version: '1'
      }
    };

    beforeEach(function () {
      sinon.stub(App.RepositoryVersion, 'find').returns([Em.Object.create({
        repositoryVersion: '1',
        displayName: 'HDP-1'
      })]);
      sinon.stub(controller, 'setDBProperties');
      sinon.stub(controller, 'loadRepoVersionsToModel', function () {
        return {
          done: function (callback) {
            callback();
          }
        }
      });
      sinon.stub(controller, 'setDBProperty');
      sinon.stub(controller, 'initDBProperties');
      sinon.stub(controller, 'loadUpgradeData');
      controller.restoreLastUpgrade(data);
    });
    afterEach(function () {
      App.RepositoryVersion.find.restore();
      controller.setDBProperties.restore();
      controller.loadRepoVersionsToModel.restore();
      controller.setDBProperty.restore();
      controller.initDBProperties.restore();
      controller.loadUpgradeData.restore();
    });
    it('proper data is saved to the localDB', function () {
      expect(controller.setDBProperties.getCall(0).args[0]).to.eql({
        upgradeId: 1,
        isDowngrade: false,
        upgradeState: 'PENDING',
        upgradeType: "ROLLING",
        downgradeAllowed: true,
        upgradeTypeDisplayName: Em.I18n.t('admin.stackVersions.version.upgrade.upgradeOptions.RU.title'),
        failuresTolerance: Em.Object.create({
          skipComponentFailures: true,
          skipSCFailures: true
        })
      });
    });
    it('models are saved', function () {
      expect(controller.loadRepoVersionsToModel.calledOnce).to.be.true;
    });
    it('correct upgradeVersion is saved to the DB', function () {
      expect(controller.setDBProperty.calledWith('upgradeVersion', 'HDP-1')).to.be.true;
    });
    it('initDBProperties is called', function () {
      expect(controller.initDBProperties.calledOnce).to.be.true;
    });
    it('loadUpgradeData called with valid arguments', function () {
      expect(controller.loadUpgradeData.calledWith(true)).to.be.true;
    });
  });

  describe("#getServiceCheckItemSuccessCallback()", function() {
    var testCases = [
      {
        title: 'no tasks',
        data: {
          tasks: []
        },
        expected: {
          slaveComponentStructuredInfo: null,
          serviceCheckFailuresServicenames: []
        }
      },
      {
        title: 'no structured_out property',
        data: {
          tasks: [
            {
              Tasks: {}
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: null,
          serviceCheckFailuresServicenames: []
        }
      },
      {
        title: 'no failures',
        data: {
          tasks: [
            {
              Tasks: {
                structured_out: {}
              }
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: null,
          serviceCheckFailuresServicenames: []
        }
      },
      {
        title: 'service check failures',
        data: {
          tasks: [
            {
              Tasks: {
                structured_out: {
                  failures: {
                    service_check: ['HDSF', 'YARN']
                  }
                }
              }
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: {
            hosts: [],
            host_detail: {}
          },
          serviceCheckFailuresServicenames: ['HDSF', 'YARN']
        }
      },
      {
        title: 'host-component failures',
        data: {
          tasks: [
            {
              Tasks: {
                structured_out: {
                  failures: {
                    service_check: ['HDSF'],
                    host_component: {
                      "host1": [
                        {
                          component: "DATANODE",
                          service: 'HDFS'
                        }
                      ]
                    }
                  }
                }
              }
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: {
            hosts: ['host1'],
            host_detail: {
              "host1": [
                {
                  component: "DATANODE",
                  service: 'HDFS'
                }
              ]
            }
          },
          serviceCheckFailuresServicenames: ['HDSF']
        }
      }
    ];

    testCases.forEach(function(test) {
      it(test.title, function() {
        controller.set('slaveComponentStructuredInfo', null);
        controller.set('serviceCheckFailuresServicenames', []);
        controller.getServiceCheckItemSuccessCallback(test.data);
        expect(controller.get('serviceCheckFailuresServicenames')).eql(test.expected.serviceCheckFailuresServicenames);
        expect(controller.get('slaveComponentStructuredInfo')).eql(test.expected.slaveComponentStructuredInfo);
      });
    });
  });

  describe("#getSlaveComponentItemSuccessCallback()", function () {
    var testCases = [
      {
        title: 'no tasks',
        data: {
          tasks: []
        },
        expected: {
          slaveComponentStructuredInfo: null
        }
      },
      {
        title: 'structured_out property absent',
        data: {
          tasks: [
            {
              Tasks: {}
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: null
        }
      },
      {
        title: 'structured_out property present',
        data: {
          tasks: [
            {
              Tasks: {
                "structured_out" : {
                  "hosts" : [
                    "host1"
                  ],
                  "host_detail" : {
                    "host1" : [
                      {
                        "service" : "FLUME",
                        "component" : "FLUME_HANDLER"
                      }
                    ]
                  }
                }
              }
            }
          ]
        },
        expected: {
          slaveComponentStructuredInfo: {
            "hosts" : [
              "host1"
            ],
            "host_detail" : {
              "host1" : [
                {
                  "service" : "FLUME",
                  "component" : "FLUME_HANDLER"
                }
              ]
            }
          }
        }
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('slaveComponentStructuredInfo', null);
        controller.getSlaveComponentItemSuccessCallback(test.data);
        expect(controller.get('slaveComponentStructuredInfo')).eql(test.expected.slaveComponentStructuredInfo);
      });
    });
  });

  describe('#getConfigsWarnings', function () {

    var cases = [
      {
        configs: [],
        title: 'no warning'
      },
      {
        configsMergeWarning: {},
        configs: [],
        title: 'empty data'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {}
        },
        configs: [],
        title: 'incomplete data'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            failed_detail: {}
          }
        },
        configs: [],
        title: 'invalid data'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            failed_detail: []
          }
        },
        configs: [],
        title: 'empty configs array'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            status: 'FAIL',
            failed_detail: [
              {
                type: 't0',
                property: 'p0',
                current: 'c0',
                new_stack_value: 'n0',
                result_value: 'r0'
              },
              {
                type: 't1',
                property: 'p1',
                current: 'c1',
                new_stack_value: 'n1'
              },
              {
                type: 't2',
                property: 'p2',
                current: 'c2',
                result_value: 'r2'
              }
            ]
          }
        },
        configs: [],
        title: 'not a warning'
      },
      {
        configsMergeWarning: {
          UpgradeChecks: {
            status: 'WARNING',
            failed_detail: [
              {
                type: 't0',
                property: 'p0',
                current: 'c0',
                new_stack_value: 'n0',
                result_value: 'r0'
              },
              {
                type: 't1',
                property: 'p1',
                current: 'c1',
                new_stack_value: 'n1'
              },
              {
                type: 't2',
                property: 'p2',
                current: 'c2',
                result_value: 'r2'
              }
            ]
          }
        },
        configs: [
          {
            type: 't0',
            name: 'p0',
            currentValue: 'c0',
            recommendedValue: 'n0',
            isDeprecated: false,
            resultingValue: 'r0',
            willBeRemoved: false
          },
          {
            type: 't1',
            name: 'p1',
            currentValue: 'c1',
            recommendedValue: 'n1',
            isDeprecated: false,
            resultingValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved'),
            willBeRemoved: true
          },
          {
            type: 't2',
            name: 'p2',
            currentValue: 'c2',
            recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
            isDeprecated: true,
            resultingValue: 'r2',
            willBeRemoved: false
          }
        ],
        title: 'normal case'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(controller.getConfigsWarnings(item.configsMergeWarning)).to.eql(item.configs);
      });
    });

  });

  describe('#runPreUpgradeCheckOnly', function () {

    var appGetMock,
      upgradeMethods = controller.get('upgradeMethods'),
      cases = [
      {
        supportsPreUpgradeCheck: false,
        ru: {
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        },
        eu: {
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        },
        ajaxCallCount: 0,
        runningCheckRequestsLength: 0,
        title: 'pre-upgrade checks not supported'
      },
      {
        supportsPreUpgradeCheck: true,
        ru: {
          isCheckComplete: false,
          isCheckRequestInProgress: true,
          action: ''
        },
        eu: {
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        },
        ajaxCallCount: 1,
        type: 'ROLLING',
        runningCheckRequestsLength: 1,
        title: 'rolling upgrade'
      },
      {
        supportsPreUpgradeCheck: true,
        ru: {
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        },
        eu: {
          isCheckComplete: false,
          isCheckRequestInProgress: true,
          action: ''
        },
        ajaxCallCount: 1,
        type: 'NON_ROLLING',
        runningCheckRequestsLength: 1,
        title: 'express upgrade'
      }
    ];

    beforeEach(function () {
      appGetMock = sinon.stub(App, 'get');
      controller.get('runningCheckRequests').clear();
      upgradeMethods.forEach(function (method) {
        method.setProperties({
          isCheckComplete: true,
          isCheckRequestInProgress: false,
          action: 'a'
        });
      });
      App.ajax.send.restore();
      sinon.stub(App.ajax, 'send').returns({});
    });

    afterEach(function () {
      appGetMock.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {
        var runningCheckRequests;
        beforeEach(function () {
          runningCheckRequests = controller.get('runningCheckRequests');
          appGetMock.returns(item.supportsPreUpgradeCheck);
          controller.runPreUpgradeCheckOnly({
            type: item.type
          });
          this.ajaxCalls = testHelpers.filterAjaxRequests('name', 'admin.upgrade.pre_upgrade_check');
        });

        it('ROLLING properties', function () {
          expect(upgradeMethods.findProperty('type', 'ROLLING').getProperties('isCheckComplete', 'isCheckRequestInProgress', 'action')).to.eql(item.ru);
        });

        it('NON_ROLLING properties', function () {
          expect(upgradeMethods.findProperty('type', 'NON_ROLLING').getProperties('isCheckComplete', 'isCheckRequestInProgress', 'action')).to.eql(item.eu);
        });

        it(item.ajaxCallCount + ' requests sent', function () {
          expect(this.ajaxCalls.length).to.be.equal(item.ajaxCallCount);
        });

        it('runningCheckRequests length is ' + item.runningCheckRequestsLength, function () {
          expect(runningCheckRequests).to.have.length(item.runningCheckRequestsLength);
        });

        if (item.runningCheckRequestsLength) {
          it('runningCheckRequests.type is ' + item.type, function () {
            expect(runningCheckRequests[0].type).to.equal(item.type);
          });
        }

      });
    });

  });


  describe("#openConfigsInNewWindow()", function () {

    var mock = {
      document: {
        write: function () {}
      },
      focus: function () {}
    };

    beforeEach(function(){
      sinon.stub(window, 'open', function () {
        return mock;
      });
      sinon.spy(mock.document, 'write');
      sinon.spy(mock, 'focus');
      controller.openConfigsInNewWindow({
        context: [
          {
            type: 'type1',
            name: 'name1',
            currentValue: 'currentValue1',
            recommendedValue: 'recommendedValue1',
            resultingValue: 'resultingValue1'
          },
          {
            type: 'type2',
            name: 'name2',
            currentValue: 'currentValue2',
            recommendedValue: 'recommendedValue2',
            resultingValue: 'resultingValue2'
          }
        ]
      });
    });

    afterEach(function(){
      window.open.restore();
      mock.document.write.restore();
      mock.focus.restore();
    });

    it('new window is open', function () {
      expect(window.open.calledOnce).to.be.true;
    });

    it('new window content is valid', function () {
      /*eslint-disable no-useless-concat */
      expect(mock.document.write.calledWith('<table style="text-align: left;"><thead><tr>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.configType') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.propertyName') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.currentValue') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.recommendedValue') + '</th>' +
        '<th>' + Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.resultingValue') + '</th>' +
        '</tr></thead><tbody>' +
        '<tr>' +
        '<td>' + 'type1' + '</td>' +
        '<td>' + 'name1' + '</td>' +
        '<td>' + 'currentValue1' + '</td>' +
        '<td>' + 'recommendedValue1' + '</td>' +
        '<td>' + 'resultingValue1' + '</td>' +
        '</tr>' +
        '<tr>' +
        '<td>' + 'type2' + '</td>' +
        '<td>' + 'name2' + '</td>' +
        '<td>' + 'currentValue2' + '</td>' +
        '<td>' + 'recommendedValue2' + '</td>' +
        '<td>' + 'resultingValue2' + '</td>' +
        '</tr></tbody></table>')).to.be.true;
      /*eslint-enable no-useless-concat */
    });

    it('document.focus is called once', function () {
      expect(mock.focus.calledOnce).to.be.true;
    });
  });

});
