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
require('/views/main/service/services/oozie');

describe('App.MainDashboardServiceOozieView', function () {
  var view;

  beforeEach(function() {
    view = App.MainDashboardServiceOozieView.create();
  });

  describe("#webUi", function () {

    it("singleNodeInstall is true", function () {
      App.set('singleNodeInstall', true);
      App.set('singleNodeAlias', 'host1');

      view.propertyDidChange('webUi');
      expect(view.get('webUi')).to.be.equal("http://host1:11000/oozie");
    });

    it("singleNodeInstall is false", function () {
      App.set('singleNodeInstall', false);
      view.set('service', Em.Object.create({
        hostComponents: [
          Em.Object.create({
            componentName: 'OOZIE_SERVER',
            host: Em.Object.create({
              publicHostName: 'host2'
            })
          })
        ]
      }));
      view.propertyDidChange('webUi');
      expect(view.get('webUi')).to.be.equal("http://host2:11000/oozie");
    });
  });
});