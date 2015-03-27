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

    var mockObj = {
      key: 'value'
    };

    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show').returns(mockObj);
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('should show modal popup', function () {
      obj.showReloadPopup();
      expect(obj.get('reloadPopup')).to.eql(mockObj);
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
