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

/**
 * Show alert popup
 *
 * @param {String} header - header of the popup
 * @param {String} body - body of the popup
 * @param {Function} primary - function to call upon clicking the OK button
 * @return {*}
 */
App.showAlertPopup = function (header, body, primary) {
  return App.ModalPopup.show({
    primary: Em.I18n.t('ok'),
    secondary: null,
    header: header,
    body: body,
    onPrimary: function () {
      this.hide();
      if (primary) {
        primary();
      }
    }
  });
};
