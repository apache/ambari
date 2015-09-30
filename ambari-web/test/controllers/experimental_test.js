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

require('controllers/experimental');

var controller,
  transition,
  supports = {},
  transitionStubbed = false,
  controllerSupports = [
    Em.Object.create({
      name: 'sup0',
      selected: true
    }),
    Em.Object.create({
      name: 'sup1',
      selected: false
    })
  ],
  saveObject = {};

describe('App.ExperimentalController', function () {

  before(function () {
    controllerSupports.forEach(function(item) {
      supports[item.get('name')] = item.get('selected');
    });
    sinon.stub(App, 'get', function(k) {
      if (k === 'supports') return supports;
      return Em.get(App, k);
    });
  });

  beforeEach(function () {
    controller = App.ExperimentalController.create();
  });

  after(function () {
    App.get.restore();
  });

  describe('#supports', function () {
    it('should take data from App.supports', function () {
      expect(controller.get('supports')).to.eql(controllerSupports);
    });
  });

  describe('#doSave', function () {

    before(function () {
      sinon.stub(Ember, 'set', function (p, v) {
        if (p.indexOf('App.supports.' != -1)) {
          p = p.replace('App.supports.', '');
          saveObject[p] = v;
          return;
        }
        return Ember.set(p, v);
      });
      sinon.stub(App.router, 'transitionTo', Em.K);
    });

    after(function () {
      Em.set.restore();
      App.router.transitionTo.restore();
    });

    it('should pass data to App.supports', function () {
      controller.set('supports', controllerSupports);
      controller.doSave();
      expect(saveObject).to.eql(supports);
    });

  });

});
