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

var adapter;

QUnit.module('unit/adapters - QueueAdapter#parseNodeLabels', {
  setup: function () {
    adapter = App.QueueAdapter.create();
  },
  teardown: function () {
    adapter = null;
  }
});

test('parsed object with nodeLabelsInfo wrapper and mixed exclusivity', function () {
  var result = adapter.parseNodeLabels({
    nodeLabelsInfo: {
      nodeLabelInfo: [
        { name: 'label-a', exclusivity: true },
        { name: 'label-b', exclusivity: 'false' }
      ]
    }
  });
  equal(result.configured, true, 'configured is true');
  deepEqual(result.labels, [
    { name: 'label-a', exclusivity: true },
    { name: 'label-b', exclusivity: false }
  ], 'wrapper unwrapped, exclusivity normalized');
});

test('JSON string response with single nodeLabelInfo object and string "true"', function () {
  var result = adapter.parseNodeLabels(JSON.stringify({
    nodeLabelInfo: { name: 'label-a', exclusivity: 'true' }
  }));
  equal(result.configured, true);
  deepEqual(result.labels, [{ name: 'label-a', exclusivity: true }],
    'string parsed, single object wrapped, "true" -> true');
});

test('nodeLabelInfo as array', function () {
  var result = adapter.parseNodeLabels({
    nodeLabelInfo: [{ name: 'label-a', exclusivity: false }]
  });
  deepEqual(result.labels, [{ name: 'label-a', exclusivity: false }]);
});

test('exclusivity string "false" and boolean false both normalize to false', function () {
  var result = adapter.parseNodeLabels({
    nodeLabelInfo: [
      { name: 'label-a', exclusivity: 'false' },
      { name: 'label-b', exclusivity: false }
    ]
  });
  deepEqual(result.labels, [
    { name: 'label-a', exclusivity: false },
    { name: 'label-b', exclusivity: false }
  ]);
});

test('legacy nodeLabels with mixed string and object entries', function () {
  var result = adapter.parseNodeLabels({
    nodeLabels: ['label-a', { name: 'label-b' }]
  });
  equal(result.configured, true);
  deepEqual(result.labels, [{ name: 'label-a' }, { name: 'label-b' }],
    'string and object legacy entries both normalize to {name}');
});

test('legacy nodeLabels as a single string', function () {
  var result = adapter.parseNodeLabels({ nodeLabels: 'label-a' });
  deepEqual(result.labels, [{ name: 'label-a' }]);
});

test('malformed JSON string logs a warning and returns empty labels', function () {
  var warned = false,
      originalWarn = console.warn;
  console.warn = function () { warned = true; };
  try {
    var result = adapter.parseNodeLabels('{ this is not json');
    ok(warned, 'a parse warning was logged');
    equal(result.configured, false, 'configured is false on parse failure');
    deepEqual(result.labels, [], 'empty label list on parse failure');
  } finally {
    console.warn = originalWarn;
  }
});

test('empty valid object returns empty labels and configured true', function () {
  var result = adapter.parseNodeLabels({});
  equal(result.configured, true, 'a valid empty object still counts as configured');
  deepEqual(result.labels, []);
});
