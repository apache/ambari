function getServicesStatus(clusterName) {
  YUI().use("node", "dump", "io", "datatable", "json", "arraysort", "datasource", "datasource-io", "datasource-jsonschema", "datasource-polling", function (Y) {

     // setNavigationContent(Y, "Clusters > Cluster - " + clusterName);

     var id;
     var errorCount = 0;
     var myDataSource = new Y.DataSource.IO({source:"../php/frontend/servicesStatus.php?clusterName=" + clusterName });
     myDataSource.plug(Y.Plugin.DataSourceJSONSchema, {
        schema: {
            metaFields: { serviceStatus: "serviceStatus" }
        }
     });
 
    Y.one("#displayServiceStatusContentDivId").setContent("Getting status of all services");

     var callback = {
            success: function (e) {
                Y.log("RAW DATA: " + e.response.meta.serviceStatus);

                serviceStatusInfo = e.response.meta.serviceStatus;

                /* Render the obtained service status info */
                renderServicesStatus(Y, clusterName, serviceStatusInfo);
           },
           failure: function (x,o) {
               alert("Async call failed!");
               ++errorCount;
               Y.log("In serviceStatus callback: failed, error count=" + errorCount);
               if (errorCount > 3 ){
                  myDataSource.clearInterval(id); // end polling 
               }
           }
       };
    id = myDataSource.setInterval(5000, {'request': {},'callback':callback}); // Starts polling
  });
}

function renderServicesStatus(Y, clusterName, servicesStatusInfo) {

  var svcStatusMarkup = "<table>" +
                           "<caption>Installed services</caption>" +
                           "<thead><tr><th>Name</th><th>Description</th><th>Status</th><th>Management</th></tr></thead>";

  for (var svcKey in servicesStatusInfo) {
    if (servicesStatusInfo.hasOwnProperty( svcKey )) {

      var statusNum = serviceStatusInfo[svcKey]["statusNum"];
      if (statusNum == 4) {
        // Separate tables for installed services
        continue;
      }

      var status = serviceStatusInfo[svcKey]["status"];
      if (statusNum == 5) {
        status = status + '&nbsp;<img src="../images/loading.gif"/>';
      }

      /* Inefficient, with all the string concatenations, but clear to read. */
      svcStatusMarkup +=
        '<tr>' +
          '<td>' + svcKey + '</td>' +
          '<td> ' + serviceStatusInfo[svcKey]["description"] + ' </td>' +
          '<td>' + status + '</td>' +
          '<td> Management</td>' +
        '</tr>';
    }
  } 
  svcStatusMarkup += '</table>';

  Y.one("#displayServiceStatusContentDivId").setContent( svcStatusMarkup );

}
