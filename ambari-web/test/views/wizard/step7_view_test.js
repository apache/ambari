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
require('views/wizard/step7_view');
var view;

describe('App.WizardStep7View', function() {

  beforeEach(function() {
    view = App.WizardStep7View.create({
      controller: App.WizardStep7Controller.create()
    });
  });

  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(App.get('router'), 'set', Em.K);
    });
    afterEach(function() {
      App.get('router').set.restore();
    });

    it('should call loadStep', function() {
      view.didInsertElement();
      expect(App.get('router').set.calledWith('transitionInProgress', false)).to.be.false;
    });
  });
});
