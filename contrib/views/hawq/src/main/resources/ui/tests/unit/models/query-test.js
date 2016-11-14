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

import {moduleForModel, test} from 'ember-qunit';
import Utils from 'hawq-view/utils/utils';

moduleForModel('query', 'Unit | Model | query', {});

test('#computeClientAddress called ', function (assert) {
  assert.expect(0);
  let options = {
    clientHost: 'host',
    clientPort: 7777
  };
  let model = this.subject(options);
  var spy = sinon.spy(Utils, 'computeClientAddress');

  model.get('clientAddress');
  sinon.assert.calledOnce(spy);
  sinon.assert.calledWith(spy, options.clientHost, options.clientPort);
  Utils.computeClientAddress.restore();
});

test('#generateStatusString called', function (assert) {
  assert.expect(0);
  let options = {
    waiting: false,
    waitingResource: false
  };
  let model = this.subject(options);
  var spy = sinon.spy(Utils, 'generateStatusString');

  model.get('status');
  sinon.assert.calledOnce(spy);
  sinon.assert.calledWith(spy, options.waiting, options.waitingResource);
  Utils.generateStatusString.restore();
});