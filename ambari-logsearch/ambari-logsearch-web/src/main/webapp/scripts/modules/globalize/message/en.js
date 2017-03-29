/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
define(['require','globalize'],function(require,Globalize) {
	'use strict';

	Globalize.addCultureInfo( "en", {
        messages:                  {
        	// Form labels, Table headers etc
			lbl : {
				// Common
				// Accounts
				// MSLinks
				/*
				 * Menu related
				 */
				home 						: 'Home',
				name 						: 'Name',
				password					: 'Password',
				passwordConfirm				: 'Password Confirm',
			},
			btn : {
				add							: 'Add',
				save						: 'Save',
				cancel 						: 'Cancel',
				addMore						: 'Add More..',
				stayOnPage					: 'Stay on this page',
				leavePage					: 'Leave this page',
				setVisibility               : 'Set Visibility' 
				
			},
			// h1, h2, h3, fieldset, title
			h : {
				welcome						: 'Welcome',
				logout 						: 'Logout',
	
				// Menu
				dashboard					: 'Dashboard',
			},
			msg : {
				noRecordsFound			  : 'No Records Found',
			},
			plcHldr : {
				search 						:'Search',
			},
			dialogMsg :{
			},	
			validationMessages : {
				required 					: "* This field is required",
				onlyLetterNumberUnderscore :'* Only Alpha Numeric and underscore characters are allowed',
				alphaNumericUnderscoreDotComma :'* Only Alpha Numeric,underscore,comma,hypen,dot and space characters are allowed',
				oldPasswordError :'Your password does not match. Please try again with proper password',
				oldPasswordRepeatError :'You can not use old password.',
				newPasswordError :'Invalid Password.Minimum 8 characters with min one alphabet and one numeric.',
				emailIdError				: 'Please enter valid email address.',
				enterValidName				: 'Please enter valid name.',
				passwordError	            :'Invalid Password.Minimum 8 characters with min one alphabet and one numeric.'
			},
			serverMsg : {
			}
			

        }
    });
});
