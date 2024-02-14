import * as Lint from 'tslint';
import * as ts from 'typescript';
export declare class Rule extends Lint.Rules.AbstractRule {
    static FAILURE_STRING: string;
    private static walkerBuilder;
    private static decoratorIsAttribute(dec);
    apply(sourceFile: ts.SourceFile): Lint.RuleFailure[];
}
