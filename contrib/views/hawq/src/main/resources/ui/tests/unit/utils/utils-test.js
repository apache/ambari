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

/*jshint node:true*/
/* global sinon */

import Utils from 'hawq-view/utils/utils';
import {module, test} from 'qunit';
import ENV from 'hawq-view/config/environment';

module('Unit | Utility | utils');

test('#computeClientAddress: compute clientAddress', function (assert) {
  assert.equal(Utils.computeClientAddress('host', '9999'), 'host:9999');
});

test('#computeClientAddress: computed host address', function (assert) {
  assert.equal(Utils.computeClientAddress('host', 7777), 'host:7777', 'invalid computed client address');
});

test('#computeClientAddress: computed host address, -1 port', function (assert) {
  assert.equal(Utils.computeClientAddress('host', -1), 'local', 'invalid computed client address');
});

test('#computeClientAddress: computed host address, Undefined/Null/Empty Host', function (assert) {
  assert.equal(Utils.computeClientAddress(undefined, 7777), 'local', 'Invalid hostname: Undefined clientHost');
  assert.equal(Utils.computeClientAddress(null, 7777), 'local', 'Invalid hostname: Null clientHost');
  assert.equal(Utils.computeClientAddress('', 7777), 'local', 'Invalid hostname: Empty clientHost');
});

test('#formatDuration returns 00:00:00 for null or undefined duration', function (assert) {
  assert.equal(Utils.formatDuration(null), "00:00:00");
  assert.equal(Utils.formatDuration(), "00:00:00");
});

test('#formatDuration returns correct string for query running for seconds', function (assert) {
  assert.equal(Utils.formatDuration(0), "00:00:00");
  assert.equal(Utils.formatDuration(32), "00:00:32");
  assert.equal(Utils.formatDuration(59), "00:00:59");
});

test('#formatDuration returns correct string for query running for minutes', function (assert) {
  assert.equal(Utils.formatDuration(60), "00:01:00");
  assert.equal(Utils.formatDuration(72), "00:01:12");
  assert.equal(Utils.formatDuration(3599), "00:59:59");
});

test('#formatDuration returns correct string for query running for hours', function (assert) {
  assert.equal(Utils.formatDuration(3600), "01:00:00");
  assert.equal(Utils.formatDuration(7272), "02:01:12");
  assert.equal(Utils.formatDuration(363599), "100:59:59");
});

test('#getNamespace returns namespace for testing', function (assert) {
  assert.equal(Utils.getNamespace(), ENV.apiURL);
});

test('#getNamespace returns namespace for production', function (assert) {
  var baseNamespace = '/views/HAWQ/1.0.0/HAWQView/';
  var oldEnvironment = ENV.environment;
  ENV.environment = 'production';
  sinon.stub(Utils, 'getWindowPathname').returns(baseNamespace);
  assert.equal(Utils.getNamespace(), '/api/v1/views/HAWQ/versions/1.0.0/instances/HAWQView');
  Utils.getWindowPathname.restore();
  ENV.environment = oldEnvironment;
});

test('#generateStatusString returns "Running" when waiting and waitingResource are both false', function (assert) {
  assert.equal(Utils.generateStatusString(false, false), 'Running');
});

test('#generateStatusString returns "Waiting on Lock" when waiting is true', function (assert) {
  assert.equal(Utils.generateStatusString(true, false), 'Waiting on Lock');
});

test('#generateStatusString returns "Queued" when waitingResource is true', function (assert) {
  assert.equal(Utils.generateStatusString(true, true), 'Queued');
});