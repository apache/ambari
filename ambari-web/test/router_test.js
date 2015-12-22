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

  describe.skip('#loginSuccessCallback()', function() {

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

  describe.skip("#savePreferedPath()", function() {
    beforeEach(function () {
      router.set('preferedPath', null);
    });
    it("has no key", function() {
      router.savePreferedPath('path');
      expect(router.get('preferedPath')).to.equal('path');
    });
    it("path does not contain key", function() {
      router.savePreferedPath('path', 'key');
      expect(router.get('preferedPath')).to.be.null;
    });
    it("path contains key", function() {
      router.savePreferedPath('key=path', 'key=');
      expect(router.get('preferedPath')).to.equal('path');
    });
  });

  describe.skip("#restorePreferedPath()", function() {
    it("preferedPath is null", function() {
      router.set('preferedPath', null);
      expect(router.restorePreferedPath()).to.be.false;
      expect(router.get('preferedPath')).to.be.null;
    });
    it("preferedPath is '/relativeURL'", function() {
      router.set('preferedPath', '/relativeURL');
      expect(router.restorePreferedPath()).to.be.true;
      expect(router.get('preferedPath')).to.be.null;
    });
    it("preferedPath is '#/relativeURL'", function() {
      router.set('preferedPath', '#/relativeURL');
      expect(router.restorePreferedPath()).to.be.true;
      expect(router.get('preferedPath')).to.be.null;
    });
    it("preferedPath is '#/login'", function() {
      router.set('preferedPath', '#/login');
      expect(router.restorePreferedPath()).to.be.false;
      expect(router.get('preferedPath')).to.be.null;
    });
    it("preferedPath is 'http://absoluteURL'", function() {
      router.set('preferedPath', 'http://absoluteURL');
      expect(router.restorePreferedPath()).to.be.false;
      expect(router.get('preferedPath')).to.be.null;
    });
  });

  describe.skip("#loginGetClustersSuccessCallback()", function () {
    var mock = {dataLoading: Em.K};
    beforeEach(function () {
      sinon.stub(router, 'setClusterInstalled', Em.K);
      sinon.stub(router, 'transitionToApp', Em.K);
      sinon.stub(router, 'transitionToViews', Em.K);
      sinon.stub(router, 'transitionToAdminView', Em.K);
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'dataLoading');
      App.setProperties({
        isAdmin: false,
        isOperator: false,
        isPermissionDataLoaded: false
      });
    });
    afterEach(function () {
      router.setClusterInstalled.restore();
      router.transitionToApp.restore();
      router.transitionToViews.restore();
      router.transitionToAdminView.restore();
      App.router.get.restore();
      mock.dataLoading.restore();
    });
    it("cluster exists, OPERATOR privileges", function () {
      var clusterData = {
        items: [{
          Clusters: {
            cluster_name: 'c1'
          }
        }]
      };
      var params = {
        loginData: {
          privileges: [{
            PrivilegeInfo: {
              cluster_name: 'c1',
              permission_name: 'CLUSTER.OPERATE'
            }
          }]
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.setClusterInstalled.calledWith(clusterData)).to.be.true;
      expect(router.transitionToApp.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.true;
      expect(App.get('isOperator')).to.be.true;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster exists, READ privileges", function () {
      var clusterData = {
        items: [{
          Clusters: {
            cluster_name: 'c1'
          }
        }]
      };
      var params = {
        loginData: {
          privileges: [{
            PrivilegeInfo: {
              cluster_name: 'c1',
              permission_name: 'CLUSTER.READ'
            }
          }]
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.setClusterInstalled.calledWith(clusterData)).to.be.true;
      expect(router.transitionToApp.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.false;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster exists, ADMIN privileges", function () {
      var clusterData = {
        items: [{
          Clusters: {
            cluster_name: 'c1'
          }
        }]
      };
      var params = {
        loginData: {
          privileges: [{
            PrivilegeInfo: {
              cluster_name: 'c1',
              permission_name: 'AMBARI.ADMIN'
            }
          }]
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.setClusterInstalled.calledWith(clusterData)).to.be.true;
      expect(router.transitionToApp.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.true;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster exists, no privileges", function () {
      var clusterData = {
        items: [{
          Clusters: {
            cluster_name: 'c1'
          }
        }]
      };
      var params = {
        loginData: {
          privileges: []
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.setClusterInstalled.calledWith(clusterData)).to.be.true;
      expect(router.transitionToViews.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.false;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster not installed, ADMIN privileges", function () {
      var clusterData = {
        items: []
      };
      var params = {
        loginData: {
          privileges: [{
            PrivilegeInfo: {
              cluster_name: 'c1',
              permission_name: 'AMBARI.ADMIN'
            }
          }]
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.transitionToAdminView.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.true;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
    it("cluster not installed, non-admin privileges", function () {
      var clusterData = {
        items: []
      };
      var params = {
        loginData: {
          privileges: []
        }
      };
      router.loginGetClustersSuccessCallback(clusterData, {}, params);
      expect(router.transitionToViews.calledOnce).to.be.true;
      expect(App.get('isAdmin')).to.be.false;
      expect(App.get('isOperator')).to.be.false;
      expect(App.get('isPermissionDataLoaded')).to.be.true;
      expect(mock.dataLoading.calledOnce).to.be.true;
    });
  });

  describe("#transitionToAdminView()", function () {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send');
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it("", function () {
      router.transitionToAdminView();
      expect(App.ajax.send.calledWith({
        name: 'ambari.service.load_server_version',
        sender: router,
        success: 'adminViewInfoSuccessCallback',
        error: 'adminViewInfoErrorCallback'
      })).to.be.true;
    });
  });

  describe("#transitionToApp()", function () {
    beforeEach(function () {
      this.mock = sinon.stub(router, 'restorePreferedPath');
      sinon.stub(router, 'getSection', function (callback) {
        callback('route');
      });
      sinon.stub(router, 'transitionTo');
    });
    afterEach(function () {
      this.mock.restore();
      router.getSection.restore();
      router.transitionTo.restore();
    });
    it("has restore path", function () {
      this.mock.returns(true);
      router.transitionToApp();
      expect(router.getSection.called).to.be.false;
      expect(router.transitionTo.called).to.be.false;
    });
    it("does not have restore path", function () {
      this.mock.returns(false);
      router.transitionToApp();
      expect(router.getSection.calledOnce).to.be.true;
      expect(router.transitionTo.calledWith('route')).to.be.true;
    });
  });

  describe("#transitionToViews()", function () {
    var mock = {loadAmbariViews: Em.K};
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(router, 'transitionTo');
      sinon.spy(mock, 'loadAmbariViews');
    });
    afterEach(function () {
      App.router.get.restore();
      router.transitionTo.restore();
      mock.loadAmbariViews.restore();
    });
    it("", function () {
      router.transitionToViews();
      expect(mock.loadAmbariViews.calledOnce).to.be.true;
      expect(router.transitionTo.calledWith('main.views.index')).to.be.true;
    });
  });

  describe("#adminViewInfoErrorCallback()", function () {
    beforeEach(function () {
      sinon.stub(router, 'transitionToViews');
    });
    afterEach(function () {
      router.transitionToViews.restore();
    });
    it("", function () {
      router.adminViewInfoErrorCallback();
      expect(router.transitionToViews.calledOnce).to.be.true;
    });
  });
});
