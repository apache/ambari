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

function getView() {
  return App.MainHostStackVersionsView.create({
    filteredCount: 0,
    totalCount: 0
  });
}
var view;
describe('App.MainHostStackVersionsView', function() {

  beforeEach(function () {
    view = getView();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'host', 'App.router.mainHostDetailsController.content', 'object');

  App.TestAliases.testAsComputedFilterBy(getView(), 'content', 'host.stackVersions', 'isVisible', true);

  describe("#filteredContentInfo", function () {
    it("formatted with filteredCount and totalCount", function () {
      view.set('filteredCount', 1);
      view.set('totalCount', 2);
      view.propertyDidChange('filteredContentInfo');
      expect(view.get('filteredContentInfo')).to.eql(Em.I18n.t('hosts.host.stackVersions.table.filteredInfo').format(1, 2));
    });
  });
});
