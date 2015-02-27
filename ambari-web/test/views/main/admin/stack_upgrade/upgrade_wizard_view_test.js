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
require('views/main/admin/stack_upgrade/upgrade_wizard_view');

describe('App.upgradeWizardView', function () {
  var view = App.upgradeWizardView.create({
    failedStatuses: ['FAILED']
  });
  view.reopen({
    controller: Em.Object.create({
      upgradeData: Em.Object.create(),
      loadUpgradeData: Em.K,
      setUpgradeItemStatus: Em.K
    })
  });
  view.removeObserver('App.clusterName', view, 'startPolling');

  describe("#upgradeGroups", function () {
    it("upgradeGroups is null", function () {
      view.set('controller.upgradeData.upgradeGroups', null);
      view.propertyDidChange('upgradeGroups');
      expect(view.get('upgradeGroups')).to.be.empty;
    });
    it("upgradeGroups is valid", function () {
      view.set('controller.upgradeData.upgradeGroups', [1]);
      view.propertyDidChange('upgradeGroups');
      expect(view.get('upgradeGroups')).to.eql([1]);
    });
  });

  describe("#activeGroup", function () {
    after(function () {
      view.reopen({
        activeGroup: Em.Object.create()
      });
    });
    it("", function () {
      view.reopen({
        activeStatuses: ['IN_PROGRESS'],
        upgradeGroups: [Em.Object.create({
          status: 'IN_PROGRESS'
        })]
      });
      view.propertyDidChange('activeGroup');
      expect(view.get('activeGroup')).to.eql(Em.Object.create({
        status: 'IN_PROGRESS'
      }));
    });
  });

  describe("#overallProgress", function () {
    it("progress is 1.9", function () {
      view.set('controller.upgradeData', {
        Upgrade: {
          progress_percent: 1.9
        }
      });
      expect(view.get('overallProgress')).to.equal(1);
    });
    it("progress is 1", function () {
      view.set('controller.upgradeData', {
        Upgrade: {
          progress_percent: 1
        }
      });
      expect(view.get('overallProgress')).to.equal(1);
    });
  });

  describe("#startPolling()", function () {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'loadUpgradeData', function () {
        return {
          done: function (callback) {
            callback();
          }
        }
      });
      sinon.stub(view, 'doPolling', Em.K);
      view.set('isLoaded', false);
    });
    afterEach(function () {
      view.get('controller').loadUpgradeData.restore();
      view.doPolling.restore();
    });
    it("clusterName is null", function () {
      App.set('clusterName', null);
      view.startPolling();
      expect(view.doPolling.called).to.be.false;
      expect(view.get('isLoaded')).to.be.false;
    });
    it("clusterName set", function () {
      App.set('clusterName', 'c1');
      view.startPolling();
      expect(view.get('controller').loadUpgradeData.calledOnce).to.be.true;
      expect(view.doPolling.calledOnce).to.be.true;
      expect(view.get('isLoaded')).to.be.true;
    });
  });

  describe("#willInsertElement()", function () {
    before(function () {
      sinon.stub(view, 'startPolling', Em.K);
    });
    after(function () {
      view.startPolling.restore();
    });
    it("call startPolling()", function () {
      view.willInsertElement();
      expect(view.startPolling.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function () {
    it("call startPolling()", function () {
      view.set('isLoaded', true);
      view.willDestroyElement();
      expect(view.get('isLoaded')).to.be.false;
    });
  });

  describe("#doPolling()", function () {
    beforeEach(function () {
      sinon.stub(view.get('controller'), 'loadUpgradeData', function () {
        return {
          done: function (callback) {
            callback();
          }
        }
      });
      sinon.spy(view, 'doPolling');
      this.clock = sinon.useFakeTimers();
    });
    afterEach(function () {
      view.get('controller').loadUpgradeData.restore();
      view.doPolling.restore();
      this.clock.restore();
    });
    it("", function () {
      view.doPolling();
      this.clock.tick(App.bgOperationsUpdateInterval);
      expect(view.get('controller').loadUpgradeData.calledOnce).to.be.true;
      expect(view.doPolling.calledTwice).to.be.true;
    });
  });

  describe("#continue()", function () {
    before(function () {
      sinon.stub(view.get('controller'), 'setUpgradeItemStatus', Em.K);
    });
    after(function () {
      view.get('controller').setUpgradeItemStatus.restore();
    });
    it("", function () {
      view.continue({context: Em.Object.create({'status': 'HOLDING_FAILED'})});
      expect(view.get('controller').setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'HOLDING_FAILED'}), 'FAILED')).to.be.true;
    });
  });

  describe("#complete()", function () {
    before(function () {
      sinon.stub(view.get('controller'), 'setUpgradeItemStatus', Em.K);
    });
    after(function () {
      view.get('controller').setUpgradeItemStatus.restore();
    });
    it("", function () {
      view.complete({context: Em.Object.create({'status': 'FAILED'})});
      expect(view.get('controller').setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'FAILED'}), 'COMPLETED')).to.be.true;
      expect(view.get('isManualDone')).to.be.false;
    });
  });

  describe("#retry()", function () {
    before(function () {
      sinon.stub(view.get('controller'), 'setUpgradeItemStatus', Em.K);
    });
    after(function () {
      view.get('controller').setUpgradeItemStatus.restore();
    });
    it("", function () {
      view.retry({context: Em.Object.create({'status': 'FAILED'})});
      expect(view.get('controller').setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'FAILED'}), 'PENDING')).to.be.true;
    });
  });

  describe("#manualItem", function () {
    it("no running item", function () {
      view.set('activeGroup.upgradeItems', []);
      view.propertyDidChange('manualItem');
      expect(view.get('manualItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('activeGroup.upgradeItems', [Em.Object.create({status: 'HOLDING'})]);
      view.propertyDidChange('manualItem');
      expect(view.get('manualItem')).to.be.eql(Em.Object.create({status: 'HOLDING'}));
    });
  });

  describe("#isManualProceedDisabled", function () {
    it("requestInProgress is false", function () {
      view.set('isManualDone', true);
      view.set('controller.requestInProgress', false);
      view.propertyDidChange('isManualProceedDisabled');
      expect(view.get('isManualProceedDisabled')).to.be.false;
    });
    it("requestInProgress is true", function () {
      view.set('controller.requestInProgress', true);
      view.propertyDidChange('isManualProceedDisabled');
      expect(view.get('isManualProceedDisabled')).to.be.true;
    });

  });

  describe("#failedItem", function () {
    it("no running item", function () {
      view.set('activeGroup.upgradeItems', []);
      view.propertyDidChange('failedItem');
      expect(view.get('failedItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('activeGroup.upgradeItems', [Em.Object.create({status: 'FAILED'})]);
      view.propertyDidChange('failedItem');
      expect(view.get('failedItem')).to.be.eql(Em.Object.create({status: 'FAILED'}));
    });
  });

  describe("#runningItem", function () {
    it("no running item", function () {
      view.set('activeGroup.upgradeItems', []);
      view.propertyDidChange('runningItem');
      expect(view.get('runningItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('activeGroup.upgradeItems', [
        {status: 'IN_PROGRESS'}
      ]);
      view.propertyDidChange('runningItem');
      expect(view.get('runningItem')).to.be.eql({status: 'IN_PROGRESS'});
    });
  });

  describe("#noActiveItem", function () {
    beforeEach(function () {
      this.mock = sinon.stub(App, 'get');
    });
    afterEach(function () {
      this.mock.restore();
    });
    var testCases = [
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: null,
          upgradeState: 'COMPLETED'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: null,
          upgradeState: 'INIT'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: null,
          upgradeState: 'IN_PROGRESS'
        },
        result: true
      },
      {
        data: {
          failedItem: {},
          runningItem: null,
          manualItem: null,
          upgradeState: 'IN_PROGRESS'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: {},
          manualItem: null,
          upgradeState: 'IN_PROGRESS'
        },
        result: false
      },
      {
        data: {
          failedItem: null,
          runningItem: null,
          manualItem: {},
          upgradeState: 'IN_PROGRESS'
        },
        result: false
      }
    ].forEach(function (test) {
        it("failedItem = " + test.data.failedItem +
        ";runningItem = " + test.data.runningItem +
        ";manualItem = " + test.data.manualItem +
        ";upgradeState = " + test.data.upgradeState, function () {
          view.reopen(test.data);
          this.mock.withArgs('upgradeState').returns(test.data.upgradeState);
          view.propertyDidChange('noActiveItem');
          expect(view.get('noActiveItem')).to.equal(test.result);
        });
      });
  });

  describe("#isHoldingState", function () {
    var testCases = [
      {
        data: {
          failedItem: {status: 'PENDING'}
        },
        result: false
      },
      {
        data: {
          failedItem: {status: 'HOLDING_FAILED'}
        },
        result: true
      },
      {
        data: {
          failedItem: {status: 'HOLDING_TIMED_OUT'}
        },
        result: true
      }
    ];
    testCases.forEach(function (test) {
      it('failedItem - ' + test.data.failedItem, function () {
        view.reopen({
          failedItem: test.data.failedItem
        });
        view.propertyDidChange('isHoldingState');
        expect(view.get('isHoldingState')).to.equal(test.result);
      });
    });
  });

  describe("#isDowngradeAvailable", function () {
    it("downgrade available", function () {
      view.set('controller.isDowngrade', false);
      view.propertyDidChange('isDowngradeAvailable');
      expect(view.get('isDowngradeAvailable')).to.be.true;
    });
    it("downgrade unavailable", function () {
      view.set('controller.isDowngrade', true);
      view.propertyDidChange('isDowngradeAvailable');
      expect(view.get('isDowngradeAvailable')).to.be.false;
    });
  });

  describe("#taskDetails", function () {
    it("runningItem present", function () {
      view.reopen({
        runningItem: Em.Object.create({
          tasks: [{status: "IN_PROGRESS"}]
        })
      });
      view.propertyDidChange('taskDetails');
      expect(view.get('taskDetails')).to.eql({status: "IN_PROGRESS"});
    });
    it("failedItem present", function () {
      view.reopen({
        failedItem: Em.Object.create({
          tasks: [Em.Object.create({status: "FAILED"})]
        }),
        failedStatuses: ['FAILED'],
        runningItem: null
      });
      view.propertyDidChange('taskDetails');
      expect(view.get('taskDetails').get('status')).to.equal('FAILED');
    });
    it("failedItem and runningItem are absent", function () {
      view.reopen({
        failedItem: null,
        runningItem: null
      });
      view.propertyDidChange('taskDetails');
      expect(view.get('taskDetails')).to.be.null;
    });
  });

  describe("#isFinalizeItem", function () {
    it("", function () {
      view.reopen({
        manualItem: {
          context: 'Confirm Finalize'
        }
      });
      view.propertyDidChange('isFinalizeItem');
      expect(view.get('isFinalizeItem')).to.be.true;
    });
  });

  describe("#toggleDetails()", function () {
    before(function () {
      sinon.stub(view, 'toggleProperty', Em.K);
    });
    after(function () {
      view.toggleProperty.restore();
    });
    it("", function () {
      view.toggleDetails();
      expect(view.toggleProperty.calledWith('isDetailsOpened')).to.be.true;
    });
  });

  describe("#upgradeStatusLabel", function () {
    var testCases = [
      {
        data: {
          status: 'QUEUED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress')
      },
      {
        data: {
          status: 'PENDING',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress')
      },
      {
        data: {
          status: 'IN_PROGRESS',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress')
      },
      {
        data: {
          status: 'COMPLETED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.completed')
      },
      {
        data: {
          status: 'ABORTED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'TIMEDOUT',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'FAILED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'HOLDING_FAILED',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'HOLDING_TIMEDOUT',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: 'HOLDING',
          isDowngrade: false
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused')
      },
      {
        data: {
          status: '',
          isDowngrade: false
        },
        result: ''
      },
      {
        data: {
          status: 'QUEUED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress.downgrade')
      },
      {
        data: {
          status: 'PENDING',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress.downgrade')
      },
      {
        data: {
          status: 'IN_PROGRESS',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.inProgress.downgrade')
      },
      {
        data: {
          status: 'COMPLETED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.completed.downgrade')
      },
      {
        data: {
          status: 'ABORTED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'TIMEDOUT',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'FAILED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'HOLDING_FAILED',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'HOLDING_TIMEDOUT',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      },
      {
        data: {
          status: 'HOLDING',
          isDowngrade: true
        },
        result: Em.I18n.t('admin.stackUpgrade.state.paused.downgrade')
      }
    ].forEach(function (test) {
        it('status = ' + test.data.status + ", isDowngrade = " + test.data.isDowngrade, function () {
          view.set('controller.upgradeData.Upgrade.request_status', test.data.status);
          view.set('controller.isDowngrade', test.data.isDowngrade);
          view.propertyDidChange('upgradeStatusLabel');
          expect(view.get('upgradeStatusLabel')).to.equal(test.result);
        });
      });
  });

});
