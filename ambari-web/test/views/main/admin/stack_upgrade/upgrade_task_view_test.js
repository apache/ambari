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
require('views/main/admin/stack_upgrade/upgrade_task_view');

describe('App.upgradeTaskView', function () {
  var view = App.upgradeTaskView.create({
    content: Em.Object.create()
  });

  describe("#iconClass", function () {
    it("status has icon", function () {
      view.set('statusIconMap', {
        'S1': 'icon1'
      });
      view.set('content.status', 'S1');
      view.propertyDidChange('iconClass');
      expect(view.get('iconClass')).to.equal('icon1');
    });
    it("status undefined", function () {
      view.set('statusIconMap', {
        'S1': 'icon1'
      });
      view.set('content.status', 'S2');
      view.propertyDidChange('iconClass');
      expect(view.get('iconClass')).to.equal('icon-question-sign');
    });
  });

  describe("#isFailed", function () {
    it("task is not failed", function () {
      view.set('content.status', 'COMPLETED');
      view.propertyDidChange('isFailed');
      expect(view.get('isFailed')).to.be.false;
    });
    it("task is not failed", function () {
      view.set('content.status', 'FAILED');
      view.propertyDidChange('isFailed');
      expect(view.get('isFailed')).to.be.true;
    });
  });
});
