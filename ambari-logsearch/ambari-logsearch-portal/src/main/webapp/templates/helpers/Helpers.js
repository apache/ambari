/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 define(function ( require ){

	var Handlebars 		= require('handlebars');
	var Util	   		= require('utils/Utils');
    var localization 	= require('utils/LangSupport');
    var SessionMgr   	= require('mgrs/SessionMgr');
	require('moment');
	/*
	 * General guidelines while writing helpers:
	 * 
	 * - If returning HTML use return new Handlebars.SafeString();
	 * - If the helper needs optional arguments use the "hash arguments"
	 *   Eg. {{{link . "See more..." story.url class="story"}}}
	 *   NOTE: the first argument after the helper name should be . which will be context in the helper function
	 *   Handlebars.registerHelper('link', function(context, text, url, options) {
	 *   	var attrs = [];
	 * 		
	 *   	for(var prop in options.hash) {
	 *   		attrs.push(prop + '="' + options.hash[prop] + '"');
	 *   	}	
	 *   	return new Handlebars.SafeString("<a " + attrs.join(" ") + ">" + text + "</a>");
	 *   });
	 * 
	 * 
	 * NOTE: Due to some limitations in the require-handlebars-plugin, we cannot have helper that takes zero arguments,
	 *       for such helpers we have to pass a "." as first argument. [https://github.com/SlexAxton/require-handlebars-plugin/issues/72] 
	 */
	
	var HHelpers = {};
	
	/*
	 * ACL related helpers
	 */

	Handlebars.registerHelper("canRead", function(resource,  options) {
		var roles = _.has(options.hash, 'roles') ? options.hash.roles : undefined;
		if(MSAcl.canRead(resource,roles)){
			return options.fn(this);
		}
	});

	Handlebars.registerHelper("canCreate", function(resource, options) {
		var roles = _.has(options.hash, 'roles') ? options.hash.roles : undefined;
		if(MSAcl.canCreate(resource,roles)){
			return options.fn(this);
		}
	});

	Handlebars.registerHelper("canUpdate", function(resource, options) {
		var roles = _.has(options.hash, 'roles') ? options.hash.roles : undefined;
		if(MSAcl.canUpdate(resource,roles)){
			return options.fn(this);
		}
	});

	Handlebars.registerHelper("canDelete", function(resource, options) {
		var roles = _.has(options.hash, 'roles') ? options.hash.roles : undefined;
		if(MSAcl.canDelete(resource,roles)){
			return options.fn(this);
		}
	});

	/**
     * Convert new line (\n\r) to <br>
     * from http://phpjs.org/functions/nl2br:480
     */
	HHelpers.nl2br = function(text) {
        text = Handlebars.Utils.escapeExpression(text);
        var nl2br = (text + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1' + '<br>' + '$2');
        return new Handlebars.SafeString(nl2br);
    };
	Handlebars.registerHelper('nl2br', HHelpers.nl2br);
	
	/*
	 * escapeHtmlChar
	 */
	Handlebars.registerHelper("escapeHtmlChar", function(str) {
		return Util.escapeHtmlChar(str);
	});
	
	Handlebars.registerHelper("nl2brAndEscapeHtmlChar", function(str) {
		return Util.nl2brAndEscapeHtmlChar(str);
	});
	
	/*
	 * required to fetch label for enum value
	 */ 
	Handlebars.registerHelper('convertEnumValueToLabel', function(enumName,
			enumValue) {
		return Util.enumValueToLabel(
				Util.getEnum(enumName), enumValue);
	});
	
	/*
	 * required to format date time
	 */
	Handlebars.registerHelper('convertFormatDateTime', function(cellValue) {
		return Util.formatDateTime(cellValue);
	});
	Handlebars.registerHelper('formatDate', function(val) {
        if(!val) return "";
		return Util.formatDate(val);
	});
	Handlebars.registerHelper('formatDateCustom', function(val,format) {
        if(!val) return "";
		var dateObj = Util.DBToDateObj(val); 
		return Globalize.format(dateObj,format);
		//return Globalize.format((_.isString(val)?new Date(val):val),format);
	});
	
	Handlebars.registerHelper('toHumanDate', function(val) {
        if(!val) return "";
		//return Util.toHumanDate(val);
		return localization.formatDate(val, 'f');
	});
	
	Handlebars.registerHelper('dateFormat', function(context, block) {
		if (window.moment) {
			var f = block.hash.format || "MMM Do, YYYY";
			return moment(context).format(f);
		}else{
			return context;   //  moment plugin not available. return data as is.
		};
	});

	/*
	 * Truncate the String till n positions
	 */
	Handlebars.registerHelper('truncStr', function(str, n, useWordBoundary) {
		var len = n || 1;
		var useWordBn = useWordBoundary || false;
		return str.trunc(len, useWordBn);
	});
	
	
	Handlebars.registerHelper('tt', function(str) {
		return localization.tt(str);
		return str;
	});
	
	Handlebars.registerHelper('getCopyrightDate', function() {
		return new Date().getFullYear().toString();
	});
	
	Handlebars.registerHelper('paginate', function(totalCount,pageSize) {
        if(typeof pageSize === 'undefined'){
            pageSize = 25;
        }
        var html = '',
        fromPage = 0, i = 1;
        var index = parseInt(totalCount/pageSize);
        if(index == 0){
            return html;
        }
        for (; i <= index; i++) {
            if(i == 1){
                html += '<li class="active" data-page='+fromPage+'><a href="javascript:;">'+i+'</a></li>';
            }else{
                html += '<li data-page='+fromPage+'><a href="javascript:;">'+i+'</a></li>';
            }
            fromPage = pageSize * i; 
        }
        if((totalCount - pageSize*index) > 0){
            html += '<li data-page='+fromPage+'><a href="javascript:;">'+i+'</a></li>';
        }
        return html;
	});
	
	Handlebars.registerHelper('customPermString', function(permsString,kclass) {
		if(permsString == "--")
			return permsString;
		permArr = permsString.split(',');
		//return permArr.join(', ');
		var cl = _.isObject(kclass) ? 'label label-info' : kclass;
		var tempArr = [];
		_.each(permArr, function(val){
			tempArr.push('<label class="'+cl+'">'+val+'</label>');
		});
		return tempArr.join(' ');
		
	});

	/*
	 * Link helper
	 * @linkType : type of link from XALinks.js
	 * The options to be passed to XALinks get method :
	 * @linkOpts : {
	 * 		model : model,
	 * }
	 * @htmlOpts : {
	 * 		class : "myClass"
	 * }
	 * {{{a Account null class="myClass" }}}
	 * 
	 */
	
	// HHelpers.a = function(linkType, linkOpts, htmlOpts) {
		
	// 	var XALinks	= require("modules/XALinks");
	// 	var linkObj	= XALinks.get(linkType, linkOpts);
	// 	var attrs	= [];
	// 	htmlOpts	= htmlOpts || {}; // Handle the case if a() is called from outside of Handlebars
	// 	for(var prop in htmlOpts.hash) {
	// 		attrs.push(prop + '="' + htmlOpts.hash[prop] + '"');
	// 	}
	// 	attrs.push('href="' + linkObj.href + '"');
	// 	attrs.push('title="' + localization.tt(linkObj.title) + '"');
		
	// 	return new Handlebars.SafeString("<a " + attrs.join(" ") + ">" + localization.tt(linkObj.text) + "</a>");
	// };

	// Handlebars.registerHelper('a', HHelpers.a);
	
	// Handlebars.registerHelper('getImage', function(fileType){
	// 	var path = "images/folderIcon.png";
	// 	if(fileType == XAEnums.FileType.FILE_FILE.value){
	// 		path = "images/file-icon.png";
	// 	}
	// 	return path;
	// });
	
	// HHelpers.showWeekAbbr = function() { 
	// 	var html = '';
	// 	localization.getDaysOfWeek().forEach(function(v,idx){
	// 	   if(v){
	// 		   html += '<option value="'+idx+'">'+v+'</option>';
	// 	   }
	// 	});   	
	// 	return html;
	// };
	// Handlebars.registerHelper('showWeekAbbr', HHelpers.showWeekAbbr);
	
	// HHelpers.showDays = function() { 
	// 	var html = '';
	// 	for(var i=0,j=1;i<28;i++){
	// 	   html += '<option value="'+i+'">'+(j++)+'</option>';
	// 	}   	
	// 	return html;
	// };
	// Handlebars.registerHelper('showDays', HHelpers.showDays);
	
	Handlebars.registerHelper('if_eq', function(context, options) {
	        if (context == options.hash.compare)
	            return options.fn(this);
	        return options.inverse(this);
	    });
	
	Handlebars.registerHelper('if_gt', function(context, options) {
	        if (context > options.hash.compare)
	            return options.fn(this);
	        return options.inverse(this);
	    });
	
    Handlebars.registerHelper('avatarHack', function(model) {
            return 'styles/images/avatar' + parseInt(((Math.random()*10)%4) + 1,10) + '.png';
        });    
	Handlebars.registerHelper('isSystemAdmin', function(context, options) {
            if(SessionMgr.isSystemAdmin())
            	return options.fn(this);
			return options.inverse(this);
	});
	Handlebars.registerHelper('isSchoolAdmin', function(context, options) {
            if(SessionMgr.isSchoolAdmin() || SessionMgr.isSystemAdmin())
            	return options.fn(this);
			return options.inverse(this);
	});
	Handlebars.registerHelper('isTeacher', function(context, options) {
            if(SessionMgr.isTeacher())
            	return options.fn(this);
			return options.inverse(this);
	});
	Handlebars.registerHelper('getAvatar', function(options,size) {
		var path;
			if(_.has(options,'profileImageGId')){
				if(_.isUndefined(size)) 
					path = "service/content/multimedia/image/"+options.profileImageGId;
				else
					path = "service/content/multimedia/image/"+options.profileImageGId+"/small";
			}else{
				path = "styles/images/s-avatar.png";
			}
		return path;
			//return path;
            //return Util.getImgPath(id,size);
	});

	Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {

		switch (operator) {
			case '==':
				return (v1 == v2) ? options.fn(this) : options.inverse(this);
			break;
			case '===':
				return (v1 === v2) ? options.fn(this) : options.inverse(this);
			break;
			case '<':
				return (v1 < v2) ? options.fn(this) : options.inverse(this);
			break;
			case '<=':
				return (v1 <= v2) ? options.fn(this) : options.inverse(this);
			break;
			case '>':
				return (v1 > v2) ? options.fn(this) : options.inverse(this);
			break;
			case '>=':
				return (v1 >= v2) ? options.fn(this) : options.inverse(this);
			break;
			default:
				return options.inverse(this);     
			break;
		}
		//return options.inverse(this);
	});
	
	//For Example
	/*{{#compare numberone "eq" numbretwo}}
	  do something
	{{else}}
	  do something else
	{{/compare}}
*/	
	Handlebars.registerHelper( "compare", function( v1, op, v2, options ) {

		  var c = {
		    "eq": function( v1, v2 ) {
		      return v1 == v2;
		    },
		    "neq": function( v1, v2 ) {
		      return v1 != v2;
		    }
		   
		  };

		  if( Object.prototype.hasOwnProperty.call( c, op ) ) {
		    return c[ op ].call( this, v1, v2 ) ? options.fn( this ) : options.inverse( this );
		  }
		  return options.inverse( this );
	} );
	//For Example
	//{{#eachProperty object}}
    //{{property}}: {{value}}<br/>
    //{{/eachProperty }}
	Handlebars.registerHelper('eachProperty', function(context, options) {
	    var ret = "";
	    for(var prop in context)
	    {
	        ret = ret + options.fn({property:prop,value:context[prop]});
	    }
	    return new Handlebars.SafeString(ret);
	});
	Handlebars.registerHelper('highlightNewForAttr', function(newValue, oldValue, hightlightValue) {
		var html='';
		if(hightlightValue == 'new'){
			if(_.isNull(oldValue) || oldValue == '--' || oldValue == "" || _.isUndefined(oldValue)){
				html = '<span class="add-text">'+newValue+'</span>';
			}else
				html = '<span class="">'+newValue+'</span>';
		}else{
			if(_.isNull(newValue) || newValue == '--' || newValue == ""){
				html = '<span class="delete-text">'+oldValue+'</span>';
			}else
				html = '<span class="">'+oldValue+'</span>';
		}
	    return html;
	});
	Handlebars.registerHelper('highlightNewForObj', function(prop, newValue, oldValue, hightlightValue) {
		var html='';
		if(hightlightValue == 'new'){
			if(_.isNull(oldValue[prop]) || oldValue[prop] == ""){
				html = '<span class="add-text">'+newValue+'</span>';
			}else
				html = '<span class="">'+newValue+'</span>';
		}else{
			if(_.isNull(oldValue[prop]) || oldValue[prop] == ""){
				html = '<span class="delete-text">'+newValue+'</span>';
			}else
				html = '<span class="">'+newValue+'</span>';
		}
	    return html;
	});
	Handlebars.registerHelper('highlightForPlugableServiceModel', function(newValue, oldValue, hightlightValue, attrName) {
		if(attrName != 'Policy Resources'){
			return hightlightValue == 'old' ? _.escape(oldValue) : _.escape(newValue);
		}
		newValue = newValue.split(',')
		oldValue = oldValue.split(',')
		var html='';
		if(hightlightValue == 'new'){
			_.each(newValue, function(val) {
				if($.inArray(val, oldValue) < 0){
					html += '<span class="add-text">'+_.escape(val)+'</span>';
				}else{
					html += '<span>'+_.escape(val)+'</span>';
				}
				html+='<span>,</span>';
			});
		}else{
			_.each(oldValue, function(val) {
				if($.inArray(val, newValue) < 0){
					html += '<span class="delete-text">'+_.escape(val)+'</span>';
				}else{
					html += '<span>'+_.escape(val)+'</span>';
				}
				html+='<span>,</span>';
				
			});
		}
	    return html;
	});
	Handlebars.registerHelper('highlightUsersForArr', function(val, arr, hightlightValue) {
		var html = val;
		if(hightlightValue == 'new'){
			if($.inArray(val, arr) < 0)
				html = '<span class="add-text">'+val+'</span>';
		}else{
			if($.inArray(val, arr) < 0)
				return html = '<span class="delete-text">'+val+'</span>';
		}
	    return html;
	});
	Handlebars.registerHelper('highlightPermissionsForUser', function(perm, newValue, pemList, hightlightValue) {
		var type = 'permType';
		if(_.isUndefined(perm.permType))
			type = 'ipAddress';
		var html = perm[type];
		if(hightlightValue == 'old'){
			if(_.isNull(perm[type]) || perm[type] != ""){
				if(!_.isUndefined(pemList[perm.userName]) || _.isEmpty(pemList)){
					var isRemoved = true;
					_.each(pemList[perm.userName] ,function(m){
						if(m[type] == perm[type])
							isRemoved = false;
					});
					if(isRemoved)
						return html = '<span class="delete-text">'+perm[type]+'</span>';
				}else{
					return html = '<span class="delete-text">'+perm[type]+'</span>';
				}
			}
		}else{
			if(_.isNull(perm[type]) || perm[type] != ""){
				if(!_.isUndefined(pemList[perm.userName])){
					var isNewAdd = true;
					_.each(pemList[perm.userName] ,function(m){
						if(m[type] == perm[type])
							isNewAdd = false;
					});
					if(isNewAdd)
						return html = '<span class="add-text">'+perm[type]+'</span>';
				}else{
					return html = '<span class="delete-text">'+perm[type]+'</span>';
				}
			}
		}
	    return html;
	});
	Handlebars.registerHelper('highlightPermissionsForGroup', function(perm, newValue, pemList, hightlightValue) {
		var type = 'permType';
		if(_.isUndefined(perm.permType))
			type = 'ipAddress';
		var html = perm[type];
		if(hightlightValue == 'old'){
			if(_.isNull(perm[type]) || perm[type] != ""){
				if(!_.isUndefined(pemList[perm.groupName]) || _.isEmpty(pemList)){
					var isRemoved = true;
					_.each(pemList[perm.groupName] ,function(m){
						if(m[type] == perm[type])
							isRemoved = false;
					});
					if(isRemoved)
						return html = '<span class="delete-text">'+perm[type]+'</span>';
				}else{
					return html = '<span class="delete-text">'+perm[type]+'</span>';
				}
			}
		}else{
			if(_.isNull(perm[type]) || perm[type] != ""){
				if(!_.isUndefined(pemList[perm.groupName])){
					var isNewAdd = true;
					_.each(pemList[perm.groupName] ,function(m){
						if(m[type] == perm[type])
							isNewAdd = false;
					});
					if(isNewAdd)
						return html = '<span class="add-text">'+perm[type]+'</span>';
				}
				else{
					return html = '<span class="add-text">'+perm[type]+'</span>';
				}
			}
		}
	    return new Handlebars.SafeString(html);
	});
	// Handlebars.registerHelper('getServices', function(services, serviceDef) {
	// 	var XAEnums			= require('utils/XAEnums');
	// 	var tr = '', serviceOperationDiv = '';
	// 	var serviceType = serviceDef.get('name');
	// 	if(!_.isUndefined(services[serviceType])){
	// 		_.each(services[serviceType],function(serv){
	// 			serviceName = serv.get('name');
	// 			if(SessionMgr.isSystemAdmin()){
	// 				serviceOperationDiv = '<div class="pull-right">\
	// 				<a data-id="'+serv.id+'" class="btn btn-mini" href="#!/service/'+serviceDef.id+'/edit/'+serv.id+'" title="Edit"><i class="icon-edit"></i></a>\
	// 				<a data-id="'+serv.id+'" class="deleteRepo btn btn-mini btn-danger" href="javascript:void(0);" title="Delete">\
	// 				<i class="icon-trash"></i></a>\
	// 				</div>'
	// 			}
	// 			tr += '<tr><td><div>\
	// 					<a data-id="'+serv.id+'" href="#!/service/'+serv.id+'/policies">'+_.escape(serv.attributes.name)+'</a>'+serviceOperationDiv+'\
	// 				  </div></td></tr>';
	// 		});
	// 	}
	// 	return tr;
	// });
	Handlebars.registerHelper('capitaliseLetter', function(str) {
		return str.toUpperCase();
	});
	// Handlebars.registerHelper('hasAccessToTab', function(tabName,options) {
	// 	var vxPortalUser = SessionMgr.getUserProfile();
	// 	var userModules = _.pluck(vxPortalUser.get('userPermList'), 'moduleName');
	// 	var groupModules = _.pluck(vxPortalUser.get('groupPermissions'), 'moduleName');
	// 	var moduleNames =  _.union(userModules,groupModules);
	// 	var returnFlag = _.contains(moduleNames, tabName);
	// 	if (returnFlag)
	// 		return options.fn(this);
	// 	else
	// 		return options.inverse(this);
	// });

	/*logserach*/
	Handlebars.registerHelper ("setChecked", function (value, currentValue) {
	    if ( value == currentValue ) {
	       return "checked"
	    } else {
	       return "";
	    }
 	});

	return HHelpers;
});
