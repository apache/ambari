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
require('views/common/modal_popup');

describe('App.ModalPopup', function() {

  var popup;

  beforeEach(function () {
    popup = App.ModalPopup.create(
      {
        primary: 'test',
        secondary: 'test',
        header: 'test',
        body: '<p>text</p><input type="text"><input type="checkbox"><input type="button">',
        $: function () {
          return $(this);
        }
      }
    );
  });

  describe('#didInsertElement', function () {

    it('should focus on the first input element', function () {
      var spy = sinon.spy(popup, "focusElement");
      popup.didInsertElement();
      expect(spy.called).to.be.true;
    });
  });

});