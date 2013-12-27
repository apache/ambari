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
          obj: {
            status: 'failed',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'success',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'success',
            progress: 99
          },
          e: false
        }
      ]
    },
    {
      p: 'isSuccess',
      tests: [
        {
          obj: {
            status: 'success',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'success',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        }
      ]
    },
    {
      p: 'isWarning',
      tests: [
        {
          obj: {
            status: 'warning',
            progress: 100
          },
          e: true
        },
        {
          obj: {
            status: 'warning',
            progress: 99
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 100
          },
          e: false
        },
        {
          obj: {
            status: 'failed',
            progress: 99
          },
          e: false
        }
      ]
    }
  ];
  tests.forEach(function(test) {
    describe(test.p, function() {
      test.tests.forEach(function(t) {
        var hostStatusView = App.HostStatusView.create();
        it('obj.progress = ' + t.obj.progress + '; obj.status = ' + t.obj.status, function() {
          hostStatusView.set('obj', t.obj);
          expect(hostStatusView.get(test.p)).to.equal(t.e);
        });
      });
    });
  });
});
