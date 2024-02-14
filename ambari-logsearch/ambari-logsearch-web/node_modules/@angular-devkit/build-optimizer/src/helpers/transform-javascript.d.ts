import { RawSourceMap } from 'source-map';
import * as ts from 'typescript';
export interface TransformJavascriptOptions {
    content: string;
    inputFilePath?: string;
    outputFilePath?: string;
    emitSourceMap?: boolean;
    strict?: boolean;
    getTransforms: Array<(program: ts.Program) => ts.TransformerFactory<ts.SourceFile>>;
}
export interface TransformJavascriptOutput {
    content: string | null;
    sourceMap: RawSourceMap | null;
    emitSkipped: boolean;
}
export declare function transformJavascript(options: TransformJavascriptOptions): TransformJavascriptOutput;
