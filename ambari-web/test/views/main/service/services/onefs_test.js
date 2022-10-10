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
require('/views/main/service/services/onefs');

var view;

function testDiskPart(propertyName, i18nKey, totalKey, usedKey) {
  describe('#' + propertyName, function () {

    beforeEach(function () {
      view.reopen({
        service: Em.Object.create()
      });
    });

    it('n/a / n/a', function () {
      expect(view.get(propertyName)).to.equal('n/a / n/a');
    });

    it('"value / n/a" and "n/a / n/a" for usedKey === "nonDfsUsed"', function () {
      if (usedKey === 'nonDfsUsed') {
        expect(view.get(propertyName)).to.equal('n/a / n/a');
      } else {
        view.set(usedKey, 8);
        expect(view.get(propertyName)).to.equal('8.0 Bytes / n/a');
      }
    });

    it('n/a / value', function () {
      view.set(totalKey, 20);
      expect(view.get(propertyName)).to.equal('n/a / 20.0 Bytes');
    });

    it('value / value', function () {
      view.set(totalKey, 20);
      if (usedKey === 'nonDfsUsed') {
        view.set('service.capacityRemaining', 2);
        view.set('service.capacityUsed', 8);
        expect(view.get(propertyName)).to.equal('10.0 Bytes / 20.0 Bytes');
      } else {
        view.set(usedKey, 8);
        expect(view.get(propertyName)).to.equal('8.0 Bytes / 20.0 Bytes');
      }
    });
  });
}

function testDiskPartPercent(propertyName, i18nKey, totalKey, usedKey) {
  describe('#' + propertyName, function () {

    beforeEach(function () {
      view.reopen({
        service: Em.Object.create()
      });
    });

    it('n/a and 0.00% for usedKey === "nonDfsUsed"', function () {
      view.set(totalKey, 20);
      if (usedKey === 'nonDfsUsed') {
        expect(view.get(propertyName)).to.equal('0.00%');
      } else {
        expect(view.get(propertyName)).to.equal('n/a %');
      }
    });

    it('0 %', function () {
      expect(view.get(propertyName)).to.equal('0%');
    });

    it('should return percent', function () {
      view.set(totalKey, 20);
      if (usedKey === 'nonDfsUsed') {
        view.set('service.capacityRemaining', 2);
        view.set('service.capacityUsed', 8);
        expect(view.get(propertyName)).to.equal('50.00%');
      } else {
        view.set(usedKey, 8);
        expect(view.get(propertyName)).to.equal('40.00%');
      }
    });
  });
}

function getView(options) {
  return App.MainDashboardServiceOnefsView.create(options || {});
}

