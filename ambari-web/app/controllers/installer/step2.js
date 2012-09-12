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

App.InstallerStep2Controller = Em.Controller.extend({
  name: 'installerStep2Controller',
  content: [],
  hostNames: '',
  hostNameArr: [],
  hostNameEmptyError: false,
  hostNameErr: false,
  manualInstall: false,
  hostNameNotRequireErr: false,
  sshKey: '',
  passphrase: '',
  confirmPassphrase: '',
  sshKeyNullErr: false,
  passphraseNullErr: false,
  passphraseMatchErr: false,
  localRepo: false,
  localRepoPath: '',
  softRepoLocalPathNullErr: false,
  showPopup: false,

  installType: function () {
    if (this.get('manualInstall') === true) {
      return 'manualDriven';
    } else {
      return 'ambariDriven';
    }
  }.observes('manualInstall'),

  hideRepoErrMsg: function () {
    if (this.get('localRepo') === false) {
      this.set('softRepoLocalPathNullErr', false);
    }
  }.observes('localRepo'),

  hostNamesErr: function () {
    if (this.get('hostNameEmptyError') || this.get('hostNameNotRequireErr') || this.get('hostNameErr')) {
      this.set('sshKeyNullErr', false);
      this.set('passphraseMatchErr', false);
      this.set('softRepoLocalPathNullErr', false);
      return true;
    } else {
      return false;
    }
  }.property('hostNameEmptyError', 'hostNameNotRequireErr', 'hostNameErr'),

  hostManageErr: function () {
    if (this.get('hostNameEmptyError') || this.get('hostNameNotRequireErr') || this.get('hostNameErr') || this.get('sshKeyNullErr') || this.get('passphraseMatchErr')) {
      return true;
    } else {
      return false
    }
  }.property('hostNameEmptyError', 'hostNameNotRequireErr', 'hostNameErr', 'sshKeyNullErr', 'passphraseMatchErr'),

  sshKeyErr: function () {
    if (this.get('sshKeyNullErr') === true) {
      this.set('passphraseMatchErr', false);
    }

  }.observes('sshKeyNullErr'),

  sshLessInstall: function () {
    if (this.get('manualInstall') === true) {
      this.set('hostManageErr', false);
      this.set('hostNameEmptyError', false);
      this.set('hostNameNotRequireErr', false);
      this.set('sshKeyNullErr', false);
      this.set('passphraseMatchErr', false);
    }
  }.observes('manualInstall'),

  advOptErr: function () {
    if (this.get('softRepoLocalPathNullErr')) {
      return true;
    } else {
      return false
    }
  }.property('softRepoLocalPathNullErr'),

  softRepo: function () {
    if (this.get('localRepo') === false) {
      this.set('localRepoPath', '');
    }
  }.observes('localRepo'),


  evaluateStep2: function () {
    // TODO:DONE
    //task1 = do primary validations on whole step before executing any further steps
    //task2 = parsing hostnames string to hostnames json array
    //task3 = check validation for every hostname and store it in localstorage
    //task4 = Storing ambari agent Install type in localStorage (installType maps at host level and so every host will have this as an property)
    //task5 = Storing path of software repository(remote/local repo) to localStorage
    //task6 = call to rest API: @Post http://ambari_server/api/bootstrap
    //task7 = On manual Install, next button click pops up a warning with "proceed" and "close" buttons
    //task8 =  On faliure of the previous call, show 'error injecting host information in server db'
    //task9 =  On success of the previous call, go to step 3(awesome....)

    console.log('TRACE: Entering controller:InstallerStep2:evaluateStep2 function');
    /**                 task1                **/
    console.log('value of manual install is: ' + this.get('manualInstall'));
    if (this.get('hostNames') === '' && this.get('manualInstall') === false) {
      this.set('hostNameEmptyError', true);
      this.set('hostNameNotRequireErr', false);
      return false;
    } else if (this.get('hostNames') !== '' && this.get('manualInstall') === true) {
      this.set('hostNameNotRequireErr', true);
      this.set('hostNameEmptyError', false);
      return false;
    } else {
      this.set('hostNameEmptyError', false);
      this.set('hostNameNotRequireErr', false);
    }

    if (this.get('manualInstall') === false) {
      if (this.get('sshKey') === '') {
        this.set('sshKeyNullErr', true);
        return false;
      }
      else {
        this.set('sshKeyNullErr', false);
      }
      if (this.get('passphrase') !== this.get('confirmPassphrase')) {
        this.set('passphraseMatchErr', true);
        return false;
      } else {
        this.set('passphraseMatchErr', false);
      }
    }

    if (this.get('localRepo') === true) {
      if (this.get('localRepoPath') === '') {
        this.set('softRepoLocalPathNullErr', true);
        return false;
      } else {
        this.set('softRepoLocalPathNullErr', false);
      }
    } else {
      this.set('softRepoLocalPathNullErr', false);
    }


    /**                 task2  task3 task4                      **/
    this.hostNameArr = this.get('hostNames').split('\s');
    for (var i = 0; i < this.hostNameArr.length; i++) {
      //TODO: other validation for hostnames will be covered over here
      // For now hostname that are starting or ending with '-' are not allowed
      if (/^\-/.test(this.hostNameArr[i]) || /\-$/.test(this.hostNameArr[i])) {
        console.log('Invalide host name' + this.hostNameArr[i]);
        alert('Invalide host name: ' + this.hostNameArr[i]);
        this.set('hostNameErr', true);
        return false;
      } else {
        this.set('hostNameErr', false);
      }
    }
    var hostInfo = {};
    for (var i = 0; i < this.hostNameArr.length; i++) {
      hostInfo[this.hostNameArr[i]] = {'name': this.hostNameArr[i]};
      // hostInfo[this.hostNameArr[i]].name =  this.hostNameArr[i];
      hostInfo[this.hostNameArr[i]].installType = this.get('installType');
    }
    App.db.setHosts(hostInfo);


    /**                     task5                    **/
    var repoType;
    var repoPath;
    if (this.get('localRepo') === false) {
      repoType = 'remote';
      repoPath = null;
    } else {
      repoType = 'local';
      repoPath = this.get('localRepoPath');
    }
    var softRepoInfo = {'type': repoType, 'path': repoPath};
    App.db.setSoftRepo(softRepoInfo);


    /**                      task6                  **/


    if (this.get('manualInstall') === false) {
      // For now using mock jquery call
      //TODO: hook up with bootstrap call
      var bootStrapData = {'sshKey': this.get('sshKey'), 'sshKeyPassphrase': this.get('passphrase'), hosts: this.get('hostNameArr')}.stringify;
      $.ajax({
        type: 'POST',
        url: '/ambari_server/api/bootstrap',
        data: bootStrapData,
        async: false,
        timeout: 2000,
        success: function () {
          console.log("TRACE: In success function for the post bootstrap function");
          return true;
        },
        error: function () {
          console.log("ERRORRORR");
          return false;
        },
        statusCode: {
          404: function () {
            console.log("URI not found.");
            alert("URI not found");
            //After the bootstrap call hook up change the below return statement to "return false"
            return true;
          }
        },
        dataType: 'application/json'
      });
    } else {
      //popup window for "manual install" caution. if "OK" button is hit return true else return false
      console.log('In showpopup function');
      this.set('showPopup', true);

      return true;
    }

    console.log("TRACE: program should never reach over here!!!:( ");

    if ('success' == 'success') {
      return true;
    } else {
      return false;
    }
  }
});