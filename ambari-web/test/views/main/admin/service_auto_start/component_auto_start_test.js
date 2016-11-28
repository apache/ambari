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

describe('App.MainAdminServiceAutoStartComponentView', function () {

  beforeEach(function () {
    view = App.MainAdminServiceAutoStartComponentView.create();
  });


  describe('#didInsertElement()', function() {

    beforeEach(function() {
      sinon.stub(view, 'initSwitcher');
    });

    afterEach(function() {
      view.initSwitcher.restore();
    });

    it('initSwitcher should be called', function() {
      view.didInsertElement();
      expect(view.initSwitcher).to.be.calledOnce;
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

    it('bootstrapSwitch should be called', function() {
      view.set('switcher', mock);
      view.onValueChange();
      expect(mock.bootstrapSwitch).to.be.calledOnce;
    });
  });
});