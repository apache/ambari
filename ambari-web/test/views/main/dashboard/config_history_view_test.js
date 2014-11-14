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
require('views/main/dashboard/config_history_view');

describe('App.MainConfigHistoryView', function() {
  var view = App.MainConfigHistoryView.create({
    controller: Em.Object.create({
      name: 'mainConfigHistoryController',
      paginationProps: [
        {
          name: 'displayLength'
        },
        {
          name: 'startIndex'
        }
      ],
      doPolling: Em.K,
      load: function () {
        return {done: Em.K};
      }
    }),
    filteredCount: 0
  });
  view.removeObserver('controller.resetStartIndex', view, 'resetStartIndex');

  describe('#didInsertElement()', function() {
    it('', function() {
      sinon.stub(view, 'addObserver', Em.K);
      sinon.spy(view.get('controller'), 'doPolling');

      view.didInsertElement();
      expect(view.addObserver.calledTwice).to.be.true;
      expect(view.get('controller.isPolling')).to.be.true;
      expect(view.get('controller').doPolling.calledOnce).to.be.true;

      view.addObserver.restore();
      view.get('controller').doPolling.restore();
    });
  });

  describe('#willDestroyElement()', function() {
    it('', function() {
      view.willDestroyElement();
      expect(view.get('controller.isPolling')).to.be.false;
    });
  });
  describe('#refresh()', function() {
    it('', function() {
      sinon.spy(view.get('controller'), 'load');
      view.refresh();
      expect(view.get('filteringComplete')).to.be.false;
      expect(view.get('controller').load.calledOnce).to.be.true;
      view.get('controller').load.restore();
    });
  });
});
