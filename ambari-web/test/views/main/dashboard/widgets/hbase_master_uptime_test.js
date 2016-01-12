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

require('messages');
require('views/main/dashboard/widgets/hbase_master_uptime');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widget');

describe('App.HBaseMasterUptimeView', function () {

  var tests = [
    {
      model: Em.Object.create({
        masterStartTime: new Date().getTime() - 192.1 * 24 * 3600 * 1000
      }),
      e: {
        isGreen: true,
        isNA: false,
        content: '192.1 d',
        data: 192.1
      }
    },
    {
      model: Em.Object.create({
        masterStartTime: 0
      }),
      e: {
        isGreen: false,
        isNA: true,
        content: Em.I18n.t('services.service.summary.notAvailable'),
        data: null
      }
    },
    {
      model: Em.Object.create({
        masterStartTime: null
      }),
      e: {
        isGreen: false,
        isNA: true,
        content: Em.I18n.t('services.service.summary.notAvailable'),
        data: null
      }
    }
  ];

  beforeEach(function () {
    sinon.stub(App.router, 'get').withArgs('userSettingsController.userSettings.timezone').returns('');
  });

  afterEach(function () {
    App.router.get.restore();
  });

  tests.forEach(function (test) {
    var hBaseMasterUptimeView = App.HBaseMasterUptimeView.create({model_type: null, model: test.model});
    hBaseMasterUptimeView.calc();
    describe('#masterStartTime - ' + test.model.masterStartTime, function () {
      it('content', function () {
        expect(hBaseMasterUptimeView.get('content')).to.equal(test.e.content);
      });
      it('data', function () {
        expect(hBaseMasterUptimeView.get('data')).to.equal(test.e.data);
      });
      it('isGreen', function () {
        expect(hBaseMasterUptimeView.get('isGreen')).to.equal(test.e.isGreen);
      });
      it('isNA', function () {
        expect(hBaseMasterUptimeView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

});
