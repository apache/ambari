"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
// Matches <%= expr %>. This does not support structural JavaScript (for/if/...).
const kInterpolateRe = /<%=([\s\S]+?)%>/g;
// Used to match template delimiters.
// <%- expr %>: HTML escape the value.
// <% ... %>: Structural template code.
const kEscapeRe = /<%-([\s\S]+?)%>/g;
const kEvaluateRe = /<%([\s\S]+?)%>/g;
/** Used to map characters to HTML entities. */
const kHtmlEscapes = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
    '`': '&#96;',
};
// Used to match HTML entities and HTML characters.
const reUnescapedHtml = new RegExp(`[${Object.keys(kHtmlEscapes).join('')}]`, 'g');
// Used to match empty string literals in compiled template source.
const reEmptyStringLeading = /\b__p \+= '';/g;
const reEmptyStringMiddle = /\b(__p \+=) '' \+/g;
const reEmptyStringTrailing = /(__e\(.*?\)|\b__t\)) \+\n'';/g;
// Used to escape characters for inclusion in compiled string literals.
const stringEscapes = {
    '\\': '\\\\',
    "'": "\\'",
    '\n': '\\n',
    '\r': '\\r',
    '\u2028': '\\u2028',
    '\u2029': '\\u2029',
};
// Used to match unescaped characters in compiled string literals.
const reUnescapedString = /['\n\r\u2028\u2029\\]/g;
function _escape(s) {
    return s ? s.replace(reUnescapedHtml, key => kHtmlEscapes[key]) : '';
}
/**
 * An equivalent of lodash templates, which is based on John Resig's `tmpl` implementation
 * (http://ejohn.org/blog/javascript-micro-templating/) and Laura Doktorova's doT.js
 * (https://github.com/olado/doT).
 *
 * This version differs from lodash by removing support from ES6 quasi-literals, and making the
 * code slightly simpler to follow. It also does not depend on any third party, which is nice.
 *
 * @param content
 * @param options
 * @return {any}
 */
function template(content, options) {
    const interpolate = kInterpolateRe;
    let isEvaluating;
    let index = 0;
    let source = `__p += '`;
    // Compile the regexp to match each delimiter.
    const reDelimiters = RegExp(`${kEscapeRe.source}|${interpolate.source}|${kEvaluateRe.source}|$`, 'g');
    // Use a sourceURL for easier debugging.
    const sourceURL = options.sourceURL ? '//# sourceURL=' + options.sourceURL + '\n' : '';
    content.replace(reDelimiters, (match, escapeValue, interpolateValue, evaluateValue, offset) => {
        // Escape characters that can't be included in string literals.
        source += content.slice(index, offset).replace(reUnescapedString, chr => stringEscapes[chr]);
        // Replace delimiters with snippets.
        if (escapeValue) {
            source += `' +\n__e(${escapeValue}) +\n  '`;
        }
        if (evaluateValue) {
            isEvaluating = true;
            source += `';\n${evaluateValue};\n__p += '`;
        }
        if (interpolateValue) {
            source += `' +\n((__t = (${interpolateValue})) == null ? '' : __t) +\n  '`;
        }
        index = offset + match.length;
        return match;
    });
    source += "';\n";
    // Cleanup code by stripping empty strings.
    source = (isEvaluating ? source.replace(reEmptyStringLeading, '') : source)
        .replace(reEmptyStringMiddle, '$1')
        .replace(reEmptyStringTrailing, '$1;');
    // Frame code as the function body.
    source = `
  return function(obj) {
    obj || (obj = {});
    let __t;
    let __p = '';
    const __e = _.escape;
    with (obj) {
      ${source.replace(/\n/g, '\n      ')}
    }
    return __p;
  };
  `;
    const fn = Function('_', sourceURL + source);
    const result = fn({ escape: _escape });
    // Provide the compiled function's source by its `toString` method or
    // the `source` property as a convenience for inlining compiled templates.
    result.source = source;
    return result;
}
exports.template = template;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidGVtcGxhdGUuanMiLCJzb3VyY2VSb290IjoiL1VzZXJzL2hhbnNsL1NvdXJjZXMvZGV2a2l0LyIsInNvdXJjZXMiOlsicGFja2FnZXMvYW5ndWxhcl9kZXZraXQvc2NoZW1hdGljcy9zcmMvcnVsZXMvdGVtcGxhdGUvdGVtcGxhdGUudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IjtBQUFBOzs7Ozs7R0FNRzs7QUFFSCxpRkFBaUY7QUFDakYsTUFBTSxjQUFjLEdBQUcsa0JBQWtCLENBQUM7QUFFMUMscUNBQXFDO0FBQ3JDLHNDQUFzQztBQUN0Qyx1Q0FBdUM7QUFDdkMsTUFBTSxTQUFTLEdBQUcsa0JBQWtCLENBQUM7QUFDckMsTUFBTSxXQUFXLEdBQUcsaUJBQWlCLENBQUM7QUFFdEMsK0NBQStDO0FBQy9DLE1BQU0sWUFBWSxHQUE2QjtJQUM3QyxHQUFHLEVBQUUsT0FBTztJQUNaLEdBQUcsRUFBRSxNQUFNO0lBQ1gsR0FBRyxFQUFFLE1BQU07SUFDWCxHQUFHLEVBQUUsUUFBUTtJQUNiLEdBQUcsRUFBRSxPQUFPO0lBQ1osR0FBRyxFQUFFLE9BQU87Q0FDYixDQUFDO0FBRUYsbURBQW1EO0FBQ25ELE1BQU0sZUFBZSxHQUFHLElBQUksTUFBTSxDQUFDLElBQUksTUFBTSxDQUFDLElBQUksQ0FBQyxZQUFZLENBQUMsQ0FBQyxJQUFJLENBQUMsRUFBRSxDQUFDLEdBQUcsRUFBRSxHQUFHLENBQUMsQ0FBQztBQVFuRixtRUFBbUU7QUFDbkUsTUFBTSxvQkFBb0IsR0FBRyxnQkFBZ0IsQ0FBQztBQUM5QyxNQUFNLG1CQUFtQixHQUFHLG9CQUFvQixDQUFDO0FBQ2pELE1BQU0scUJBQXFCLEdBQUcsK0JBQStCLENBQUM7QUFHOUQsdUVBQXVFO0FBQ3ZFLE1BQU0sYUFBYSxHQUE2QjtJQUM5QyxJQUFJLEVBQUUsTUFBTTtJQUNaLEdBQUcsRUFBRSxLQUFLO0lBQ1YsSUFBSSxFQUFFLEtBQUs7SUFDWCxJQUFJLEVBQUUsS0FBSztJQUNYLFFBQVEsRUFBRSxTQUFT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LyoqXG4gKiBAbGljZW5zZVxuICogQ29weXJpZ2h0IEdvb2dsZSBJbmMuIEFsbCBSaWdodHMgUmVzZXJ2ZWQuXG4gKlxuICogVXNlIG9mIHRoaXMgc291cmNlIGNvZGUgaXMgZ292ZXJuZWQgYnkgYW4gTUlULXN0eWxlIGxpY2Vuc2UgdGhhdCBjYW4gYmVcbiAqIGZvdW5kIGluIHRoZSBMSUNFTlNFIGZpbGUgYXQgaHR0cHM6Ly9hbmd1bGFyLmlvL2xpY2Vuc2VcbiAqL1xuXG4vLyBNYXRjaGVzIDwlPSBleHByICU+LiBUaGlzIGRvZXMgbm90IHN1cHBvcnQgc3RydWN0dXJhbCBKYXZhU2NyaXB0IChmb3IvaWYvLi4uKS5cbmNvbnN0IGtJbnRlcnBvbGF0ZVJlID0gLzwlPShbXFxzXFxTXSs/KSU+L2c7XG5cbi8vIFVzZWQgdG8gbWF0Y2ggdGVtcGxhdGUgZGVsaW1pdGVycy5cbi8vIDwlLSBleHByICU+OiBIVE1MIGVzY2FwZSB0aGUgdmFsdWUuXG4vLyA8JSAuLi4gJT46IFN0cnVjdHVyYWwgdGVtcGxhdGUgY29kZS5cbmNvbnN0IGtFc2NhcGVSZSA9IC88JS0oW1xcc1xcU10rPyklPi9nO1xuY29uc3Qga0V2YWx1YXRlUmUgPSAvPCUoW1xcc1xcU10rPyklPi9nO1xuXG4vKiogVXNlZCB0byBtYXAgY2hhcmFjdGVycyB0byBIVE1MIGVudGl0aWVzLiAqL1xuY29uc3Qga0h0bWxFc2NhcGVzOiB7W2NoYXI6IHN0cmluZ106IHN0cmluZ30gPSB7XG4gICcmJzogJyZhbXA7JyxcbiAgJzwnOiAnJmx0OycsXG4gICc+JzogJyZndDsnLFxuICAnXCInOiAnJnF1b3Q7JyxcbiAgXCInXCI6ICcmIzM5OycsXG4gICdgJzogJyYjOTY7Jyxcbn07XG5cbi8vIFVzZWQgdG8gbWF0Y2ggSFRNTCBlbnRpdGllcyBhbmQgSFRNTCBjaGFyYWN0ZXJzLlxuY29uc3QgcmVVbmVzY2FwZWRIdG1sID0gbmV3IFJlZ0V4cChgWyR7T2JqZWN0LmtleXMoa0h0bWxFc2NhcGVzKS5qb2luKCcnKX1dYCwgJ2cnKTtcblxuLy8gT3B0aW9ucyB0byBwYXNzIHRvIHRlbXBsYXRlLlxuZXhwb3J0IGludGVyZmFjZSBUZW1wbGF0ZU9wdGlvbnMge1xuICBzb3VyY2VVUkw/OiBzdHJpbmc7XG59XG5cblxuLy8gVXNlZCB0byBtYXRjaCBlbXB0eSBzdHJpbmcgbGl0ZXJhbHMgaW4gY29tcGlsZWQgdGVtcGxhdGUgc291cmNlLlxuY29uc3QgcmVFbXB0eVN0cmluZ0xlYWRpbmcgPSAvXFxiX19wIFxcKz0gJyc7L2c7XG5jb25zdCByZUVtcHR5U3RyaW5nTWlkZGxlID0gL1xcYihfX3AgXFwrPSkgJycgXFwrL2c7XG5jb25zdCByZUVtcHR5U3RyaW5nVHJhaWxpbmcgPSAvKF9fZVxcKC4qP1xcKXxcXGJfX3RcXCkpIFxcK1xcbicnOy9nO1xuXG5cbi8vIFVzZWQgdG8gZXNjYXBlIGNoYXJhY3RlcnMgZm9yIGluY2x1c2lvbiBpbiBjb21waWxlZCBzdHJpbmcgbGl0ZXJhbHMuXG5jb25zdCBzdHJpbmdFc2NhcGVzOiB7W2NoYXI6IHN0cmluZ106IHN0cmluZ30gPSB7XG4gICdcXFxcJzogJ1xcXFxcXFxcJyxcbiAgXCInXCI6IFwiXFxcXCdcIixcbiAgJ1xcbic6ICdcXFxcbicsXG4gICdcXHInOiAnXFxcXHInLFxuICAnXFx1MjAyOCc6ICdcXFxcdTIwMjgnLFxuICAnXFx1MjAyOSc6ICdcXFxcdTIwMjknLFxufTtcblxuLy8gVXNlZCB0byBtYXRjaCB1bmVzY2FwZWQgY2hhcmFjdGVycyBpbiBjb21waWxlZCBzdHJpbmcgbGl0ZXJhbHMuXG5jb25zdCByZVVuZXNjYXBlZFN0cmluZyA9IC9bJ1xcblxcclxcdTIwMjhcXHUyMDI5XFxcXF0vZztcblxuXG5mdW5jdGlvbiBfZXNjYXBlKHM6IHN0cmluZykge1xuICByZXR1cm4gcyA/IHMucmVwbGFjZShyZVVuZXNjYXBlZEh0bWwsIGtleSA9PiBrSHRtbEVzY2FwZXNba2V5XSkgOiAnJztcbn1cblxuXG4vKipcbiAqIEFuIGVxdWl2YWxlbnQgb2YgbG9kYXNoIHRlbXBsYXRlcywgd2hpY2ggaXMgYmFzZWQgb24gSm9obiBSZXNpZydzIGB0bXBsYCBpbXBsZW1lbnRhdGlvblxuICogKGh0dHA6Ly9lam9obi5vcmcvYmxvZy9qYXZhc2NyaXB0LW1pY3JvLXRlbXBsYXRpbmcvKSBhbmQgTGF1cmEgRG9rdG9yb3ZhJ3MgZG9ULmpzXG4gKiAoaHR0cHM6Ly9naXRodWIuY29tL29sYWRvL2RvVCkuXG4gKlxuICogVGhpcyB2ZXJzaW9uIGRpZmZlcnMgZnJvbSBsb2Rhc2ggYnkgcmVtb3Zpbmcgc3VwcG9ydCBmcm9tIEVTNiBxdWFzaS1saXRlcmFscywgYW5kIG1ha2luZyB0aGVcbiAqIGNvZGUgc2xpZ2h0bHkgc2ltcGxlciB0byBmb2xsb3cuIEl0IGFsc28gZG9lcyBub3QgZGVwZW5kIG9uIGFueSB0aGlyZCBwYXJ0eSwgd2hpY2ggaXMgbmljZS5cbiAqXG4gKiBAcGFyYW0gY29udGVudFxuICogQHBhcmFtIG9wdGlvbnNcbiAqIEByZXR1cm4ge2FueX1cbiAqL1xuZXhwb3J0IGZ1bmN0aW9uIHRlbXBsYXRlPFQ+KGNvbnRlbnQ6IHN0cmluZywgb3B0aW9uczogVGVtcGxhdGVPcHRpb25zKTogKGlucHV0OiBUKSA9PiBzdHJpbmcge1xuICBjb25zdCBpbnRlcnBvbGF0ZSA9IGtJbnRlcnBvbGF0ZVJlO1xuICBsZXQgaXNFdmFsdWF0aW5nO1xuICBsZXQgaW5kZXggPSAwO1xuICBsZXQgc291cmNlID0gYF9fcCArPSAnYDtcblxuICAvLyBDb21waWxlIHRoZSByZWdleHAgdG8gbWF0Y2ggZWFjaCBkZWxpbWl0ZXIuXG4gIGNvbnN0IHJlRGVsaW1pdGVycyA9IFJlZ0V4cChcbiAgICBgJHtrRXNjYXBlUmUuc291cmNlfXwke2ludGVycG9sYXRlLnNvdXJjZX18JHtrRXZhbHVhdGVSZS5zb3VyY2V9fCRgLCAnZycpO1xuXG4gIC8vIFVzZSBhIHNvdXJjZVVSTCBmb3IgZWFzaWVyIGRlYnVnZ2luZy5cbiAgY29uc3Qgc291cmNlVVJMID0gb3B0aW9ucy5zb3VyY2VVUkwgPyAnLy8jIHNvdXJjZVVSTD0nICsgb3B0aW9ucy5zb3VyY2VVUkwgKyAnXFxuJyA6ICcnO1xuXG4gIGNvbnRlbnQucmVwbGFjZShyZURlbGltaXRlcnMsIChtYXRjaCwgZXNjYXBlVmFsdWUsIGludGVycG9sYXRlVmFsdWUsIGV2YWx1YXRlVmFsdWUsIG9mZnNldCkgPT4ge1xuICAgIC8vIEVzY2FwZSBjaGFyYWN0ZXJzIHRoYXQgY2FuJ3QgYmUgaW5jbHVkZWQgaW4gc3RyaW5nIGxpdGVyYWxzLlxuICAgIHNvdXJjZSArPSBjb250ZW50LnNsaWNlKGluZGV4LCBvZmZzZXQpLnJlcGxhY2UocmVVbmVzY2FwZWRTdHJpbmcsIGNociA9PiBzdHJpbmdFc2NhcGVzW2Nocl0pO1xuXG4gICAgLy8gUmVwbGFjZSBkZWxpbWl0ZXJzIHdpdGggc25pcHBldHMuXG4gICAgaWYgKGVzY2FwZVZhbHVlKSB7XG4gICAgICBzb3VyY2UgKz0gYCcgK1xcbl9fZSgke2VzY2FwZVZhbHVlfSkgK1xcbiAgJ2A7XG4gICAgfVxuICAgIGlmIChldmFsdWF0ZVZhbHVlKSB7XG4gICAgICBpc0V2YWx1YXRpbmcgPSB0cnVlO1xuICAgICAgc291cmNlICs9IGAnO1xcbiR7ZXZhbHVhdGVWYWx1ZX07XFxuX19wICs9ICdgO1xuICAgIH1cbiAgICBpZiAoaW50ZXJwb2xhdGVWYWx1ZSkge1xuICAgICAgc291cmNlICs9IGAnICtcXG4oKF9fdCA9ICgke2ludGVycG9sYXRlVmFsdWV9KSkgPT0gbnVsbCA/ICcnIDogX190KSArXFxuICAnYDtcbiAgICB9XG4gICAgaW5kZXggPSBvZmZzZXQgKyBtYXRjaC5sZW5ndGg7XG5cbiAgICByZXR1cm4gbWF0Y2g7XG4gIH0pO1xuXG4gIHNvdXJjZSArPSBcIic7XFxuXCI7XG5cbiAgLy8gQ2xlYW51cCBjb2RlIGJ5IHN0cmlwcGluZyBlbXB0eSBzdHJpbmdzLlxuICBzb3VyY2UgPSAoaXNFdmFsdWF0aW5nID8gc291cmNlLnJlcGxhY2UocmVFbXB0eVN0cmluZ0xlYWRpbmcsICcnKSA6IHNvdXJjZSlcbiAgICAucmVwbGFjZShyZUVtcHR5U3RyaW5nTWlkZGxlLCAnJDEnKVxuICAgIC5yZXBsYWNlKHJlRW1wdHlTdHJpbmdUcmFpbGluZywgJyQxOycpO1xuXG4gIC8vIEZyYW1lIGNvZGUgYXMgdGhlIGZ1bmN0aW9uIGJvZHkuXG4gIHNvdXJjZSA9IGBcbiAgcmV0dXJuIGZ1bmN0aW9uKG9iaikge1xuICAgIG9iaiB8fCAob2JqID0ge30pO1xuICAgIGxldCBfX3Q7XG4gICAgbGV0IF9fcCA9ICcnO1xuICAgIGNvbnN0IF9fZSA9IF8uZXNjYXBlO1xuICAgIHdpdGggKG9iaikge1xuICAgICAgJHtzb3VyY2UucmVwbGFjZSgvXFxuL2csICdcXG4gICAgICAnKX1cbiAgICB9XG4gICAgcmV0dXJuIF9fcDtcbiAgfTtcbiAgYDtcblxuICBjb25zdCBmbiA9IEZ1bmN0aW9uKCdfJywgc291cmNlVVJMICsgc291cmNlKTtcbiAgY29uc3QgcmVzdWx0ID0gZm4oeyBlc2NhcGU6IF9lc2NhcGUgfSk7XG5cbiAgLy8gUHJvdmlkZSB0aGUgY29tcGlsZWQgZnVuY3Rpb24ncyBzb3VyY2UgYnkgaXRzIGB0b1N0cmluZ2AgbWV0aG9kIG9yXG4gIC8vIHRoZSBgc291cmNlYCBwcm9wZXJ0eSBhcyBhIGNvbnZlbmllbmNlIGZvciBpbmxpbmluZyBjb21waWxlZCB0ZW1wbGF0ZXMuXG4gIHJlc3VsdC5zb3VyY2UgPSBzb3VyY2U7XG5cbiAgcmV0dXJuIHJlc3VsdDtcbn1cbiJdfQ==