/*global require:true*/
/*global module:true*/
(function(){
	"use strict";

	var fs = require( 'fs-extra' );
	var path = require( 'path' );
	var Handlebars = require( 'handlebars' );
	var SvgURIEncoder = require( './svg-uri-encoder' );
	var PngURIEncoder = require( './png-uri-encoder' );

	function DirectoryEncoder( input, output, options ){
		var files;
		if( typeof input === "string" && fs.lstatSync( input ).isDirectory()){
			files = fs.readdirSync( input ).map( function( file ){
				return path.join( input, file );
			});
		} else if( Array.isArray( input ) ){
			files = input;
		} else {
			throw new Error( "Input must be Array of files or String that is a directory" );
		}

		if( typeof output !== "string" ){
			throw new Error( "Output must be file name of desired encoded file" );
		}

		this.output = output;
		this.options = options || {};
		this.files = files;
		this.prefix = this.options.prefix || ".icon-";

		this.options.pngfolder = this.options.pngfolder || "";
		this.options.pngpath = this.options.pngpath || this.options.pngfolder;

		this.customselectors = this.options.customselectors || {};
		this.template = this._loadTemplate( this.options.template );
		this.templatePrepend = this.options.templatePrepend || "";
		this.templateAppend = this.options.templateAppend || "";
	}

	DirectoryEncoder.encoders = {
		svg: SvgURIEncoder,
		png: PngURIEncoder
	};

	DirectoryEncoder.prototype.encode = function() {
		var self = this, seen = {};

		// remove the file if it's there
		if( fs.existsSync(this.output) ) {
			fs.unlinkSync( this.output );
		}

		if( !fs.existsSync(path.dirname( this.output )) ){
			fs.mkdirpSync( path.dirname( this.output ) );
		}
		
		// add templatePrepend to the output file
		fs.appendFileSync( self.output, self.templatePrepend );

		// append each selector
		this.files.filter(function( filepath ){
			var file = path.basename( filepath ),
				extension = path.extname( file );
			return extension === ".svg" || extension === ".png";
		})
		.filter(function( filepath ){
			return fs.lstatSync( filepath ).isFile();
		})
		.forEach(function( filepath ) {
			var css, datauri, stats,
				file = path.basename( filepath ),
				extension = path.extname( file );

			self._checkName(seen, file.replace( extension, '' ));

			stats = self._stats( filepath );
			datauri = self._datauri( filepath );

			css = self._css( file.replace( extension, '' ), datauri, stats );

			fs.appendFileSync( self.output, css + "\n\n" );
		});
		
		// add templateAppend to the output file
		fs.appendFileSync( self.output, self.templateAppend );
		
	};
	DirectoryEncoder.prototype._css = function( name, datauri, stats ) {
		var self = this, width, height;

		if( stats ){
			width = stats.width;
			height = stats.height;
		}
		this.customselectors = this.customselectors || {};
		this.prefix = this.prefix || ".icon-";

		if( this.customselectors[ "*" ] ){
			this.customselectors[ name ] = this.customselectors[ name ] || [];
			var selectors = this.customselectors[ "*" ];
			selectors.forEach( function( el ){
				var s = name.replace( new RegExp( "(" + name + ")" ), el );
				if( self.customselectors[ name ].indexOf( s ) === -1 ) {
					self.customselectors[ name ].push( s );
				}
			});
		}

		var data = {
			prefix: this.prefix,
			name: name,
			datauri: datauri,
			width: width,
			height: height,
			customselectors: this.customselectors[ name ]
		}, css = "";

		if( this.template ){
			css = this.template( data );
		} else {
			for( var i in data.customselectors ){
				if( data.customselectors.hasOwnProperty( i ) ){
					css += data.customselectors[i] + ",\n";
				}
			}
			css += this.prefix + name +
				" { background-image: url('" +
				datauri +
				"'); background-repeat: no-repeat; }";
		}

		return css;
	};

	DirectoryEncoder.prototype._stats = function( file ){
		var encoder, extension = path.extname( file );

		if( typeof DirectoryEncoder.encoders[extension.replace(".", "")] === "undefined" ){
			throw new Error( "Encoder does not recognize file type: " + file );
		}

		encoder = new DirectoryEncoder.encoders[extension.replace(".", "")]( file );

		return encoder.stats();
	};

	DirectoryEncoder.prototype._datauri = function( file ) {
		var encoder, extension = path.extname( file );

		if( typeof DirectoryEncoder.encoders[extension.replace(".", "")] === "undefined" ){
			throw new Error( "Encoder does not recognize file type: " + file );
		}

		encoder = new DirectoryEncoder.encoders[extension.replace(".", "")]( file );

		// TODO passthrough of options is generally a code smell
		return encoder.encode( this.options );
	};

	DirectoryEncoder.prototype._checkName = function( seen, name ) {
		if( seen[name] ){
			throw new Error("Two files with the same name: `" + name + "` exist in the input directory");
		}

		seen[name] = true;
	};

	DirectoryEncoder.prototype._loadTemplate = function( templateFile ) {
		var tmpl;

		if( !templateFile ) { return false; }

		if( fs.existsSync( templateFile ) && fs.lstatSync( templateFile ).isFile() ){
			var source = fs.readFileSync( templateFile ).toString( 'utf-8' );
			tmpl = Handlebars.compile(source);
		} else {
			throw new Error( "Template file either doesn't exist or isn't a file" );
		}

		return tmpl;
	};

	module.exports = DirectoryEncoder;
}());
