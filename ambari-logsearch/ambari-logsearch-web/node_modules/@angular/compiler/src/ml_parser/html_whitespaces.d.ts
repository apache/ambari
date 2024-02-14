import { ParseTreeResult } from './parser';
export declare const PRESERVE_WS_ATTR_NAME = "ngPreserveWhitespaces";
/**
 * Angular Dart introduced &ngsp; as a placeholder for non-removable space, see:
 * https://github.com/dart-lang/angular/blob/0bb611387d29d65b5af7f9d2515ab571fd3fbee4/_tests/test/compiler/preserve_whitespace_test.dart#L25-L32
 * In Angular Dart &ngsp; is converted to the 0xE500 PUA (Private Use Areas) unicode character
 * and later on replaced by a space. We are re-implementing the same idea here.
 */
export declare function replaceNgsp(value: string): string;
export declare function removeWhitespaces(htmlAstWithErrors: ParseTreeResult): ParseTreeResult;
