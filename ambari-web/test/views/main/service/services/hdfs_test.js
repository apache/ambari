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
var date = require('utils/date/date');
require('/views/main/service/services/hdfs');

function getView(options) {
  return App.MainDashboardServiceHdfsView.create(options || {});
}

describe('App.MainDashboardServiceHdfsView', function () {
  var view;

  beforeEach(function() {
    view = getView({service: Em.Object.create()});
  });

  App.TestAliases.testAsComputedAlias(getView(), 'dataNodesDead', 'service.dataNodesInstalled', 'boolean');

  App.TestAliases.testAsComputedAlias(getView(), 'journalNodesTotal', 'service.journalNodes.length', 'number');

  describe("#Chart", function() {
    var chartView;

    beforeEach(function() {
      chartView = view.get('Chart').create();
    });

    describe("#data", function () {

      it("should return data", function () {
        chartView.set('service', Em.Object.create({
          capacityTotal: 100,
          capacityRemaining: 1
        }));
        chartView.propertyDidChange('data');
        expect(chartView.get('data')).to.be.eql([99, 1]);
      });
    });
  });

  describe("#dashboardMasterComponentView", function() {
    var dashboardMasterComponentView;

    beforeEach(function() {
      dashboardMasterComponentView = view.get('dashboardMasterComponentView').create({
        parentView: Em.Object.create({
          service: Em.Object.create({
            hostComponents: []
          })
        })
      });
    });

    describe("#mastersComp", function () {

      it("should return master components", function () {
        dashboardMasterComponentView.set('parentView.service', Em.Object.create({
          hostComponents: [
            Em.Object.create({
              componentName: 'ZKFC'
            }),
            Em.Object.create({
              componentName: 'JOURNALNODE'
            }),
            Em.Object.create({
              componentName: 'NAMENODE',
              isMaster: true
            })
          ]
        }));
        dashboardMasterComponentView.propertyDidChange('mastersComp');
        expect(dashboardMasterComponentView.get('mastersComp').mapProperty('componentName')).to.be.eql(['NAMENODE', 'ZKFC']);
        expect(dashboardMasterComponentView.get('mastersComp')[0].get('isMaster')).to.be.true;
        expect(dashboardMasterComponentView.get('mastersComp')[1].get('isSubComponent')).to.be.true;
      });
    });

  });

  describe("#metricsNotAvailableObserver()", function() {

    beforeEach(function() {
      sinon.stub(App, 'tooltip');
    });
    afterEach(function() {
      App.tooltip.restore();
    });

    it("App.tooltip should be called", function() {
      view.set("service", Em.Object.create({
        metricsNotAvailable: false
      }));
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function() {
    var mock = {
      tooltip: Em.K
    };

    beforeEach(function() {
      sinon.stub(mock, 'tooltip');
      sinon.stub(window, '$').returns(mock);
    });
    afterEach(function() {
      mock.tooltip.restore();
      window.$.restore();
    });

    it("tooltip destroy should be called", function() {
      view.willDestroyElement();
      expect(mock.tooltip.calledWith('destroy')).to.be.true;
    });
  });

  describe("#journalNodesLive", function() {

    it("should return live journal nodes count", function() {
      view.set('service', Em.Object.create({
        journalNodes: [
          Em.Object.create({workStatus: 'STARTED'}),
          Em.Object.create()
        ]
      }));
      view.propertyDidChange('journalNodesLive');
      expect(view.get('journalNodesLive')).to.be.equal(1);
    });
  });

  describe("#nodeUptime", function() {

    beforeEach(function() {
      sinon.stub(App, 'dateTime').returns(10);
      sinon.stub(date, 'timingFormat').returns('11');
    });
    afterEach(function() {
      App.dateTime.restore();
      date.timingFormat.restore();
    });

    it("nameNodeStartTime is 0", function() {
      view.set('service.nameNodeStartTime', 0);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.be.equal(view.t('services.service.summary.notRunning'));
    });

    it("nameNodeStartTime is -1", function() {
      view.set('service.nameNodeStartTime', -1);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.be.equal(view.t('services.service.summary.notRunning'));
    });

    it("nameNodeStartTime is 1", function() {
      view.set('service.nameNodeStartTime', 1);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.be.equal(view.t('dashboard.services.uptime').format('11'));
      expect(date.timingFormat.calledWith(9)).to.be.true;
    });

    it("nameNodeStartTime is 11", function() {
      view.set('service.nameNodeStartTime', 11);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.be.equal(view.t('dashboard.services.uptime').format('11'));
      expect(date.timingFormat.calledWith(0)).to.be.true;
    });
  });

  describe("#nonDfsUsed", function() {
    var testCases = [
      {
        input: {
          capacityTotal: null,
          capacityRemaining: 1,
          capacityUsed: 90
        },
        expected: null
      },
      {
        input: {
          capacityTotal: 100,
          capacityRemaining: null,
          capacityUsed: 90
        },
        expected: null
      },
      {
        input: {
          capacityTotal: 100,
          capacityRemaining: 1,
          capacityUsed: null
        },
        expected: null
      },
      {
        input: {
          capacityTotal: 100,
          capacityRemaining: 1,
          capacityUsed: 90
        },
        expected: 9
      }
    ];

    testCases.forEach(function(test) {
      it("total=" + test.input.capacityTotal + " remaining" + test.input.capacityRemaining + " used" + test.input.capacityUsed, function() {
        view.get('service').setProperties(test.input);
        view.propertyDidChange('nonDfsUsed');
        expect(view.get('nonDfsUsed')).to.be.equal(test.expected);
      });
    });
  });

  describe("#isNfsInStack", function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
    });
    afterEach(function() {
      this.mock.restore();
    });

    it("no NFS_GATEWAY component", function() {
      this.mock.returns([]);
      view.propertyDidChange('isNfsInStack');
      expect(view.get('isNfsInStack')).to.be.false;
    });

    it("NFS_GATEWAY component present", function() {
      this.mock.returns([{componentName: 'NFS_GATEWAY'}]);
      view.propertyDidChange('isNfsInStack');
      expect(view.get('isNfsInStack')).to.be.true;
    });
  });

  describe("#safeModeStatus", function() {

    it("safeModeStatus is null", function() {
      view.set('service.safeModeStatus', null);
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t("services.service.summary.notAvailable"));
    });

    it("safeModeStatus is empty", function() {
      view.set('service.safeModeStatus', "");
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t("services.service.summary.safeModeStatus.notInSafeMode"));
    });

    it("safeModeStatus is on", function() {
      view.set('service.safeModeStatus', 'on');
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.be.equal(Em.I18n.t("services.service.summary.safeModeStatus.inSafeMode"));
    });
  });

  describe("#upgradeStatus", function() {

    it("upgradeStatus is 'true'", function() {
      view.set('service.upgradeStatus', 'true');
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending'));
    });

    it("upgradeStatus is 'false', healthStatus is 'green'", function() {
      view.set('service.upgradeStatus', 'false');
      view.set('service.healthStatus', 'green');
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notFinalized'));
    });

    it("upgradeStatus is null", function() {
      view.set('service.upgradeStatus', null);
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });
  });

  describe("#isUpgradeStatusWarning", function() {

    it("upgradeStatus is 'false', healthStatus is 'green'", function() {
      view.set('service.upgradeStatus', 'false');
      view.set('service.healthStatus', 'green');
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.true;
    });

    it("upgradeStatus is 'true', healthStatus is 'green'", function() {
      view.set('service.upgradeStatus', 'true');
      view.set('service.healthStatus', 'green');
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.false;
    });

    it("upgradeStatus is 'false', healthStatus is 'red'", function() {
      view.set('service.upgradeStatus', 'false');
      view.set('service.healthStatus', 'red');
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.false;
    });
  });


});
