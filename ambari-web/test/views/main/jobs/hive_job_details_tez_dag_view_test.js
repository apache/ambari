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
require('views/main/jobs/hive_job_details_tez_dag_view');

describe('App.MainHiveJobDetailsTezDagView', function() {
  var tezDagView = App.MainHiveJobDetailsTezDagView.create();

  describe('#getNodeCalculatedDimensions()', function() {
    var tests = [
      {
        i: {
          node: {
            operations: [],
            duration: 100
          },
          minDuration: 1
        },
        e: {
          width : 1800,
          height : 400,
          drawWidth : 180,
          drawHeight : 40,
          scale : 10
        },
        m: 'Node(ops=0,duration=100) minDuration=1'
      },
      {
        i: {
          node: {
            operations: [1,2,3,4,5],
            duration: 4
          },
          minDuration: 1
        },
        e: {
          width : 360,
          height : 160,
          drawWidth : 180,
          drawHeight : 40+40,
          scale : 2
        },
        m: 'Node(ops=5,duration=4) minDuration=1'
      },
      {
        i: {
          node: {
            operations: [1],
            duration: 1
          },
          minDuration: 1
        },
        e: {
          width : 180,
          height : 60,
          drawWidth : 180,
          drawHeight : 60,
          scale : 1
        },
        m: 'Node(ops=1,duration=1) minDuration=1'
      },
      { // Error case
        i: {
          node: {
            operations: [1],
            duration: 1
          },
          minDuration: 3
        },
        e: {
          width : 180,
          height : 60,
          drawWidth : 180,
          drawHeight : 60,
          scale : 1
        },
        m: 'Node(ops=1,duration=1) minDuration=3'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var nodeDim = tezDagView.getNodeCalculatedDimensions(test.i.node, test.i.minDuration);
        for(var key in test.e) {
          expect(nodeDim[key]).to.equal(test.e[key]);
        }
      });
    });
  });

});
