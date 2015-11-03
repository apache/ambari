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
var view;

describe('App.HostProgressPopupBodyView', function () {

  beforeEach(function () {
    view = App.HostProgressPopupBodyView.create({
      controller: Em.Object.create({
        dataSourceController: Em.Object.create({})
      })
    });
  });

  describe('#switchLevel', function () {

    var map = App.HostProgressPopupBodyView.create().get('customControllersSwitchLevelMap');

    Object.keys(map).forEach(function (controllerName) {
      var methodName = map [controllerName];
      var levelName = 'REQUESTS_LIST';

      beforeEach(function () {
        sinon.stub(view, methodName, Em.K);
      });

      afterEach(function () {
        view[methodName].restore();
      });

      it('should call ' + methodName, function () {
        view.set('controller.dataSourceController.name', controllerName);
        view.switchLevel(levelName);
        expect(view[methodName].args[0]).to.eql([levelName]);
      });

    });

  });

});