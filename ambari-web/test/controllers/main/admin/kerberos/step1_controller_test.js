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
var testHelpers = require('test/helpers');

function getController() {
  return App.KerberosWizardStep1Controller.create({});
}

describe('App.KerberosWizardStep1Controller', function() {
  var controller;

  beforeEach(function() {
    controller = getController();
  });

  describe("#selectedOption", function () {

    it("test", function() {
      controller.setProperties({
        options: [{value: 'item1'}],
        selectedItem: 'item1'
      });
      controller.propertyDidChange('selectedOption');
      expect(controller.get('selectedOption')).to.be.eql({value: 'item1'});
    });
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      controller.set('options', []);
    });

    it("enableIpa is true", function() {
      App.set('supports.enableIpa', true);
      controller.loadStep();
      expect(controller.get('selectedItem')).to.be.equal(Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'));
      expect(controller.get('options')).to.not.be.empty;
    });

    it("enableIpa is false", function() {
      App.set('supports.enableIpa', false);
      controller.loadStep();
      expect(controller.get('selectedItem')).to.be.equal(Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'));
      expect(controller.get('options')).to.be.empty;
    });
  });

  describe("#next()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'send');
    });

    afterEach(function() {
      App.router.send.restore();
    });

    it("App.router.send should be called", function() {
      controller.reopen({
        'isSubmitDisabled': false
      });
      controller.next();
      expect(App.router.send.calledOnce).to.be.true;
    });

    it("App.router.send should not be called", function() {
      controller.reopen({
        'isSubmitDisabled': true
      });
      controller.next();
      expect(App.router.send.called).to.be.false;
    });
  });

});
