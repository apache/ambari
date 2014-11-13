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

require('models/alert_definition');

var model;

describe('App.AlertDefinition', function() {

  beforeEach(function() {

    model = App.AlertDefinition.createRecord();

  });

  describe('#status', function() {

    Em.A([
      {
        summary: {OK: 1, UNKNOWN: 1, WARNING: 2},
        m: 'No CRITICAL',
        e: '1 <span class="icon-ok-sign alert-state-OK"></span> / ' +
          '2 <span class="icon-warning-sign alert-state-WARNING"></span> / ' +
          '1 <span class="icon-question-sign alert-state-UNKNOWN"></span>'
      },
      {
        summary: {WARNING: 2, CRITICAL: 3, UNKNOWN: 1, OK: 1},
        m: 'All states exists',
        e: '1 <span class="icon-ok-sign alert-state-OK"></span> / ' +
          '2 <span class="icon-warning-sign alert-state-WARNING"></span> / ' +
          '3 <span class="icon-remove alert-state-CRITICAL"></span> / ' +
          '1 <span class="icon-question-sign alert-state-UNKNOWN"></span>'
      }
    ]).forEach(function(test) {
        it(test.m, function() {
          model.set('summary', test.summary);
          expect(model.get('status')).to.equal(test.e);
        });
      });

  });

});
