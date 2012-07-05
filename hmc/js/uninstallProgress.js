/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

renderUninstallProgress = function (context) {

  var successMessage =
    '<p>' +
      'Uninstalled the cluster successfully.' +
      '<a href="javascript:void(null)" style="margin-left:20px" class="btn btn-large" id="txnProgressWidgetCloseLink">' +
      'Continue' +
      '</a>' +
      '</p>';

  var failureMessage =
    '<p>' +
      'There was a problem with uninstall.<br />Take a look at ' +
      '<a href="javascript:void(null)" id="txnProgressWidgetShowLogsLink">Uninstall Logs</a>' +
      ' to see what might have happened.<br>' +
      '<a href="javascript:void(null)" class="btn btn-large" style="margin-top:10px" id="txnProgressWidgetCloseLink">' +
      'Close' +
      '</a>' +
      '</p>';

  var onClose = function (txnProgressWidget) {
    document.location.href = App.props.homeUrl;
  };

  var config = {
    context: context,
    title: 'Uninstall Cluster',
    successMessage: successMessage,
    failureMessage: failureMessage,
    onClose: onClose
  };

  var progressWidget = new App.ui.TxnProgressWidget(config);

  progressWidget.show();
};
