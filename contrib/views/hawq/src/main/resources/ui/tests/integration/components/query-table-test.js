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

import {moduleForComponent, test} from 'ember-qunit';
import hbs from 'htmlbars-inline-precompile';
import {makeQueryObjects} from 'hawq-view/tests/helpers/test-helper';

/*jshint node:true*/

moduleForComponent('query-table', 'Integration | Component | query table', {
  integration: true
});

test('it renders a table with mock data', function (assert) {
  let model = makeQueryObjects();
  this.set('model', model);

  this.render(hbs`{{query-table queries=model}}`);

  // Test Column Titles
  assert.equal(this.$('table#query-table').length, 1);
  assert.equal(this.$('thead#query-table-header').length, 1);
  assert.equal(this.$('tr#query-table-header-row').length, 1);
  assert.equal(this.$('th#query-table-header-pid').text(), 'PID');
  assert.equal(this.$('th#query-table-header-status').text(), 'Status');
  assert.equal(this.$('th#query-table-header-username').text(), 'User');
  assert.equal(this.$('th#query-table-header-databasename').text(), 'Database');
  assert.equal(this.$('th#query-table-header-submittime').text(), 'Submit Time');
  assert.equal(this.$('th#query-table-header-duration').text(), 'Duration');
  assert.equal(this.$('th#query-table-header-clientaddress').text(), 'Client');

  // Test Rows
  assert.equal(this.$('tbody#query-table-body').length, 1);
  assert.equal(this.$('tr#query-table-row0').length, 1);
  assert.equal(this.$('td#query-table-row0-pid').text(), model[0].get('pid'));
  let statusDOM = this.$('td#query-table-row0-status');
  assert.equal(statusDOM.text(), model[0].get('status'));
  assert.ok(statusDOM.attr('class').match(model[0].get('statusClass')));
  assert.equal(this.$('td#query-table-row0-username').text(), model[0].get('userName'));
  assert.equal(this.$('td#query-table-row0-databasename').text(), model[0].get('databaseName'));
  assert.equal(this.$('td#query-table-row0-submittime').text(), model[0].get('queryStartTime'));
  assert.equal(this.$('td#query-table-row0-duration').text(), model[0].get('formattedDuration'));
  assert.equal(this.$('td#query-table-row0-clientaddress').text(), model[0].get('clientAddress'));

  assert.equal(this.$('tr#query-table-row1').length, 1);
  assert.ok(this.$('td#query-table-row1-status').attr('class').match(model[1].get('statusClass')));

  assert.equal(this.$('tr#query-table-row2').length, 1);
  assert.ok(this.$('td#query-table-row2-status').attr('class').match(model[2].get('statusClass')));

  assert.equal(this.$('tr#query-table-row3').length, 0);
});

test('Display text if there are no queries', function (assert) {
  this.render(hbs`{{query-table}}`);

  assert.equal(this.$('tr#no-queries').length, 1);
});

/*
 TESTS DISABLED UNTIL SORTING FEATURE IS IMPLEMENTED

 function testColumnSort(_this, assert, columnHeaderSelector, expectedRows) {
 let model = makeQueryObjects();
 _this.set('model', model);

 _this.render(hbs`{{query-table queries=model}}`);
 Ember.$.bootstrapSortable(false);

 // Ascending order
 _this.$(columnHeaderSelector).click();
 let ascendingRows = this.$('tbody').find('tr');

 assert.equal(expectedRows.length, ascendingRows.length);

 for (let i = 0, ii = expectedRows.length; i < ii; ++i) {
 assert.equal(ascendingRows[i].getAttribute('id'), expectedRows[i]);
 }

 // Descending order
 _this.$(columnHeaderSelector).click();
 let descendingRows = this.$('tbody').find('tr');

 assert.equal(expectedRows.length, ascendingRows.length);

 for (let i = 0, ii = expectedRows.length; i < ii; ++i) {
 assert.equal(descendingRows[i].getAttribute('id'), expectedRows[ii - i - 1]);
 }
 }

 test('Clicking on the "Duration" column header toggles the sorting order of the elements of the column', function (assert) {
 let expectedRows = [
 'query-table-row2',
 'query-table-row1',
 'query-table-row0'
 ];

 testColumnSort(this, assert, 'th#query-table-header-duration', expectedRows);
 });
 */