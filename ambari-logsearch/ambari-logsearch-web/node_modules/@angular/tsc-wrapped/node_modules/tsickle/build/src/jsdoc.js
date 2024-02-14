/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
"use strict";
function arrayIncludes(array, key) {
    for (var _i = 0, array_1 = array; _i < array_1.length; _i++) {
        var elem = array_1[_i];
        if (elem === key)
            return true;
    }
    return false;
}
/**
 * A list of all JSDoc tags allowed by the Closure compiler.
 * The public Closure docs don't list all the tags it allows; this list comes
 * from the compiler source itself.
 * https://github.com/google/closure-compiler/blob/master/src/com/google/javascript/jscomp/parsing/Annotation.java
 */
var JSDOC_TAGS_WHITELIST = [
    'ngInject',
    'abstract',
    'argument',
    'author',
    'consistentIdGenerator',
    'const',
    'constant',
    'constructor',
    'copyright',
    'define',
    'deprecated',
    'desc',
    'dict',
    'disposes',
    'enum',
    'export',
    'expose',
    'extends',
    'externs',
    'fileoverview',
    'final',
    'hidden',
    'idGenerator',
    'implements',
    'implicitCast',
    'inheritDoc',
    'interface',
    'record',
    'jaggerInject',
    'jaggerModule',
    'jaggerProvidePromise',
    'jaggerProvide',
    'lends',
    'license',
    'meaning',
    'modifies',
    'noalias',
    'nocollapse',
    'nocompile',
    'nosideeffects',
    'override',
    'owner',
    'package',
    'param',
    'polymerBehavior',
    'preserve',
    'preserveTry',
    'private',
    'protected',
    'public',
    'return',
    'returns',
    'see',
    'stableIdGenerator',
    'struct',
    'suppress',
    'template',
    'this',
    'throws',
    'type',
    'typedef',
    'unrestricted',
    'version',
    'wizaction',
];
/**
 * A list of JSDoc @tags that are never allowed in TypeScript source. These are Closure tags that
 * can be expressed in the TypeScript surface syntax. As tsickle's emit will mangle type names,
 * these will cause Closure Compiler issues and should not be used.
 */
var JSDOC_TAGS_BLACKLIST = [
    'constructor',
    'enum',
    'extends',
    'implements',
    'interface',
    'lends',
    'private',
    'public',
    'record',
    'template',
    'this',
    'type',
    'typedef',
];
/**
 * A list of JSDoc @tags that might include a {type} after them. Only banned when a type is passed.
 */
var JSDOC_TAGS_WITH_TYPES = [
    'const',
    'export',
    'param',
    'return',
];
/**
 * parse parses JSDoc out of a comment string.
 * Returns null if comment is not JSDoc.
 */
function parse(comment) {
    // TODO(evanm): this is a pile of hacky regexes for now, because we
    // would rather use the better TypeScript implementation of JSDoc
    // parsing.  https://github.com/Microsoft/TypeScript/issues/7393
    var match = comment.match(/^\/\*\*([\s\S]*?)\*\/$/);
    if (!match)
        return null;
    comment = match[1].trim();
    // Strip all the " * " bits from the front of each line.
    comment = comment.replace(/^\s*\*? ?/gm, '');
    var lines = comment.split('\n');
    var tags = [];
    var warnings = [];
    for (var _i = 0, lines_1 = lines; _i < lines_1.length; _i++) {
        var line = lines_1[_i];
        match = line.match(/^@(\S+) *(.*)/);
        if (match) {
            var _ = match[0], tagName = match[1], text = match[2];
            if (tagName === 'returns') {
                // A synonym for 'return'.
                tagName = 'return';
            }
            if (arrayIncludes(JSDOC_TAGS_BLACKLIST, tagName)) {
                warnings.push("@" + tagName + " annotations are redundant with TypeScript equivalents");
                continue; // Drop the tag so Closure won't process it.
            }
            else if (arrayIncludes(JSDOC_TAGS_WITH_TYPES, tagName) && text[0] === '{') {
                warnings.push("the type annotation on @" + tagName + " is redundant with its TypeScript type, " +
                    "remove the {...} part");
                continue;
            }
            else if (tagName === 'dict') {
                warnings.push('use index signatures (`[k: string]: type`) instead of @dict');
                continue;
            }
            // Grab the parameter name from @param tags.
            var parameterName = void 0;
            if (tagName === 'param') {
                match = text.match(/^(\S+) ?(.*)/);
                if (match)
                    _ = match[0], parameterName = match[1], text = match[2];
            }
            var tag = { tagName: tagName };
            if (parameterName)
                tag.parameterName = parameterName;
            if (text)
                tag.text = text;
            tags.push(tag);
        }
        else {
            // Text without a preceding @tag on it is either the plain text
            // documentation or a continuation of a previous tag.
            if (tags.length === 0) {
                tags.push({ text: line });
            }
            else {
                var lastTag = tags[tags.length - 1];
                lastTag.text = (lastTag.text || '') + '\n' + line;
            }
        }
    }
    if (warnings.length > 0) {
        return { tags: tags, warnings: warnings };
    }
    return { tags: tags };
}
exports.parse = parse;
/**
 * Serializes a Tag into a string usable in a comment.
 * Returns a string like " @foo {bar} baz" (note the whitespace).
 */
