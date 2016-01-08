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

  describe('#popupText', function () {
    var cases = [
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

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(obj.popupText(item.text)).to.equal(item.result);
      });
    });

  });

  describe('#closeReloadPopup', function () {

    it('should hide modal popup', function () {
      obj.showReloadPopup();
      obj.closeReloadPopup();
      expect(obj.get('reloadPopup')).to.be.null;
    });

  });

});
