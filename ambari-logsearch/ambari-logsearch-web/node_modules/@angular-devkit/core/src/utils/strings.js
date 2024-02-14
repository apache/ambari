"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const STRING_DASHERIZE_REGEXP = (/[ _]/g);
const STRING_DECAMELIZE_REGEXP = (/([a-z\d])([A-Z])/g);
const STRING_CAMELIZE_REGEXP = (/(-|_|\.|\s)+(.)?/g);
const STRING_UNDERSCORE_REGEXP_1 = (/([a-z\d])([A-Z]+)/g);
const STRING_UNDERSCORE_REGEXP_2 = (/-|\s+/g);
/**
 * Converts a camelized string into all lower case separated by underscores.
 *
 ```javascript
 decamelize('innerHTML');         // 'inner_html'
 decamelize('action_name');       // 'action_name'
 decamelize('css-class-name');    // 'css-class-name'
 decamelize('my favorite items'); // 'my favorite items'
 ```

 @method decamelize
 @param {String} str The string to decamelize.
 @return {String} the decamelized string.
 */
function decamelize(str) {
    return str.replace(STRING_DECAMELIZE_REGEXP, '$1_$2').toLowerCase();
}
exports.decamelize = decamelize;
/**
 Replaces underscores, spaces, or camelCase with dashes.

 ```javascript
 dasherize('innerHTML');         // 'inner-html'
 dasherize('action_name');       // 'action-name'
 dasherize('css-class-name');    // 'css-class-name'
 dasherize('my favorite items'); // 'my-favorite-items'
 ```

 @method dasherize
 @param {String} str The string to dasherize.
 @return {String} the dasherized string.
 */
function dasherize(str) {
    return decamelize(str).replace(STRING_DASHERIZE_REGEXP, '-');
}
exports.dasherize = dasherize;
/**
 Returns the lowerCamelCase form of a string.

 ```javascript
 camelize('innerHTML');          // 'innerHTML'
 camelize('action_name');        // 'actionName'
 camelize('css-class-name');     // 'cssClassName'
 camelize('my favorite items');  // 'myFavoriteItems'
 camelize('My Favorite Items');  // 'myFavoriteItems'
 ```

 @method camelize
 @param {String} str The string to camelize.
 @return {String} the camelized string.
 */
function camelize(str) {
    return str
        .replace(STRING_CAMELIZE_REGEXP, (_match, _separator, chr) => {
        return chr ? chr.toUpperCase() : '';
    })
        .replace(/^([A-Z])/, (match) => match.toLowerCase());
}
exports.camelize = camelize;
/**
 Returns the UpperCamelCase form of a string.

 ```javascript
 'innerHTML'.classify();          // 'InnerHTML'
 'action_name'.classify();        // 'ActionName'
 'css-class-name'.classify();     // 'CssClassName'
 'my favorite items'.classify();  // 'MyFavoriteItems'
 ```

 @method classify
 @param {String} str the string to classify
 @return {String} the classified string
 */
function classify(str) {
    return str.split('.').map(part => capitalize(camelize(part))).join('.');
}
exports.classify = classify;
/**
 More general than decamelize. Returns the lower\_case\_and\_underscored
 form of a string.

 ```javascript
 'innerHTML'.underscore();          // 'inner_html'
 'action_name'.underscore();        // 'action_name'
 'css-class-name'.underscore();     // 'css_class_name'
 'my favorite items'.underscore();  // 'my_favorite_items'
 ```

 @method underscore
 @param {String} str The string to underscore.
 @return {String} the underscored string.
 */
function underscore(str) {
    return str
        .replace(STRING_UNDERSCORE_REGEXP_1, '$1_$2')
        .replace(STRING_UNDERSCORE_REGEXP_2, '_')
        .toLowerCase();
}
exports.underscore = underscore;
/**
 Returns the Capitalized form of a string

 ```javascript
 'innerHTML'.capitalize()         // 'InnerHTML'
 'action_name'.capitalize()       // 'Action_name'
 'css-class-name'.capitalize()    // 'Css-class-name'
 'my favorite items'.capitalize() // 'My favorite items'
 ```

 @method capitalize
 @param {String} str The string to capitalize.
 @return {String} The capitalized string.
 */
