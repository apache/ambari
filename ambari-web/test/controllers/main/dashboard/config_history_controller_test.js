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

require('controllers/main/dashboard/config_history_controller');

describe('MainConfigHistoryController', function () {

  var controller = App.MainConfigHistoryController.create();

  describe('#realUrl', function () {
    it('cluster name is empty', function () {
      App.set('clusterName', '');
      expect(controller.get('realUrl')).to.equal('/api/v1/clusters//configurations/service_config_versions?<parameters>fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true');
    });
    it('cluster name is "mycluster"', function () {
      App.set('clusterName', 'mycluster');
      expect(controller.get('realUrl')).to.equal('/api/v1/clusters/mycluster/configurations/service_config_versions?<parameters>fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true');
    });
  });
  describe('#load()', function () {
    it('', function () {
      sinon.stub(controller, 'updateTotalCounter', Em.K);
      sinon.stub(controller, 'loadConfigVersionsToModel').returns({done: Em.K});

      controller.load();
      expect(controller.updateTotalCounter.calledOnce).to.be.true;

      controller.updateTotalCounter.restore();
      controller.loadConfigVersionsToModel.restore();
    });
  });
  describe('#loadConfigVersionsToModel()', function () {
    it('', function () {
      sinon.stub(App.HttpClient, 'get', Em.K);
      sinon.stub(controller, 'getUrl', Em.K);
      sinon.stub(controller, 'getQueryParameters', function(){
        return [1];
      });

      controller.loadConfigVersionsToModel();
      expect(App.HttpClient.get.calledOnce).to.be.true;
      expect(controller.getQueryParameters.calledOnce).to.be.true;
      expect(controller.getUrl.calledWith([1])).to.be.true;


      controller.getUrl.restore();
      controller.getQueryParameters.restore();
      App.HttpClient.get.restore();
    });
  });

  describe('#updateTotalCounter()', function () {
    it('', function () {
      sinon.stub(App.ajax, 'send', Em.K);

      controller.updateTotalCounter();
      expect(App.ajax.send.calledOnce).to.be.true;

      App.ajax.send.restore();
    });
  });

  describe('#updateTotalCounterSuccess()', function () {
    it('', function () {
      controller.updateTotalCounterSuccess({itemTotal: 1});
      expect(controller.get('totalCount')).to.equal(1);
    });
  });
  describe('#getUrl()', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get', function () {
        return {
          computeParameters: function () {
            return 'params'
          }
        }
      });
    });
    afterEach(function () {
      App.router.get.restore();
      App.get.restore();
    });
    it('testMode is true', function () {
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return true;
        return Em.get(App, k);
      });
      expect(controller.getUrl()).to.equal('/data/configurations/service_versions.json');
    });
    it('query params is empty', function () {
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return false;
        return Em.get(App, k);
      });
      expect(controller.getUrl()).to.equal('/api/v1/clusters/mycluster/configurations/service_config_versions?fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true');
    });
    it('query params is correct', function () {
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return false;
        return Em.get(App, k);
      });
      expect(controller.getUrl({})).to.equal('/api/v1/clusters/mycluster/configurations/service_config_versions?params&fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true');
    });
  });

  describe('#doPolling()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'load', function(){
        return {done: Em.K};
      });
      this.clock = sinon.useFakeTimers();
    });
    afterEach(function () {
      this.clock.restore();
      controller.load.restore();
    });
    it('isPolling false', function () {
      controller.set('isPolling', false);
      controller.doPolling();
      this.clock.tick(App.componentsUpdateInterval);
      expect(controller.load.called).to.be.false;
    });
    it('isPolling true', function () {
      controller.set('isPolling', true);
      controller.doPolling();
      this.clock.tick(App.componentsUpdateInterval);
      expect(controller.load.calledOnce).to.be.true;
    });
  });
});

