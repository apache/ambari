import { InterpolationConfig } from '../ml_parser/interpolation_config';
import { AST, ASTWithSource, BindingPipe, LiteralMap, ParseSpan, ParserError, TemplateBinding } from './ast';
import { Lexer, Token } from './lexer';
export declare class SplitInterpolation {
    strings: string[];
    expressions: string[];
    offsets: number[];
    constructor(strings: string[], expressions: string[], offsets: number[]);
}
export declare class TemplateBindingParseResult {
    templateBindings: TemplateBinding[];
    warnings: string[];
    errors: ParserError[];
    constructor(templateBindings: TemplateBinding[], warnings: string[], errors: ParserError[]);
}
export declare class Parser {
    private _lexer;
    private errors;
    constructor(_lexer: Lexer);
    parseAction(input: string, location: any, interpolationConfig?: InterpolationConfig): ASTWithSource;
    parseBinding(input: string, location: any, interpolationConfig?: InterpolationConfig): ASTWithSource;
    parseSimpleBinding(input: string, location: string, interpolationConfig?: InterpolationConfig): ASTWithSource;
    private _reportError(message, input, errLocation, ctxLocation?);
    private _parseBindingAst(input, location, interpolationConfig);
    private _parseQuote(input, location);
    parseTemplateBindings(prefixToken: string | null, input: string, location: any): TemplateBindingParseResult;
    parseInterpolation(input: string, location: any, interpolationConfig?: InterpolationConfig): ASTWithSource | null;
    splitInterpolation(input: string, location: string, interpolationConfig?: InterpolationConfig): SplitInterpolation | null;
    wrapLiteralPrimitive(input: string | null, location: any): ASTWithSource;
    private _stripComments(input);
    private _commentStart(input);
    private _checkNoInterpolation(input, location, interpolationConfig);
    private _findInterpolationErrorColumn(parts, partInErrIdx, interpolationConfig);
}
export declare class _ParseAST {
    input: string;
    location: any;
    tokens: Token[];
    inputLength: number;
    parseAction: boolean;
    private errors;
    private offset;
    private rparensExpected;
    private rbracketsExpected;
    private rbracesExpected;
    index: number;
    constructor(input: string, location: any, tokens: Token[], inputLength: number, parseAction: boolean, errors: ParserError[], offset: number);
    peek(offset: number): Token;
    readonly next: Token;
    readonly inputIndex: number;
    span(start: number): ParseSpan;
    advance(): void;
    optionalCharacter(code: number): boolean;
    peekKeywordLet(): boolean;
    peekKeywordAs(): boolean;
    expectCharacter(code: number): void;
    optionalOperator(op: string): boolean;
    expectOperator(operator: string): void;
    expectIdentifierOrKeyword(): string;
    expectIdentifierOrKeywordOrString(): string;
    parseChain(): AST;
    parsePipe(): AST;
    parseExpression(): AST;
    parseConditional(): AST;
    parseLogicalOr(): AST;
    parseLogicalAnd(): AST;
    parseEquality(): AST;
    parseRelational(): AST;
    parseAdditive(): AST;
    parseMultiplicative(): AST;
    parsePrefix(): AST;
    parseCallChain(): AST;
    parsePrimary(): AST;
    parseExpressionList(terminator: number): AST[];
    parseLiteralMap(): LiteralMap;
    parseAccessMemberOrMethodCall(receiver: AST, isSafe?: boolean): AST;
    parseCallArguments(): BindingPipe[];
    /**
     * An identifier, a keyword, a string with an optional `-` inbetween.
     */
    expectTemplateBindingKey(): string;
    parseTemplateBindings(): TemplateBindingParseResult;
    error(message: string, index?: number | null): void;
    private locationText(index?);
    private skip();
}
