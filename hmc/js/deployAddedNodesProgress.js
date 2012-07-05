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

renderDeployAddedNodesProgress = function (context) {

  /* At this point, our users are done with the installation wizard
   * and have asked for a deploy, so there's no going back - remove
   * all traces of #installationWizardProgressBarDivId.
   */
  var progressBarDiv = Y.one('#installationWizardProgressBarDivId');
  if (progressBarDiv) {
    progressBarDiv.hide();
  }

  var successMessage =
    '<p>' +
      'Successfully added new nodes to your cluster.<br><a href="index.php" id="addMoreNodesSuccessLink" style="margin-top:10px" class="btn btn-large">Continue</a>' +
      '</p>';

  var failureMessage =
    '<p>' +
      'Failed to add new nodes to the cluster.<br>Take a look at the ' +
      '<a href="javascript:void(null)" id="txnProgressWidgetShowLogsLink">deploy logs</a>' +
      ' to find out what might have gone wrong.' +
      '<a href="index.php" class="btn btn-large" style="margin-top:10px" id="addMoreNodesFailedLink">' +
      'Continue' +
      '</a>' +
      '</p>';

  var config = {
    context: context,
    title: 'Add Nodes Progress',
    successMessage: successMessage,
    failureMessage: failureMessage
  };

  var progressWidget = new App.ui.TxnProgressWidget(config);

  progressWidget.show();
};
