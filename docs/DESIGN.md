# NXP Mini Configuration Tool Design

## Purpose and architecture

The application configures fictive processor definitions, not physical registers or real NXP devices. Its central design goal is to support a growing set of processors and interfaces without duplicating configuration logic. The implementation is a modular monolith with ports and adapters: it keeps deployment simple while enforcing dependency direction toward domain and application abstractions.

```text
Swing GUI ─┐
           ├── ConfigurationService ── ValidationEngine
Picocli ───┘          │                      │
                      ├── ProcessorRepository ── JSON definitions
                      ├── Importer / Exporter ── JSON and XML
                      └── TemplateRepository ── reusable snapshots
```

The `domain` package contains immutable processor capabilities, immutable configuration snapshots, signal classification, and validation contracts. It depends only on the JDK. The `application` package coordinates use cases through `ConfigurationService` and defines ports for processor discovery and configuration export. `infrastructure` implements those ports with Jackson and filesystem operations. `presentation` contains Swing and Picocli adapters; both receive the same service through constructor injection. `bootstrap` is the explicit composition root.

This shape prevents Swing widgets, Picocli commands, Jackson annotations, and filesystem concerns from entering the domain. It also avoids a framework or multi-module build that would add ceremony without improving this desktop tool.

## Static capabilities and selected state

`ProcessorDefinition`, `PinDefinition`, and `PeripheralDefinition` describe hardware capabilities loaded from JSON. They are immutable and reject duplicate pin names, physical numbers, peripheral IDs, blank functions, and other malformed structural data. Processor-specific Java subclasses are intentionally absent.

`Configuration` represents user choices separately: processor ID, power mode, peripheral and clock states, and pin assignments. Configuration operations return a new snapshot. `ConfigurationService` owns the current snapshot, tracks unsaved changes, prevents unsupported assignment operations, and requires explicit consent before a dirty configuration is replaced. The same immutable snapshots provide bounded undo/redo history, reusable templates, import, and field-level configuration diff without a second state model.

Peripheral instances and signal policies can be declared explicitly in each processor JSON through optional `peripherals` metadata (`id`, `category`, `signals`, and `exclusiveSignals`). `ProcessorDefinition` is the context-aware resolver used by validation, GUI, summary, and export, so an identifier such as `WAKE_REQUEST` can belong to `SAFETY0` without a Java regex or code change. The model itself validates metadata against pin functions, rejects duplicate signal ownership, and requires exclusivity to be a subset of the peripheral's signals. A non-GPIO function that has no peripheral must be listed in `independentSignals`; unmanaged functions are rejected instead of silently bypassing clock validation.

`SignalResolver` remains an isolated compatibility adapter for definitions that do not yet contain metadata. The raw identifier `I2C_SCL` remains unchanged but maps to `I2C0`; `LPUART` remains the signal prefix while its power category is `UART`; `ETH_*` maps to the Ethernet category. Convention-derived legacy signals retain the previous exclusive behavior. Explicit definitions choose exclusivity per signal and can therefore model repeatable non-GPIO signals.

Bundled definitions are listed by `processors/index.json`, which works identically from the classpath and the shaded JAR. `JsonProcessorRepository` also scans an optional external directory supplied through `configtool.processorDir` or `CONFIGTOOL_PROCESSOR_DIR`. Therefore, a schema-compatible `ProcessorD` can appear in GUI and CLI without recompilation. Duplicate IDs across bundled and external sources are rejected rather than silently overridden.

## Validation framework

`ValidationRule` is a small functional interface. `ValidationEngine` receives a list of rules, executes each rule, and returns a deterministic `ValidationResult` containing structured issues. Every issue carries a diagnostic code, severity, location, explanation, and corrective action.

The mandatory rules cover pin multiplexing, peripheral clock dependency, and documented power-mode compatibility. Independent additional rules cover unsupported pin functions, duplicate signals marked exclusive by processor metadata, and assignments to disabled peripherals. A separate advisory rule warns when an enabled peripheral has no resources. Errors block export; warnings remain visible but allow export. Interactive assignment replaces the previous function on the same pin; `PinMultiplexingRule` remains a defensive boundary for imported or independently constructed snapshots, with tests for both paths.

No monolithic validator or interface-specific rule set exists. A new Java rule is implemented and registered without rewriting `ValidationEngine`. User-managed JSON rules add conditions for processor, power mode, enabled peripherals, and assigned functions at the composition root, so GUI and CLI execute the same custom policy. At load time, their processor, peripheral, and signal references are checked against the relevant discovered processor definitions; typos fail with a file-specific diagnostic instead of creating dormant rules. An integration test builds the same configuration through GUI table models and CLI commands, then compares state and validation results.

## Interfaces and export

The Swing UI is a multi-pane engineering workspace with light/dark FlatLaf themes, processor and power controls, searchable pin table, peripheral and clock table, navigable validation issues, dependency tree, summary, keyboard shortcuts, and guarded destructive transitions. Menus expose JSON import, JSON/XML export, undo/redo, deterministic auto-assignment, templates, and diff. Function editors expose only functions supported by the current pin. User-facing errors do not expose stack traces.

The Picocli adapter maintains one in-memory configuration across an interactive session. It covers every GUI workflow except visual theme selection, including import/export, history, auto-assignment, templates, dependencies, and diff. A semicolon-delimited scripted mode supports deterministic smoke tests without creating a second state model.

`ConfigurationExporter` decouples output format from configuration behavior; JSON and XML adapters serialize the same validated domain state. `ConfigurationImporter` performs structural JSON parsing and the service verifies processor-specific semantics before replacing a session. A structurally sound snapshot may still contain business conflicts: this is an intentional repair workflow, not silent acceptance. The GUI validates immediately after import, opens the Validation section when issues exist, and reports the counts and corrective actions. Exporters write to a temporary sibling and attempt an atomic replacement. Filesystems that reject atomic moves use an ordinary replacement fallback; therefore atomicity is not guaranteed in every environment, although generation never writes directly into the target file.

## Decisions and trade-offs

- Java 21 records and defensive copies express immutable capabilities without inheritance hierarchies.
- The application uses one stateful service per UI session. It is synchronized for predictable adapter access and avoids global mutable state.
- Auto-assignment uses deterministic, all-or-nothing backtracking over free candidate pins and never moves an existing assignment. Activation is a visible caller choice; the assignment-plus-enable mode updates pins, peripheral, and clock in one undoable snapshot, while failure leaves the original state unchanged.
- Intermediate invalid configurations are allowed where useful, so users receive complete actionable diagnostics. Unsupported functions are rejected immediately by the service and still have a defensive validation rule for imported or malformed snapshots.
- Power restrictions are implemented only for CAN, UART, SPI, ADC, and Ethernet as specified. No undocumented restrictions are applied to I2C, PWM, TIMER, FLEXIO, external interrupts, or trace.
- The dependency tree visualizes logical relationships rather than imitating a package drawing. The pin table remains the authoritative editor because physical drag-and-drop was not required.
- SHA-256 regression tests lock the canonical pin maps. `tools/verify_pin_maps.py` establishes their provenance by parsing the supplied DOCX in place and comparing all 144 definitions without publishing the source document.