function tagToString(tag, escapeExtraTags) {
    if (escapeExtraTags === void 0) { escapeExtraTags = []; }
    var out = '';
    if (tag.tagName) {
        if (!arrayIncludes(JSDOC_TAGS_WHITELIST, tag.tagName) ||
            arrayIncludes(escapeExtraTags, tag.tagName)) {
            // Escape tags we don't understand.  This is a subtle
            // compromise between multiple issues.
            // 1) If we pass through these non-Closure tags, the user will
            //    get a warning from Closure, and the point of tsickle is
            //    to insulate the user from Closure.
            // 2) The output of tsickle is for Closure but also may be read
            //    by humans, for example non-TypeScript users of Angular.
            // 3) Finally, we don't want to warn because users should be
            //    free to add whichever JSDoc they feel like.  If the user
            //    wants help ensuring they didn't typo a tag, that is the
            //    responsibility of a linter.
            out += " \\@" + tag.tagName;
        }
        else {
            out += " @" + tag.tagName;
        }
    }
    if (tag.type) {
        out += ' {';
        if (tag.restParam) {
            out += '...';
        }
        out += tag.type;
        if (tag.optional) {
            out += '=';
        }
        out += '}';
    }
    if (tag.parameterName) {
        out += ' ' + tag.parameterName;
    }
    if (tag.text) {
        out += ' ' + tag.text.replace(/@/g, '\\@');
    }
    return out;
}
/** Serializes a Comment out to a string usable in source code. */
function toString(tags, escapeExtraTags) {
    if (escapeExtraTags === void 0) { escapeExtraTags = []; }
    if (tags.length === 0)
        return '';
    if (tags.length === 1) {
        var tag = tags[0];
        if (tag.tagName === 'type' && (!tag.text || !tag.text.match('\n'))) {
            // Special-case one-liner "type" tags to fit on one line, e.g.
            //   /** @type {foo} */
            return '/**' + tagToString(tag, escapeExtraTags) + ' */\n';
        }
    }
    var out = '';
    out += '/**\n';
    for (var _i = 0, tags_1 = tags; _i < tags_1.length; _i++) {
        var tag = tags_1[_i];
        out += ' *';
        // If the tagToString is multi-line, insert " * " prefixes on subsequent lines.
        out += tagToString(tag, escapeExtraTags).split('\n').join('\n * ');
        out += '\n';
    }
    out += ' */\n';
    return out;
}
exports.toString = toString;
/** Merges multiple tags (of the same tagName type) into a single unified tag. */
function merge(tags) {
    var tagNames = new Set();
    var parameterNames = new Set();
    var types = new Set();
    var texts = new Set();
    // If any of the tags are optional/rest, then the merged output is optional/rest.
    var optional = false;
    var restParam = false;
    for (var _i = 0, tags_2 = tags; _i < tags_2.length; _i++) {
        var tag_1 = tags_2[_i];
        if (tag_1.tagName)
            tagNames.add(tag_1.tagName);
        if (tag_1.parameterName)
            parameterNames.add(tag_1.parameterName);
        if (tag_1.type)
            types.add(tag_1.type);
        if (tag_1.text)
            texts.add(tag_1.text);
        if (tag_1.optional)
            optional = true;
        if (tag_1.restParam)
            restParam = true;
    }
    if (tagNames.size !== 1) {
        throw new Error("cannot merge differing tags: " + JSON.stringify(tags));
    }
    var tagName = tagNames.values().next().value;
    var parameterName = parameterNames.size > 0 ? Array.from(parameterNames).join('_or_') : undefined;
    var type = types.size > 0 ? Array.from(types).join('|') : undefined;
    var text = texts.size > 0 ? Array.from(texts).join(' / ') : undefined;
    var tag = { tagName: tagName, parameterName: parameterName, type: type, text: text };
    if (optional)
        tag.optional = true;
    if (restParam)
        tag.restParam = true;
    return tag;
}
exports.merge = merge;

//# sourceMappingURL=jsdoc.js.map
