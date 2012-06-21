<?php require_once "./head.inc" ?>
<html>
  <head>
    <?php require "./head.htmli" ?>
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>
      
    <div id="ContentDivId">
      <?php require "./subnav.htmli"; ?>
      <?php require "./utils.htmli"; ?>            

      <!-- List of clusters -->
      <div id="clustersListDivId" style="display:none">
      </div>

      <div id="clusterHostRoleMappingDivId" style="display:none">
        <div id="clusterHostRoleMappingDynamicRenderDivId">
        </div>      
      </div>

    </div>        
    <!-- End of contentDivId -->
    
    <?php require "./footer.htmli"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">

      var jsFilesToLoad = [ 
        'js/utils.js',
        'js/clustersList.js' 
      ];
    </script>

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->
  </body>
</html> 
