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
                                     '<a href="/hdp/dashboard/ui/home.html">Show Monitoring Dashboard</a>' +
                                   '</div>' +
                                   '<div class="formElement">' +
                                     '<label>Actions</label>' +
                                     '<a class="btn" href="manageServices.php?clusterName=' + clusterName + '" id="existingClusterLinkDivId">Manage services</a>' +
                                     '<a class="btn" style="margin-left:10px" href="addNodesWizard.php?clusterName=' + clusterName + '">Add slave nodes to cluster</a>' +
                                     '<a class="btn" style="margin-left:10px" href="uninstallWizard.php?clusterName=' + clusterName + '">Uninstall cluster</a>' +
                                   '</div>' +
                                 '</div>';
          }
        }

        var newClusterLinkHTML = "<div class='alert alert-info' style='margin-top:40px;padding:20px'>" +
        	"<h2 style='margin-bottom:10px'>Welcome to Hortonworks Management Center!</h2>" +
        	"<p>Hortonworks Management Center makes it really easy for you to install, configure, and manage your Hadoop cluster.</p>" +
        	"<p>First, we'll walk you through the cluster set up with a 7-step wizard.</p>" +
        	"<p><span class='label label-info'>Note</span><span style='margin-left:10px;'>Before you proceed, make sure you have performed all the pre-installation steps (REFERENCE MATERIAL HERE).</span></p>" + 
        	"<a class='btn btn-large' style='margin-top:20px' href='initializeCluster.php'>Let's Get Started!</a>" +
        	"</div>";
        if (multipleClustersSupported || numClusters == 0) {
          clustersListMarkup += newClusterLinkHTML;
        }
        
        // document.location.href = "initializeCluster.php";

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