function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.substr(1);
}
exports.capitalize = capitalize;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoic3RyaW5ncy5qcyIsInNvdXJjZVJvb3QiOiIvVXNlcnMvaGFuc2wvU291cmNlcy9kZXZraXQvIiwic291cmNlcyI6WyJwYWNrYWdlcy9hbmd1bGFyX2RldmtpdC9jb3JlL3NyYy91dGlscy9zdHJpbmdzLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7O0FBQUE7Ozs7OztHQU1HO0FBQ0gsTUFBTSx1QkFBdUIsR0FBRyxDQUFDLE9BQU8sQ0FBQyxDQUFDO0FBQzFDLE1BQU0sd0JBQXdCLEdBQUcsQ0FBQyxtQkFBbUIsQ0FBQyxDQUFDO0FBQ3ZELE1BQU0sc0JBQXNCLEdBQUcsQ0FBQyxtQkFBbUIsQ0FBQyxDQUFDO0FBQ3JELE1BQU0sMEJBQTBCLEdBQUcsQ0FBQyxvQkFBb0IsQ0FBQyxDQUFDO0FBQzFELE1BQU0sMEJBQTBCLEdBQUcsQ0FBQyxRQUFRLENBQUMsQ0FBQztBQUU5Qzs7Ozs7Ozs7Ozs7OztHQWFHO0FBQ0gsb0JBQTJCLEdBQVc7SUFDcEMsTUFBTSxDQUFDLEdBQUcsQ0FBQyxPQUFPLENBQUMsd0JBQXdCLEVBQUUsT0FBTyxDQUFDLENBQUMsV0FBVyxFQUFFLENBQUM7QUFDdEUsQ0FBQztBQUZELGdDQUVDO0FBRUQ7Ozs7Ozs7Ozs7Ozs7R0FhRztBQUNILG1CQUEwQixHQUFXO0lBQ25DLE1BQU0sQ0FBQyxVQUFVLENBQUMsR0FBRyxDQUFDLENBQUMsT0FBTyxDQUFDLHVCQUF1QixFQUFFLEdBQUcsQ0FBQyxDQUFDO0FBQy9ELENBQUM7QUFGRCw4QkFFQztBQUVEOzs7Ozs7Ozs7Ozs7OztHQWNHO0FBQ0gsa0JBQXlCLEdBQVc7SUFDbEMsTUFBTSxDQUFDLEdBQUc7U0FDUCxPQUFPLENBQUMsc0JBQXNCLEVBQUUsQ0FBQyxNQUFjLEVBQUUsVUFBa0IsRUFBRSxHQUFXO1FBQy9FLE1BQU0sQ0FBQyxHQUFHLEdBQUcsR0FBRyxDQUFDLFdBQVcsRUFBRSxHQUFHLEVBQUUsQ0FBQztJQUN0QyxDQUFDLENBQUM7U0FDRCxPQUFPLENBQUMsVUFBVSxFQUFFLENBQUMsS0FBYSxLQUFLLEtBQUssQ0FBQyxXQUFXLEVBQUUsQ0FBQyxDQUFDO0FBQ2pFLENBQUM7QUFORCw0QkFNQztBQUVEOzs7Ozs7Ozs7Ozs7O0dBYUc7QUFDSCxrQkFBeUIsR0FBVztJQUNsQyxNQUFNLENBQUMsR0FBRyxDQUFDLEtBQUssQ0FBQyxHQUFHLENBQUMsQ0FBQyxHQUFHLENBQUMsSUFBSSxJQUFJLFVBQVUsQ0FBQyxRQUFRLENBQUMsSUFBSSxDQUFDLENBQUMsQ0FBQyxDQUFDLElBQUksQ0FBQyxHQUFHLENBQUMsQ0FBQztBQUMxRSxDQUFDO0FBRkQsNEJBRUM7QUFFRDs7Ozs7Ozs7Ozs7Ozs7R0FjRztBQUNILG9CQUEyQixHQUFXO0lBQ3BDLE1BQU0sQ0FBQyxHQUFHO1NBQ1AsT0FBTyxDQUFDLDBCQUEwQixFQUFFLE9BQU8sQ0FBQztTQUM1QyxPQUFPLENBQUMsMEJBQTBCLEVBQUUsR0FBRyxDQUFDO1NBQ3hDLFdBQVcsRUFBRSxDQUFDO0FBQ25CLENBQUM7QUFMRCxnQ0FLQztBQUVEOzs7Ozs7Ozs7Ozs7O0dBYUc7QUFDSCxvQkFBMkIsR0FBVztJQUNwQyxNQUFNLENBQUMsR0FBRyxDQUFDLE1BQU0sQ0FBQyxDQUFDLENBQUMsQ0FBQyxXQUFXLEVBQUUsR0FBRyxHQUFHLENBQUMsTUFBTSxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3JELENBQUM7QUFGRCxnQ0FFQyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGxpY2Vuc2VcbiAqIENvcHlyaWdodCBHb29nbGUgSW5jLiBBbGwgUmlnaHRzIFJlc2VydmVkLlxuICpcbiAqIFVzZSBvZiB0aGlzIHNvdXJjZSBjb2RlIGlzIGdvdmVybmVkIGJ5IGFuIE1JVC1zdHlsZSBsaWNlbnNlIHRoYXQgY2FuIGJlXG4gKiBmb3VuZCBpbiB0aGUgTElDRU5TRSBmaWxlIGF0IGh0dHBzOi8vYW5ndWxhci5pby9saWNlbnNlXG4gKi9cbmNvbnN0IFNUUklOR19EQVNIRVJJWkVfUkVHRVhQID0gKC9bIF9dL2cpO1xuY29uc3QgU1RSSU5HX0RFQ0FNRUxJWkVfUkVHRVhQID0gKC8oW2EtelxcZF0pKFtBLVpdKS9nKTtcbmNvbnN0IFNUUklOR19DQU1FTElaRV9SRUdFWFAgPSAoLygtfF98XFwufFxccykrKC4pPy9nKTtcbmNvbnN0IFNUUklOR19VTkRFUlNDT1JFX1JFR0VYUF8xID0gKC8oW2EtelxcZF0pKFtBLVpdKykvZyk7XG5jb25zdCBTVFJJTkdfVU5ERVJTQ09SRV9SRUdFWFBfMiA9ICgvLXxcXHMrL2cpO1xuXG4vKipcbiAqIENvbnZlcnRzIGEgY2FtZWxpemVkIHN0cmluZyBpbnRvIGFsbCBsb3dlciBjYXNlIHNlcGFyYXRlZCBieSB1bmRlcnNjb3Jlcy5cbiAqXG4gYGBgamF2YXNjcmlwdFxuIGRlY2FtZWxpemUoJ2lubmVySFRNTCcpOyAgICAgICAgIC8vICdpbm5lcl9odG1sJ1xuIGRlY2FtZWxpemUoJ2FjdGlvbl9uYW1lJyk7ICAgICAgIC8vICdhY3Rpb25fbmFtZSdcbiBkZWNhbWVsaXplKCdjc3MtY2xhc3MtbmFtZScpOyAgICAvLyAnY3NzLWNsYXNzLW5hbWUnXG4gZGVjYW1lbGl6ZSgnbXkgZmF2b3JpdGUgaXRlbXMnKTsgLy8gJ215IGZhdm9yaXRlIGl0ZW1zJ1xuIGBgYFxuXG4gQG1ldGhvZCBkZWNhbWVsaXplXG4gQHBhcmFtIHtTdHJpbmd9IHN0ciBUaGUgc3RyaW5nIHRvIGRlY2FtZWxpemUuXG4gQHJldHVybiB7U3RyaW5nfSB0aGUgZGVjYW1lbGl6ZWQgc3RyaW5nLlxuICovXG5leHBvcnQgZnVuY3Rpb24gZGVjYW1lbGl6ZShzdHI6IHN0cmluZyk6IHN0cmluZyB7XG4gIHJldHVybiBzdHIucmVwbGFjZShTVFJJTkdfREVDQU1FTElaRV9SRUdFWFAsICckMV8kMicpLnRvTG93ZXJDYXNlKCk7XG59XG5cbi8qKlxuIFJlcGxhY2VzIHVuZGVyc2NvcmVzLCBzcGFjZXMsIG9yIGNhbWVsQ2FzZSB3aXRoIGRhc2hlcy5cblxuIGBgYGphdmFzY3JpcHRcbiBkYXNoZXJpemUoJ2lubmVySFRNTCcpOyAgICAgICAgIC8vICdpbm5lci1odG1sJ1xuIGRhc2hlcml6ZSgnYWN0aW9uX25hbWUnKTsgICAgICAgLy8gJ2FjdGlvbi1uYW1lJ1xuIGRhc2hlcml6ZSgnY3NzLWNsYXNzLW5hbWUnKTsgICAgLy8gJ2Nzcy1jbGFzcy1uYW1lJ1xuIGRhc2hlcml6ZSgnbXkgZmF2b3JpdGUgaXRlbXMnKTsgLy8gJ215LWZhdm9yaXRlLWl0ZW1zJ1xuIGBgYFxuXG4gQG1ldGhvZCBkYXNoZXJpemVcbiBAcGFyYW0ge1N0cmluZ30gc3RyIFRoZSBzdHJpbmcgdG8gZGFzaGVyaXplLlxuIEByZXR1cm4ge1N0cmluZ30gdGhlIGRhc2hlcml6ZWQgc3RyaW5nLlxuICovXG5leHBvcnQgZnVuY3Rpb24gZGFzaGVyaXplKHN0cjogc3RyaW5nKTogc3RyaW5nIHtcbiAgcmV0dXJuIGRlY2FtZWxpemUoc3RyKS5yZXBsYWNlKFNUUklOR19EQVNIRVJJWkVfUkVHRVhQLCAnLScpO1xufVxuXG4vKipcbiBSZXR1cm5zIHRoZSBsb3dlckNhbWVsQ2FzZSBmb3JtIG9mIGEgc3RyaW5nLlxuXG4gYGBgamF2YXNjcmlwdFxuIGNhbWVsaXplKCdpbm5lckhUTUwnKTsgICAgICAgICAgLy8gJ2lubmVySFRNTCdcbiBjYW1lbGl6ZSgnYWN0aW9uX25hbWUnKTsgICAgICAgIC8vICdhY3Rpb25OYW1lJ1xuIGNhbWVsaXplKCdjc3MtY2xhc3MtbmFtZScpOyAgICAgLy8gJ2Nzc0NsYXNzTmFtZSdcbiBjYW1lbGl6ZSgnbXkgZmF2b3JpdGUgaXRlbXMnKTsgIC8vICdteUZhdm9yaXRlSXRlbXMnXG4gY2FtZWxpemUoJ015IEZhdm9yaXRlIEl0ZW1zJyk7ICAvLyAnbXlGYXZvcml0ZUl0ZW1zJ1xuIGBgYFxuXG4gQG1ldGhvZCBjYW1lbGl6ZVxuIEBwYXJhbSB7U3RyaW5nfSBzdHIgVGhlIHN0cmluZyB0byBjYW1lbGl6ZS5cbiBAcmV0dXJuIHtTdHJpbmd9IHRoZSBjYW1lbGl6ZWQgc3RyaW5nLlxuICovXG5leHBvcnQgZnVuY3Rpb24gY2FtZWxpemUoc3RyOiBzdHJpbmcpOiBzdHJpbmcge1xuICByZXR1cm4gc3RyXG4gICAgLnJlcGxhY2UoU1RSSU5HX0NBTUVMSVpFX1JFR0VYUCwgKF9tYXRjaDogc3RyaW5nLCBfc2VwYXJhdG9yOiBzdHJpbmcsIGNocjogc3RyaW5nKSA9PiB7XG4gICAgICByZXR1cm4gY2hyID8gY2hyLnRvVXBwZXJDYXNlKCkgOiAnJztcbiAgICB9KVxuICAgIC5yZXBsYWNlKC9eKFtBLVpdKS8sIChtYXRjaDogc3RyaW5nKSA9PiBtYXRjaC50b0xvd2VyQ2FzZSgpKTtcbn1cblxuLyoqXG4gUmV0dXJucyB0aGUgVXBwZXJDYW1lbENhc2UgZm9ybSBvZiBhIHN0cmluZy5cblxuIGBgYGphdmFzY3JpcHRcbiAnaW5uZXJIVE1MJy5jbGFzc2lmeSgpOyAgICAgICAgICAvLyAnSW5uZXJIVE1MJ1xuICdhY3Rpb25fbmFtZScuY2xhc3NpZnkoKTsgICAgICAgIC8vICdBY3Rpb25OYW1lJ1xuICdjc3MtY2xhc3MtbmFtZScuY2xhc3NpZnkoKTsgICAgIC8vICdDc3NDbGFzc05hbWUnXG4gJ215IGZhdm9yaXRlIGl0ZW1zJy5jbGFzc2lmeSgpOyAgLy8gJ015RmF2b3JpdGVJdGVtcydcbiBgYGBcblxuIEBtZXRob2QgY2xhc3NpZnlcbiBAcGFyYW0ge1N0cmluZ30gc3RyIHRoZSBzdHJpbmcgdG8gY2xhc3NpZnlcbiBAcmV0dXJuIHtTdHJpbmd9IHRoZSBjbGFzc2lmaWVkIHN0cmluZ1xuICovXG5leHBvcnQgZnVuY3Rpb24gY2xhc3NpZnkoc3RyOiBzdHJpbmcpOiBzdHJpbmcge1xuICByZXR1cm4gc3RyLnNwbGl0KCcuJykubWFwKHBhcnQgPT4gY2FwaXRhbGl6ZShjYW1lbGl6ZShwYXJ0KSkpLmpvaW4oJy4nKTtcbn1cblxuLyoqXG4gTW9yZSBnZW5lcmFsIHRoYW4gZGVjYW1lbGl6ZS4gUmV0dXJucyB0aGUgbG93ZXJcXF9jYXNlXFxfYW5kXFxfdW5kZXJzY29yZWRcbiBmb3JtIG9mIGEgc3RyaW5nLlxuXG4gYGBgamF2YXNjcmlwdFxuICdpbm5lckhUTUwnLnVuZGVyc2NvcmUoKTsgICAgICAgICAgLy8gJ2lubmVyX2h0bWwnXG4gJ2FjdGlvbl9uYW1lJy51bmRlcnNjb3JlKCk7ICAgICAgICAvLyAnYWN0aW9uX25hbWUnXG4gJ2Nzcy1jbGFzcy1uYW1lJy51bmRlcnNjb3JlKCk7ICAgICAvLyAnY3NzX2NsYXNzX25hbWUnXG4gJ215IGZhdm9yaXRlIGl0ZW1zJy51bmRlcnNjb3JlKCk7ICAvLyAnbXlfZmF2b3JpdGVfaXRlbXMnXG4gYGBgXG5cbiBAbWV0aG9kIHVuZGVyc2NvcmVcbiBAcGFyYW0ge1N0cmluZ30gc3RyIFRoZSBzdHJpbmcgdG8gdW5kZXJzY29yZS5cbiBAcmV0dXJuIHtTdHJpbmd9IHRoZSB1bmRlcnNjb3JlZCBzdHJpbmcuXG4gKi9cbmV4cG9ydCBmdW5jdGlvbiB1bmRlcnNjb3JlKHN0cjogc3RyaW5nKTogc3RyaW5nIHtcbiAgcmV0dXJuIHN0clxuICAgIC5yZXBsYWNlKFNUUklOR19VTkRFUlNDT1JFX1JFR0VYUF8xLCAnJDFfJDInKVxuICAgIC5yZXBsYWNlKFNUUklOR19VTkRFUlNDT1JFX1JFR0VYUF8yLCAnXycpXG4gICAgLnRvTG93ZXJDYXNlKCk7XG59XG5cbi8qKlxuIFJldHVybnMgdGhlIENhcGl0YWxpemVkIGZvcm0gb2YgYSBzdHJpbmdcblxuIGBgYGphdmFzY3JpcHRcbiAnaW5uZXJIVE1MJy5jYXBpdGFsaXplKCkgICAgICAgICAvLyAnSW5uZXJIVE1MJ1xuICdhY3Rpb25fbmFtZScuY2FwaXRhbGl6ZSgpICAgICAgIC8vICdBY3Rpb25fbmFtZSdcbiAnY3NzLWNsYXNzLW5hbWUnLmNhcGl0YWxpemUoKSAgICAvLyAnQ3NzLWNsYXNzLW5hbWUnXG4gJ215IGZhdm9yaXRlIGl0ZW1zJy5jYXBpdGFsaXplKCkgLy8gJ015IGZhdm9yaXRlIGl0ZW1zJ1xuIGBgYFxuXG4gQG1ldGhvZCBjYXBpdGFsaXplXG4gQHBhcmFtIHtTdHJpbmd9IHN0ciBUaGUgc3RyaW5nIHRvIGNhcGl0YWxpemUuXG4gQHJldHVybiB7U3RyaW5nfSBUaGUgY2FwaXRhbGl6ZWQgc3RyaW5nLlxuICovXG5leHBvcnQgZnVuY3Rpb24gY2FwaXRhbGl6ZShzdHI6IHN0cmluZyk6IHN0cmluZyB7XG4gIHJldHVybiBzdHIuY2hhckF0KDApLnRvVXBwZXJDYXNlKCkgKyBzdHIuc3Vic3RyKDEpO1xufVxuIl19