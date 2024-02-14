import * as ts from "typescript";
import * as Lint from "../index";
export declare class Rule extends Lint.Rules.AbstractRule {
    static metadata: Lint.IRuleMetadata;
    static FAILURE_STRING_OMITTING_SINGLE_PARAMETER: string;
    static FAILURE_STRING_OMITTING_REST_PARAMETER: string;
    static FAILURE_STRING_SINGLE_PARAMETER_DIFFERENCE(type1: string, type2: string): string;
    apply(sourceFile: ts.SourceFile): Lint.RuleFailure[];
}
