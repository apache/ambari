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

function ClustersList() {

  var managerHostName = '';

  function populateHostToMasterRoleMapping(clusterServices, hostMap) {

    var hostName;

    for (var serviceName in clusterServices) {
      if (clusterServices.hasOwnProperty(serviceName)) {

        if (clusterServices[serviceName].isEnabled == "1" &&
          clusterServices[serviceName].attributes.runnable &&
          !clusterServices[serviceName].attributes.noDisplay) {

          Y.Array.each( clusterServices[serviceName].components, function (serviceComponent) {
            if (serviceComponent.isMaster) {
              // just add the master to the hostname object
              for (var i in serviceComponent.hostNames) {
                hostName = serviceComponent.hostNames[i];
                if ( !( hostName in hostMap ) ) {
                  hostMap[hostName] = [];
                }
                hostMap[hostName].push({ serviceName: serviceComponent.displayName, isMaster: true });
              }
            }
          });
        }
      }
    }
    hostName = managerHostName;
    if (!(hostName in hostMap)) {
      hostMap[hostName] = [];
    }
    hostMap[hostName].push({ serviceName: App.props.managerServiceName + ' Server', isMaster: true });
  }

  function populateHostToClientRoleMapping(clusterServices, hostMap) {

    for (var serviceName in clusterServices) {
      if (clusterServices.hasOwnProperty(serviceName)) {

        if (clusterServices[serviceName].isEnabled == "1" &&
          !clusterServices[serviceName].attributes.noDisplay) {

          Y.Array.each( clusterServices[serviceName].components, function (serviceComponent) {
            if (serviceComponent.isClient) {
              // just add the client to the hostname object
              for (var i in serviceComponent.hostNames) {
                var hostName = serviceComponent.hostNames[i];
                if ( !( hostName in hostMap ) ) {
                  hostMap[hostName] = [];
                  hostMap[hostName].push({ serviceName: serviceComponent.displayName, isMaster: false });
                } else {
                  hostMap[hostName].push({ serviceName: serviceComponent.displayName, isMaster: false });
                }
              }
            }
          });
        }
      }
    }
  }

  function generateHostRoleMappingMarkup( clusterServices ) {

    var hostMap = {};
    var markup = '';

    populateHostToMasterRoleMapping(clusterServices, hostMap);
    populateHostToClientRoleMapping(clusterServices, hostMap);

    markup += '<div>';
    for (var hostName in hostMap) {
      markup += '<div class="hostToServices clearfix"><h3>' + hostName + '</h3>' + '<ul>';
      for (var service in hostMap[hostName]) {
        markup += '<li class="' + ((hostMap[hostName][service].isMaster) ? 'master' : 'client') + '">' + hostMap[hostName][service].serviceName + '</li>';
      }
      markup += '</ul></div>';
    }
    markup += '</div>';

    return markup;
  }

  this.render = function() {

    Y.io("/hmc/php/frontend/listClusters.php", {
      method: 'GET',
      timeout : App.io.DEFAULT_AJAX_TIMEOUT_MS,
      on: {
        success: function (x,o) {
          var clusterListInfoJson;
          
          Y.log("RAW JSON DATA: " + o.responseText);

          // Process the JSON data returned from the server
          try {
            clusterListInfoJson = Y.JSON.parse(o.responseText);
          }
          catch (e) {
            alert("JSON Parse failed!");
            return;
          }

          Y.log("PARSED DATA: " + Y.Lang.dump(clusterListInfoJson));

          if (clusterListInfoJson.result != 0) {
            // Error!
            alert("Got error!" + clusterListInfoJson.error);
            return;
          }

          clusterListInfoJson = clusterListInfoJson.response;
          var numClusters = clusterListInfoJson.length;
          var clustersListMarkup;
          var clusterId;
          var multipleClustersSupported = false;

          if (numClusters == 0) {
            clustersListMarkup = "";
          } else {
            if (multipleClustersSupported) {
              clustersListMarkup =    "<table>" +
                "<caption>List of clusters</caption>" +
                "<thead><tr><th>Name of the cluster</th><th>Cluster status</th><th>Actions</th></tr></thead>";

              for (clusterId in clusterListInfoJson) {
                clustersListMarkup += "<tr><td><a href='manageServices.php?clusterId=" + clusterId + "' id='existingClusterLinkDivId'>" + clusterId + "</a></td><td>" + clusterListInfoJson[clusterId] + "</td><td>Uninstall</td></tr>" ;
              }
              clustersListMarkup += "</table>";
            } else {
              var clusterName; var clusterInfo;
              for (clusterId in clusterListInfoJson) {
                clusterName = clusterId;
                clusterInfo = Y.JSON.parse(clusterListInfoJson[clusterName]);
                Y.log( "Cluster Info: " + Y.Lang.dump(clusterInfo.displayName));
              }
            }
          }

          var newClusterLinkHTML = "";
          if (multipleClustersSupported || numClusters == 0) {
            document.location.href = "/hmc/html/welcome.php";
            return;
          }

          /* Beginning of adding Role Topology information. */
          Y.io( "/hmc/php/frontend/fetchClusterServices.php?clusterName=" + clusterName + "&getConfigs=true&getComponents=true", {
            timeout: App.io.DEFAULT_AJAX_TIMEOUT_MS,
            on: {
              success: function(x1, o1) {

                App.ui.hideLoadingOverlay();

                Y.log("RAW JSON DATA: " + o1.responseText);

                var responseJson;

                try {
                  responseJson = Y.JSON.parse(o1.responseText);
                }
                catch (e) {
                  alert("JSON Parse failed");
                  return;
                }

                managerHostName = responseJson.response.managerHostName;

                Y.log(Y.Lang.dump(responseJson));

                /* Check that responseJson actually indicates success. */
                if (responseJson.result == 0) {

                  var clusterServices = responseJson.response.services;
                  var versionInfo = responseJson.response.versionInfo;

                  var clusterInfoMarkup =
                    '<div><label>Cluster:</label> ' + clusterName + '</div>' +
                    '<div><label>Hadoop Stack:</label> HDP ' + versionInfo.currentStackVersion +
                    ((App.util.compareVersionStrings(versionInfo.currentStackVersion, versionInfo.latestStackVersion) < 0) ? ' (<a href="upgradeStack">Upgrade available</a>)' : '') +
                    '</div>' +
                    '<div style="clear:both"></div>';

                  var markup =
                    '<div class="clearfix">' +
                      '<div id="serviceLegend">' +
                      '<span class="masterLegend">Master</span><span class="clientLegend">Client</span>' +
                      '</div>' +
                      '</div>' +
                      '</div>';

                  Y.one('#clusterInfoContent').setContent(clusterInfoMarkup);

                  Y.one("#clusterHostRoleMappingContent").setContent(
                    markup + generateHostRoleMappingMarkup(clusterServices) );
                  Y.one("#clusterHostRoleMapping").show();
                }
                else {
                  alert("Fetching Cluster Services failed");
                }
              },
              failure: function(x1, o1) {
                App.ui.hideLoadingOverlay();
                alert(App.io.DEFAULT_AJAX_ERROR_MESSAGE);
              }
            }
          });
          /* End of adding Role Topology information. */

        },
        failure: function (x,o) {
          //    e.target.set('disabled', false);
          alert(App.io.DEFAULT_AJAX_ERROR_MESSAGE);
        }
      }
    });
  }; // end render
};

new ClustersList().render();

