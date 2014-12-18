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
require('views/main/admin/stack_upgrade/upgrade_group_view');

describe('App.upgradeGroupView', function () {
  var view = App.upgradeGroupView.create({
    content: Em.Object.create({}),
    failedStatuses: ['FAILED']
  });

  describe("#runningItem", function () {
    it("no running item", function () {
      view.set('content.upgradeItems', []);
      view.propertyDidChange('runningItem');
      expect(view.get('runningItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('content.upgradeItems', [
        {status: 'IN_PROGRESS'}
      ]);
      view.propertyDidChange('runningItem');
      expect(view.get('runningItem')).to.be.eql({status: 'IN_PROGRESS'});
    });
  });

  describe("#failedItem", function () {
    it("no running item", function () {
      view.set('content.upgradeItems', []);
      view.propertyDidChange('failedItem');
      expect(view.get('failedItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('content.upgradeItems', [Em.Object.create({status: 'FAILED'})]);
      view.propertyDidChange('failedItem');
      expect(view.get('failedItem')).to.be.eql(Em.Object.create({status: 'FAILED'}));
    });
  });

  describe("#manualItem", function () {
    it("no running item", function () {
      view.set('content.upgradeItems', []);
      view.propertyDidChange('manualItem');
      expect(view.get('manualItem')).to.be.undefined;
    });
    it("running item present", function () {
      view.set('content.upgradeItems', [Em.Object.create({status: 'HOLDING'})]);
      view.propertyDidChange('manualItem');
      expect(view.get('manualItem')).to.be.eql(Em.Object.create({status: 'HOLDING'}));
    });
  });

  describe("#isManualOpened", function () {
    it("manual item null", function () {
      view.reopen({manualItem: null});
      view.propertyDidChange('isManualOpened');
      expect(view.get('isManualOpened')).to.be.false;
    });
    it("manual item correct", function () {
      view.reopen({manualItem: {}});
      view.propertyDidChange('isManualOpened');
      expect(view.get('isManualOpened')).to.be.true;
    });
  });

  describe("#showProgressInfo", function () {
    var testCases = [
      {
        data: {
          runningItem: undefined,
          isRunning: false
        },
        result: false
      },
      {
        data: {
          runningItem: undefined,
          isRunning: true
        },
        result: false
      },
      {
        data: {
          runningItem: {},
          isRunning: false
        },
        result: false
      },
      {
        data: {
          runningItem: {},
          isRunning: true
        },
        result: true
      }
    ];
    testCases.forEach(function (test) {
      it('runningItem - ' + test.data.runningItem + ', isRunning - ' + test.data.isRunning, function () {
        view.reopen({
          runningItem: test.data.runningItem
        });
        view.set('content.isRunning', test.data.isRunning);
        view.propertyDidChange('showProgressInfo');
        expect(view.get('showProgressInfo')).to.equal(test.result);
      });
    });
  });

  describe("#isManualProceedDisabled", function () {
    it("", function () {
      view.set('isManualDone', true);
      view.propertyDidChange('isManualProceedDisabled');
      expect(view.get('isManualProceedDisabled')).to.be.false;
    });
  });

  describe("#showFailedInfo", function () {
    var testCases = [
      {
        data: {
          failedItem: undefined,
          status: 'PENDING'
        },
        result: false
      },
      {
        data: {
          failedItem: undefined,
          status: 'FAILED'
        },
        result: false
      },
      {
        data: {
          failedItem: {},
          status: 'PENDING'
        },
        result: false
      },
      {
        data: {
          failedItem: {},
          status: 'FAILED'
        },
        result: true
      }
    ];
    testCases.forEach(function (test) {
      it('failedItem - ' + test.data.failedItem + ', status - ' + test.data.status, function () {
        view.reopen({
          failedItem: test.data.failedItem
        });
        view.set('content.status', test.data.status);
        view.propertyDidChange('showFailedInfo');
        expect(view.get('showFailedInfo')).to.equal(test.result);
      });
    });
  });

  describe("#ignoreAvailable", function () {
    var testCases = [
      {
        data: {
          failedItem: undefined
        },
        result: false
      },
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
        view.propertyDidChange('ignoreAvailable');
        expect(view.get('ignoreAvailable')).to.equal(test.result);
      });
    });
  });

  describe("#setUpgradeItemStatus()", function () {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
    });
    it("", function () {
      view.setUpgradeItemStatus(Em.Object.create({request_id: 1, stage_id: 1, group_id: 1}), 'PENDING');
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'admin.upgrade.upgradeItem.setState',
        sender: view,
        data: {
          upgradeId: 1,
          itemId: 1,
          groupId: 1,
          status: 'PENDING'
        }
      })
    });
  });

  describe("#continue()", function () {
    before(function () {
      sinon.stub(view, 'setUpgradeItemStatus', Em.K);
    });
    after(function () {
      view.setUpgradeItemStatus.restore();
    });
    it("", function () {
      view.continue({context: Em.Object.create({'status': 'HOLDING_FAILED'})});
      expect(view.setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'HOLDING_FAILED'}), 'FAILED')).to.be.true;
    });
  });

  describe("#complete()", function () {
    before(function () {
      sinon.stub(view, 'setUpgradeItemStatus', Em.K);
    });
    after(function () {
      view.setUpgradeItemStatus.restore();
    });
    it("", function () {
      view.complete({context: Em.Object.create({'status': 'FAILED'})});
      expect(view.setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'FAILED'}), 'COMPLETED')).to.be.true;
    });
  });

  describe("#retry()", function () {
    before(function () {
      sinon.stub(view, 'setUpgradeItemStatus', Em.K);
    });
    after(function () {
      view.setUpgradeItemStatus.restore();
    });
    it("", function () {
      view.retry({context: Em.Object.create({'status': 'FAILED'})});
      expect(view.setUpgradeItemStatus.calledWith(Em.Object.create({'status': 'FAILED'}), 'PENDING')).to.be.true;
    });
  });

  describe("#toggleExpanded()", function () {
    before(function () {
      sinon.stub(view, 'collapseLowerLevels', Em.K);
    });
    after(function () {
      view.collapseLowerLevels.restore();
    });
    it("", function () {
      var data = {
        context: Em.Object.create({
          isExpanded: true
        }),
        contexts: [
          [],
          [
            Em.Object.create({
              isExpanded: true
            })
          ]
        ]
      };
      view.toggleExpanded(data);
      expect(view.collapseLowerLevels.calledTwice).to.be.true;
      expect(data.context.get('isExpanded')).to.be.false;
      expect(data.contexts[1][0].get('isExpanded')).to.be.false;
    });
  });

  describe("#collapseLowerLevels()", function () {
    beforeEach(function () {
      sinon.spy(view, 'collapseLowerLevels');
    });
    afterEach(function () {
      view.collapseLowerLevels.restore();
    });
    it("isExpanded false", function () {
      var data = Em.Object.create({
        isExpanded: false
      });
      view.collapseLowerLevels(data);
      expect(view.collapseLowerLevels.calledOnce).to.be.true;
      expect(data.get('isExpanded')).to.be.false;
    });
    it("ITEM expanded", function () {
      var data = Em.Object.create({
        isExpanded: true,
        type: 'ITEM',
        tasks: [
          Em.Object.create({
            isExpanded: true
          })
        ]
      });
      view.collapseLowerLevels(data);
      expect(view.collapseLowerLevels.calledOnce).to.be.true;
      expect(data.get('tasks')[0].get('isExpanded')).to.be.false;
    });
    it("GROUP expanded", function () {
      var data = Em.Object.create({
        isExpanded: true,
        type: 'GROUP',
        upgradeItems: [
          Em.Object.create({
            isExpanded: true
          })
        ]
      });
      view.collapseLowerLevels(data);
      expect(view.collapseLowerLevels.calledTwice).to.be.true;
      expect(data.get('upgradeItems')[0].get('isExpanded')).to.be.false;
    });
  });

});