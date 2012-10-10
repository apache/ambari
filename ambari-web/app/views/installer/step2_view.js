/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


var App = require('app');

App.InstallerStep2View = Em.View.extend({

  templateName: require('templates/installer/step2'),
  hostNameErr: false,

  didInsertElement: function () {
    $("[rel=popover]").popover({'placement': 'right', 'trigger': 'hover'});
    this.set('hostNameErr',false);
    this.set('controller.passphraseMatchErr',false);
    this.set('controller.sshKeyNullErr',false);
    this.get('controller').loadStep();
  },


  onHostNameErr: function () {
    if (this.get('controller.hostNameEmptyError') === false && this.get('controller.hostNameNotRequiredErr') === false && this.get('controller.hostNameErr') === false) {
      this.set('hostNameErr',false);
    } else {
      this.set('hostNameErr',true);
    }
  }.observes('controller.hostNameEmptyError', 'controller.hostNameNotRequiredErr', 'controller.hostNameErr'),


});


