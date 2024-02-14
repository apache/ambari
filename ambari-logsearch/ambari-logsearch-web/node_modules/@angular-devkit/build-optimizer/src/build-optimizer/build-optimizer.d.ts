import { TransformJavascriptOutput } from '../helpers/transform-javascript';
export interface BuildOptimizerOptions {
    content?: string;
    inputFilePath?: string;
    outputFilePath?: string;
    emitSourceMap?: boolean;
    strict?: boolean;
}
export declare function buildOptimizer(options: BuildOptimizerOptions): TransformJavascriptOutput;
