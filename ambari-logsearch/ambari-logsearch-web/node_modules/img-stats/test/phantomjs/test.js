/*global require:true, fs:true, console:true */
(function(){
  "use strict";
  var img_stats = require('../../lib/img-stats.js'),
    fs = require('fs');

  img_stats.stats( fs.workingDirectory + '/test/cat.png' , function( data ){
    console.log("Cat image is " + data.width + " by " + data.height);
  });
}());
