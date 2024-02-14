/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */

/**
 * TypeScript has an API for JSDoc already, but it's not exposed.
 * https://github.com/Microsoft/TypeScript/issues/7393
 * For now we create types that are similar to theirs so that migrating
 * to their API will be easier.  See e.g. ts.JSDocTag and ts.JSDocComment.
 */
export interface Tag {
  // tagName is e.g. "param" in an @param declaration.  It's absent
  // for the plain text documentation that occurs before any @foo lines.
  tagName?: string;
  // parameterName is the the name of the function parameter, e.g. "foo"
  // in
  //   @param foo The foo param.
  parameterName?: string;
  type?: string;
  // optional is true for optional function parameters.
  optional?: boolean;
  // restParam is true for "...x: foo[]" function parameters.
  restParam?: boolean;
  // destructuring is true for destructuring bind parameters, which require
  // non-null arguments on the Closure side.  Can likely remove this
  // once TypeScript nullable types are available.
  destructuring?: boolean;
  text?: string;
}

function arrayIncludes<T>(array: T[], key: T): boolean {
  for (const elem of array) {
    if (elem === key) return true;
  }
  return false;
}

/**
 * A list of all JSDoc tags allowed by the Closure compiler.
 * The public Closure docs don't list all the tags it allows; this list comes
 * from the compiler source itself.
 * https://github.com/google/closure-compiler/blob/master/src/com/google/javascript/jscomp/parsing/Annotation.java
 */
const JSDOC_TAGS_WHITELIST = [
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
const JSDOC_TAGS_BLACKLIST = [
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
const JSDOC_TAGS_WITH_TYPES = [
  'const',
  'export',
  'param',
  'return',
];

/**
 * parse parses JSDoc out of a comment string.
 * Returns null if comment is not JSDoc.
 */
export function parse(comment: string): {tags: Tag[], warnings?: string[]}|null {
  // TODO(evanm): this is a pile of hacky regexes for now, because we
  // would rather use the better TypeScript implementation of JSDoc
  // parsing.  https://github.com/Microsoft/TypeScript/issues/7393
  let match = comment.match(/^\/\*\*([\s\S]*?)\*\/$/);
  if (!match) return null;
  comment = match[1].trim();
  // Strip all the " * " bits from the front of each line.
  comment = comment.replace(/^\s*\*? ?/gm, '');
  let lines = comment.split('\n');
  let tags: Tag[] = [];
  let warnings: string[] = [];
  for (let line of lines) {
    match = line.match(/^@(\S+) *(.*)/);
    if (match) {
      let [_, tagName, text] = match;
      if (tagName === 'returns') {
        // A synonym for 'return'.
        tagName = 'return';
      }
      if (arrayIncludes(JSDOC_TAGS_BLACKLIST, tagName)) {
        warnings.push(`@${tagName} annotations are redundant with TypeScript equivalents`);
        continue;  // Drop the tag so Closure won't process it.
      } else if (arrayIncludes(JSDOC_TAGS_WITH_TYPES, tagName) && text[0] === '{') {
        warnings.push(
            `the type annotation on @${tagName} is redundant with its TypeScript type, ` +
            `remove the {...} part`);
        continue;
      } else if (tagName === 'dict') {
        warnings.push('use index signatures (`[k: string]: type`) instead of @dict');
        continue;
      }

      // Grab the parameter name from @param tags.
      let parameterName: string|undefined;
      if (tagName === 'param') {
        match = text.match(/^(\S+) ?(.*)/);
        if (match) [_, parameterName, text] = match;
      }

      let tag: Tag = {tagName};
      if (parameterName) tag.parameterName = parameterName;
      if (text) tag.text = text;
      tags.push(tag);
    } else {
      // Text without a preceding @tag on it is either the plain text
      // documentation or a continuation of a previous tag.
      if (tags.length === 0) {
        tags.push({text: line});
      } else {
        const lastTag = tags[tags.length - 1];
        lastTag.text = (lastTag.text || '') + '\n' + line;
      }
    }
  }
  if (warnings.length > 0) {
    return {tags, warnings};
  }
  return {tags};
}

/**
 * Serializes a Tag into a string usable in a comment.
 * Returns a string like " @foo {bar} baz" (note the whitespace).
 */
function tagToString(tag: Tag, escapeExtraTags: string[] = []): string {
  let out = '';
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
      out += ` \\@${tag.tagName}`;
    } else {
      out += ` @${tag.tagName}`;
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
export function toString(tags: Tag[], escapeExtraTags: string[] = []): string {
  if (tags.length === 0) return '';
  if (tags.length === 1) {
    let tag = tags[0];
    if (tag.tagName === 'type' && (!tag.text || !tag.text.match('\n'))) {
      // Special-case one-liner "type" tags to fit on one line, e.g.
      //   /** @type {foo} */
      return '/**' + tagToString(tag, escapeExtraTags) + ' */\n';
    }
    // Otherwise, fall through to the multi-line output.
  }

  let out = '';
  out += '/**\n';
  for (let tag of tags) {
    out += ' *';
    // If the tagToString is multi-line, insert " * " prefixes on subsequent lines.
    out += tagToString(tag, escapeExtraTags).split('\n').join('\n * ');
    out += '\n';
  }
  out += ' */\n';
  return out;
}

/** Merges multiple tags (of the same tagName type) into a single unified tag. */
export function merge(tags: Tag[]): Tag {
  let tagNames = new Set<string>();
  let parameterNames = new Set<string>();
  let types = new Set<string>();
  let texts = new Set<string>();
  // If any of the tags are optional/rest, then the merged output is optional/rest.
  let optional = false;
  let restParam = false;
  for (const tag of tags) {
    if (tag.tagName) tagNames.add(tag.tagName);
    if (tag.parameterName) parameterNames.add(tag.parameterName);
    if (tag.type) types.add(tag.type);
    if (tag.text) texts.add(tag.text);
    if (tag.optional) optional = true;
    if (tag.restParam) restParam = true;
  }

  if (tagNames.size !== 1) {
    throw new Error(`cannot merge differing tags: ${JSON.stringify(tags)}`);
  }
  const tagName = tagNames.values().next().value;
  const parameterName =
      parameterNames.size > 0 ? Array.from(parameterNames).join('_or_') : undefined;
  const type = types.size > 0 ? Array.from(types).join('|') : undefined;
  const text = texts.size > 0 ? Array.from(texts).join(' / ') : undefined;
  let tag: Tag = {tagName, parameterName, type, text};
  if (optional) tag.optional = true;
  if (restParam) tag.restParam = true;
  return tag;
}
