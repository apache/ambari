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
    tagName?: string;
    parameterName?: string;
    type?: string;
    optional?: boolean;
    restParam?: boolean;
    destructuring?: boolean;
    text?: string;
}
/**
 * parse parses JSDoc out of a comment string.
 * Returns null if comment is not JSDoc.
 */
export declare function parse(comment: string): {
    tags: Tag[];
    warnings?: string[];
} | null;
/** Serializes a Comment out to a string usable in source code. */
export declare function toString(tags: Tag[], escapeExtraTags?: string[]): string;
/** Merges multiple tags (of the same tagName type) into a single unified tag. */
export declare function merge(tags: Tag[]): Tag;
