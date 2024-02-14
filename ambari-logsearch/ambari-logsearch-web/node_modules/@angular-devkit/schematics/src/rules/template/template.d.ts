export interface TemplateOptions {
    sourceURL?: string;
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
export declare function template<T>(content: string, options: TemplateOptions): (input: T) => string;
