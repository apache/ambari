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

require('models/host_component');
require('models/host');
require('router');
require('models/service/hdfs');
require('controllers/main/service');


describe('App.HostComponent', function () {


  var hostComponentData = [
    {
      id: 'component1',
      component_name: 'DATANODE',
      host_id: 'host1'
    },
    {
      id: 'component2',
      component_name: 'DATANODE',
      host_id: 'host3'
    },
    {
      id: 'component3',
      component_name: 'TASKTRACKER',
      host_id: 'host2'
    }
  ];

  var hdfsData = {
    id: 'HDFS',
    service_name: 'HDFS',
    decommission_data_nodes: ['host1', 'host2']
  };


  //3 hosts are loaded to the model (host1, host2, host3) in models/host_test
  App.store.load(App.HDFSService, hdfsData);
  App.store.loadMany(App.HostComponent, hostComponentData);



  describe('#isDecommissioning', function () {

    it('component1 is DATANODE and on decommissioned host', function () {
      var hostComponent = App.HostComponent.find().findProperty('id', 'component1');
      expect(hostComponent.get('isDecommissioning')).to.equal(true);
    });
    it('component2 is DATANODE but not on decommissioned host', function () {
      var hostComponent = App.HostComponent.find().findProperty('id', 'component2');
      expect(hostComponent.get('isDecommissioning')).to.equal(false);
    });
    it('component3 isn\'t DATANODE', function () {
      var hostComponent = App.HostComponent.find().findProperty('id', 'component3');
      expect(hostComponent.get('isDecommissioning')).to.equal(false);
    });
  });


});
