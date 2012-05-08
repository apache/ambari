<html>
  <head>
    <title id="pageTitleId">Hortonworks Data Platform Portal</title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="http://yui.yahooapis.com/3.4.1/build/cssreset/cssreset-min.css"> 
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/manageServices.css" media="screen"/>
    <!-- End CSS -->
  </head>

  <body class="yui3-skin-sam">
    <?php require "./header.html"; ?>

    <hr/>

    <h1>Service Management for Cluster 
      <a href="../html/clusters.php?clusterName=<?php echo $_GET['clusterName']; ?>" target="_blank">
        <?php echo $_GET['clusterName']; ?>
      </a>
    </h1>

    <br/>

    <div id="contentDivId"> 

      <!-- Service Management: The Main Event -->
      <div id="serviceManagementCoreDivId">
        <ul id="serviceManagementListId">
          <div id="serviceManagementDynamicRenderDivId">
            <!--
            <li class="serviceManagementEntry serviceManagementEntryStopped">
              <div>
                <span class="serviceManagementEntryNameContainer">
                  <a href="javascript:void(null)" class="serviceManagementEntryName">HDFS</a>
                </span>
                <div class="serviceManagementEntryActionsContainer">
                  <a href="javascript:void(null)" title="Start" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStart"></a>
                  <a href="javascript:void(null)" title="Stop" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStop"></a>
                  <a href="javascript:void(null)" title="Reconfigure" 
                     class="serviceManagementEntryAction serviceManagementEntryActionReconfigure"></a>
                </div>
              </div>
            </li>
            <li class="serviceManagementEntry serviceManagementEntryStarted">
              <div>
                <span class="serviceManagementEntryNameContainer">
                  <a href="javascript:void(null)" class="serviceManagementEntryName">MapReduce</a>
                </span>
                <div class="serviceManagementEntryActionsContainer">
                  <a href="javascript:void(null)" title="Start" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStart"></a>
                  <a href="javascript:void(null)" title="Stop" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStop"></a>
                  <a href="javascript:void(null)" title="Reconfigure" 
                     class="serviceManagementEntryAction serviceManagementEntryActionReconfigure"></a>
                </div>
              </div>
            </li>
            <li class="serviceManagementEntry serviceManagementEntryStarted">
              <div>
                <span class="serviceManagementEntryNameContainer">
                  <a href="javascript:void(null)" class="serviceManagementEntryName">HBase</a>
                </span>
                <div class="serviceManagementEntryActionsContainer">
                  <a href="javascript:void(null)" title="Start" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStart"></a>
                  <a href="javascript:void(null)" title="Stop" 
                     class="serviceManagementEntryAction serviceManagementEntryActionStop"></a>
                  <a href="javascript:void(null)" title="Reconfigure" 
                     class="serviceManagementEntryAction serviceManagementEntryActionReconfigure"></a>
                </div>
              </div>
            </li>
            -->
          </div>
        </ul>

        <br/>

        <div id="serviceManagementGlobalActionsDivId" style="display:none">
          <button id="serviceManagementGlobalActionsStartAllButtonId" name="startAll">
            Start All
          </button>
          <button id="serviceManagementGlobalActionsStopAllButtonId" name="stopAll">
            Stop All
          </button>
        </div>

        <br/>

      </div>
      <!-- End of serviceManagementCoreDivId -->

      <div id="txnProgressCoreDivId" style="display:none">

        <!-- Used to render informational/status messages (error, success reports and such like) -->
        <div id="txnProgressStatusDivId" class="formStatusBar" style="display:none">
          <div id="txnProgressStatusMessageDivId"></div>
          <div id="txnProgressStatusActionsDivId"></div>
        </div>

        <fieldset id="txnProgressFieldSetId">
          <div id="txnProgressDynamicRenderDivId"></div>
        </fieldset>
      </div>
      <!-- End of txnProgressCoreDivId -->

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
      <div id="informationalPanelContainerDivId"></div>

    </div>
    <!-- End of contentDivId -->

    <hr/>
    <?php require "./footer.html"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">
      /* Minimal data required to bootstrap manageServices.js. */
      var clusterName = '<?php echo $_GET['clusterName']; ?>';

      var jsFilesToLoad = [ 
        '../js/utils.js',
        '../js/txnUtils.js',
        '../js/configureServicesUtils.js',
        '../js/manageServices.js' 
      ];
    </script>

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->

  </body>
</html> 
