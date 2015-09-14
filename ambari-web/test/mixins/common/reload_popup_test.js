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

require('mixins/common/reload_popup');

describe('App.ReloadPopupMixin', function () {

  var obj;

  beforeEach(function () {
    obj = Em.Object.create(App.ReloadPopupMixin);
  });

  describe('#showReloadPopup', function () {

    var spanRegExp = new RegExp('<span>([\\s\\S]+)<\/span>'),
      cases = [
        {
          result: Em.I18n.t('app.reloadPopup.text'),
          title: 'should show modal popup with default message'
        },
        {
          text: 'text',
          result: 'text',
          title: 'should show modal popup with custom message'
        }
      ];

    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show', function (popup) {
        return popup.body;
      });
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        obj.showReloadPopup(item.text);
        expect(obj.get('reloadPopup').match(spanRegExp)[1]).to.equal(item.result);
      });
    });

  });

  describe('#closeReloadPopup', function () {

    it('should hide modal popup', function () {
      obj.set('retryCount', 1);
      obj.showReloadPopup();
      obj.closeReloadPopup();
      expect(obj.get('reloadPopup')).to.be.null;
      expect(obj.get('retryCount')).to.equal(0);
    });

  });

  describe('#reloadSuccessCallback', function () {

    it('should hide modal popup', function () {
      obj.set('retryCount', 1);
      obj.showReloadPopup();
      obj.reloadSuccessCallback();
      expect(obj.get('reloadPopup')).to.be.null;
      expect(obj.get('retryCount')).to.equal(0);
    });

  });

  describe('#reloadErrorCallback', function () {

    var cases = [
      {
        args: [{status: 404}, null, null, {}, {shouldUseDefaultHandler: true}],
        closeReloadPopupCallCount: 1,
        consoleLogCallCount: 0,
        defaultErrorHandlerCallCount: 1,
        showReloadPopupCallCount: 0,
        setTimeoutCount: 0,
        title: 'status received, no console message, default error handler'
      },
      {
        args: [{status: 404}, null, null, {}, {errorLogMessage: 'error'}],
        closeReloadPopupCallCount: 1,
        consoleLogCallCount: 1,
        defaultErrorHandlerCallCount: 0,
        showReloadPopupCallCount: 0,
        setTimeoutCount: 0,
        title: 'status received, console message displayed, no default error handler'
      },
      {
        args: [{status: 0}, null, null, {}, {times: 5}],
        retryCount: 5,
        retryCountResult: 5,
        closeReloadPopupCallCount: 0,
        consoleLogCallCount: 0,
        defaultErrorHandlerCallCount: 0,
        showReloadPopupCallCount: 1,
        setTimeoutCount: 0,
        title: 'no status received, custom retries count, max retries reached'
      },
      {
        args: [{status: 0}, null, null, {}, {}],
        retryCount: 2,
        retryCountResult: 3,
        closeReloadPopupCallCount: 0,
        consoleLogCallCount: 0,
        defaultErrorHandlerCallCount: 0,
        showReloadPopupCallCount: 1,
        setTimeoutCount: 0,
        title: 'no status received, default retries count, max retries not reached, no callback'
      },
      {
        args: [{status: 0}, null, null, {}, {callback: Em.K}],
        retryCount: 2,
        retryCountResult: 3,
        closeReloadPopupCallCount: 0,
        consoleLogCallCount: 0,
        defaultErrorHandlerCallCount: 0,
        showReloadPopupCallCount: 1,
        setTimeoutCount: 1,
        title: 'no status received, default retries count, max retries not reached, callback specified'
      }
    ];

    beforeEach(function () {
      sinon.stub(obj, 'closeReloadPopup', Em.K);
      sinon.stub(console, 'log', Em.K);
      sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
      sinon.stub(obj, 'showReloadPopup', Em.K);
      sinon.stub(App, 'get').withArgs('maxRetries').returns(3);
      sinon.stub(window, 'setTimeout', Em.K);
    });

    afterEach(function () {
      obj.closeReloadPopup.restore();
      console.log.restore();
      App.ajax.defaultErrorHandler.restore();
      obj.showReloadPopup.restore();
      App.get.restore();
      window.setTimeout.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        if (!Em.isNone(item.retryCount)) {
          obj.set('retryCount', item.retryCount);
        }
        obj.reloadErrorCallback.apply(obj, item.args);
        expect(obj.closeReloadPopup.callCount).to.equal(item.closeReloadPopupCallCount);
        expect(console.log.callCount).to.equal(item.consoleLogCallCount);
        expect(App.ajax.defaultErrorHandler.callCount).to.equal(item.defaultErrorHandlerCallCount);
        expect(obj.showReloadPopup.callCount).to.equal(item.showReloadPopupCallCount);
        expect(window.setTimeout.callCount).to.equal(item.setTimeoutCount);
        if (!Em.isNone(item.retryCountResult)) {
          obj.set('retryCount', item.retryCountResult);
        }
      });
    });

  });

});
