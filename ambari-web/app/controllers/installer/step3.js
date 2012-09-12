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
  evaluateStep3: function () {
    // TODO: evaluation at the end of step3
    /* Not sure if below tasks are to be covered over here
     * as these functions are meant to be called at the end of a step
     * and the following tasks are interactive to the page and not on clicking next button.
     *
     * task1 will be a function called on entering step3 from step3 connectoutlet or init function in InstallerStep3 View.
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


  }
});
