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
require('/views/main/service/info/summary/hdfs/widgets');
var date = require('utils/date/date');

function getView(options) {
  return App.HDFSSummaryWidgetsView.create(options || {});
}

describe('App.HDFSSummaryWidgetsView', function () {
  var view;

  beforeEach(function () {
    view = getView({
      services: Em.Object.create(),
      model: Em.Object.create(),
      hostName: 'host1'
    });
  });

  describe('#nodeUptime', function () {

    beforeEach(function () {
      this.mock = sinon.stub(App, 'dateTime');
    });
    afterEach(function () {
      this.mock.restore();
    });

    it('node not started', function () {
      view.set('model.nameNodeStartTimeValues', Em.Object.create());
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.equal(Em.I18n.t('services.service.summary.notRunning'));
    });

    it('node started', function () {
      this.mock.returns(1590575011209);
      view.reopen({
        model: Em.Object.create({
          nameNodeStartTimeValues: {
            host1: 1590576011209
          }
        })
      });
      view.propertyDidChange('nodeUptime');
      expect(view.get('nodeUptime')).to.equal(Em.I18n.t('dashboard.services.uptime').format(date.timingFormat(0)));
    });
  });

  describe('#upgradeStatus', function () {

    it('upgrade status == null', function () {
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('upgrade status true', function () {
      view.reopen({
        model: Em.Object.create({
          upgradeStatusValues: {
            host1: true
          }
        })
      });
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending'));
    });

    it('upgrade status false and healthStatus === "green"', function () {
      view.reopen({
        model: Em.Object.create({
          upgradeStatusValues: {
            host1: false
          },
          healthStatusValues: {
            host1: 'green'
          }
        })
      });
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.equal(Em.I18n.t('services.service.summary.pendingUpgradeStatus.notFinalized'));
    });

    it('upgrade status false and healthStatus is not "green"', function () {
      view.reopen({
        model: Em.Object.create({
          upgradeStatusValues: {
            host1: false
          },
          healthStatusValues: {
            host1: 'red'
          }
        })
      });
      view.propertyDidChange('upgradeStatus');
      expect(view.get('upgradeStatus')).to.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });
  });

  describe('#isUpgradeStatusWarning', function () {

    it('upgrade status false and healthStatus is not "green"', function () {
      view.reopen({
        model: Em.Object.create({
          upgradeStatusValues: {
            host1: false
          },
          healthStatusValues: {
            host1: 'red'
          }
        })
      });
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.false;
    });

    it('upgrade status false and healthStatus is "green"', function () {
      view.reopen({
        model: Em.Object.create({
          upgradeStatusValues: {
            host1: false
          },
          healthStatusValues: {
            host1: 'green'
          }
        })
      });
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.true;
    });

    it('upgrade status is true', function () {
      view.reopen({
        model: Em.Object.create({
          upgradeStatusValues: {
            host1: true
          },
          healthStatusValues: {
            host1: 'green'
          }
        })
      });
      view.propertyDidChange('isUpgradeStatusWarning');
      expect(view.get('isUpgradeStatusWarning')).to.be.false;
    });
  });

  describe('#safeModeStatus', function () {

    it('safe mode is active', function () {
      view.reopen({
        model: Em.Object.create({
          safeModeStatusValues: {
            host1: true
          }
        })
      });
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.equal(Em.I18n.t('services.service.summary.safeModeStatus.inSafeMode'));
    });

    it('safe mode is not available', function () {
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('safe mode is not active', function () {
      view.reopen({
        model: Em.Object.create({
          safeModeStatusValues: {
            host1: ''
          }
        })
      });
      view.propertyDidChange('safeModeStatus');
      expect(view.get('safeModeStatus')).to.equal(Em.I18n.t('services.service.summary.safeModeStatus.notInSafeMode'));
    });
  });

});
