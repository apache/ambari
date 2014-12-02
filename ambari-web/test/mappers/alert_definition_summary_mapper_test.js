/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

require('mappers/alert_definition_summary_mapper');

describe('App.alertDefinitionSummaryMapper', function () {

  describe('#map', function() {

    var testModels = [
        App.PortAlertDefinition.createRecord({id: 1, enabled: true, type: 'PORT'}),
        App.MetricsAlertDefinition.createRecord({id: 2, enabled: true, type: 'METRICS'}),
        App.WebAlertDefinition.createRecord({id: 3, enabled: true, type: 'WEB'}),
        App.AggregateAlertDefinition.createRecord({id: 4, enabled: true, type: 'AGGREGATE'}),
        App.ScriptAlertDefinition.createRecord({id: 5, enabled: true, type: 'SCRIPT'}),
        App.ScriptAlertDefinition.createRecord({id: 6, enabled: false, type: 'SCRIPT', summary: {OK: 1}})
      ],
      dataToMap = {
        alerts_summary_grouped: [
          {
            definition_id: 1,
            summary: {
              OK: {count: 1, original_timestamp: 1},
              WARNING: {count: 1, original_timestamp: 2},
              CRITICAL: {count: 0, original_timestamp: 0},
              UNKNOWN: {count: 0, original_timestamp: 0}
            }
          },
          {
            definition_id: 2,
            summary: {
              OK: {count: 1, original_timestamp: 1},
              WARNING: {count: 5, original_timestamp: 2},
              CRITICAL: {count: 1, original_timestamp: 1},
              UNKNOWN: {count: 1, original_timestamp: 3}
            }
          },
          {
            definition_id: 3,
            summary: {
              OK: {count: 1, original_timestamp: 1},
              WARNING: {count: 2, original_timestamp: 2},
              CRITICAL: {count: 3, original_timestamp: 4},
              UNKNOWN: {count: 4, original_timestamp: 3}
            }
          },
          {
            definition_id: 4,
            summary: {
              OK: {count: 4, original_timestamp: 1},
              WARNING: {count: 3, original_timestamp: 2},
              CRITICAL: {count: 2, original_timestamp: 1},
              UNKNOWN: {count: 1, original_timestamp: 2}
            }
          },
          {
            definition_id: 5,
            summary: {
              OK: {count: 1, original_timestamp: 1},
              WARNING: {count: 1, original_timestamp: 2},
              CRITICAL: {count: 1, original_timestamp: 3},
              UNKNOWN: {count: 1, original_timestamp: 4}
            }
          }
        ]
      };

    beforeEach(function() {

      sinon.stub(App.PortAlertDefinition, 'find', function() {return testModels.filterProperty('type', 'PORT');});
      sinon.stub(App.MetricsAlertDefinition, 'find', function() {return testModels.filterProperty('type', 'METRICS');});
      sinon.stub(App.WebAlertDefinition, 'find', function() {return testModels.filterProperty('type', 'WEB');});
      sinon.stub(App.AggregateAlertDefinition, 'find', function() {return testModels.filterProperty('type', 'AGGREGATE');});
      sinon.stub(App.ScriptAlertDefinition, 'find', function() {return testModels.filterProperty('type', 'SCRIPT');});

    });

    afterEach(function() {

      App.PortAlertDefinition.find.restore();
      App.MetricsAlertDefinition.find.restore();
      App.WebAlertDefinition.find.restore();
      App.AggregateAlertDefinition.find.restore();
      App.ScriptAlertDefinition.find.restore();

    });

    it('should map summary info for each alert', function() {

      App.alertDefinitionSummaryMapper.map(dataToMap);
      expect(App.PortAlertDefinition.find().findProperty('id', 1).get('lastTriggered')).to.equal(2);
      expect(App.PortAlertDefinition.find().findProperty('id', 1).get('summary')).to.eql({OK: 1, WARNING: 1, CRITICAL: 0, UNKNOWN: 0});

      expect(App.MetricsAlertDefinition.find().findProperty('id', 2).get('lastTriggered')).to.equal(3);
      expect(App.MetricsAlertDefinition.find().findProperty('id', 2).get('summary')).to.eql({OK: 1, WARNING: 5, CRITICAL: 1, UNKNOWN: 1});

      expect(App.WebAlertDefinition.find().findProperty('id', 3).get('lastTriggered')).to.equal(4);
      expect(App.WebAlertDefinition.find().findProperty('id', 3).get('summary')).to.eql({OK: 1, WARNING: 2, CRITICAL: 3, UNKNOWN: 4});

      expect(App.AggregateAlertDefinition.find().findProperty('id', 4).get('lastTriggered')).to.equal(2);
      expect(App.AggregateAlertDefinition.find().findProperty('id', 4).get('summary')).to.eql({OK: 4, WARNING: 3, CRITICAL: 2, UNKNOWN: 1});

      expect(App.ScriptAlertDefinition.find().findProperty('id', 5).get('lastTriggered')).to.equal(4);
      expect(App.ScriptAlertDefinition.find().findProperty('id', 5).get('summary')).to.eql({OK: 1, WARNING: 1, CRITICAL: 1, UNKNOWN: 1});

    });

    it('should clear summary for disabled definitions', function () {

      App.alertDefinitionSummaryMapper.map(dataToMap);
      expect(App.ScriptAlertDefinition.find().findProperty('id', 6).get('summary')).to.eql({});

    });

  });

});
