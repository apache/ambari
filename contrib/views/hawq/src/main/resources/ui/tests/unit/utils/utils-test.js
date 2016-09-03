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
import { module, test } from 'qunit';
import ENV from 'hawq-view/config/environment';

module('Unit | Utility | utils');

test('#formatTimeOfDay: extracts time of day from a string', function(assert) {
  assert.equal(Utils.formatTimeOfDay('2016-02-16T16:41:13'), '16:41:13');
});

// TODO:  make this TZ-independent for testing purposes
//test('#formatTimeOfDay: extracts time of day from a string, in the local time zone', function(assert) {
//  assert.equal(Utils.formatTimeOfDay('2016-02-17T00:41:13+00:00'), '16:41:13');
//});

test('#formatTimeDelta: extracts time difference between two date strings', function(assert) {
  var spy = sinon.spy(Utils, 'calculateTimeDelta');
  let backendStartTime = '2016-02-17T00:41:13-08:00';
  let queryStartTime = '2016-02-17T01:43:43-08:00';
  assert.equal(Utils.formatTimeDelta(backendStartTime, queryStartTime).text, '1h 2m 30s');
  sinon.assert.calledOnce(spy);
  sinon.assert.calledWith(spy, backendStartTime, queryStartTime);
  Utils.calculateTimeDelta.restore();
});

test('#formatTimeDelta: shows "0m 0s" for two identical times', function(assert) {
  var response = Utils.formatTimeDelta('2016-02-17T00:41:13-08:00', '2016-02-17T00:41:13-08:00');
  assert.equal(response.text, '0s');
  assert.equal(response.value, 0);
});

test('#formatTimeDelta: shows "0m 0s" for the second time being earlier than the first', function(assert) {
  var response = Utils.formatTimeDelta('2016-02-17T00:45:13-08:00', '2016-02-17T00:41:13-08:00');
  assert.equal(response.text, '0s');
  assert.equal(response.value, 0);
});

test('#computeClientAddress: compute clientAddress', function(assert) {
  assert.equal(Utils.computeClientAddress('host', '9999'), 'host:9999');
});

test('#computeClientAddress: computed host address', function(assert) {
  assert.equal(Utils.computeClientAddress('host', 7777), 'host:7777', 'invalid computed client address');
});

test('#computeClientAddress: computed host address, -1 port', function(assert) {
  assert.equal(Utils.computeClientAddress('host', -1), 'local', 'invalid computed client address');
});

test('#computeClientAddress: computed host address, Undefined/Null/Empty Host', function(assert) {
  assert.equal(Utils.computeClientAddress(undefined, 7777), 'local', 'Invalid hostname: Undefined clientHost');
  assert.equal(Utils.computeClientAddress( null, 7777), 'local', 'Invalid hostname: Null clientHost');
  assert.equal(Utils.computeClientAddress('', 7777), 'local', 'Invalid hostname: Empty clientHost');
});

test('#getNamespace returns namespace for testing', function(assert) {
  assert.equal(Utils.getNamespace(), ENV.apiURL);
});

test('#getNamespace returns namespace for production', function(assert) {
  var baseNamespace = 'foo/';
  var oldEnvironment = ENV.environment;
  ENV.environment = 'production';
  sinon.stub(Utils, 'getWindowPathname').returns(baseNamespace);
  assert.equal(Utils.getNamespace(), baseNamespace + ENV.apiURL);
  Utils.getWindowPathname.restore();
  ENV.environment = oldEnvironment;
});

test('#generateStatusString returns "Running" when waiting and waitingResource are both false', function(assert) {
  assert.equal(Utils.generateStatusString(false, false), 'Running');
});

test('#generateStatusString returns "Waiting on Lock" when waiting is true', function(assert) {
  assert.equal(Utils.generateStatusString(true, false), 'Waiting on Lock');
});

test('#generateStatusString returns "Queued" when waitingResource is true', function(assert) {
  assert.equal(Utils.generateStatusString(true, true), 'Queued');
});

test('#calculateTimeDelta returns difference between two time objects', function(assert) {
  assert.equal(Utils.calculateTimeDelta('2016-02-17T00:41:13-08:00', '2016-02-17T11:52:24-08:00'), 40271000);
});
