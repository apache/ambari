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

var Ember = require('ember');
var App = require('app');

require('mappers/server_data_mapper');
require('mappers/runs_mapper');

describe('App.runsMapper', function () {

  var tests = [
    {
      i: {
        "workflowContext": {
          "workflowDag": {
            "entries": [
              {
                "source": "scope-5",
                "targets": []
              }
            ]
          }
        }
      },
      index: 0,
      e: '{dag: {"scope-5": []}}',
      m: 'One entry. Without targets'
    },
    {
      i: {
        "workflowContext": {
          "workflowDag": {
            "entries": [
              {
                "source": "scope-5",
                "targets": ['t1']
              }
            ]
          }
        }
      },
      index: 0,
      e: '{dag: {"scope-5": ["t1"]}}',
      m: 'One entry. With one target'
    },
    {
      i: {
        "workflowContext": {
          "workflowDag": {
            "entries": [
              {
                "source": "scope-5",
                "targets": ['t1,t2,t3']
              }
            ]
          }
        }
      },
      index: 0,
      e: '{dag: {"scope-5": ["t1,t2,t3"]}}',
      m: 'One entry. With multiple targets'
    },
    {
      i: {
        "workflowContext": {
          "workflowDag": {
            "entries": [
              {
                "source": "scope-5",
                "targets": []
              },
              {
                "source": "scope-4",
                "targets": []
              }
            ]
          }
        }
      },
      index: 0,
      e: '{dag: {"scope-5": [],"scope-4": []}}',
      m: 'Two entries. Without targets'
    },
    {
      i: {
        "workflowContext": {
          "workflowDag": {
            "entries": [
              {
                "source": "scope-5",
                "targets": ['t1,t2,t3']
              },
              {
                "source": "scope-4",
                "targets": ['t1']
              }
            ]
          }
        }
      },
      index: 0,
      e: '{dag: {"scope-5": ["t1,t2,t3"],"scope-4": ["t1"]}}',
      m: 'Two entries. With multiple targets'
    },
    {
      i: {
        "workflowContext": {
          "workflowDag": {
            "entries": [
              {
                "source": "scope-5",
                "targets": ['t1,t2,t3']
              },
              {
                "source": "scope-4",
                "targets": ['t1,t2,t3']
              }
            ]
          }
        }
      },
      index: 0,
      e: '{dag: {"scope-5": ["t1,t2,t3"],"scope-4": ["t1,t2,t3"]}}',
      m: 'Two entries. With multiple targets'
    }
  ];

  describe('#generateWorkflow', function() {
    tests.forEach(function(test) {
      it (test.m, function() {
        var result = App.runsMapper.generateWorkflow(test.i, test.index);
        expect(result.workflowContext).to.equal(test.e);
        expect(result.index).to.equal(test.index + 1);
      });
    });
  });

});
