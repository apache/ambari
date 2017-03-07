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


import Ember from 'ember';

/** Example :
 * {{#validated-text-field
 * inputValue=bindedTextValue invalidClass='form-control red-border' validClass='form-control' regex="^[a-z]+$"
 * allowEmpty=false tooltip="Enter valid word" errorMessage="Please enter valid word" placeholder="Enter Word"}}
 * {{/validated-text-field}}
 */
export default Ember.Component.extend({
  classNameBindings: ['tagClassName'],
  tagClassName : false, // set it to non false value if you want a specific class to be assigned
  allowEmpty: true,
  valid: true,
  setValid: function () {
    this.set("valid", true);
    this.set("inputClass", this.get("validClass"));
    this.set("message", this.get("tooltip"));
  },
  setInvalid: function () {
    this.set("valid", false);
    this.set("inputClass", this.get("invalidClass"));
    this.set("message", this.get("errorMessage"));
  },
  onChangeInputValue: function () {
    var regStr = this.get("regex");
    var regExp = new RegExp(regStr, "g");
    if (this.get("inputValue")) {
      var arr = this.get("inputValue").match(regExp);
      if (arr != null && arr.length === 1) {
        this.setValid();
      }
      else {
        this.setInvalid();
      }
    } else {
      if (this.get("allowEmpty")) {
        this.setValid();
      } else {
        this.setInvalid();
      }
    }
  }.observes("inputValue").on('init')
});
