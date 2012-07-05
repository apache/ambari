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

renderManageServicesProgress = function (context) {

  var onSuccess = function (txnProgressWidget) {
    /* Resume polling for information about the cluster's services. */
    if (typeof fetchClusterServicesPoller != 'undefined') {
      fetchClusterServicesPoller.start();
    }
  };

  var onClose = function (e) {
    var uriPath = '/hmc/html/manageServices.php';
    var uriPathRegEx = new RegExp(uriPath);

    if (!window.location.pathname.match(uriPathRegEx)) {
      document.location.href = uriPath + '?clusterName=' + context.clusterName;
    }
  };

  var config = {
    context: context,
    title: 'Manage Services',
    onSuccess: onSuccess,
    onClose: onClose
  };

  var progressWidget = new App.ui.TxnProgressWidget(config);

  progressWidget.show();
};
