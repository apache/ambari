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

  describe('_determineRoleRelation', function() {
    var cases;

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([{componentName: 'DATANODE'}])
      sinon.stub(App.StackService, 'find').returns([{serviceName: 'HDFS'}])
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.StackService.find.restore();
    });

    cases = [
      {
        task: { role: 'HDFS_SERVICE_CHECK'},
        m: 'Role is HDFS_SERVICE_CHECK',
        e: {
          type: 'service',
          value: 'HDFS'
        }
      },
      {
        task: { role: 'DATANODE'},
        m: 'Role is DATANODE',
        e: {
          type: 'component',
          value: 'DATANODE'
        }
      },
      {
        task: { role: 'UNDEFINED'},
        m: 'Role is UNDEFINED',
        e: false
      }
    ];

    cases.forEach(function(test) {
      it(test.m, function() {
        view.reopen({
          currentHost: Em.Object.create({
            logTasks: [
              { Tasks: { id: 1, role: test.task.role }}
            ]
          })
        });

        var ret = view._determineRoleRelation(Em.Object.create({ id: 1 }));
        expect(ret).to.be.eql(test.e);
      });
    });
  });
});