describe('App.MainDashboardServiceOnefsView', function () {

  beforeEach(function () {
    view = getView({
      service: Em.Object.create()
    });
  });

  testDiskPart('dfsUsedDisk', 'dashboard.services.hdfs.capacityUsed', 'service.capacityTotal', 'service.capacityUsed');
  testDiskPart('nonDfsUsedDisk', 'dashboard.services.hdfs.capacityUsed', 'service.capacityTotal', 'nonDfsUsed');
  testDiskPart('remainingDisk', 'dashboard.services.hdfs.capacityUsed', 'service.capacityTotal', 'service.capacityRemaining');

  testDiskPartPercent(
    'dfsUsedDiskPercent', 'dashboard.services.hdfs.capacityUsedPercent', 'service.capacityTotal', 'service.capacityUsed');
  testDiskPartPercent(
    'nonDfsUsedDiskPercent', 'dashboard.services.hdfs.capacityUsedPercent', 'service.capacityTotal', 'nonDfsUsed');
  testDiskPartPercent(
    'remainingDiskPercent', 'dashboard.services.hdfs.capacityUsedPercent', 'service.capacityTotal', 'service.capacityRemaining');
  
  describe('#nonDfsUsed', function () {

    it('should return null', function () {
      view.set('service.capacityTotal', 20);
      view.set('service.capacityRemaining', 2);
      view.propertyDidChange('nonDfsUsed');
      expect(view.get('nonDfsUsed')).to.equal(null);
    });

    it('should return number of nonDfsUsed capacity', function () {
      view.set('service.capacityTotal', 20);
      view.set('service.capacityRemaining', 2);
      view.set('service.capacityUsed', 8);
      view.propertyDidChange('nonDfsUsed');
      expect(view.get('nonDfsUsed')).to.equal(10);
    });
  });

  describe('#isJournalNodeCreated', function () {

    beforeEach(function () {
      sinon.stub(view, 'isServiceComponentCreated').withArgs('JOURNALNODE').returns(true);
    });

    afterEach(function () {
      view.isServiceComponentCreated.restore();
    });

    it('should execute isServiceComponentCreated function', function () {
      view.propertyDidChange('isJournalNodeCreated');
      expect(view.get('isJournalNodeCreated')).to.be.true;
    });
  });

  describe('#isDataNodeCreated', function () {

    beforeEach(function () {
      sinon.stub(view, 'isServiceComponentCreated').withArgs('DATANODE').returns(true);
    });

    it('should execute isServiceComponentCreated function', function () {
      view.propertyDidChange('isDataNodeCreated');
      expect(view.get('isDataNodeCreated')).to.be.true;
    });
  });

  describe('#isUpgradeStatusWarning', function () {

    it('should return true', function () {
      view.set('service.upgradeStatus', 'false');
      view.set('service.healthStatus', 'green');
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.true;
    });

    it('should return false{1}', function () {
      view.set('service.upgradeStatus', 'true');
      view.set('service.healthStatus', 'green');
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.false;
    });

    it('should return false{2}', function () {
      view.set('service.upgradeStatus', 'true');
      view.set('service.healthStatus', 'red');
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.false;
    });

    it('should return false{3}', function () {
      view.set('service.upgradeStatus', 'false');
      view.set('service.healthStatus', 'red');
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.false;
    });
  });

  describe('#upgradeStatus', function () {

    it('should return notFinalized', function () {
      view.set('service.upgradeStatus', 'false');
      view.set('service.healthStatus', 'green');
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notFinalized'));
    });

    it('should return notPending', function () {
      view.set('service.upgradeStatus', 'true');
      view.set('service.healthStatus', 'green');
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending'));
    });

    it('should return notAvailable', function () {
      view.set('service.upgradeStatus', 'false');
      view.set('service.healthStatus', 'red');
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.equal(Em.I18n.t("services.service.summary.notAvailable"));
    });
  });

  describe('#safeModeStatus', function () {

    it('should return n/a', function () {
      view.set('service.safeModeStatus', null);
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('should return notInSafeMode', function () {
      view.set('service.safeModeStatus', []);
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.equal(Em.I18n.t('services.service.summary.safeModeStatus.notInSafeMode'));
    });

    it('should return inSafeMode', function () {
      view.set('service.safeModeStatus', ['enabled']);
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.equal(Em.I18n.t('services.service.summary.safeModeStatus.inSafeMode'));
    });
  });

  describe('#isNfsInStack', function () {

    beforeEach(function () {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
    });

    afterEach(function () {
      this.mock.restore();
    });

    it('should return true', function () {
      this.mock.returns([{componentName: 'NFS_GATEWAY'}]);
      view.propertyDidChange('isNfsInStack');
      expect(view.get('isNfsInStack')).to.be.true;
    });

    it('should return true', function () {
      this.mock.returns([{componentName: 'NAMENODE'}]);
      view.propertyDidChange('isNfsInStack');
      expect(view.get('isNfsInStack')).to.be.false;
    });
  });

  describe('#nodeUptime', function () {

    beforeEach(function () {
      sinon.stub(App, 'dateTime').returns(1591168735997);
    });

    afterEach(function () {
      App.dateTime.restore();
    });

    it('uptime is not defined', function () {
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.equal(Em.I18n.t('services.service.summary.notRunning'));
    });

    it('uptime >= current time', function () {
      view.set('service.nameNodeStartTime', 1591178735997);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.equal(Em.I18n.t('dashboard.services.uptime').format(date.timingFormat(0)));
    });

    it('uptime < current time', function () {
      view.set('service.nameNodeStartTime', 1591158735997);
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.equal(Em.I18n.t('dashboard.services.uptime').format(date.timingFormat(10000000)));
    });
  });

  describe('#journalNodesLive', function () {

    it('should return live journal nodes amount', function () {
      view.set('service.journalNodes', [
        {workStatus: "STARTED"},
        {workStatus: "STARTED"}
      ]);
      view.propertyDidChange('journalNodesLive');
      expect(view.get('journalNodesLive')).to.equal(2);
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

    it("App.tooltip should not be called", function() {
      view.set("service", Em.Object.create({
        metricsNotAvailable: true
      }));
      expect(App.tooltip.calledOnce).to.be.false;
    });
  });

  describe("#Chart", function () {
    var chartView;

    beforeEach(function () {
      chartView = view.get('Chart').create({
        service: Em.Object.create()
      });
    });

    describe("#data()", function () {

      it("should return array of numbers", function () {
        chartView.set('service.capacityRemaining', 7);
        chartView.set('service.capacityTotal', 10);
        chartView.propertyDidChange('data');
        expect(chartView.get('data')).to.eql([3,7]);
      });
    });
  });

});
