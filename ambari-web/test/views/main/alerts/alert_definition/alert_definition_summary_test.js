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
require('views/main/alerts/alert_definition/alert_definition_summary');

describe('App.AlertDefinitionSummary', function () {
  var view = App.AlertDefinitionSummary.create({
    content: Em.Object.create({}),
  });

  describe("#definitionState", function () {

    it("return definition state without content", function () {
      view.set('content', null);
      expect(view.get('definitionState')).to.eql([]);
    });

    it("return definition state", function () {
      view.set('content', Em.Object.create({
        summary: {
          OK: {
            count: 1,
            maintenanceCount: 1
          },
          WARNING: {
            count: 2,
            maintenanceCount: 2
          }
        },
        hostCnt: 2,
        order: ['OK', 'WARNING']
      }));
      expect(view.get('definitionState')).to.eql([
        {
          state: 'alert-state-OK',
          count: 'OK (1)',
          maintenanceCount: 'OK (1)'
        },
        {
          state: 'alert-state-WARNING',
          count: 'WARN (2)',
          maintenanceCount: 'WARN (2)'
        }]
      );
    });
  });

});