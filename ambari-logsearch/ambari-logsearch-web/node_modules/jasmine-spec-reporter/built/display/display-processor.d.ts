import { Configuration } from "../configuration";
export declare class DisplayProcessor {
    protected configuration: Configuration;
    constructor(configuration: Configuration);
    displayJasmineStarted(info: any, log: String): String;
    displaySuite(suite: any, log: String): String;
    displaySpecStarted(spec: any, log: String): String;
    displaySuccessfulSpec(spec: any, log: String): String;
    displayFailedSpec(spec: any, log: String): String;
    displaySpecErrorMessages(spec: any, log: String): String;
    displaySummaryErrorMessages(spec: any, log: String): String;
    displayPendingSpec(spec: any, log: String): String;
}
