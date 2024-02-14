/*
 * img-stats
 * https://github.com/jlembeck/img-stats
 *
 * Copyright (c) 2013 Jeffrey Lembeck
 * Licensed under the MIT license.
 */

/*global require:true, console:true*/
/*global window:true*/

(function(exports) {
  "use strict";
  var fs = require( 'fs' );
  var DOMParser = require( 'xmldom' ).DOMParser;

  var isPng = function( data ){
    var d = data.slice(0, 16);
    return d === "89504e470d0a1a0a";
  };

  var isSVG = function( data ){
    for( var i=0, l = data.length; i < l; i++ ){
      var d = data.slice(i, i+2).toString( 'hex' );
      if( d === "73" ){
        i=i+2;
        d = data.slice( i, i+2 ).toString( 'hex' );
        if( d === "76" ){
          i=i+2;
          d = data.slice( i, i+2 ).toString( 'hex' );
          if( d === "67" ){
            return true;
          }
        }
      }
    }
    return false;
  };

  var padHexStringToTwoDigits = function( num ) {
    return ( num.length === 1 ? "0" : "" ) + num;
  };

  var getData = function(filename){
    var data, hexString = "", hexData = [];
    if( fs.readFileSync ) {
      data = fs.readFileSync( filename );
      hexString = data.toString( "hex" );
    } else {
      // PhantomJS compatible
      data = fs.open( filename, "r+b" ).read();
      for(var j=0, k=data.length; j<k; j++) {
        hexData.push( padHexStringToTwoDigits( data.charCodeAt(j).toString(16) ));
      }
      hexString = hexData.join("");
    }
    return { data: data, hexString: hexString };
  };
  exports.stats = function( filename , callback ) {
    var ret = {};
    if( !filename ){ throw new Error("Needs a filename"); }

    var da = getData( filename ),
    data = da.data,
    hexString = da.hexString;

    if( isPng( hexString ) ){
      var i = 16,
        l;
      for( l = hexString.length; i < l; i++ ){
        var d = hexString.slice(i, i+8);
        if( d === "49484452" ){
          i = i+8;
          break;
        }
      }

      ret.width = parseInt(hexString.slice( i, i+8 ).toString( 16 ) , 16 );
      i = i+8;
      ret.height = parseInt(hexString.slice( i, i+8 ).toString( 16 ) , 16 );
      ret.type = "PNG";

    } else if( isSVG( hexString ) ){
      ret.type = "SVG";
      var pxre = /(\d+)px/;
      var width, height;
      if( fs.readFileSync ){
        var doc = new DOMParser().parseFromString( data.toString( 'utf-8' ) );
        width = doc.documentElement.getAttribute( 'width' );
        height = doc.documentElement.getAttribute( 'height' );
        ret.width = width.replace(pxre, "$1");
        ret.height = height.replace(pxre, "$1");
      } else {
        var frag = window.document.createElement( "div" );
        frag.innerHTML = data;
        var svgelem = frag.querySelector( "svg" );
        width = svgelem.getAttribute( "width" );
        height = svgelem.getAttribute( "height" );
        ret.width = width.replace(pxre, "$1");
        ret.height = height.replace(pxre, "$1");
      }
    }
    callback( ret );
  };

  exports.statsSync = function( filename , callback ) {
    var ret = {};
    if( !filename ){ throw new Error("Needs a filename"); }

    var da = getData( filename ),
    data = da.data,
    hexString = da.hexString;

    if( isPng( hexString ) ){
      var i = 16,
        l;
      for( l = hexString.length; i < l; i++ ){
        var d = hexString.slice(i, i+8);
        if( d === "49484452" ){
          i = i+8;
          break;
        }
      }

      ret.width = parseInt(hexString.slice( i, i+8 ).toString( 16 ) , 16 );
      i = i+8;
      ret.height = parseInt(hexString.slice( i, i+8 ).toString( 16 ) , 16 );
      ret.type = "PNG";

    } else if( isSVG( hexString ) ){
      ret.type = "SVG";
      var pxre = /(\d+)px/;
      var width, height, viewBox;
      if( fs.readFileSync ){
        var doc = new DOMParser().parseFromString( data.toString( 'utf-8' ) );
        viewBox = doc.documentElement.getAttribute( 'viewBox' );
        width = doc.documentElement.getAttribute( 'width' );
        if ( !width && viewBox ) {
          width = viewBox.split(' ')[2];
        }
        height = doc.documentElement.getAttribute( 'height' );
        if ( !height && viewBox ) {
          height = viewBox.split(' ')[3];
        }
        ret.width = width.replace(pxre, "$1");
        ret.height = height.replace(pxre, "$1");
      } else {
        var frag = window.document.createElement( "div" );
        frag.innerHTML = data;
        var svgelem = frag.querySelector( "svg" );
        viewBox = svgelem.getAttribute( 'viewBox' );
        width = svgelem.getAttribute( "width" );
        if ( !width && viewBox ) {
          width = viewBox.split(' ')[2];
        }
        height = svgelem.getAttribute( "height" );
        if ( !height && viewBox ) {
          height = viewBox.split(' ')[3];
        }
        ret.width = width.replace(pxre, "$1");
        ret.height = height.replace(pxre, "$1");
      }
    }
    return ret;
  };


}(typeof exports === 'object' && exports || this));
