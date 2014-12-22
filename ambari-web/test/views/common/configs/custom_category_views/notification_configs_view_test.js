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
require('views/common/configs/custom_category_views/notification_configs_view');
var view;

describe('App.NotificationsConfigsView', function () {

  beforeEach(function () {
    view = App.NotificationsConfigsView.create({
      $: function() {
        return {show: Em.K, hide: Em.K};
      },
      category: {
        name: 'name'
      },
      serviceConfigs: [],
      parentView: Em.View.create({
        filter: '',
        columns: []
      })
    });
  });

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.stub(view, 'updateCategoryConfigs', Em.K);
    });

    afterEach(function () {
      view.updateCategoryConfigs.restore();
    });

    it('should not do nothing if no configs', function () {

      view.set('categoryConfigsAll', []);
      view.didInsertElement();
      expect(view.updateCategoryConfigs.called).to.equal(false);

    });

  });

});
