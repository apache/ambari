import * as ts from 'typescript';
import { RuleWalker, RuleFailure, IOptions } from 'tslint';
import { CodeWithSourceMap } from './metadata';
export declare class SourceMappingVisitor extends RuleWalker {
    protected codeWithMap: CodeWithSourceMap;
    protected basePosition: number;
    constructor(sourceFile: ts.SourceFile, options: IOptions, codeWithMap: CodeWithSourceMap, basePosition: number);
    createFailure(start: number, length: number, message: string): RuleFailure;
    private getMappedPosition(pos, consumer);
}
