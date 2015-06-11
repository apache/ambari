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

import {
  moduleFor,
  test
} from 'ember-qunit';

var view;

moduleFor('view:visual-explain', 'VisualExplainView', {
  setup: function() {
    var controller = Ember.Controller.extend({}).create();

    view = this.subject({
      controller: controller
    });

    Ember.run(function() {
      view.appendTo('#ember-testing');
    });
  },

  teardown: function() {
    Ember.run(view, view.destroy);
  },
});

//select count (*) from power
var selectCountJson = {"STAGE PLANS":{"Stage-1":{"Tez":{"DagName:":"hive_20150608120000_b930a285-dc6a-49b7-86b6-8bee5ecdeacd:96","Vertices:":{"Reducer 2":{"Reduce Operator Tree:":{"Group By Operator":{"mode:":"mergepartial","aggregations:":["count(VALUE._col0)"],"outputColumnNames:":["_col0"],"children":{"Select Operator":{"expressions:":"_col0 (type: bigint)","outputColumnNames:":["_col0"],"children":{"File Output Operator":{"Statistics:":"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE","compressed:":"false","table:":{"serde:":"org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe","input format:":"org.apache.hadoop.mapred.TextInputFormat","output format:":"org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"}}},"Statistics:":"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE"}},"Statistics:":"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE"}}},"Map 1":{"Map Operator Tree:":[{"TableScan":{"alias:":"power","children":{"Select Operator":{"children":{"Group By Operator":{"mode:":"hash","aggregations:":["count()"],"outputColumnNames:":["_col0"],"children":{"Reduce Output Operator":{"sort order:":"","value expressions:":"_col0 (type: bigint)","Statistics:":"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE"}},"Statistics:":"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE"}},"Statistics:":"Num rows: 0 Data size: 132960632 Basic stats: PARTIAL Column stats: COMPLETE"}},"Statistics:":"Num rows: 0 Data size: 132960632 Basic stats: PARTIAL Column stats: COMPLETE"}}]}},"Edges:":{"Reducer 2":{"parent":"Map 1","type":"SIMPLE_EDGE"}}}},"Stage-0":{"Fetch Operator":{"limit:":"-1","Processor Tree:":{"ListSink":{}}}}},"STAGE DEPENDENCIES":{"Stage-1":{"ROOT STAGE":"TRUE"},"Stage-0":{"DEPENDENT STAGES":"Stage-1"}}};

//select power.adate, power.atime from power join power2 on power.adate = power2.adate
var joinJson = {"STAGE PLANS":{"Stage-1":{"Tez":{"DagName:":"hive_20150608124141_acde7f09-6b72-4ad4-88b0-807d499724eb:107","Vertices:":{"Reducer 2":{"Reduce Operator Tree:":{"Merge Join Operator":{"outputColumnNames:":["_col0","_col1"],"children":{"Select Operator":{"expressions:":"_col0 (type: string), _col1 (type: string)","outputColumnNames:":["_col0","_col1"],"children":{"File Output Operator":{"Statistics:":"Num rows: 731283 Data size: 73128349 Basic stats: COMPLETE Column stats: NONE","compressed:":"false","table:":{"serde:":"org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe","input format:":"org.apache.hadoop.mapred.TextInputFormat","output format:":"org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"}}},"Statistics:":"Num rows: 731283 Data size: 73128349 Basic stats: COMPLETE Column stats: NONE"}},"Statistics:":"Num rows: 731283 Data size: 73128349 Basic stats: COMPLETE Column stats: NONE","condition map:":[{"":"Inner Join 0 to 1"}],"condition expressions:":{"1":"","0":"{KEY.reducesinkkey0} {VALUE._col0}"}}}},"Map 1":{"Map Operator Tree:":[{"TableScan":{"filterExpr:":"adate is not null (type: boolean)","alias:":"power2","children":{"Filter Operator":{"predicate:":"adate is not null (type: boolean)","children":{"Reduce Output Operator":{"Map-reduce partition columns:":"adate (type: string)","sort order:":"+","Statistics:":"Num rows: 664803 Data size: 66480316 Basic stats: COMPLETE Column stats: NONE","key expressions:":"adate (type: string)"}},"Statistics:":"Num rows: 664803 Data size: 66480316 Basic stats: COMPLETE Column stats: NONE"}},"Statistics:":"Num rows: 1329606 Data size: 132960632 Basic stats: COMPLETE Column stats: NONE"}}]},"Map 3":{"Map Operator Tree:":[{"TableScan":{"filterExpr:":"adate is not null (type: boolean)","alias:":"power","children":{"Filter Operator":{"predicate:":"adate is not null (type: boolean)","children":{"Reduce Output Operator":{"Map-reduce partition columns:":"adate (type: string)","sort order:":"+","value expressions:":"atime (type: string)","Statistics:":"Num rows: 332402 Data size: 66480416 Basic stats: COMPLETE Column stats: NONE","key expressions:":"adate (type: string)"}},"Statistics:":"Num rows: 332402 Data size: 66480416 Basic stats: COMPLETE Column stats: NONE"}},"Statistics:":"Num rows: 664803 Data size: 132960632 Basic stats: COMPLETE Column stats: NONE"}}]}},"Edges:":{"Reducer 2":[{"parent":"Map 1","type":"SIMPLE_EDGE"},{"parent":"Map 3","type":"SIMPLE_EDGE"}]}}},"Stage-0":{"Fetch Operator":{"limit:":"-1","Processor Tree:":{"ListSink":{}}}}},"STAGE DEPENDENCIES":{"Stage-1":{"ROOT STAGE":"TRUE"},"Stage-0":{"DEPENDENT STAGES":"Stage-1"}}};

// Replace this with your real tests.
test('it renders dag when controller.json changes.', function (assert) {
  assert.expect(1);

  view.renderDag = function () {
    assert.ok(true, 'dag rendering has been called on json set.');
  };

  view.set('controller.json', selectCountJson);
});

test('renderDag generates correct number of nodes and edges.', function (assert) {
  assert.expect(4);

  Ember.run(function () {
    view.set('controller.json', selectCountJson);

    assert.equal(view.get('graph').nodes().length, 4);
    assert.equal(view.get('graph').edges().length, 3);

    view.set('controller.json', joinJson);

    assert.equal(view.get('graph').nodes().length, 7);
    assert.equal(view.get('graph').edges().length, 6);
  });
});

test('progress gets updated for each node.', function (assert) {
  expect(2);

  Ember.run(function () {
    view.set('controller.json', selectCountJson);

    var targetNode;
    var verticesGroups = view.get('verticesGroups');

    verticesGroups.some(function (verticesGroup) {
      var node = verticesGroup.contents.findBy('label', 'Map 1');

      if (node) {
        targetNode = node;
        return true;
      }
    });

    assert.equal(targetNode.get('progress'), undefined, 'initial progress is falsy.');

    view.set('controller.verticesProgress', [
      Ember.Object.create({
        name: 'Map 1',
        value: 1
      })
    ]);

    assert.equal(targetNode.get('progress'), 1, 'progress gets updated to given value.');
  });
});