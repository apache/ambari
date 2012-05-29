<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../yui-3.5.1/build/cssreset/cssreset-min.css"> 
    <link type="text/css" rel="stylesheet" href="../css/bootstrap.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/selectHosts.css" media="screen"/>
    <link rel="shortcut icon" href="../images/logo-micro.gif">
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>
    <?php 
     include_once ("../php/util/clusterState.php"); 
    // $clusterName = "m4v1";
    // $clusterState = needWipeOut($clusterName); 
    ?>
    <hr/>

    <div name="ContentDiv" id="ContentDivId"> 

      <!-- Installation Wizard -->
      <div name="installationWizardDiv" id="installationWizardDivId" style="display:block">
        <div name="installationWizardProgressBarDiv" id="installationWizardProgressBarDivId">
          <ol id="installationWizardProgressBarListId">
            <li id="addNodesStageId" class="installationWizardCurrentStage">
              <div>
                <span class="installationWizardStageNumber">
                  1   
                </span>
                Wipe out cluster
              </div>
            </li>
          </ol>
        </div>
        
        <br/>

        <?php require "./utils.htmli"; ?>

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
              <div name="confirmWipeOutDiv" id="confirmWipeOutDivId">
                <label for="confirmWipeOutId">Confirm Wipeout(Data will be lost)</label>
                <input type="checkbox" id="confirmWipeOutCheckId" value="true">
              </div>

                    <div id="fileUploadWrapperDivId">
                      <iframe name="fileUploadTarget" id="fileUploadTargetId" src="about:blank" style="display:none"></iframe>
                    </div>
                  <input type="button" id="addNodesSubmitButtonId" value="Confirm" class="submitButton">
                </p>
              </div>

              <?php require "./txnUtils.htmli"; ?>

              </div>
            </div>
            <!-- End of installation Wizard -->

            <hr/>
            <?php require "./footer.htmli"; ?>

            <!-- Javascript Scaffolding -->
            <script type="text/javascript">

            var freshInstallation = false;
            var nodesAction = "uninstall";
            var clusterName = "<?php echo $_GET['clusterName']; ?>";

            var InstallationWizard = {

              AddNodes: 
              {},
              AddNodesProgress:
              {}
            };

            var jsFilesToLoad = [ 
                '../js/utils.js', 
                '../js/txnUtils.js',
                '../js/uninstall.js', 
                '../js/uninstallProgress.js', 
              ];
            </script>

            <?php require "./bootstrapJs.htmli"; ?>
            <!-- End of Javascript Scaffolding -->

          </body>
        </html> 
