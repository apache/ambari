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
var view;

describe('App.MainAdminServiceAutoStartView', function () {

  beforeEach(function () {
    view = App.MainAdminServiceAutoStartView.create({
      controller: Em.Object.create({
        load: Em.K
      })
    });
  });


  describe('#didInsertElement()', function() {

    beforeEach(function() {
      sinon.stub(view, 'initSwitcher');
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(view.get('controller'), 'load').returns({
        then: Em.clb
      })
    });

    afterEach(function() {
      view.initSwitcher.restore();
      view.get('controller').load.restore();
      Em.run.next.restore();
    });

    it('initSwitcher should be called', function() {
      view.didInsertElement();
      expect(view.initSwitcher).to.be.calledOnce;
    });

    it('isLoaded should be true', function() {
      view.didInsertElement();
      expect(view.get('isLoaded')).to.be.true;
    });
  });

  describe('#onValueChange()', function() {
    var mock = {
      bootstrapSwitch: Em.K
    };

    beforeEach(function() {
      sinon.spy(mock, 'bootstrapSwitch');
    });

    afterEach(function() {
      mock.bootstrapSwitch.restore();
    });

    it('bootstrapSwitch should not be called', function() {
      view.set('switcher', null);
      view.onValueChange();
      expect(mock.bootstrapSwitch).to.not.be.called;
    });

    it('bootstrapSwitch should be called', function() {
      view.set('switcher', mock);
      view.onValueChange();
      expect(mock.bootstrapSwitch).to.be.calledOnce;
    });
  });
});