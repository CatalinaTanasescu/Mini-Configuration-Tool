# NXP Mini Configuration Tool Design

## Purpose and architecture

The application configures fictive processor definitions, not physical registers or real NXP devices. Its central design goal is to support a growing set of processors and interfaces without duplicating configuration logic. The implementation is a modular monolith with ports and adapters: it keeps deployment simple while enforcing dependency direction toward domain and application abstractions.

```text
Swing GUI ─┐
           ├── ConfigurationService ── ValidationEngine
Picocli ───┘          │                      │
                      ├── ProcessorRepository ── JSON definitions
                      └── ConfigurationExporter ─ JSON output
```

The `domain` package contains immutable processor capabilities, immutable configuration snapshots, signal classification, and validation contracts. It depends only on the JDK. The `application` package coordinates use cases through `ConfigurationService` and defines ports for processor discovery and configuration export. `infrastructure` implements those ports with Jackson and filesystem operations. `presentation` contains Swing and Picocli adapters; both receive the same service through constructor injection. `bootstrap` is the explicit composition root.

This shape prevents Swing widgets, Picocli commands, Jackson annotations, and filesystem concerns from entering the domain. It also avoids a framework or multi-module build that would add ceremony without improving this desktop tool.

## Static capabilities and selected state

`ProcessorDefinition`, `PinDefinition`, and `PeripheralDefinition` describe hardware capabilities loaded from JSON. They are immutable and reject duplicate pin names, physical numbers, peripheral IDs, blank functions, and other malformed structural data. Processor-specific Java subclasses are intentionally absent.

`Configuration` represents user choices separately: processor ID, power mode, peripheral and clock states, and pin assignments. Configuration operations return a new snapshot. `ConfigurationService` owns the current snapshot, tracks unsaved changes, prevents unsupported assignment operations, and requires explicit consent before a dirty configuration is reset by processor selection.

Peripheral instances and signal policies can be declared explicitly in each processor JSON through optional `peripherals` metadata (`id`, `category`, `signals`, and `exclusiveSignals`). `ProcessorDefinition` is the context-aware resolver used by validation, GUI, summary, and export, so an identifier such as `WAKE_REQUEST` can belong to `SAFETY0` without a Java regex or code change. Metadata is validated against the pin functions, duplicate signal ownership is rejected, and exclusivity must be a subset of the peripheral's signals.

`SignalResolver` remains an isolated compatibility adapter for definitions that do not yet contain metadata. The raw identifier `I2C_SCL` remains unchanged but maps to `I2C0`; `LPUART` remains the signal prefix while its power category is `UART`; `ETH_*` maps to the Ethernet category. Convention-derived legacy signals retain the previous exclusive behavior. Explicit definitions choose exclusivity per signal and can therefore model repeatable non-GPIO signals.

Bundled definitions are listed by `processors/index.json`, which works identically from the classpath and the shaded JAR. `JsonProcessorRepository` also scans an optional external directory supplied through `configtool.processorDir` or `CONFIGTOOL_PROCESSOR_DIR`. Therefore, a schema-compatible `ProcessorD` can appear in GUI and CLI without recompilation. Duplicate IDs across bundled and external sources are rejected rather than silently overridden.

## Validation framework

`ValidationRule` is a small functional interface. `ValidationEngine` receives a list of rules, executes each rule, and returns a deterministic `ValidationResult` containing structured issues. Every issue carries a diagnostic code, severity, location, explanation, and corrective action.

The mandatory rules cover pin multiplexing, peripheral clock dependency, and documented power-mode compatibility. Independent additional rules cover unsupported pin functions, duplicate signals marked exclusive by processor metadata, and assignments to disabled peripherals. A separate advisory rule warns when an enabled peripheral has no resources. Errors block export; warnings remain visible but allow export. Interactive assignment replaces the previous function on the same pin; `PinMultiplexingRule` remains a defensive boundary for imported or independently constructed snapshots, with tests for both paths.

No monolithic validator or interface-specific rule set exists. A new rule is implemented and registered without rewriting `ValidationEngine`. Both adapters call `ConfigurationService.validate()`, and an integration test builds the same configuration through GUI table models and CLI commands, then compares state and validation results.

## Interfaces and export

The Swing UI is a multi-pane engineering workspace with a restrained theme, processor and power controls, searchable pin table, peripheral and clock table, navigable validation issues, summary, keyboard shortcuts, and guarded destructive transitions. Function editors expose only functions supported by the current pin. Filesystem export is initiated through the application service, and user-facing errors do not expose stack traces.

The Picocli adapter maintains one in-memory configuration across an interactive session. It covers processor listing and selection, pins, peripherals, clocks, assignments, power mode, validation, summary, export, and help. A semicolon-delimited scripted mode supports deterministic smoke tests without creating a second state model.

`ConfigurationExporter` decouples output format from configuration behavior. The JSON adapter emits schema version `1.0`, processor ID, power mode, peripheral/clock states, and ordered pin assignments. It writes to a temporary sibling and attempts an atomic replacement. Filesystems that reject atomic moves use an ordinary replacement fallback; therefore atomicity is not guaranteed in every environment, although configuration generation never writes directly into the target file. Another exporter can be injected without changing the service, domain, GUI, or CLI.

## Decisions and trade-offs

- Java 21 records and defensive copies express immutable capabilities without inheritance hierarchies.
- The application uses one stateful service per UI session. It is synchronized for predictable adapter access and avoids global mutable state.
- Intermediate invalid configurations are allowed where useful, so users receive complete actionable diagnostics. Unsupported functions are rejected immediately by the service and still have a defensive validation rule for imported or malformed snapshots.
- Power restrictions are implemented only for CAN, UART, SPI, ADC, and Ethernet as specified. No undocumented restrictions are applied to I2C, PWM, TIMER, FLEXIO, external interrupts, or trace.
- The initial delivery omits import, XML export, undo/redo, and a graphical package pinout. The table-first workflow is complete and accurate, while these optional features remain compatible with existing ports and models.
