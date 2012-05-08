<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="http://yui.yahooapis.com/3.4.1/build/cssreset/cssreset-min.css"> 
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/selectHosts.css" media="screen"/>
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./header.html"; ?>
    <hr/>

    <div name="ContentDiv" id="ContentDivId"> 

      <!-- Navigation bar -->
      <div name="navigationBarDiv" id="navigationBarDivId">
        <div name="navigationBarContentDiv" id="navigationBarContentDivId">
        <a href="index.php">Clusters</a>
        </div>
      <hr/>
      </div>
      <!-- List of clusters
      <div name="clustersListDiv" id="clustersListDivId">
      </div>
      -->

      <!-- Installation Wizard -->
      <div name="installationWizardDiv" id="installationWizardDivId" style="display:block">
        <div name="installationWizardProgressBarDiv" id="installationWizardProgressBarDivId">
          <ol id="installationWizardProgressBarListId">
            <li id="addNodesStageId" class="installationWizardCurrentStage">
              <div>
                <span class="installationWizardStageNumber">
                  1   
                </span>
                Add nodes
              </div>
            </li>
            <li id="selectServicesStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  2   
                </span>
                Select services
              </div>
            </li>
          </ol>
        </div>
        
        <br/>
                <div id="formStatusDivId" class="formStatusBar" style="display:none">
          <div id="formStatusCoreDivId" stle="display:none">
          </div>
        </div>


        <div name="installationMainFormsDiv" id="installationMainFormsDivId">
         
          <br/>
              <div id="addNodesCoreDivId" style="display:block">

                <fieldset>
                  <form id="addNodesFilesFormId" enctype="multipart/form-data" method="post">
                    <p>
                    <label for="clusterDeployUserId">Cluster Deploy User</label>
                    <input type="text" name="ClusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
                    </p>
                    <br/>
                    <p>
                    <label for="clusterDeployUserIdentityFileId">Cluster Deploy User Identity File</label>
                    <input type="file" name="clusterDeployUserIdentityFile" id="clusterDeployUserIdentityFileId" value="" placeholder="">
                    </p>
                    <br/>
                    <p>
                    <label for="clusterHostsFileId">Cluster Hosts File</label>
                    <input type="file" name="clusterHostsFile" id="clusterHostsFileId" value="" placeholder="">
                    </p>

                    <div id="fileUploadWrapperDivId">
                      <iframe name="fileUploadTarget" id="fileUploadTargetId" src="about:blank" style="display:none"></iframe>
                    </div>
                  </form>
                </fieldset>
                <br/>
                <p>
                  <input type="button" id="addNodesSubmitButtonId" value="Add nodes" class="submitButton">
                </p>
              </div>

              <div name="selectServicesCoreDiv" id="selectServicesCoreDivId" style="display:none">
                <fieldset>
                  <!--<legend>Select Services</legend>-->
                  <form id="addNodesDataFormId">
              <div name="selectComponentsDynamicRenderDiv" id="selectComponentsDynamicRenderDivId"></div>

                    <!-- <p>
                    <label for="installMapReduceId">MapReduce (TASKTRACKER)</label>
                    <input type="checkbox" name="installMR" id="installMRId" value="installMapReduceValue" disabled="disabled" checked="yes">
                    </p>
                    <p>
                    <label for="installHdfsId">HDFS (DATANODE)</label>
                    <input type="checkbox" name="installHDFS" id="installHDFSId" value="installHdfsValue" disabled="disabled" checked="yes">
                    </p> -->
                  </form>
                </fieldset>
                <br/>
                <p>
                <input type="button" name="deployAddedNodesSubmitButton" id="deployAddedNodesSubmitButtonId" value="Deploy Nodes" class="submitButton">
                </p>
              </div>
                <div name="txnProgressCoreDiv" id="txnProgressCoreDivId" style="display:none">

                   <!-- Used to render informational/status messages (error, success reports and such like) -->
                <div id="txnProgressStatusDivId" class="formStatusBar" style="display:none">
                  <div id="txnProgressStatusMessageDivId"></div>
                  <div id="txnProgressStatusActionsDivId"></div>
                </div>
 
                  <fieldset id="txnProgressFieldSetId">
               <!--     <legend>Display Progress</legend> -->
                    <div id="txnProgressDynamicRenderDivId"></div>
                  </fieldset>
                </div>

              </div>
            </div>
            <!-- End of installation Wizard -->

            <!-- The mechanism by which we black out the screen and affix the
                 the spotlight on a smaller portion of it. -->
            <div id="blackScreenDivId" style="display:none"></div>
            <!-- The image we use to let users know something is loading, and 
                 that they should wait. -->
            <div id="loadingDivId" style="display:none">
              <div id="loadingBlackScreenDivId"></div>
              <img id="loadingImgId" src="../images/loadingLarge.gif"/>
            </div>

            <!-- Placeholder for our informational YUI panel. -->
            <div id="informationalPanelContainerDivId">
            </div>

            <hr/>
            <?php require "./footer.html"; ?>

            <!-- Javascript Scaffolding -->
            <script type="text/javascript">

            var freshInstallation = false;
            var clusterName = "<?php echo $_GET['clusterName']; ?>";

            var InstallationWizard = {

              AddNodes: 
              {},
              AddNodesProgress:
              {},
              SelectComponents: 
              {},
              DeployProgress:
              {}
            };

            var jsFilesToLoad = [ 
                '../js/utils.js', 
                '../js/txnUtils.js',
                '../js/addNodes.js', 
                '../js/addNodesProgress.js', 
                '../js/selectComponents.js', 
                '../js/deployProgress.js', 
                '../js/addNodesWizardInit.js'
              ];
            </script>

            <?php require "./bootstrapJs.htmli"; ?>
            <!-- End of Javascript Scaffolding -->

          </body>
        </html> 
