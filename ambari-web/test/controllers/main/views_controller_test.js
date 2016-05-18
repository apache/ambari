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
require('controllers/main/views_controller');
var testHelpers = require('test/helpers');

var mainViewsController;
describe('MainViewsController', function () {

  beforeEach(function () {
    mainViewsController = App.MainViewsController.create();
  });

  describe('#loadAmbariViews()', function () {
    beforeEach(function () {
      this.stub = sinon.stub(App.router, 'get');
    });
    afterEach(function () {
      App.router.get.restore();
    });

    it('should load views if the user is logged in', function () {
      this.stub.withArgs('loggedIn').returns(true);
      mainViewsController.loadAmbariViews();
      var args = testHelpers.findAjaxRequest('name', 'views.info');
      expect(args).to.exists;
    });

    it('should not load views if the user is not logged in', function () {
      this.stub.withArgs('loggedIn').returns(false);
      mainViewsController.loadAmbariViews();
      var args = testHelpers.findAjaxRequest('name', 'views.info');
      expect(args).to.not.exists;
    })
  });

  describe('#loadAmbariViewsSuccess()', function () {

    it('data has items', function () {
      mainViewsController.loadAmbariViewsSuccess({items: [{}]});
      var args = testHelpers.findAjaxRequest('name', 'views.instances');
      expect(args).to.exists;
    });

    it('data is empty', function () {
      mainViewsController.loadAmbariViewsSuccess({items: []});
      var args = testHelpers.findAjaxRequest('name', 'views.instances');
      expect(args).to.not.exists;
      expect(mainViewsController.get('ambariViews')).to.be.empty;
      expect(mainViewsController.get('isDataLoaded')).to.be.true;
    });
  });

  describe('#loadAmbariViewsError()', function () {

    it('ambariViews should be empty', function () {
      mainViewsController.loadAmbariViewsError();
      expect(mainViewsController.get('ambariViews')).to.be.empty;
      expect(mainViewsController.get('isDataLoaded')).to.be.true;
    });
  });

  describe("#loadViewInstancesSuccess()", function () {

    var data = {
      items: [
        {
          versions: [
            {
              instances: [
                {
                  ViewInstanceInfo: {
                    icon_path: 'icon_path1',
                    label: 'label1',
                    visible: true,
                    version: '1.0',
                    description: 'desc1',
                    viewName: 'view_name1',
                    instanceName: 'instance_name1',
                    context_path: 'path1'
                  }
                }
              ]
            }
          ]
        }
      ]
    };

    it("should parse view instance data", function () {
      mainViewsController.loadViewInstancesSuccess(data);
      expect(JSON.parse(JSON.stringify(mainViewsController.get('ambariViews')))).to.be.eql([{
        "iconPath": "icon_path1",
        "label": "label1",
        "visible": true,
        "version": "1.0",
        "description": "desc1",
        "href": "path1/"
      }]);
      expect(mainViewsController.get('isDataLoaded')).to.be.true;
    });
  });

  describe('#loadViewInstancesError()', function () {

    it('ambariViews should be empty', function () {
      mainViewsController.loadViewInstancesError();
      expect(mainViewsController.get('ambariViews')).to.be.empty;
      expect(mainViewsController.get('isDataLoaded')).to.be.true;
    });
  });

  describe("#setView", function () {

    beforeEach(function () {
      sinon.stub(App.router, 'route');
    });
    afterEach(function () {
      App.router.route.restore();
    });

    it("no context", function () {
      mainViewsController.setView({});
      expect(App.router.route.called).to.be.false;
    });

    it("context exist", function () {
      mainViewsController.setView({
        context: {
          viewName: 'view1',
          version: '1',
          instanceName: 'instance1'
        }
      });
      expect(App.router.route.calledWith('main/views/view1/1/instance1')).to.be.true;
    });
  });

});
