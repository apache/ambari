import { DisplayProcessor } from "../display-processor";
export declare class DefaultProcessor extends DisplayProcessor {
    private static displaySpecDescription(spec);
    displayJasmineStarted(): String;
    displaySuite(suite: any): String;
    displaySuccessfulSpec(spec: any): String;
    displayFailedSpec(spec: any): String;
    displaySpecErrorMessages(spec: any): String;
    displaySummaryErrorMessages(spec: any): String;
    displayPendingSpec(spec: any): String;
    private displayErrorMessages(spec, withStacktrace);
}
