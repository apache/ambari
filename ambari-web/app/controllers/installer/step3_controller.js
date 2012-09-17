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

App.InstallerStep3Controller = Em.ArrayController.extend({
  name: 'installerStep3Controller',
  hostInfo: [],
  hostNames: [],
  category: 'All',
  content: [],
  /*
   This flag is "true" While bootstrapping is in process.
   Parsing function or timeout on bootstrap rest call can make it false.
   */
  bootstrap: '',
  mockData:{},


  renderHosts: function() {
    var hostInfo = [];
    hostInfo = App.db.getHosts();
    this.hostNames.clear();
    this.clear();

    this.hostNames = new Ember.Set();
    for(var index in hostInfo) {
      this.hostNames.add(hostInfo[index].name);
     // alert(hostInfo[index].name);
    }
    /*hostInfo.forEach(function(_hostNames) {
      hostNames.add = _hostNames.name;
    });*/
    console.log("TRACE: step3->controller->renderHosts");
/*
   this.hostInfo = [
    {
      hostName: 'jaimin',
      status:'success'
    },
    {
      hostName: 'jetly',
      status:'success'
    },
    {
      hostName: 'villa',
      status:'Verifying SSH connection'
    },
    {
      hostName: 'jack',
      status:'SSH connection failed'
    },
    {
      hostName: 'george',
      status:'success'
    },
    {
      hostName: 'maria',
      status:'success'
    },
    {
      hostName: 'adam',
      status:'Verifying SSH connection'
    },
    {
      hostName: 'jennifer',
      status:'SSH connection failed'
    },
    {
      hostName: 'john',
      status:'success'
    },
    {
      hostName: 'tom',
      status:'success'
    },
    {
      hostName: 'harry',
      status:'success'
    },
    {
      hostName: 'susan',
      status:'success'
    }
  ];
  */
  var self = this;
    this.hostNames.forEach(function(_hostInfo) {
        var hostInfo = App.HostInfo.create({
          hostName: _hostInfo
        });

      console.log('pushing ' + hostInfo.hostName);
      //self.set('content',hostInfo);
      //self.replaceContent(0, hostInfo.get('length'), hostInfo);
      self.content.pushObject(hostInfo);
    });

  //this.startBootstrap();
  },

  /*
   * Below function will be called on successfully leaving step2 and entering
   * step3. "Retry" button shall also make use of it.
   */

  startBootstrap: function () {
    console.log("TRACE: Entering controller->installer->step3->startBootstrap() function");
    var self = this;
    this.set('bootstrap',window.setInterval(function () {
      self.doBootstrap()
    }, 5000));
  },


  stopBootstrap: function () {
    window.clearInterval(this.bootstrap);
  },

  doBootstrap: function () {
    var self = this;
    $.ajax({
      type: 'GET',
      url: '/ambari_server/api/bootstrap',
      async: false,
      timeout: 2000,
      success: function (data) {
        console.log("TRACE: In success function for the GET bootstrap call");


      },
      error: function () {
        console.log("ERROR");

        self.stopBootstrap(); //Never toggle this for now, flow goes in infinite loop
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

  remove: function () {

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

