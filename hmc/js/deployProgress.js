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

renderDeployProgress = function (context) {

  /* At this point, our users are done with the installation wizard
   * and have asked for a deploy, so there's no going back - remove
   * all traces of #installationWizardProgressBarDivId.
   */
  var progressBarDiv = Y.one('#installationWizardProgressBarDivId');
  if (progressBarDiv) {
    progressBarDiv.hide();
  }

  var hmcRestartMsg = '';
  if (context.nagiosGangliaCoHosted != null && context.nagiosGangliaCoHosted) {
    hmcRestartMsg = '<span style="color:red"><strong>Note:</strong> You need to restart '
      + App.Props.managerServiceName + ' as Nagios/Ganglia are co-hosted on this server.<br>Restart '
      + App.Props.managerServiceName + ' by executing <i>sudo service ' + App.Props.managerServiceName.toLowerCase()
      + ' restart</i> from the command line.</span><br>After that is done, ';
  } else {
    hmcRestartMsg = 'Please ';
  }

  hmcRestartMsg +=
    '<a href="javascript:void(null)" id="clustersListLinkId">' +
      'click here to start managing your cluster.' +
      '</a>';

  var successMessage =
    '<p>' +
      'Your cluster is ready! <br/>' + hmcRestartMsg +
      '</p>';

  var failureMessage =
    '<p>' +
      'Failed to finish setting up the cluster.<br>Take a look at the ' +
      '<a href="javascript:void(null)" id="txnProgressWidgetShowLogsLink">deploy logs</a>' +
      ' to find out what might have gone wrong.' +
      '<a href="javascript:void(null)" class="btn btn-large" style="margin-top:10px" id="restartInstallationWizardLinkId">' +
      'Reinstall Cluster' +
      '</a>' +
      '</p>';

  var onSuccess = function (txnProgressWidget) {
    Y.one('#clustersListLinkId').on('click', function (e) {
      document.location.href = App.props.homeUrl;
    });
  };

  var onFailure = function (txnProgressWidget) {
    Y.one('#restartInstallationWizardLinkId').on('click', function (e) {
      document.location.href = App.props.homeUrl;
    });
  };

  var config = {
    context: context,
    title: 'Deployment Progress',
    successMessage: successMessage,
    failureMessage: failureMessage,
    onSuccess: onSuccess,
    onFailure: onFailure
  };

  var progressWidget = new App.ui.TxnProgressWidget(config);

  progressWidget.show();
};
