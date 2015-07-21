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

    beforeEach(function () {
      sinon.stub(App.usersMapper, 'map');
      sinon.stub(router, 'setUserLoggedIn');
      sinon.stub(App.ajax, 'send');
    });

    afterEach(function() {
      App.usersMapper.map.restore();
      router.setUserLoggedIn.restore();
      App.ajax.send.restore();
    });

    it('should log in user and load views', function () {
      var userName = 'test';
      router.loginSuccessCallback({},{},{loginName: userName});
      expect(router.setUserLoggedIn.calledOnce).to.be.true;
      expect(router.setUserLoggedIn.calledWith(userName)).to.be.true;
    })
  });

  describe('#initAdmin()', function () {

    var cases = [
      {
        user: {
          admin: true
        },
        isAdmin: true,
        isOperator: false,
        isPermissionDataLoaded: true,
        title: 'admin'
      },
      {
        user: {
          operator: true
        },
        isAdmin: false,
        isOperator: true,
        isPermissionDataLoaded: true,
        title: 'operator'
      },
      {
        user: {},
        isAdmin: false,
        isOperator: false,
        isPermissionDataLoaded: true,
        title: 'read only access'
      },
      {
        user: null,
        isAdmin: false,
        isOperator: false,
        isPermissionDataLoaded: false,
        title: 'no user'
      }
    ];

    beforeEach(function () {
      App.setProperties({
        isAdmin: false,
        isOperator: false,
        isPermissionDataLoaded: false
      });
    });

    afterEach(function () {
      App.db.getUser.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App.db, 'getUser').returns(item.user);
        router.initAdmin();
        expect(App.get('isAdmin')).to.equal(item.isAdmin);
        expect(App.get('isOperator')).to.equal(item.isOperator);
        expect(App.get('isPermissionDataLoaded')).to.equal(item.isPermissionDataLoaded);
      });
    });

  });

  describe('#adminViewInfoSuccessCallback', function() {
    beforeEach(function() {
      sinon.stub(window.location, 'replace', Em.K);
    });
    afterEach(function() {
      window.location.replace.restore();
    });

    it('should redirect to the latest version of admin view', function() {
      var tests = [{
        mockData: {
          components: [{
            'RootServiceComponents': {
              'component_version': '1.9.0'
            }
          }, {
            'RootServiceComponents': {
              'component_version': '2.0.0'
            }
          }]
        },
        expected: '/views/ADMIN_VIEW/2.0.0/INSTANCE/#/'
      }, {
        mockData: {
          components: [{
            'RootServiceComponents': {
              'component_version': '1.9.0'
            }
          }, {
            'RootServiceComponents': {
              'component_version': '2.1.0'
            }
          }, {
            'RootServiceComponents': {
              'component_version': '2.0.0'
            }
          }]
        },
        expected: '/views/ADMIN_VIEW/2.1.0/INSTANCE/#/'
      }, {
        mockData: {
          versions: [{
            'RootServiceComponents': {
              version: '2.1.0'
            }
          }]
        },
        expected: '/views/ADMIN_VIEW/2.1.0/INSTANCE/#/'
      }];

      tests.forEach(function(data) {
        router.adminViewInfoSuccessCallback(data.mockData);
        expect(window.location.replace.calledWith(data.expected)).to.be.true;
      });
    });
  });
});
