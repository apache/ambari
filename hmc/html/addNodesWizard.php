<?php require_once "./head.inc" ?>
<html>
  <head>
    <title id="pageTitleId"><?php echo $RES['page.title'] ?></title>

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

    <div id="ContentDivId"> 
      <?php require "./subnav.htmli"; ?>
      
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

        <?php require "./utils.htmli"; ?>

        <div name="installationMainFormsDiv" id="installationMainFormsDivId">
          <div id="addNodesCoreDivId" style="display:block">
            <div class="pageSummary">
              <h2><?php echo $RES['addNodesWizard.addNodes.pageSummary.header'] ?></h2>
              <p><?php echo $RES['addNodesWizard.addNodes.pageSummary.body'] ?></p>
            </div>
            <div id="formStatusDivId" class="formStatusBar" style="display:none">					
            </div>
            <div class="pageContent">
 
              <fieldset>
                <form id="addNodesFilesFormId" enctype="multipart/form-data" method="post">
                  <p>
                  <label for="clusterDeployUserId"><?php echo $RES['common.sshUsername.label'] ?></label>
                  <input type="text" name="ClusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
                  </p>
                  <br/>
                  <p>
                  <label for="clusterDeployUserIdentityFileId"><?php echo $RES['common.sshPrivateKeyFile.label'] ?></label>
                  <input type="file" name="clusterDeployUserIdentityFile" id="clusterDeployUserIdentityFileId" value="" placeholder="">
                  </p>
                  <br/>
                  <p>
                  <label for="clusterHostsFileId"><?php echo $RES['common.hostsFile.label'] ?></label>
                  <input type="file" name="clusterHostsFile" id="clusterHostsFileId" value="" placeholder="">
                  </p>

                  <div id="fileUploadWrapperDivId">
                    <iframe name="fileUploadTarget" id="fileUploadTargetId" src="about:blank" style="display:none"></iframe>
                  </div>
                </form>
              </fieldset>
              <button class="btn btn-large" id="addNodesSubmitButtonId"><?php echo $RES['addNodesWizard.addNodes.submit.label'] ?></button>
            </div>
          </div>

          <div name="selectServicesCoreDiv" id="selectServicesCoreDivId" style="display:none">
            <div class="pageSummary">
              <h2><?php echo $RES['addNodesWizard.selectServices.pageSummary.header'] ?></h2>
              <p><?php echo $RES['addNodesWizard.selectServices.pageSummary.body'] ?></p>
            </div>
            <div id="formStatusDivId" class="formStatusBar" style="display:none">					
            </div>
            <fieldset>
              <!--<legend>Select Services</legend>-->
              <form id="addNodesDataFormId">
                <div name="selectComponentsDynamicRenderDiv" id="selectComponentsDynamicRenderDivId"></div>
              </form>
            </fieldset>
            <button class="btn btn-large" id="deployAddedNodesSubmitButtonId"><?php echo $RES['addNodesWizard.selectServices.submit.label'] ?></button>
          </div>

          <?php require "./txnUtils.htmli"; ?>

        </div>
      </div>
      <!-- End of installation Wizard -->

    </div>
    <?php require "./footer.htmli"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">

    var freshInstall = false;
    var clusterName = "<?php echo $_GET['clusterName']; ?>";
    var nodesAction = "addNodes";

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
