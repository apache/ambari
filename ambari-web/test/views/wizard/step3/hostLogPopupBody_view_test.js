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
require('views/wizard/step3/hostLogPopupBody_view');
var view;
describe('App.WizardStep3HostLogPopupBody', function() {

  beforeEach(function() {
    view = App.WizardStep3HostLogPopupBody.create({
      parentView: Em.Object.create({
        host: Em.Object.create()
      })
    });
  });

  describe('#textArea', function() {

    var v;

    beforeEach(function() {
      v = view.get('textArea').create();
    });

    describe('#value', function() {
      it('should be equal to content', function() {
        var c = 'Relax, you are doing fine';
        v.set('content', c);
        expect(v.get('value')).to.equal(c);
      });
    });

  });

  describe('#bootLog', function() {
    it('should be equal to parentView.host.bootLog', function() {
      var log = 'i wanna play a game';
      view.set('parentView.host.bootLog', log);
      expect(view.get('bootLog')).to.equal(log);
    });
  });

});