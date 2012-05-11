function renderClusterList(Y) {
  Y.io("../php/frontend/listClusters.php", {
    method: 'GET',
    timeout : 10000,
    on: {
      success: function (x,o) {
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
          clustersListMarkup = "You don't have any installed cluster.";
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
              clusterInfo = clusterListInfoJson[clusterName];
            }
            clustersListMarkup = '<h3>Cluster information</h3>';
            clustersListMarkup += '<div class="clusterDiv">' +
                                   '<div class="formElement">' +
                                     '<label>ClusterName</label>' +
                                     '<input type=text readonly=readonly value=' + clusterName + '>' +
                                   '</div>' +
                                   '<div class="formElement">' +
                                     '<label>State</label>' +
                                     '<input type=text readonly=readonly value=' + clusterInfo + '>' +
                                   '</div>' +
                                   '<div class="formElement">' +
                                     '<label>Monitoring dashboard</label>' +
                                     '<a href="#">monitoringDashboard.com:4060</a>' +
                                   '</div>' +
                                   '<div class="formElement">' +
                                     '<label>Actions</label>' +
                                     '<a href="manageServices.php?clusterName=' + clusterName + '" id="existingClusterLinkDivId">[ Manage services ]</a>' +
                                     '<a href="addNodesWizard.php?clusterName=' + clusterName + '">[ Add slave nodes to cluster ]</a>' +
                                   '</div>' +
                                 '</div>';
          }
        }

        var newClusterLinkHTML = "<br/>Want to create a new cluster? Click <a href='initializeCluster.php' id='newClusterLinkDivId'>here</a> ";
        if (multipleClustersSupported || numClusters == 0) {
          clustersListMarkup += newClusterLinkHTML;
        }

        Y.one("#clustersListDivId").setContent( clustersListMarkup );

        if (Y.one('#newClusterLinkDivId') != null) {
          Y.one('#newClusterLinkDivId').on('click',function (e) {
            /* Done with this stage, hide it. */
            Y.one("#clustersListDivId").setStyle('display','none');
            // Y.one("#installationWizardDivId").setStyle('display','block');
        });
        }

        if(numClusters !=0) {
          Y.one('#existingClusterLinkDivId').on('click',function (e) {

            e.target.set('disabled', true);

            /* Done with this stage, hide it. */
            Y.one("#clustersListDivId").setStyle('display','none');

            /* Render the next stage. */
            getServicesStatus(Y, clusterId);

            /* Show off our rendering. */
            Y.one("#displayServiceStatusCoreDivId").setStyle('display','block');
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

// Create business logic in a YUI sandbox using the 'io' and 'json' modules
YUI().use("node", "io", "dump", "json", "arraysort", "panel", function (Y) {

  Y.log( Y.one("#pageTitleId").get('innerHTML') );

  // Y.one('#createClusterSubmitButtonId').on('click',function (e) {
    renderClusterList(Y);

});
