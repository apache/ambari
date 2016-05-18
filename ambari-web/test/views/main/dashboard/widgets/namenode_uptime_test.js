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
require('views/main/dashboard/widgets/namenode_uptime');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widget');

describe('App.NameNodeUptimeView', function() {

  var tests = [
    {
      model: Em.Object.create({
        nameNodeStartTime: new Date().getTime() - 192.1*24*3600*1000
      }),
      e: {
        isRed: false,
        isOrange: false,
        isGreen: true,
        isNA: false,
        content: '192.1 d',
        data: 192.1
      }
    },
    {
      model:  Em.Object.create({
        nameNodeStartTime: 0
      }),
      e: {
        isRed: false,
        isOrange: false,
        isGreen: false,
        isNA: true,
        content: Em.I18n.t('services.service.summary.notAvailable'),
        data: null
      }
    },
    {
      model:  Em.Object.create({
        nameNodeStartTime: null
      }),
      e: {
        isRed: false,
        isOrange: false,
        isGreen: false,
        isNA: true,
        content: Em.I18n.t('services.service.summary.notAvailable'),
        data: null
      }
    }
  ];

  tests.forEach(function(test) {
    var nameNodeUptimeView = App.NameNodeUptimeView.create({model_type:null, model: test.model});
    nameNodeUptimeView.calc();
    describe('nameNodeStartTime - ' + test.model.nameNodeStartTime, function() {
      it('content', function() {
        expect(nameNodeUptimeView.get('content')).to.equal(test.e.content);
      });
      it('data', function() {
        expect(nameNodeUptimeView.get('data')).to.equal(test.e.data);
      });
      it('isRed', function() {
        expect(nameNodeUptimeView.get('isRed')).to.equal(test.e.isRed);
      });
      it('isOrange', function() {
        expect(nameNodeUptimeView.get('isOrange')).to.equal(test.e.isOrange);
      });
      it('isGreen', function() {
        expect(nameNodeUptimeView.get('isGreen')).to.equal(test.e.isGreen);
      });
      it('isNA', function() {
        expect(nameNodeUptimeView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

});
