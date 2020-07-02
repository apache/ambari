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
require('/views/main/service/services/hbase');

var view;

function testGetMasterTime(propertyName, key) {
  describe('#' + propertyName, function () {

    beforeEach(function () {
      view.reopen({
        service: Em.Object.create()
      });
      sinon.stub(App, 'dateTime').returns(1591168735997);
    });

    afterEach(function () {
      App.dateTime.restore();
    });

    it('uptime is not defined', function () {
      view.propertyDidChange(propertyName);
      expect(view.get(propertyName)).to.equal(Em.I18n.t('services.service.summary.notRunning'));
    });

    it('uptime >= current time', function () {
      view.set('service.' + key, 1591178735997);
      view.propertyDidChange(propertyName);
      expect(view.get(propertyName)).to.equal(Em.I18n.t('dashboard.services.uptime').format(date.timingFormat(0)));
    });

    it('uptime < current time', function () {
      view.set('service.' + key, 1591158735997);
      view.propertyDidChange(propertyName);
      expect(view.get(propertyName)).to.equal(Em.I18n.t('dashboard.services.uptime').format(date.timingFormat(10000000)));
    });
  });
}

function getView() {
  return App.MainDashboardServiceHbaseView.create();
}

describe('App.MainDashboardServiceHbaseView', function () {

  beforeEach(function () {
    view = getView();
  });

  testGetMasterTime('masterStartedTime', 'masterStartTime');
  testGetMasterTime('masterActivatedTime', 'masterActiveTime');

  App.TestAliases.testAsComputedFilterBy(getView(), 'masters', 'service.hostComponents', 'isMaster', true);

  App.TestAliases.testAsComputedFilterBy(getView(), 'passiveMasters', 'masters', 'haStatus', 'false');

  App.TestAliases.testAsComputedFindBy(getView(), 'activeMaster', 'masters', 'haStatus', 'true');

  App.TestAliases.testAsComputedCountBasedMessage(getView(), 'regionServesText', 'service.regionServersTotal', '', Em.I18n.t('services.service.summary.viewHost'), Em.I18n.t('services.service.summary.viewHosts'));

  App.TestAliases.testAsComputedCountBasedMessage(getView(), 'phoenixServersText', 'service.phoenixServersTotal', '', Em.I18n.t('services.service.summary.viewHost'), Em.I18n.t('services.service.summary.viewHosts'));

  describe('#averageLoad', function () {

    beforeEach(function () {
      view.reopen({service: Em.Object.create()});
    });

    Em.A([
      {
        averageLoad: NaN,
        e: Em.I18n.t('dashboard.services.hbase.averageLoadPerServer').format(Em.I18n.t('services.service.summary.notAvailable'))
      },
      {
        averageLoad: 12,
        e: Em.I18n.t('dashboard.services.hbase.averageLoadPerServer').format(12)
      }
    ]).forEach(function (test, index) {
      it('test# ' + (index + 1), function () {
        view.set('service.averageLoad', test.averageLoad);
        expect(view.get('averageLoad')).to.be.equal(test.e);
      });
    });

  });

  describe("#summaryHeader", function() {

    it("averageLoad is NaN", function() {
      view.set('service', Em.Object.create({
        averageLoad: 'null',
        regionServersTotal: 1
      }));
      view.propertyDidChange('summaryHeader');
      expect(view.get('summaryHeader')).to.be.equal(view.t("dashboard.services.hbase.summary").format(1, view.t("services.service.summary.unknown")));
    });

    it("averageLoad is number", function() {
      view.set('service', Em.Object.create({
        averageLoad: 99,
        regionServersTotal: 1
      }));
      view.propertyDidChange('summaryHeader');
      expect(view.get('summaryHeader')).to.be.equal(view.t("dashboard.services.hbase.summary").format(1, 99));
    });
  });

  describe('#isPhoenixQueryServerCreated', function () {

    beforeEach(function () {
      sinon.stub(view, 'isServiceComponentCreated').withArgs('PHOENIX_QUERY_SERVER').returns(true);
    });

    afterEach(function () {
      view.isServiceComponentCreated.restore();
    });

    it('should return true', function () {
      view.propertyDidChange('isPhoenixQueryServerCreated');
      expect(view.get('isPhoenixQueryServerCreated')).to.be.true;
    });
  });

  describe('#isRegionServerCreated', function () {

    beforeEach(function () {
      sinon.stub(view, 'isServiceComponentCreated').withArgs('HBASE_REGIONSERVER').returns(true);
    });

    afterEach(function () {
      view.isServiceComponentCreated.restore();
    });

    it('should return true', function () {
      view.propertyDidChange('isRegionServerCreated');
      expect(view.get('isRegionServerCreated')).to.be.true;
    });
  });

});