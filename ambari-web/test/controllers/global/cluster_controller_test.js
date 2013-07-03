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
require('controllers/global/cluster_controller');
require('models/host_component');
require('utils/http_client');
require('models/service');

describe('App.clusterController', function () {
  var controller = App.ClusterController.create();
  App.Service.FIXTURES = [{service_name: 'NAGIOS'}];

  describe('#updateLoadStatus()', function () {

    controller.set('dataLoadList', Em.Object.create({
      'item1':false,
      'item2':false
    }));

    it('when none item is loaded then width should be "width:0"', function(){
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:0');
    });
    it('when first item is loaded then isLoaded should be false', function(){
      controller.updateLoadStatus.call(controller, 'item1');
      expect(controller.get('isLoaded')).to.equal(false);
    });
    it('when first item is loaded then width should be "width:50%"', function(){
      controller.updateLoadStatus.call(controller, 'item1');
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:50%');
    });

    it('when all items are loaded then isLoaded should be true', function(){
      controller.updateLoadStatus.call(controller, 'item2');
      expect(controller.get('isLoaded')).to.equal(true);
    });
    it('when all items are loaded then width should be "width:100%"', function(){
      controller.updateLoadStatus.call(controller, 'item2');
      expect(controller.get('clusterDataLoadedPercent')).to.equal('width:100%');
    });
  });

  describe('#loadClusterNameSuccessCallback', function() {
    var test_data = {
      "items" : [
        {
          "Clusters" : {
            "cluster_name" : "tdk",
            "version" : "HDP-1.3.0"
          }
        }
      ]
    };
    controller.loadClusterNameSuccessCallback(test_data);
    it('Check cluster', function() {
      expect(controller.get('cluster.Clusters.cluster_name')).to.equal('tdk');
      expect(controller.get('cluster.Clusters.version')).to.equal('HDP-1.3.0');
      expect(App.get('clusterName')).to.equal('tdk');
    });
  });

  describe('#loadClusterNameErrorCallback', function() {
    controller.loadClusterNameErrorCallback();
    it('', function() {
      expect(controller.get('isLoaded')).to.equal(true);
    });
  });

  describe('#getUrl', function() {
    controller.set('clusterName', 'tdk');
    var tests = ['test1', 'test2', 'test3'];
    it('testMode = true', function() {
      App.testMode = true;
      tests.forEach(function(test) {
        expect(controller.getUrl(test, test)).to.equal(test);
      });
    });
    it('testMode = false', function() {
      App.testMode = false;
      tests.forEach(function(test) {
        expect(controller.getUrl(test, test)).to.equal(App.apiPrefix + '/clusters/' + controller.get('clusterName') + test);
      });
    });
  });

});