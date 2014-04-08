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
require('views/wizard/step0_view');

var view, controller = Em.Object.create({
  clusterNameError: ''
});

describe('App.WizardStep0View', function () {

  beforeEach(function() {
    view = App.WizardStep0View.create({'controller': controller});
  });

  describe('#onError', function() {
    it('should be true if clusterNameError appears', function() {
      controller.set('clusterNameError', 'ERROR');
      expect(view.get('onError')).to.equal(true);
    });
    it('should be false if clusterNameError doesn\'t appears', function() {
      controller.set('clusterNameError', '');
      expect(view.get('onError')).to.equal(false);
    });
  });

});
