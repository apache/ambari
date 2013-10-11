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
require('views/wizard/step9_view');

describe('App.HostStatusView', function () {
  var tests = [
    {
      p: 'isFailed',
      tests: [
        {
          controller: {isStepCompleted: true},
          obj: {status: 'failed'},
          e: true
        },
        {
          controller: {isStepCompleted: false},
          obj: {status: 'failed'},
          e: false
        },
        {
          controller: {isStepCompleted: true},
          obj: {status: 'success'},
          e: false
        },
        {
          controller: {isStepCompleted: false},
          obj: {status: 'success'},
          e: false
        }
      ]
    },
    {
      p: 'isSuccess',
      tests: [
        {
          controller: {isStepCompleted: true},
          obj: {status: 'success'},
          e: true
        },
        {
          controller: {isStepCompleted: false},
          obj: {status: 'success'},
          e: false
        },
        {
          controller: {isStepCompleted: true},
          obj: {status: 'failed'},
          e: false
        },
        {
          controller: {isStepCompleted: false},
          obj: {status: 'failed'},
          e: false
        }
      ]
    },
    {
      p: 'isWarning',
      tests: [
        {
          controller: {isStepCompleted: true},
          obj: {status: 'warning'},
          e: true
        },
        {
          controller: {isStepCompleted: false},
          obj: {status: 'warning'},
          e: false
        },
        {
          controller: {isStepCompleted: true},
          obj: {status: 'failed'},
          e: false
        },
        {
          controller: {isStepCompleted: false},
          obj: {status: 'failed'},
          e: false
        }
      ]
    }
  ];
  tests.forEach(function(test) {
    describe(test.p, function() {
      test.tests.forEach(function(t) {
        var hostStatusView = App.HostStatusView.create();
        it('controller.isStepCompleted = ' + t.controller.isStepCompleted + '; obj.status = ' + t.obj.status, function() {
          hostStatusView.set('controller', t.controller);
          hostStatusView.set('obj', t.obj);
          expect(hostStatusView.get(test.p)).to.equal(t.e);
        });
      });
    });
  });
});
