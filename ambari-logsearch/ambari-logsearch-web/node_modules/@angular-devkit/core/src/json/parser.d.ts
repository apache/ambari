/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { BaseException } from '..';
import { JsonAstNode, JsonValue, Position } from './interface';
/**
 * A character was invalid in this context.
 */
export declare class InvalidJsonCharacterException extends BaseException {
    constructor(context: JsonParserContext);
}
/**
 * More input was expected, but we reached the end of the stream.
 */
export declare class UnexpectedEndOfInputException extends BaseException {
    constructor(_context: JsonParserContext);
}
/**
 * Context passed around the parser with information about where we currently are in the parse.
 */
export interface JsonParserContext {
    position: Position;
    previous: Position;
    readonly original: string;
    readonly mode: JsonParseMode;
}
/**
 * The Parse mode used for parsing the JSON string.
 */
export declare enum JsonParseMode {
    Strict = 0,
    CommentsAllowed = 1,
    SingleQuotesAllowed = 2,
    IdentifierKeyNamesAllowed = 4,
    Default = 0,
    Loose = 7,
}
/**
 * Parse the JSON string and return its AST. The AST may be losing data (end comments are
 * discarded for example, and space characters are not represented in the AST), but all values
 * will have a single node in the AST (a 1-to-1 mapping).
 * @param input The string to use.
 * @param mode The mode to parse the input with. {@see JsonParseMode}.
 * @returns {JsonAstNode} The root node of the value of the AST.
 */
export declare function parseJsonAst(input: string, mode?: JsonParseMode): JsonAstNode;
/**
 * Parse a JSON string into its value.  This discards the AST and only returns the value itself.
 * @param input The string to parse.
 * @param mode The mode to parse the input with. {@see JsonParseMode}.
 * @returns {JsonValue} The value represented by the JSON string.
 */
export declare function parseJson(input: string, mode?: JsonParseMode): JsonValue;
