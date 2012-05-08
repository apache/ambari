function getGsInstallLogs(Y, clusterName) {
  var errorCount = 0;
  var lastSize = 0;
  var sizeRepeatedCount = 0;

  YUI().use("node", "io", "dump", "json", "datasource", "datasource-io", "datasource-jsonschema", "datasource-polling", function(Y) {

/*
     var getLogFunction = function() {
       Y.io("../php/displayLogs.php?clusterName=" + clusterName + "&lines=all", {
        method: 'GET',
        timeout : 1000,
        on: {
            success: function (x,o)   {
                Y.log("RAW LOG DATA: " + o.responseText);
                return o.responseText;
            },
            failure: function (x,o) {
               alert("Async call failed!");
            }
        }
    });

   };
*/

   var id;
//   var myDataSource = new Y.DataSource.Function({source:getLogFunction});
   var myDataSource = new Y.DataSource.IO({source:"../php/displayLogs.php?clusterName=" + clusterName + "&lines=all"});
   myDataSource.plug(Y.Plugin.DataSourceJSONSchema, {
        schema: {
            metaFields: {logs:"logs", clusterName:"clusterName", gsInstallDone:"gsInstallDone"}
        }
    });
   var callback = {
            success: function(e){
               var logs = e.response.meta.logs;
               Y.log("IN LOG CALLBACK: got logs of length: " + logs.length);
               errorCount=0;
               var length = logs.length;
               var newlogs;
               if (length > lastSize ) {
                 newlogs = logs.split('\n').join('<br/>');
               }
/*
               if (length == lastSize) {
                  ++sizeRepeatedCount;
               } else {
                  sizeRepeatedCount = 0;
                  lastSize = length;
               }
               if (sizeRepeatedCount == 5 ) {
                  Y.log("In log callback: stopping, repeat count=" + sizeRepeatedCount);
                  myDataSource.clearInterval(id); // end polling
               }
*/
               Y.log("IN LOG CALLBACK: got installdone as " + e.response.meta.gsInstallDone);
               if (e.response.meta.gsInstallDone) {
                  Y.log("In log callback: stopping, got installdone as " + e.response.meta.gsInstallDone);
                  myDataSource.clearInterval(id); // end polling
                  newlogs += 'Installation script finished...<br/>';
      //            Y.one("#installDoneDivId").setStyle('display', 'block');
               } else {
                  newlogs += '<img src="../images/loading.gif"/><br/>';
               }
               Y.one("#displayLogsContentDivId").setContent(newlogs);
               Y.one("#displayLogsContentDivId").scrollTop = Y.one("#displayLogsContentDivId").scrollHeight;
            },
            failure: function(e){
               ++errorCount;
               Y.log("In log callback: failed, error count=" + errorCount);
               if (errorCount > 3 ){
                  myDataSource.clearInterval(id); // end polling 
               }
            }
        };
    id = myDataSource.setInterval(5000, {'request': {},'callback':callback}); // Starts polling
    });
}


function renderDisplayLogs(Y, clusterName) {
  /*
  Y.one('#installDoneDivId').on('click',function (e) {

      e.target.set('disabled', true);
  */

      /* Done with this stage, hide it. */
    //  Y.one("#displayLogsCoreDivId").setStyle('display','none');

      /* Render the next stage. */
    //  getServicesStatus(Y, clusterName);

     // /* Show off our rendering. */
     // Y.one("#displayServiceStatusCoreDivId").setStyle('display','block');
     // });

  getGsInstallLogs(Y, clusterName);
}
