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
require('views/common/configs/overriddenProperty_view');

describe('App.ServiceConfigView.SCPOverriddenRowsView', function () {

  var view;
  beforeEach(function () {
    view = App.ServiceConfigView.SCPOverriddenRowsView.create();
  });

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.stub(App, 'tooltip', Em.K);
    });

    afterEach(function () {
      App.tooltip.restore();
    });

    it('setSwitchLinks method should be executed', function () {
      view.didInsertElement();
    });
  });

  describe('#toggleFinalFlag', function () {
    it('should not change anyting if isNotEditable', function () {
      var e = {
        contexts: [Em.Object.create({isNotEditable: true, isFinal: false})]
      };
      view.toggleFinalFlag(e);
      expect(e.contexts[0].get('isFinal')).to.be.false;
    });

    it('should change isFinal if isNotEditable is false', function () {
      var e = {
        contexts: [Em.Object.create({isNotEditable: false, isFinal: false})]
      };
      view.toggleFinalFlag(e);
      expect(e.contexts[0].get('isFinal')).to.be.true;
    });
  });

  describe('#removeOverride', function () {
    it('should call removeObject if controller name is wizardStep7Controller', function () {
      var e = {
        contexts: [Em.Object.create({isNotEditable: true, isFinal: false})]
      };
      var properties = {
        removeObject: function () {}
      };
      sinon.stub(properties, 'removeObject');
      var controller = Em.Object.create({
        name: 'wizardStep7Controller',
        selectedConfigGroup: {name: 'test'},
        selectedService: {configGroups: [Em.Object.create({name: 'test', properties: properties})]}
      });
      view.set('controller', controller);
      view.set('serviceConfigProperty', Em.Object.create({overrides: null}));
      view.removeOverride(e);
      expect(properties.removeObject.calledOnce).to.be.true;
      properties.removeObject.restore();
    });

    it('should not call removeObject if controller name is not wizardStep7Controller', function () {
      var e = {
        contexts: [Em.Object.create({isNotEditable: true, isFinal: false})]
      };
      var properties = {
        removeObject: function () {}
      };
      sinon.stub(properties, 'removeObject');
      var controller = Em.Object.create({
        name: 'test',
        selectedConfigGroup: {name: 'test'},
        selectedService: {configGroups: [Em.Object.create({name: 'test', properties: properties})]}
      });
      view.set('controller', controller);
      view.set('serviceConfigProperty', Em.Object.create({overrides: null}));
      view.removeOverride(e);
      expect(properties.removeObject.calledOnce).to.be.false;
      properties.removeObject.restore();
    });

    it('should reset overrides property if overrides is defined', function () {
      var override = Em.Object.create({isNotEditable: true, isFinal: false});
      var e = {
        contexts: [override]
      };
      var properties = {
        removeObject: function () {}
      };
      var controller = Em.Object.create({
        name: 'test',
        selectedConfigGroup: {name: 'test'},
        selectedService: {configGroups: [Em.Object.create({name: 'test', properties: properties})]}
      });
      view.set('controller', controller);
      view.set('serviceConfigProperty', Em.Object.create({overrides: [override, Em.Object.create({})]}));
      view.removeOverride(e);
      expect(view.get('serviceConfigProperty.overrides').length).to.be.equal(1);
    });

    it('should reset overrides property if overrides is defined', function () {
      var serviceConfigs = {
        removeObject: function () {}
      };
      var categoryConfigsAll = {
        removeObject: function () {}
      };
      sinon.stub(serviceConfigs, 'removeObject');
      sinon.stub(categoryConfigsAll, 'removeObject');
      var parentView = Em.Object.create({
        serviceConfigs: serviceConfigs,
        categoryConfigsAll: categoryConfigsAll
      });
      var view = App.ServiceConfigView.SCPOverriddenRowsView.create({parentView: parentView});
      var override = Em.Object.create({isNotEditable: true, isFinal: false});
      var e = {
        contexts: [override]
      };
      var properties = {
        removeObject: function () {}
      };
      var controller = Em.Object.create({
        name: 'test',
        selectedConfigGroup: {name: 'test'},
        selectedService: {configGroups: [Em.Object.create({name: 'test', properties: properties})]}
      });
      view.set('controller', controller);
      view.set('serviceConfigProperty', Em.Object.create({overrides: null, isUserProperty: true}));

      view.set('parentView', parentView);
      view.removeOverride(e);
      expect(categoryConfigsAll.removeObject.calledOnce).to.be.true;
      expect(serviceConfigs.removeObject.calledOnce).to.be.true;
      categoryConfigsAll.removeObject.restore();
      serviceConfigs.removeObject.restore();
    });
  });
});
