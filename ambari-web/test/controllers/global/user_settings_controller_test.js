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

describe('App.UserSettingsController', function () {
  var controller;

  beforeEach(function () {
    controller = App.UserSettingsController.create();
  });

  afterEach(function () {
    controller.destroy();
  });

  describe('#userSettingsKeys', function () {
    it('should not be empty', function () {
      expect(Object.keys(controller.get('userSettingsKeys'))).to.have.length.gt(0);
    });
  });

  describe("#getUserPrefSuccessCallback()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'updateUserPrefWithDefaultValues');
    });

    afterEach(function() {
      controller.updateUserPrefWithDefaultValues.restore();
    });

    it("response is null, updateUserPrefWithDefaultValues should be called", function() {
      expect(controller.getUserPrefSuccessCallback(null, {url: ''})).to.be.null;
      expect(controller.updateUserPrefWithDefaultValues.calledWith(null, false)).to.be.true;
      expect(controller.get('currentPrefObject')).to.be.null;
    });

    it("response is correct, updateUserPrefWithDefaultValues should not be called", function() {
      expect(controller.getUserPrefSuccessCallback({}, {url: ''})).to.be.object;
      expect(controller.updateUserPrefWithDefaultValues.called).to.be.false;
      expect(controller.get('currentPrefObject')).to.be.object;
    });
  });

});