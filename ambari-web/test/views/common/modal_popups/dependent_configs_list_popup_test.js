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
require('views/common/modal_popups/hosts_table_list_popup');

var view;

describe('App.showDependentConfigsPopup', function () {

  beforeEach(function () {
    sinon.stub(Em.run, 'next', Em.K);
    sinon.stub(Em.run, 'once', Em.K);
  });

  afterEach(function () {
    Em.run.next.restore();
    Em.run.once.restore();
  });

  describe('#onClose', function () {

    beforeEach(function () {
      this.ff = function () {};
      sinon.spy(this, 'ff');
      view = App.showDependentConfigsPopup([], [], Em.K, this.ff);
    });

    afterEach(function () {
      this.ff.restore();
    });

    it('should call secondary-callback', function () {
      view.onClose();
      expect(this.ff.calledOnce).to.be.true;
    });

  });

});