<?php require_once "./head.inc" ?>
<html>
  <head>
    <?php require "./head.htmli" ?>
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>

    <div id="contentDivId"> 

      <?php require "./utils.htmli"; ?>
      <?php require "./txnUtils.htmli"; ?>

    </div>
    <!-- End of contentDivId -->

    <?php require "./footer.htmli"; ?>

    <!-- Javascript Scaffolding -->
    <script type="text/javascript">
      /* Minimal data required to bootstrap clusters.js. */
      var clusterName = '<?php echo $clusterName; ?>';

      var jsFilesToLoad = [ 
        'js/utils.js',
        'js/txnUtils.js',
        'js/deployProgress.js',
        'js/showDeployProgress.js' 
      ];
    </script>

    <?php require "./bootstrapJs.htmli"; ?>
    <!-- End of Javascript Scaffolding -->
  </body>
</html> 
