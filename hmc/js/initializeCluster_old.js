// Create business logic in a YUI sandbox using the 'io' and 'json' modules
function renderInitializeClusterBlockABC(Y, infoInitializeCluster) {

    var clusterName = infoInitializeCluster.clusterName;

    // REZ XXX Remove.
    //setNavigationContent(Y, "Clusters > New cluster");

    // REZ XXX Remove.
    //activateStage(Y, "#selectServicesSideBarId");

    var initializeClusterSubmitButton = Y.one('#initializeClusterSubmitButtonId');

    initializeClusterSubmitButton.on('click',function (e) {

        initializeClusterSubmitButton.set('disabled', true);

        var initializeClusterFilesForm = Y.one("#initializeClusterFilesFormId");
        
        initializeClusterFilesForm.set('action', '../php/frontend/uploadFiles.php?clusterName=' + clusterName);

        /* Set the target of the first form's upload to be a hidden iframe 
         * on the page so as not to redirect to the PHP page we're POSTing 
         * to.
         *
         * See http://www.openjs.com/articles/ajax/ajax_file_upload/ for 
         * more on this.
         */
        initializeClusterFilesForm.set('target', 'fileUploadTarget');

        /* And then programmatically submit the first of the 2 forms. */ 
        initializeClusterFilesForm.submit();

        /* The 2nd form will be sequentially submitted after the first one is 
         * done (which we are notified of by listening for the 'load' event on 
         * #fileUploadTargetId). 
         */
    });

    /* Only once the #fileUploadTargetId is done loading do we know that the
     * first form upload is done, so use this as a trigger to fire off the
     * 2nd upload; we need to be sequential to avoid race conditions at the
     * back-end.
     */
    Y.one("#fileUploadTargetId").on('load', function (e1) {

      var initializeClusterRequestData = {
        "ClusterDeployUser" : Y.one("#clusterDeployUserId").get('value'),
        "services" : [
               { "serviceName" : "HDFS", "isEnabled" : Y.one("#installHDFSId").get('checked') },
               { "serviceName" : "MAPREDUCE", "isEnabled" : Y.one("#installMRId").get('checked') },
               { "serviceName" : "HBASE", "isEnabled" : Y.one("#installHBaseId").get('checked') },
               { "serviceName" : "HCATALOG", "isEnabled" : Y.one("#installHCatalogId").get('checked') },
               { "serviceName" : "TEMPLETON", "isEnabled" : Y.one("#installTempletonId").get('checked') },
               { "serviceName" : "OOZIE", "isEnabled" : Y.one("#installOozieId").get('checked') },
               { "serviceName" : "PIG", "isEnabled" : Y.one("#installPigId").get('checked') },
               { "serviceName" : "SQOOP", "isEnabled" : Y.one("#installSqoopId").get('checked') }
        ]
      };
      
      Y.io("../php/frontend/initializeCluster.php?clusterName=" + clusterName, {

          method: 'POST',
          data: Y.JSON.stringify(initializeClusterRequestData),
          timeout : 10000,
          on: {
            start: function(x, o) {
              Y.log("In start function");
              // waitPanel.set('headerContent' ,'Loading, please wait...');
              // waitPanel.set('bodyContent', 'Discovering nodes, doing complicated things which are too hard to explain so please wait ... ( we may get back to you someday)'); 
              // waitPanel.render(document.body); 
              // waitPanel.set('display', 'block'); 
              // Y.one("#initializeClusterStatusDivId").setStyle('background-color','red');
              // Y.one("#initializeClusterStatusDivId").setStyle('font-color','white');
              // Y.one("#initializeClusterStatusDivId").setStyle('border','1px solid');
              showLoadingImg(Y);
              //waitPanel.show();
              // Y.one("#initializeClusterStatusDivId").setStyle('visible','true');
            },
            complete: function(x, o) {
              initializeClusterSubmitButton.set('disabled', false);
              Y.log("In stop function");
              hideLoadingImg(Y);
              //waitPanel.hide();
            },
            success: function (x,o) {
              initializeClusterSubmitButton.set('disabled', false);
              Y.log("RAW JSON DATA: " + o.responseText);

              // Process the JSON data returned from the server
              try {
                clusterInfoJson = Y.JSON.parse(o.responseText);
              }
              catch (e) {
                alert("JSON Parse failed!");
                return;
              }

              Y.log("PARSED DATA: " + Y.Lang.dump(clusterInfoJson));

              if (clusterInfoJson.result != 0) {
                 // Error!
                 alert("Got error!" + clusterInfoJson.error); 
                 return;
               }
              clusterInfoJson = clusterInfoJson.response;

              /* Done with this stage, transition to the next. */
              transitionToNextStage( "#initializeClusterCoreDivId", initializeClusterRequestData,
                  "#assignHostsCoreDivId", clusterInfoJson, renderAssignHosts );
            },
            failure: function (x,o) {
              initializeClusterSubmitButton.set('disabled', false);
              alert("Async call failed!");
            }
          }
      });
    });
}
