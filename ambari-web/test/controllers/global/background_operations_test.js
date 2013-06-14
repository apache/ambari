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

require('config');
require('utils/updater');
require('utils/ajax');

require('models/host_component');

require('controllers/global/background_operations_controller');
require('views/common/modal_popup');
require('utils/host_progress_popup');

describe('App.BackgroundOperationsController', function () {

  /**
   * Predefined data
   *
   */
  App.set('clusterName', 'testName');
  App.bgOperationsUpdateInterval = 100;

  /**
   * Test object
   */
  var controller = App.BackgroundOperationsController.create();

  describe('when set isWorking to true  ', function () {

    it('startPolling executes App.updater.run  ', function(done){
      sinon.stub(App.updater, 'run', function(){
        controller.set('isWorking', false);
        App.updater.run.restore();
        done();
      });

      controller.set('isWorking', true);
    });

    it('requestMostRecent should be called  ', function(done){
      this.timeout(App.bgOperationsUpdateInterval + 500);

      sinon.stub(controller, 'requestMostRecent', function(){
        App.set('testMode', true);
        controller.set('isWorking', false);
        controller.requestMostRecent.restore();
        done();
      });

      controller.set('isWorking', true);
    });

    it('callBackForMostRecent should be called  ', function(done){
      this.timeout(App.bgOperationsUpdateInterval + 1500);

      sinon.stub(controller, 'callBackForMostRecent', function(){
        controller.set('isWorking', false);
        controller.callBackForMostRecent.restore();
        done();
      });

      controller.set('isWorking', true);
    });

  });

});
