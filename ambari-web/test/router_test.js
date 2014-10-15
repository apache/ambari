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

require('router');

describe('App.Router', function () {
  var router = App.Router.create();

  describe('#loginSuccessCallback()', function() {
    it('should log in user and load views', function () {
      var mainViewsControllerMock = Em.Object.create({
        loadAmbariViews: sinon.stub()
      });
      var userName = 'test';
      sinon.stub(App.usersMapper, 'map');
      sinon.stub(router, 'setUserLoggedIn');
      sinon.stub(App.router, 'get').withArgs('mainViewsController').returns(mainViewsControllerMock);
      sinon.stub(App.ajax, 'send');

      router.loginSuccessCallback({},{},{loginName: userName});

      expect(mainViewsControllerMock.loadAmbariViews.calledOnce).to.be.true;
      expect(router.setUserLoggedIn.calledOnce).to.be.true;
      expect(router.setUserLoggedIn.calledWith(userName)).to.be.true;

      App.usersMapper.map.restore();
      router.setUserLoggedIn.restore();
      App.router.get.restore();
      App.ajax.send.restore();
    })
  });

});
