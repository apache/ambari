function ClustersList() { 
  function populateHostToMasterRoleMapping(clusterServices, hostMap) {
    
    for (var serviceName in clusterServices) {
      if (clusterServices.hasOwnProperty(serviceName)) {
  
        if (clusterServices[serviceName].isEnabled == "1" && 
            clusterServices[serviceName].attributes.runnable &&
            !clusterServices[serviceName].attributes.noDisplay) {
  
          globalYui.Array.each( clusterServices[serviceName].components, function (serviceComponent) {
            if (serviceComponent.isMaster) {
              // just add the master to the hostname object
              for (var i in serviceComponent.hostNames) {
                var hostName = serviceComponent.hostNames[i];
                if ( !( hostName in hostMap ) ) {
                  hostMap[hostName] = new Array();
                  hostMap[hostName].push({ serviceName: serviceComponent.displayName, isMaster: true });
                } else {
                  hostMap[hostName].push({ serviceName: serviceComponent.displayName, isMaster: true });
                }
              }
            }
          });
        }
      }
    }
  }
  
  function populateHostToClientRoleMapping(clusterServices, hostMap) {
  
    for (var serviceName in clusterServices) {
      if (clusterServices.hasOwnProperty(serviceName)) {
  
        if (clusterServices[serviceName].isEnabled == "1" && 
            !clusterServices[serviceName].attributes.noDisplay) {
  
          globalYui.Array.each( clusterServices[serviceName].components, function (serviceComponent) {          
            if (serviceComponent.isClient) {
              // just add the client to the hostname object
              for (var i in serviceComponent.hostNames) {
                var hostName = serviceComponent.hostNames[i];
                if ( !( hostName in hostMap ) ) {
                  hostMap[hostName] = new Array();
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
      markup += '<div class="hostToServices"><h3>' + hostName + '</h3>' + '<ul>';
      for (var service in hostMap[hostName]) {
        markup += '<li class="' + ((hostMap[hostName][service].isMaster) ? 'master' : 'client') + '">' + hostMap[hostName][service].serviceName + '</li>';
      }
      markup += '</ul><div style="clear:both"></div></div>';
    }
    markup += '</div>';
  
    return markup;
  }
  
  this.render = function() {
    
    globalYui.io("../php/frontend/listClusters.php", {
      method: 'GET',
      timeout : 10000,
      on: {
        success: function (x,o) {
          globalYui.log("RAW JSON DATA: " + o.responseText);
  
          // Process the JSON data returned from the server
          try {
            clusterListInfoJson = globalYui.JSON.parse(o.responseText);
          }
          catch (e) {
            alert("JSON Parse failed!");
            return;
          }
          
          globalYui.log("PARSED DATA: " + globalYui.Lang.dump(clusterListInfoJson));
  
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
              var i = 0;
              for (clusterId in clusterListInfoJson) {
                clustersListMarkup += "<tr><td><a href='manageServices.php?clusterId=" + clusterId + "' id='existingClusterLinkDivId'>" + clusterId + "</a></td><td>" + clusterListInfoJson[clusterId] + "</td><td>Uninstall</td></tr>" ;
              }
              clustersListMarkup += "</table>";
            } else {
              var clusterName; var clusterInfo;
              for (clusterId in clusterListInfoJson) {
                clusterName = clusterId;
                clusterInfo = globalYui.JSON.parse(clusterListInfoJson[clusterName]);
                globalYui.log( "Cluster Info: " + globalYui.Lang.dump(clusterInfo.displayName));
              }
            }
          }
  
          var newClusterLinkHTML = "";
          if (multipleClustersSupported || numClusters == 0) {
            document.location.href = "/hmc/html/welcome.php";
            return;
          }
  
          /* Beginning of adding Role Topology information. */
          globalYui.io( "../php/frontend/fetchClusterServices.php?clusterName=" + clusterName + "&getConfigs=true&getComponents=true", {
            timeout: 10000,
            on: {
              success: function(x1, o1) {
  
                hideLoadingImg();
  
                globalYui.log("RAW JSON DATA: " + o1.responseText);
  
                var clusterServicesResponseJson;
  
                try {
                  clusterServicesResponseJson = globalYui.JSON.parse(o1.responseText);
                }
                catch (e) {
                  alert("JSON Parse failed");
                  return;
                }
  
                globalYui.log(globalYui.Lang.dump(clusterServicesResponseJson));
  
                /* Check that clusterServicesResponseJson actually indicates success. */
                if( clusterServicesResponseJson.result == 0 ) {
                  
                  var clusterServices = clusterServicesResponseJson.response.services;
  
                  var markup =
                    '<div class="clearfix">' +
                      '<h2>Cluster: ' +  clusterName + '</h2>' +
                        '<div id="serviceLegend">' +
                          '<span class="masterLegend">Master</span><span class="clientLegend" style="margin-right:0">Client</span>' +
                        '</div>' +
                      '</div>' +
                    '</div>';
                  
                  /* Link the newly-generated markup into the DOM. */
                  globalYui.one("#clusterHostRoleMappingDynamicRenderDivId").setContent(
                      markup + generateHostRoleMappingMarkup(clusterServices) );
                  globalYui.one("#clusterHostRoleMappingDivId").show();
                }
                else {
                  alert("Fetching Cluster Services failed");
                }
              },
              failure: function(x1, o1) {
                hideLoadingImg();
                alert("Async call failed");
              }
            }
          });
          /* End of adding Role Topology information. */
  
          globalYui.one("#clustersListDivId").setContent( clustersListMarkup );
          globalYui.one("#clustersListDivId").setStyle('display', 'block');
  
          if (globalYui.one('#newClusterLinkDivId') != null) {
            globalYui.one('#newClusterLinkDivId').on('click',function (e) {
              /* Done with this stage, hide it. */
              globalYui.one("#clustersListDivId").setStyle('display','none');
              // globalYui.one("#installationWizardDivId").setStyle('display','block');
          });
          }
  
          if(numClusters !=0) {
            globalYui.one('#existingClusterLinkDivId').on('click',function (e) {
  
              e.target.set('disabled', true);
  
              /* Done with this stage, hide it. */
              globalYui.one("#clustersListDivId").setStyle('display','none');
  
              /* Show off our rendering. */
              globalYui.one("#displayServiceStatusCoreDivId").setStyle('display','block');
            });
         }
  
        },
        failure: function (x,o) {
          //    e.target.set('disabled', false);
          alert("Async call failed!");
        }
      }
    });
  }
};

var clustersList = new ClustersList();
clustersList.render();
