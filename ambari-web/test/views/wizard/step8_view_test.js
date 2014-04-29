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
require('utils/helper');
require('utils/string_utils');
require('views/wizard/step8_view');
var view;

describe('App.WizardStep8View', function() {

  beforeEach(function() {
    view = App.WizardStep8View.create();
  });

  describe('#didInsertElement', function() {
    it('should call loadStep', function() {
      view.set('controller', Em.Object.create({
        loadStep: Em.K
      }));
      sinon.spy(view.get('controller'), 'loadStep');
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.equal(true);
      view.get('controller').loadStep.restore();
    });
  });

  describe('#printReview', function() {
    it('should call jqprint', function() {
      sinon.stub($.fn, 'jqprint', Em.K);
      view.printReview();
      expect($.fn.jqprint.calledOnce).to.equal(true);
      $.fn.jqprint.restore();
    });
  });

  describe('#showLoadingIndicator', function() {
    it('should hide existing popup', function() {
      var popup = App.ModalPopup.show({});
      view.set('modalPopup', popup);
      view.set('controller', {isSubmitDisabled: false});
      view.showLoadingIndicator();
      expect(Em.isNone(view.get('popup'))).to.equal(true);
    });
    it('if popup exists shouldn\'t create another', function() {
      view.set('modalPopup', App.ModalPopup.show({}));
      view.set('controller', {isSubmitDisabled: true});
      sinon.spy(App.ModalPopup, 'show');
      view.showLoadingIndicator();
      expect(App.ModalPopup.show.called).to.equal(false);
      App.ModalPopup.show.restore();
    });
    it('if popup doesn\'t exist should create another', function() {
      view.set('modalPopup', null);
      view.reopen({controller: {isSubmitDisabled: true}});
      sinon.spy(App.ModalPopup, 'show');
      view.showLoadingIndicator();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });

    describe('#bodyClass', function() {

      beforeEach(function() {
        view.set('modalPopup', null);
        view.reopen({controller: {isSubmitDisabled: true}});
      });

      describe('#autoHide', function() {
        it('should be called if controller.servicesInstalled is true', function() {
          view.showLoadingIndicator();
          var v = view.get('modalPopup').get('bodyClass').create();
          v.reopen({controller: {servicesInstalled: false}, parentView: Em.Object.create({hide: Em.K})});
          sinon.spy(v.get('parentView'), 'hide');
          v.set('controller.servicesInstalled', true);
          expect(v.get('parentView').hide.calledOnce).to.equal(true);
          v.get('parentView').hide.restore();
        });
        it('shouldn\'t be called if controller.servicesInstalled is false', function() {
          view.showLoadingIndicator();
          var v = view.get('modalPopup').get('bodyClass').create();
          v.reopen({controller: {servicesInstalled: false}, parentView: Em.Object.create({hide: Em.K})});
          sinon.spy(v.get('parentView'), 'hide');
          v.set('controller.servicesInstalled', false);
          expect(v.get('parentView').hide.called).to.equal(false);
          v.get('parentView').hide.restore();
        });
      });

      describe('#ajaxQueueChangeObs', function() {
        it('should set barWidth and message', function() {
          view.showLoadingIndicator();
          var v = view.get('modalPopup').get('bodyClass').create();
          v.reopen({controller: {ajaxQueueLength: 12, ajaxRequestsQueue: Em.Object.create({queue: []})}});
          v.set('controller.ajaxRequestsQueue.queue', [{}, {}, {}, {}]);
          expect(v.get('barWidth')).to.equal('width: ' + (8 / 12 * 100) + '%;');
          expect(v.get('message')).to.equal(Em.I18n.t('installer.step8.deployPopup.message').format(8, 12));
        });
      });

    });
  });

});