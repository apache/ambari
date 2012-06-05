function generateClusterHostRoleMappingMarkup( clusterServices ) {

  var clusterHostRoleMappingMarkup = 
  '<fieldset id=clustersHostRoleMappingFieldsetId>' +
    '<legend>' +
      'Locations Of Service Masters' + 
    '</legend>';

  for (var serviceName in clusterServices) {
    if (clusterServices.hasOwnProperty(serviceName)) {

      if (clusterServices[serviceName].isEnabled == "1" && 
          clusterServices[serviceName].attributes.runnable &&
          !clusterServices[serviceName].attributes.noDisplay) {

        globalYui.Array.each( clusterServices[serviceName].components, function (serviceComponent) {
          
          if (serviceComponent.isMaster) {
            clusterHostRoleMappingMarkup += 
              '<div class=formElement>' + 
                '<label>' + serviceComponent.displayName + ': ' + '</label>' + 
                serviceComponent.hostName +
              '</div>' + 
              '<br/>';
          }
        });
      }
    }
  }

  clusterHostRoleMappingMarkup += 
  '</fieldset>';

  return clusterHostRoleMappingMarkup;
}

function renderClusterList() {
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
            clustersListMarkup = '<h3>Cluster information</h3>';
            clustersListMarkup += '<div class="clusterDiv">' +
                                   '<div class="formElement">' +
                                     '<label>Cluster Name</label>' +
                                     '<input type=text readonly=readonly value="' + clusterName + '">' +
                                   '</div>' +
                                   '<div class="formElement">' +
                                     '<label>State</label>' +
                                     '<input type=text readonly=readonly value="' + clusterInfo['displayName'] + '">' +
                                   '</div>' +
                                   '<div class="formElement">' +
                                     '<a class="btn" href="/hdp/dashboard/ui/home.html">Monitoring Dashboard</a>' +
                                     '<a class="btn" style="margin-left:10px" href="manageServices.php?clusterName=' + clusterName + '" id="existingClusterLinkDivId">Manage Services</a>' +
                                     '<a class="btn" style="margin-left:10px" href="addNodesWizard.php?clusterName=' + clusterName + '">Add Nodes</a>' +
                                     '<a class="btn" style="margin-left:10px" href="uninstallWizard.php?clusterName=' + clusterName + '">Uninstall</a>' +
                                   '</div>' +
                                 '</div>';
          }
        }

        var newClusterLinkHTML = "";
        if (multipleClustersSupported || numClusters == 0) {
          clustersListMarkup += newClusterLinkHTML;
          globalYui.one("#welcomeDivId").setStyle('display','block');
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

                /* Link the newly-generated markup into the DOM. */
                globalYui.one("#clusterHostRoleMappingDynamicRenderDivId").setContent
                  ( generateClusterHostRoleMappingMarkup(clusterServices) );
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

            /* Render the next stage. */
            getServicesStatus(globalYui, clusterId);

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


/* Main() */
renderClusterList();
