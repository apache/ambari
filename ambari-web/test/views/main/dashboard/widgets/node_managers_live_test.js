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

require('messages');
require('views/main/dashboard/widget');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widgets/node_managers_live');

describe('App.NodeManagersLiveView', function() {

  beforeEach(function () {
    sinon.stub(App, 'get').withArgs('router.clusterController.isComponentsStateLoaded').returns(true);
  });

  afterEach(function () {
    App.get.restore();
  });

  var tests = [
    {
      model: {
        nodeManagersTotal: 3,
        nodeManagerLiveNodes: 2
      },
      e: {
        isRed: false,
        isOrange: true,
        isGreen: false,
        isNA: false,
        content: '2/3',
        data: 67
      }
    },
    {
      model: {
        nodeManagersTotal: 2,
        nodeManagerLiveNodes: 2
      },
      e: {
        isRed: false,
        isOrange: false,
        isGreen: true,
        isNA: false,
        content: '2/2',
        data: 100
      }
    },
    {
      model: {
        nodeManagersTotal: 2,
        nodeManagerLiveNodes: 0
      },
      e: {
        isRed: true,
        isOrange: false,
        isGreen: false,
        isNA: false,
        content: '0/2',
        data: 0.00
      }
    }
  ];

  tests.forEach(function(test) {
    describe('nodeManagersTotal length - ' + test.model.nodeManagersTotal + ' | nodeManagerLiveNodes length - ' + test.model.nodeManagerLiveNodes, function() {
      var AppNodeManagersLiveView = App.NodeManagersLiveView.extend({nodeManagersLive: test.model.nodeManagerLiveNodes});
      var nodeManagersLiveView = AppNodeManagersLiveView.create({model_type:null, model: test.model});
      it('content', function() {
        expect(nodeManagersLiveView.get('content')).to.equal(test.e.content);
      });
      it('data', function() {
        expect(nodeManagersLiveView.get('data')).to.equal(test.e.data);
      });
      it('isRed', function() {
        expect(nodeManagersLiveView.get('isRed')).to.equal(test.e.isRed);
      });
      it('isOrange', function() {
        expect(nodeManagersLiveView.get('isOrange')).to.equal(test.e.isOrange);
      });
      it('isGreen', function() {
        expect(nodeManagersLiveView.get('isGreen')).to.equal(test.e.isGreen);
      });
      it('isNA', function() {
        expect(nodeManagersLiveView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

});
