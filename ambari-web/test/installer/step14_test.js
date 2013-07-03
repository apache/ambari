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
require('utils/config');
require('controllers/wizard/step14_controller');

describe('App.WizardStep14Controller', function() {
  var tasks = [
    Em.Object.create({status:''}),
    Em.Object.create({status:''}),
    Em.Object.create({status:''}),
    Em.Object.create({status:''}),
    Em.Object.create({status:''}),
    Em.Object.create({status:''}),
    Em.Object.create({status:''}),
    Em.Object.create({status:''})
  ];

  var tests = [
    {
      m: 'onStopServiceBeforeSend',
      t: 0,
      s: 'PENDING'
    },
    {
      m: 'onStopServiceError',
      t: 0,
      s: 'FAILED'
    },
    {
      m: 'onCreateMasterComponentBeforeSend',
      t: 1,
      s: 'PENDING'
    },
    {
      m: 'onCreateMasterComponentSuccess',
      t: 1,
      s: 'COMPLETED'
    },
    {
      m: 'onCreateMasterComponentError',
      t: 1,
      s: 'FAILED'
    },
    {
      m: 'onCreateConfigsError',
      t: 2,
      s: 'FAILED'
    },
    {
      m: 'onCheckConfigsError',
      t: 3,
      s: 'FAILED'
    },
    {
      m: 'onApplyConfigsSuccess',
      t: 3,
      s: 'COMPLETED'
    },
    {
      m: 'onApplyConfigsError',
      t: 3,
      s: 'FAILED'
    },
    {
      m: 'onPutInMaintenanceModeBeforeSend',
      t: 4,
      s: 'PENDING'
    },
    {
      m: 'onPutInMaintenanceModeSuccess',
      t: 4,
      s: 'COMPLETED'
    },
    {
      m: 'onPutInMaintenanceModeError',
      t: 4,
      s: 'FAILED'
    },
    {
      m: 'onInstallComponentBeforeSend',
      t: 5,
      s: 'PENDING'
    },
    {
      m: 'onInstallComponentError',
      t: 5,
      s: 'FAILED'
    },
    {
      m: 'onStartComponentsBeforeSend',
      t: 6,
      s: 'PENDING'
    },
    {
      m: 'onStartComponentsError',
      t: 6,
      s: 'FAILED'
    },
    {
      m: 'onRemoveComponentBeforeSend',
      t: 7,
      s: 'PENDING'
    },
    {
      m: 'onRemoveComponentSuccess',
      t: 7,
      s: 'COMPLETED'
    },
    {
      m: 'onRemoveComponentError',
      t: 7,
      s: 'FAILED'
    }
  ];

  tests.forEach(function(test) {
    describe('#' + test.m, function() {
      it('Task #'+test.t+' should be '+test.s, function() {
        var wizardStep14Controller = App.WizardStep14Controller.create();
        wizardStep14Controller.set('tasks', tasks);
        wizardStep14Controller[test.m]();
        expect(wizardStep14Controller.get('tasks')[test.t].get('status')).to.equal(test.s);
      });
    });
  });

  describe('#onGetLogsByRequestError', function() {
    it('', function() {
      var wizardStep14Controller = App.WizardStep14Controller.create();
      wizardStep14Controller.onGetLogsByRequestError();
      expect(wizardStep14Controller.get('status')).to.equal('FAILED');
    });
  });

});
