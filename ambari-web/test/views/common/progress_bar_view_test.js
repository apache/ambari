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

describe('App.ProgressBarView', function () {
  var view = App.ProgressBarView.create();

  describe("#progressWidth", function () {
    it("", function () {
      view.set('progress', 1);
      view.propertyDidChange('progressWidth');
      expect(view.get('progressWidth')).to.equal('width:1%;');
    });
  });

  describe("#barClass", function () {
    var testCases = [
      {
        status: 'FAILED',
        result: 'progress-danger'
      },
      {
        status: 'ABORTED',
        result: 'progress-warning'
      },
      {
        status: 'TIMED_OUT',
        result: 'progress-warning'
      },
      {
        status: 'COMPLETED',
        result: 'progress-success'
      },
      {
        status: 'QUEUED',
        result: 'progress-info active progress-striped'
      },
      {
        status: 'PENDING',
        result: 'progress-info active progress-striped'
      },
      {
        status: 'IN_PROGRESS',
        result: 'progress-info active progress-striped'
      },
      {
        status: null,
        result: 'progress-info'
      }
    ];
    testCases.forEach(function (test) {
      it("status is " + test.status, function () {
        view.set('status', test.status);
        view.propertyDidChange('barClass');
        expect(view.get('barClass')).to.equal(test.result);
      });
    });
  });
});