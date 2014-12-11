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
require('controllers/main/admin/stack_versions/stack_versions_controller');
var controller;

describe('App.MainStackVersionsController', function () {

  beforeEach(function () {
    controller = App.MainStackVersionsController.create();
  });

  describe('#load()', function () {
    it('loads data to model by running loadStackVersionsToModel', function () {
      sinon.stub(controller, 'loadStackVersionsToModel').returns({done: Em.K});

      controller.load();
      expect(controller.loadStackVersionsToModel.calledOnce).to.be.true;

      controller.loadConfigVersionsToModel.restore();
    });
  });

  describe('#loadStackVersionsToModel()', function () {
    it('loads data to model', function () {
      sinon.stub(App.HttpClient, 'get', Em.K);
      sinon.stub(controller, 'getUrl', Em.K);

      controller.loadConfigVersionsToModel();
      expect(App.HttpClient.get.calledOnce).to.be.true;
      expect(controller.getUrl.calledWith([1])).to.be.true;


      controller.getUrl.restore();
      App.HttpClient.get.restore();
    });
  });
});