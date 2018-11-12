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



describe('App.StatusIconView', function () {
  var view;
  beforeEach(function () {
    view = App.StatusIconView.create({});
  });

  describe('#data-original-title', function () {
    it('should return capital letters content', function () {
      view.set('content', 'init');
      expect(view.get('data-original-title')).to.be.equal('Init');
    });
  });

  describe('#iconClass', function() {
    it('should return class from statusIconMap if it is enable', function () {
      var statusIconMap = view.get('statusIconMap');
      for (var key in statusIconMap) {
        view.set('content', key);
        expect(view.get('iconClass')).to.be.equal(statusIconMap[key]);
      }
    });

    it('should return correct default icon class if there is no such status in map', function () {
      view.set('content', 'test');
      expect(view.get('iconClass')).to.be.equal('glyphicon glyphicon-question-sign');
    });
  });
});