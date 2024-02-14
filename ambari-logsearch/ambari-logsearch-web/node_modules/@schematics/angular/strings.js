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
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoic3RyaW5ncy5qcyIsInNvdXJjZVJvb3QiOiIvVXNlcnMvaGFuc2wvU291cmNlcy9kZXZraXQvIiwic291cmNlcyI6WyJwYWNrYWdlcy9zY2hlbWF0aWNzL2FuZ3VsYXIvc3RyaW5ncy50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOztBQUFBOzs7Ozs7R0FNRztBQUNILE1BQU0sdUJBQXVCLEdBQUcsQ0FBQyxPQUFPLENBQUMsQ0FBQztBQUMxQyxNQUFNLHdCQUF3QixHQUFHLENBQUMsbUJBQW1CLENBQUMsQ0FBQztBQUN2RCxNQUFNLHNCQUFzQixHQUFHLENBQUMsbUJBQW1CLENBQUMsQ0FBQztBQUNyRCxNQUFNLDBCQUEwQixHQUFHLENBQUMsb0JBQW9CLENBQUMsQ0FBQztBQUMxRCxNQUFNLDBCQUEwQixHQUFHLENBQUMsUUFBUSxDQUFDLENBQUM7QUFFOUM7Ozs7Ozs7Ozs7Ozs7R0FhRztBQUNILG9CQUEyQixHQUFXO0lBQ3BDLE1BQU0sQ0FBQyxHQUFHLENBQUMsT0FBTyxDQUFDLHdCQUF3QixFQUFFLE9BQU8sQ0FBQyxDQUFDLFdBQVcsRUFBRSxDQUFDO0FBQ3RFLENBQUM7QUFGRCxnQ0FFQztBQUVEOzs7Ozs7Ozs7Ozs7O0dBYUc7QUFDSCxtQkFBMEIsR0FBVztJQUNuQyxNQUFNLENBQUMsVUFBVSxDQUFDLEdBQUcsQ0FBQyxDQUFDLE9BQU8sQ0FBQyx1QkFBdUIsRUFBRSxHQUFHLENBQUMsQ0FBQztBQUMvRCxDQUFDO0FBRkQsOEJBRUM7QUFFRDs7Ozs7Ozs7Ozs7Ozs7R0FjRztBQUNILGtCQUF5QixHQUFXO0lBQ2xDLE1BQU0sQ0FBQyxHQUFHO1NBQ1AsT0FBTyxDQUFDLHNCQUFzQixFQUFFLENBQUMsTUFBYyxFQUFFLFVBQWtCLEVBQUUsR0FBVztRQUMvRSxNQUFNLENBQUMsR0FBRyxHQUFHLEdBQUcsQ0FBQyxXQUFXLEVBQUUsR0FBRyxFQUFFLENBQUM7SUFDdEMsQ0FBQyxDQUFDO1NBQ0QsT0FBTyxDQUFDLFVBQVUsRUFBRSxDQUFDLEtBQWEsS0FBSyxLQUFLLENBQUMsV0FBVyxFQUFFLENBQUMsQ0FBQztBQUNqRSxDQUFDO0FBTkQsNEJBTUM7QUFFRDs7Ozs7Ozs7Ozs7OztHQWFHO0FBQ0gsa0JBQXlCLEdBQVc7SUFDbEMsTUFBTSxDQUFDLEdBQUcsQ0FBQyxLQUFLLENBQUMsR0FBRyxDQUFDLENBQUMsR0FBRyxDQUFDLElBQUksSUFBSSxVQUFVLENBQUMsUUFBUSxDQUFDLElBQUksQ0FBQyxDQUFDLENBQUMsQ0FBQyxJQUFJLENBQUMsR0FBRyxDQUFDLENBQUM7QUFDMUUsQ0FBQztBQUZELDRCQUVDO0FBRUQ7Ozs7Ozs7Ozs7Ozs7O0dBY0c7QUFDSCxvQkFBMkIsR0FBVztJQUNwQyxNQUFNLENBQUMsR0FBRztTQUNQLE9BQU8sQ0FBQywwQkFBMEIsRUFBRSxPQUFPLENBQUM7U0FDNUMsT0FBTyxDQUFDLDBCQUEwQixFQUFFLEdBQUcsQ0FBQztTQUN4QyxXQUFXLEVBQUUsQ0FBQztBQUNuQixDQUFDO0FBTEQsZ0NBS0M7QUFFRDs7Ozs7Ozs7Ozs7OztHQWFHO0FBQ0gsb0JBQTJCLEdBQVc7SUFDcEMsTUFBTSxDQUFDLEdBQUcsQ0FBQyxNQUFNLENBQUMsQ0FBQyxDQUFDLENBQUMsV0FBVyxFQUFFLEdBQUcsR0FBRyxDQUFDLE1BQU0sQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNyRCxDQUFDO0FBRkQsZ0NBRUMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBsaWNlbnNlXG4gKiBDb3B5cmlnaHQgR29vZ2xlIEluYy4gQWxsIFJpZ2h0cyBSZXNlcnZlZC5cbiAqXG4gKiBVc2Ugb2YgdGhpcyBzb3VyY2UgY29kZSBpcyBnb3Zlcm5lZCBieSBhbiBNSVQtc3R5bGUgbGljZW5zZSB0aGF0IGNhbiBiZVxuICogZm91bmQgaW4gdGhlIExJQ0VOU0UgZmlsZSBhdCBodHRwczovL2FuZ3VsYXIuaW8vbGljZW5zZVxuICovXG5jb25zdCBTVFJJTkdfREFTSEVSSVpFX1JFR0VYUCA9ICgvWyBfXS9nKTtcbmNvbnN0IFNUUklOR19ERUNBTUVMSVpFX1JFR0VYUCA9ICgvKFthLXpcXGRdKShbQS1aXSkvZyk7XG5jb25zdCBTVFJJTkdfQ0FNRUxJWkVfUkVHRVhQID0gKC8oLXxffFxcLnxcXHMpKyguKT8vZyk7XG5jb25zdCBTVFJJTkdfVU5ERVJTQ09SRV9SRUdFWFBfMSA9ICgvKFthLXpcXGRdKShbQS1aXSspL2cpO1xuY29uc3QgU1RSSU5HX1VOREVSU0NPUkVfUkVHRVhQXzIgPSAoLy18XFxzKy9nKTtcblxuLyoqXG4gKiBDb252ZXJ0cyBhIGNhbWVsaXplZCBzdHJpbmcgaW50byBhbGwgbG93ZXIgY2FzZSBzZXBhcmF0ZWQgYnkgdW5kZXJzY29yZXMuXG4gKlxuIGBgYGphdmFzY3JpcHRcbiBkZWNhbWVsaXplKCdpbm5lckhUTUwnKTsgICAgICAgICAvLyAnaW5uZXJfaHRtbCdcbiBkZWNhbWVsaXplKCdhY3Rpb25fbmFtZScpOyAgICAgICAvLyAnYWN0aW9uX25hbWUnXG4gZGVjYW1lbGl6ZSgnY3NzLWNsYXNzLW5hbWUnKTsgICAgLy8gJ2Nzcy1jbGFzcy1uYW1lJ1xuIGRlY2FtZWxpemUoJ215IGZhdm9yaXRlIGl0ZW1zJyk7IC8vICdteSBmYXZvcml0ZSBpdGVtcydcbiBgYGBcblxuIEBtZXRob2QgZGVjYW1lbGl6ZVxuIEBwYXJhbSB7U3RyaW5nfSBzdHIgVGhlIHN0cmluZyB0byBkZWNhbWVsaXplLlxuIEByZXR1cm4ge1N0cmluZ30gdGhlIGRlY2FtZWxpemVkIHN0cmluZy5cbiAqL1xuZXhwb3J0IGZ1bmN0aW9uIGRlY2FtZWxpemUoc3RyOiBzdHJpbmcpOiBzdHJpbmcge1xuICByZXR1cm4gc3RyLnJlcGxhY2UoU1RSSU5HX0RFQ0FNRUxJWkVfUkVHRVhQLCAnJDFfJDInKS50b0xvd2VyQ2FzZSgpO1xufVxuXG4vKipcbiBSZXBsYWNlcyB1bmRlcnNjb3Jlcywgc3BhY2VzLCBvciBjYW1lbENhc2Ugd2l0aCBkYXNoZXMuXG5cbiBgYGBqYXZhc2NyaXB0XG4gZGFzaGVyaXplKCdpbm5lckhUTUwnKTsgICAgICAgICAvLyAnaW5uZXItaHRtbCdcbiBkYXNoZXJpemUoJ2FjdGlvbl9uYW1lJyk7ICAgICAgIC8vICdhY3Rpb24tbmFtZSdcbiBkYXNoZXJpemUoJ2Nzcy1jbGFzcy1uYW1lJyk7ICAgIC8vICdjc3MtY2xhc3MtbmFtZSdcbiBkYXNoZXJpemUoJ215IGZhdm9yaXRlIGl0ZW1zJyk7IC8vICdteS1mYXZvcml0ZS1pdGVtcydcbiBgYGBcblxuIEBtZXRob2QgZGFzaGVyaXplXG4gQHBhcmFtIHtTdHJpbmd9IHN0ciBUaGUgc3RyaW5nIHRvIGRhc2hlcml6ZS5cbiBAcmV0dXJuIHtTdHJpbmd9IHRoZSBkYXNoZXJpemVkIHN0cmluZy5cbiAqL1xuZXhwb3J0IGZ1bmN0aW9uIGRhc2hlcml6ZShzdHI6IHN0cmluZyk6IHN0cmluZyB7XG4gIHJldHVybiBkZWNhbWVsaXplKHN0cikucmVwbGFjZShTVFJJTkdfREFTSEVSSVpFX1JFR0VYUCwgJy0nKTtcbn1cblxuLyoqXG4gUmV0dXJucyB0aGUgbG93ZXJDYW1lbENhc2UgZm9ybSBvZiBhIHN0cmluZy5cblxuIGBgYGphdmFzY3JpcHRcbiBjYW1lbGl6ZSgnaW5uZXJIVE1MJyk7ICAgICAgICAgIC8vICdpbm5lckhUTUwnXG4gY2FtZWxpemUoJ2FjdGlvbl9uYW1lJyk7ICAgICAgICAvLyAnYWN0aW9uTmFtZSdcbiBjYW1lbGl6ZSgnY3NzLWNsYXNzLW5hbWUnKTsgICAgIC8vICdjc3NDbGFzc05hbWUnXG4gY2FtZWxpemUoJ215IGZhdm9yaXRlIGl0ZW1zJyk7ICAvLyAnbXlGYXZvcml0ZUl0ZW1zJ1xuIGNhbWVsaXplKCdNeSBGYXZvcml0ZSBJdGVtcycpOyAgLy8gJ215RmF2b3JpdGVJdGVtcydcbiBgYGBcblxuIEBtZXRob2QgY2FtZWxpemVcbiBAcGFyYW0ge1N0cmluZ30gc3RyIFRoZSBzdHJpbmcgdG8gY2FtZWxpemUuXG4gQHJldHVybiB7U3RyaW5nfSB0aGUgY2FtZWxpemVkIHN0cmluZy5cbiAqL1xuZXhwb3J0IGZ1bmN0aW9uIGNhbWVsaXplKHN0cjogc3RyaW5nKTogc3RyaW5nIHtcbiAgcmV0dXJuIHN0clxuICAgIC5yZXBsYWNlKFNUUklOR19DQU1FTElaRV9SRUdFWFAsIChfbWF0Y2g6IHN0cmluZywgX3NlcGFyYXRvcjogc3RyaW5nLCBjaHI6IHN0cmluZykgPT4ge1xuICAgICAgcmV0dXJuIGNociA/IGNoci50b1VwcGVyQ2FzZSgpIDogJyc7XG4gICAgfSlcbiAgICAucmVwbGFjZSgvXihbQS1aXSkvLCAobWF0Y2g6IHN0cmluZykgPT4gbWF0Y2gudG9Mb3dlckNhc2UoKSk7XG59XG5cbi8qKlxuIFJldHVybnMgdGhlIFVwcGVyQ2FtZWxDYXNlIGZvcm0gb2YgYSBzdHJpbmcuXG5cbiBgYGBqYXZhc2NyaXB0XG4gJ2lubmVySFRNTCcuY2xhc3NpZnkoKTsgICAgICAgICAgLy8gJ0lubmVySFRNTCdcbiAnYWN0aW9uX25hbWUnLmNsYXNzaWZ5KCk7ICAgICAgICAvLyAnQWN0aW9uTmFtZSdcbiAnY3NzLWNsYXNzLW5hbWUnLmNsYXNzaWZ5KCk7ICAgICAvLyAnQ3NzQ2xhc3NOYW1lJ1xuICdteSBmYXZvcml0ZSBpdGVtcycuY2xhc3NpZnkoKTsgIC8vICdNeUZhdm9yaXRlSXRlbXMnXG4gYGBgXG5cbiBAbWV0aG9kIGNsYXNzaWZ5XG4gQHBhcmFtIHtTdHJpbmd9IHN0ciB0aGUgc3RyaW5nIHRvIGNsYXNzaWZ5XG4gQHJldHVybiB7U3RyaW5nfSB0aGUgY2xhc3NpZmllZCBzdHJpbmdcbiAqL1xuZXhwb3J0IGZ1bmN0aW9uIGNsYXNzaWZ5KHN0cjogc3RyaW5nKTogc3RyaW5nIHtcbiAgcmV0dXJuIHN0ci5zcGxpdCgnLicpLm1hcChwYXJ0ID0+IGNhcGl0YWxpemUoY2FtZWxpemUocGFydCkpKS5qb2luKCcuJyk7XG59XG5cbi8qKlxuIE1vcmUgZ2VuZXJhbCB0aGFuIGRlY2FtZWxpemUuIFJldHVybnMgdGhlIGxvd2VyXFxfY2FzZVxcX2FuZFxcX3VuZGVyc2NvcmVkXG4gZm9ybSBvZiBhIHN0cmluZy5cblxuIGBgYGphdmFzY3JpcHRcbiAnaW5uZXJIVE1MJy51bmRlcnNjb3JlKCk7ICAgICAgICAgIC8vICdpbm5lcl9odG1sJ1xuICdhY3Rpb25fbmFtZScudW5kZXJzY29yZSgpOyAgICAgICAgLy8gJ2FjdGlvbl9uYW1lJ1xuICdjc3MtY2xhc3MtbmFtZScudW5kZXJzY29yZSgpOyAgICAgLy8gJ2Nzc19jbGFzc19uYW1lJ1xuICdteSBmYXZvcml0ZSBpdGVtcycudW5kZXJzY29yZSgpOyAgLy8gJ215X2Zhdm9yaXRlX2l0ZW1zJ1xuIGBgYFxuXG4gQG1ldGhvZCB1bmRlcnNjb3JlXG4gQHBhcmFtIHtTdHJpbmd9IHN0ciBUaGUgc3RyaW5nIHRvIHVuZGVyc2NvcmUuXG4gQHJldHVybiB7U3RyaW5nfSB0aGUgdW5kZXJzY29yZWQgc3RyaW5nLlxuICovXG5leHBvcnQgZnVuY3Rpb24gdW5kZXJzY29yZShzdHI6IHN0cmluZyk6IHN0cmluZyB7XG4gIHJldHVybiBzdHJcbiAgICAucmVwbGFjZShTVFJJTkdfVU5ERVJTQ09SRV9SRUdFWFBfMSwgJyQxXyQyJylcbiAgICAucmVwbGFjZShTVFJJTkdfVU5ERVJTQ09SRV9SRUdFWFBfMiwgJ18nKVxuICAgIC50b0xvd2VyQ2FzZSgpO1xufVxuXG4vKipcbiBSZXR1cm5zIHRoZSBDYXBpdGFsaXplZCBmb3JtIG9mIGEgc3RyaW5nXG5cbiBgYGBqYXZhc2NyaXB0XG4gJ2lubmVySFRNTCcuY2FwaXRhbGl6ZSgpICAgICAgICAgLy8gJ0lubmVySFRNTCdcbiAnYWN0aW9uX25hbWUnLmNhcGl0YWxpemUoKSAgICAgICAvLyAnQWN0aW9uX25hbWUnXG4gJ2Nzcy1jbGFzcy1uYW1lJy5jYXBpdGFsaXplKCkgICAgLy8gJ0Nzcy1jbGFzcy1uYW1lJ1xuICdteSBmYXZvcml0ZSBpdGVtcycuY2FwaXRhbGl6ZSgpIC8vICdNeSBmYXZvcml0ZSBpdGVtcydcbiBgYGBcblxuIEBtZXRob2QgY2FwaXRhbGl6ZVxuIEBwYXJhbSB7U3RyaW5nfSBzdHIgVGhlIHN0cmluZyB0byBjYXBpdGFsaXplLlxuIEByZXR1cm4ge1N0cmluZ30gVGhlIGNhcGl0YWxpemVkIHN0cmluZy5cbiAqL1xuZXhwb3J0IGZ1bmN0aW9uIGNhcGl0YWxpemUoc3RyOiBzdHJpbmcpOiBzdHJpbmcge1xuICByZXR1cm4gc3RyLmNoYXJBdCgwKS50b1VwcGVyQ2FzZSgpICsgc3RyLnN1YnN0cigxKTtcbn1cbiJdfQ==