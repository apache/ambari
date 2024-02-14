/*global require:true*/
/*global module:true*/
(function(){
	"use strict";

	var path = require( 'path' );
	var DataURIEncoder = require( './data-uri-encoder' );

	function PngURIEncoder(path) {
		DataURIEncoder.call( this, path );
	}

	PngURIEncoder.prefix = "data:image/png;base64,";

	PngURIEncoder.prototype.stats = function(){
		return DataURIEncoder.prototype.stats.call( this );
	};

	PngURIEncoder.prototype.encode = function( options ) {
		var datauri = PngURIEncoder.prefix + DataURIEncoder.prototype.encode.call(this);

		//IE LTE8 cannot handle datauris that are this big. Need to make sure it just
		//links to a file
		if (options && (datauri.length > 32768 || options.noencodepng )) {
			var output_path = options.pngpath || options.pngfolder;
			return path.join(output_path, path.basename(this.path))
				.split( path.sep )
				.join( "/" );
		}

		return datauri;
	};

	module.exports = PngURIEncoder;
}());
