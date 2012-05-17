<html>
  <head>
    <title id="pageTitleId">Hortonworks Management Center</title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../yui-3.4.1/build/cssreset/cssreset-min.css">
    <link type="text/css" rel="stylesheet" href="../css/bootstrap.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>    
    <link type="text/css" rel="stylesheet" href="../css/selectHosts.css" media="screen"/>
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.html"; ?>

    <div id="ContentDivId">

      <!-- List of clusters
      <div name="clustersListDiv" id="clustersListDivId">
      </div>
      -->

      <!-- Installation Wizard -->
      <div name="installationWizardDiv" id="installationWizardDivId" style="display:block">
        <div name="installationWizardProgressBarDiv" id="installationWizardProgressBarDivId">
          <ol id="installationWizardProgressBarListId">
            <li id="createClusterStageId" class="installationWizardFirstStage installationWizardCurrentStage">
              <div>
                <span class="installationWizardStageNumber">
                  1   
                </span>
                Create Cluster
              </div>
            </li>
            <li id="addNodesStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  2   
                </span>
                Add Nodes
              </div>
            </li>
            <li id="selectServicesStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  3   
                </span>
                Select Services
              </div>
            </li>
            <li id="assignHostsStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  4   
                </span>
                Assign Hosts
              </div>
            </li>
            <li id="configureClusterStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  5
                </span>
                Select Mount Points
              </div>
            </li>
            <li id="configureClusterAdvancedStageId" class="installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  6
                </span>
                Custom Config
              </div>
            </li>
            <li id="deployClusterStageId" class="installationWizardLastStage installationWizardUnvisitedStage">
              <div>
                <span class="installationWizardStageNumber">
                  7
                </span>
                Review &amp; Deploy
              </div>
            </li>
          </ol>
        </div>
        

        <?php require "./utils.htmli"; ?>

        <div id="installationMainFormsDivId">
              <div id="createClusterCoreDivId">
                <div class="pageSummary">
                  <h2>Let's create a new Hadoop cluster</h2>    
                </div>
                <div class="pageContent">       
                  <form id="createClusterFormId">
                    <label for="clusterNameId">Name of your new cluster</label>
                    <input type="text" name="clusterName" id="clusterNameId" placeholder="cluster name" value="">
                  </form>
				  <a href="javascript:void 0" class="btn btn-large" id="createClusterSubmitButtonId">Create Cluster</a>
                </div>
              </div>
          
              <div id="addNodesCoreDivId" style="display:none">
                  
                  <div class="pageSummary">
                    <h2>Which nodes are you installing Hadoop on?</h2>
                    We'll use the SSH private key and public key files to perform installation on your nodes.
                    <span>The public key that is paired with the private key must already be installed on all the nodes.</span>
                  </div>
                  <div class="pageContent">
                  <form id="addNodesFilesFormId" enctype="multipart/form-data" method="post">
                    <p>
                    <label for="clusterDeployUserId">SSH Username</label>
                    <input type="text" name="ClusterDeployUser" id="clusterDeployUserId" value="root" placeholder="">
                    </p>
                    <br/>
                    <p>
                    <label for="clusterDeployUserIdentityFileId">SSH Private Key File</label>
                    <input type="file" name="clusterDeployUserIdentityFile" id="clusterDeployUserIdentityFileId" value="" placeholder="">
                    </p>
                    <br/>
                    <p>
                    <label for="clusterHostsFileId">Newline-delimited list of node hostnames</label>
                    <input type="file" name="clusterHostsFile" id="clusterHostsFileId" value="" placeholder="">
                    </p>

                    <div id="fileUploadWrapperDivId">
                      <iframe name="fileUploadTarget" id="fileUploadTargetId" src="about:blank" style="display:none"></iframe>
                    </div>
                  </form>
                  <a href="javascript:void 0" class="btn btn-large" id="addNodesSubmitButtonId">Add Nodes</a>
                  </div>
              </div>

              <div name="selectServicesCoreDiv" id="selectServicesCoreDivId" style="display:none">
                <fieldset>
                <div class="pageSummary">
                  <h2>Which services do you want to install?</h2>
                  <p>We'll automatically take care of dependencies (e.g., HBase requires ZooKeeper, etc.)</p>
               	</div>
               	<div class="pageContent">
                  <div id="selectCoreServicesListId">
                   <ul id="selectCoreServicesListUlId">
                      <div id="selectCoreServicesDynamicRenderDivId">
                      </div>
                    </ul>
                  </div>
                  <div id="selectOptionalServicesListId">
                    <ul id="selectOptionalServicesListUlId">
                      <div id="selectOptionalServicesDynamicRenderDivId">
                      </div>
                    </ul>
                  </div>
                  <div id="selectNonSelectableServicesListId">
                    <ul id="selectNonSelectableServicesListUlId">
                      <div id="selectNonSelectableServicesDynamicRenderDivId">
                      </div>
                    </ul>
                  </div>
                </div>
                </fieldset>
                <a href="javascript:void 0" class="btn btn-large" style="margin:20px 0 0 60px" id="selectServicesSubmitButtonId" class="submitButton">Select Services</a>

              </div>
              <!-- End of selectServicesCoreDivId -->

              <div name="assignHostsCoreDiv" id="assignHostsCoreDivId" style="display:none">
                <div id="statusDivId">
                </div>

                <div class="pageSummary">
                	<h2>Assign Services to Nodes</h2>
                	<p>We have categorized the given nodes into Node-groups and also suggested the locations of various service-masters. Drag and drop service-masters that you wish to change. Click on the given links if you wish to change the node assignments.</p>
                </div>
                <div id="serviceMastersLinksDivId">
                </div>
                <div id="nodeGroupsCoreDivId">
                </div>
                <a href="javascript:void 0" style="margin:20px" class="btn btn-large" id="selectServiceMastersSubmitButtonId">Submit</a>

              </div>

              <div name="configureClusterCoreDiv" id="configureClusterCoreDivId" style="display:none">
                <div class="pageSummary">
                	<h2>Specify Mount Points</h2>
                	<p>We found the following mount points on your nodes. Please confirm/modify the mount points to use for your nodes.
                </div>
                
                <div id="configureClusterInputContainerDivId">
                  <form id="configureClusterFormId">
                    <div name="configureClusterInputDiv" id="configureClusterInputDivId">
                        <fieldset id="configureClusterInputFieldSetId">
                          <!--<legend>Select mount points</legend>-->
                          <div name="configureClusterMountPointsInputDiv" id="configureClusterMountPointsInputDivId">
                            <div id="configureClusterMountPointsDynamicRenderDivId"></div>
                            <p>
                              <label for="customMountPoints">Custom mount points</label>
                              <input type="text" name="customMountPoints" id="customMountPointsId" value="" placeholder="Comma-Separated List">
                            </p>
                          </div>  
                          <!-- Additional <div>s for other categories of cluster configuration go here -->
                        </fieldset>
                    </div>
                    <!-- <input type="button" id="configureClusterSubmitButtonId" value="Submit" class="submitButton"> -->
                  </form>
                  <a href="javascript:void 0" class="btn btn-large" id="configureClusterSubmitButtonId">Submit</a>
                </div>                                              
                <div id="configureClusterDisplayDivId" style="display:none">
                  <fieldset>
                    <!--<legend>Effective mount points</legend>-->
                    <div name="configureClusterMountPointsDisplayDiv" id="configureClusterMountPointsDisplayDivId">
                    </div>
                  </fieldset>
                </div>
              </div>

                <div id="configureClusterAdvancedCoreDivId" style="display:none">
                  <div class="pageSummary">
                  	<h2>Customize Settings</h2>
                  	<p>We have come up with reasonable default settings.  Customize as you see fit.</p>
                  </div>
                  <form id="configureClusterAdvancedFormId">
                    <fieldset id="configureClusterAdvancedFieldSetId">
                      <!--<legend>Advanced configuration</legend>-->
                      <div id="configureClusterAdvancedDynamicRenderDivId"></div>
                    </fieldset>
                  </form>
                  <label></label>
                  <a href="javascript:void 0" class="btn btn-large" id="configureClusterAdvancedSubmitButtonId">Submit</a>
                </div>

                <div name="deployCoreDiv" id="deployCoreDivId" style="display:none">
                  <div class="pageSummary">
                  	<h2>Review and Deploy</h2>
                  	<p>We are ready to deploy!  Please review your settings below.</p>
                  	<p>If you wish to make any changes, you can click on any of the installation stages above.  Note that if you do go back to a stage, you will have to go through all the subsequent stages again.</p>               
                  </div>
                  <form id="deployFormId">
                    <fieldset id="deployFieldSetId">
                      <!--<legend>Review your settings</legend>-->
                      <div id="deployDynamicRenderDivId"></div>
                    </fieldset>                    
                  </form>
                  <label></label>
                  <a href="javascript:void 0" class="btn btn-large" id="deploySubmitButtonId" value="Deploy">Deploy</a>
                </div>

                <?php require "./txnUtils.htmli"; ?>

              </div>
            </div>
            <!-- End of installation Wizard -->
    </div>
            <?php require "./footer.html"; ?>

            <!-- Javascript Scaffolding -->
            <script type="text/javascript">

            var freshInstall = true;

            var InstallationWizard = {

              CreateCluster: 
              {},
              AddNodes: 
              {},
              AddNodesProgress:
              {},
              SelectServices: 
              {},
              AssignMasters: 
              {},
              ConfigureCluster: 
              {},
              ConfigureServices: 
              {},
              ReviewAndDeploy: 
              {},
              DeployProgress:
              {}
            };

            var jsFilesToLoad = [
				'../js/jquery.min.js',
				'../js/bootstrap.min.js', 
                '../js/utils.js', 
                '../js/txnUtils.js',
                '../js/installationWizard.js',
                '../js/createCluster.js', 
                '../js/addNodes.js', 
                '../js/addNodesProgress.js', 
                '../js/selectServices.js', 
                '../js/assignMasters.js', 
                '../js/configureCluster.js', 
                '../js/configureServicesUtils.js',
                '../js/configureServices.js', 
                '../js/reviewAndDeploy.js', 
                '../js/deployProgress.js'
              ];
            </script>

            <?php require "./bootstrapJs.htmli"; ?>
            <!-- End of Javascript Scaffolding -->

          </body>
        </html> 
