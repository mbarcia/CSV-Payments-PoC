# Pipeline Step Input/Output Types

## Summary of Changes

We've updated the `@PipelineStep` annotation to include input and output type information, which will enable better type checking and documentation for pipeline steps.

## Changes Made

### 1. Updated `@PipelineStep` Annotation

Modified `PipelineStep.java` to include two new parameters:
- `inputType`: Specifies the input type for the pipeline step (default: Object.class)
- `outputType`: Specifies the output type for the pipeline step (default: Object.class)

### 2. Updated Pipeline Steps

Updated all pipeline steps to specify their input and output types:

| Step | Input Type | Output Type |
|------|------------|-------------|
| ProcessFolderStep | String | CsvPaymentsInputFile |
| ProcessInputFileStep | CsvPaymentsInputFile | InputCsvFileProcessingSvc.PaymentRecord |
| SendPaymentStep | InputCsvFileProcessingSvc.PaymentRecord | PaymentsProcessingSvc.AckPaymentSent |
| ProcessAckPaymentStep | PaymentsProcessingSvc.AckPaymentSent | PaymentsProcessingSvc.PaymentStatus |
| ProcessPaymentStatusStep | PaymentsProcessingSvc.PaymentStatus | PaymentStatusSvc.PaymentOutput |
| ProcessOutputFileStep | PaymentStatusSvc.PaymentOutput | OutputCsvFileProcessingSvc.CsvPaymentsOutputFile |

### 3. Updated Documentation

Updated `ANNOTATION_BASED_ADAPTERS.md` to document the new parameters in the `@PipelineStep` annotation.

## Benefits

1. **Better Type Safety**: The pipeline framework can now validate that steps are connected correctly based on their input/output types.
2. **Improved Documentation**: Developers can easily see what types each step expects and produces.
3. **Enhanced Tooling**: Future tooling can use this information to provide better IDE support and visualization.

## Test Results

The following modules have all tests passing:
- pipeline-framework
- common
- input-csv-file-processing-svc
- payment-status-svc

Some modules have pre-existing test issues unrelated to our changes:
- payments-processing-svc
- output-csv-file-processing-svc
- orchestrator-svc

These issues are related to Vert.x context management in tests and missing dependencies, not our annotation changes.