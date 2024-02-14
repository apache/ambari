/*global module:true*/
/*global require:true*/
(function(){
	"use strict";
	var fs = require( 'fs' );
	var imgStats = require( 'img-stats' );

	function DataURIEncoder( path ) {
		this.path = path;
		this.extension = path.split('.').pop();
	}

	DataURIEncoder.prototype.encode = function() {
		var fileData = fs.readFileSync( this.path );
		var base64 = fileData.toString( 'base64');
		return base64;
	};

	DataURIEncoder.prototype.stats = function(){
		var data = imgStats.statsSync( this.path ), stats;

		if( data.width && data.height ){
			stats = {};
			if( data.width !== "" ){
				stats.width = data.width + "px";
			}
			if( data.height !== "" ){
				stats.height = data.height + "px";
			}
		}

		return stats;
	};

	module.exports = DataURIEncoder;
}());
