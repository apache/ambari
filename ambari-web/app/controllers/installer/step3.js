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

App.InstallerStep3Controller = Em.Controller.extend({
  name: 'installerStep3Controller',
  content: [],
  /*
   This flag is "true" While bootstrapping is in process.
   Parsing function or timeout on bootstrap rest call can make it false.
   */
  bootstrap: '',

  /*
   * Below function will be called on successfully leaving step2 and entering
   * step3. "Retry" button shall also make use of it.
   */

  startBootstrap: function () {
    console.log("TRACE: Entering controller->installer->step3->startBootstrap() function");
    App.bootstrap = self.setInterval(function () {
      this.doBootstrap()
    }, 200);
  },

  stopBootstrap: function () {
    window.clearInterval(App.bootstrap);
  },

  doBootstrap: function () {
    $.ajax({
      type: 'GET',
      url: '/ambari_server/api/bootstrap',
      async: false,
      timeout: 2000,
      success: function () {
        console.log("TRACE: In success function for the GET bootstrap call");
      },
      error: function () {
        console.log("ERRORRORR");
        self.set('bootstrap', false);  //Never toggle this for now, flow goes in infinite loop
        this.stopBootstrap();
      },
      statusCode: {
        404: function () {
          console.log("URI not found.");
          alert("URI not found");
          result = false;
        }
      },
      dataType: 'application/json'
    });

  },

  retry: function () {
    this.doBootstrap();
  },


  evaluateStep3: function () {
    // TODO: evaluation at the end of step3
    /* Not sure if below tasks are to be covered over here
     * as these functions are meant to be called at the end of a step
     * and the following tasks are interactive to the page and not on clicking next button.
     *
     *
     * task2 will be a parsing function that on reaching a particular condition(all hosts are in success or faliue status)  will stop task1
     * task3 will be a function binded to remove button
     * task4 will be a function binded to retry button
     *
     *
     * keeping it over here for now
     */


    //task1 = start polling with rest API @Get http://ambari_server/api/bootstrap.
    //task2 = stop polling when all the hosts have either success or failure status.
    //task3(prerequisite = remove) = Remove set of selected hosts from the localStorage
    //task4(prerequisite = retry) = temporarily store list of checked host and call to rest API: @Post http://ambari_server/api/bootstrap

    return true;
  }
});

