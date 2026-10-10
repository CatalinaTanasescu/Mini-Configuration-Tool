# NXP Mini Configuration Tool

Desktop configuration utility implemented in Java 21 with a Swing GUI and a Picocli CLI. The application loads processor capabilities from JSON, keeps mutable user configuration separate from static hardware definitions, validates embedded constraints through composable rules, imports and compares saved configurations, and exports versioned JSON or XML.

## Requirements

- JDK 21
- Maven 3.9 or newer
- Windows, Linux, or macOS for the CLI; the GUI is optimized for Windows desktop use

## Build and verify

From the project root:

```powershell
mvn clean verify
mvn package
```

The executable artifact is:

```text
target/mini-configuration-tool-0.0.1-SNAPSHOT-all.jar
```

## Run

Launch the desktop application:

```powershell
java -jar target/mini-configuration-tool-0.0.1-SNAPSHOT-all.jar gui
```

Display CLI help or start an interactive CLI session:

```powershell
java -jar target/mini-configuration-tool-0.0.1-SNAPSHOT-all.jar cli --help
java -jar target/mini-configuration-tool-0.0.1-SNAPSHOT-all.jar cli
```

Example interactive session:

```text
processors
select ProcessorA
enable LPUART0
clock LPUART0 on
assign PTA2 LPUART0_TX
power RUN
validate
summary
export configuration.json
exit
```

Advanced CLI commands include `import`, `undo`, `redo`, `auto-assign`, `dependencies`, `diff`, `templates`, `save-template`, and `apply-template`. XML uses the existing export command:

```powershell
java -jar target/mini-configuration-tool-0.0.1-SNAPSHOT-all.jar cli --script='enable LPUART0;clock LPUART0 on;auto-assign LPUART0;export configuration.xml --format xml'
```

For repeatable smoke tests, commands can run in one persistent scripted session:

```powershell
java -jar target/mini-configuration-tool-0.0.1-SNAPSHOT-all.jar cli --script='enable LPUART0;clock LPUART0 on;assign PTA2 LPUART0_TX;validate;summary;export "configuration.json"'
```

Exit codes are `0` for success, `2` for command/configuration errors, and `3` when validation finds blocking errors.

## Eclipse setup

1. Open Eclipse with `C:\Users\Andrei\eclipse-workspace` as the workspace.
2. Choose **File → Import → Maven → Existing Maven Projects**.
3. Select the `mini-configuration-tool` directory and finish the import.
4. Right-click the project and choose **Maven → Update Project** if dependencies are not yet resolved.
5. Run **Run As → Maven test** or create a Maven build with goal `clean verify`.
6. Run `com.example.configtool.Main` as a Java application with program argument `gui` or `cli --help`.

The project compiler release is fixed at Java 21 in `pom.xml`. Eclipse should show a JavaSE-21 JRE System Library.

## Desktop workflow

The toolbar controls processor selection and power mode. The File, Edit, Tools, and View menus provide JSON import, JSON/XML export, undo/redo, auto-assignment, templates, configuration diff, dependency visualization, and light/dark theme switching. The left navigation opens:

- **Overview** for configuration metrics and workflow guidance
- **Pin Configuration** for searchable, constrained function assignment
- **Peripherals & Clocks** for enable and clock state plus resource visibility
- **Validation** for actionable errors and warnings; double-clicking navigates to the resource
- **Dependencies** for a processor → peripheral → clock → signal/pin tree
- **Configuration Summary** for a complete pre-export review

Changing processors does not silently discard edits. If the current state is dirty, the GUI asks for confirmation and the CLI requires `select <id> --discard`.

## Validation behavior

Mandatory constraints:

- one active function per physical pin;
- every enabled peripheral requires its clock;
- CAN, SPI, and Ethernet are rejected in `LOW_POWER`; UART and ADC remain allowed.

Additional independent rules:

- assigned functions must be supported by the selected pin;
- a signal explicitly declared exclusive cannot be active on more than one pin;
- an assignment cannot target a disabled peripheral.

An enabled peripheral without assigned resources is reported as a warning. Errors block export; warnings do not.

User-defined validation rules can be loaded at startup from JSON files in a directory selected with `-Dconfigtool.validationDir=<path>` or `CONFIGTOOL_VALIDATION_DIR`. Rules combine processor, power-mode, enabled-peripheral, and assigned-function conditions without Java changes:

```json
{
  "schemaVersion": 1,
  "code": "CUSTOM_UART_LOW_POWER",
  "severity": "WARNING",
  "message": "UART review is required in low power.",
  "suggestion": "Confirm the board power budget.",
  "when": {
    "processorIds": ["ProcessorA"],
    "powerModes": ["LOW_POWER"],
    "allEnabledPeripherals": ["LPUART0"]
  }
}
```

Rule files are checked against the processors available in the same application session. Unknown processor IDs, peripheral IDs, or assigned-function names fail at startup with a file-specific diagnostic. When a rule is not scoped with `processorIds`, each referenced peripheral and function must exist on every available processor; scope processor-specific policies explicitly.

Explicit processor metadata is the primary source for signal ownership and exclusivity. For legacy definitions without a `peripherals` section, the compatibility adapter maps known naming conventions, treats the inferred signals as exclusive, maps `LPUART` to the `UART` power category, and preserves the source identifier `I2C_SCL` while associating it with `I2C0`. No LOW_POWER restrictions are invented for categories not covered by the requirements.

## Add a processor without changing Java code

Create a JSON file that follows schema version 1:

```json
{
  "schemaVersion": 1,
  "id": "ProcessorD",
  "displayName": "Processor D",
  "pins": [
    {
      "name": "PTX0",
      "physicalNumber": 1,
      "availableFunctions": ["GPIO", "WAKE_REQUEST", "STATUS_BUS"]
    },
    {
      "name": "PTX1",
      "physicalNumber": 2,
      "availableFunctions": ["GPIO", "WAKE_REQUEST", "STATUS_BUS", "BOARD_WAKE"]
    }
  ],
  "peripherals": [
    {
      "id": "SAFETY0",
      "category": "OTHER",
      "signals": ["WAKE_REQUEST", "STATUS_BUS"],
      "exclusiveSignals": ["WAKE_REQUEST"]
    }
  ],
  "independentSignals": ["BOARD_WAKE"]
}
```

The optional `peripherals` metadata makes signal ownership independent of identifier naming. `exclusiveSignals` is an explicit subset of `signals`; in the example, `WAKE_REQUEST` may be assigned to only one pin while `STATUS_BUS` is repeatable. Metadata may be introduced gradually because uncovered legacy signals still use `SignalResolver`. Every remaining non-GPIO function must be listed in `independentSignals`; otherwise loading fails with an actionable unmanaged-function diagnostic. This prevents silent signals without a peripheral/clock policy.

Place one or more definitions in an external directory and provide it at startup:

```powershell
java -Dconfigtool.processorDir=C:\hardware\processors -jar target/mini-configuration-tool-0.0.1-SNAPSHOT-all.jar gui
```

Alternatively set `CONFIGTOOL_PROCESSOR_DIR`. Files are validated during startup; duplicate processor IDs, pin names, physical numbers, malformed function lists, and unsupported schema versions fail with a diagnostic instead of producing unpredictable state.

## Advanced workflows

- **Import JSON:** reopens an exported configuration after verifying its processor, peripheral states, pins, and functions. Business-invalid but structurally repairable configurations are accepted intentionally; the GUI opens Validation immediately and reports the error/warning counts.
- **Undo/redo:** keeps exactly the most recent 100 immutable session snapshots, including processor and template transitions. A new modification clears Redo history.
- **Auto pin assignment:** uses deterministic, all-or-nothing backtracking to assign every unassigned signal of a selected peripheral without moving existing assignments. The GUI explicitly offers either pin assignment only or assignment plus peripheral/clock activation. CLI uses `auto-assign PERIPHERAL --enable` for the atomic enable variant; one Undo restores the entire operation.
- **Configuration templates:** saves reusable JSON configurations. The default directory is `%USERPROFILE%\.nxp-mini-configuration-tool\templates`; override it with `configtool.templateDir` or `CONFIGTOOL_TEMPLATE_DIR`.
- **Configuration diff:** compares processor, power mode, peripheral/clock states, and pin assignments against another JSON configuration.
- **Dependency visualization:** displays peripheral enable state, clock state, signal exclusivity, and assigned pins as an expandable tree.
- **Dark mode:** recreates the Swing window with FlatLaf Dark while retaining the same `ConfigurationService` session.

## Independent pin-map verification

The regression digests were established by parsing the original DOCX directly and comparing every pin name, physical sequence, and ordered function list with the bundled JSON files. The source document is read in place and is not copied into the repository. Re-run the procedure with Python 3:

```powershell
python tools/verify_pin_maps.py --docx "C:\path\to\Mini Configuration Tool 2.docx"
```

The expected result is 32 + 48 + 64 definitions, zero differences, and the same SHA-256 values asserted by `JsonProcessorRepositoryTest`.

## Extension points

- Add validation by implementing `ValidationRule` and registering it in `DefaultValidationRules` or in another composition root. `ValidationEngine` does not change.
- Add user-managed validation without recompilation by placing declarative rule JSON files in the configured validation directory.
- Add an export format by implementing `ConfigurationExporter` and injecting it into `ConfigurationService`. Configuration and validation logic do not change.
- Add another interface by calling the same `ConfigurationService`; Swing and Picocli are existing examples.
- Define new peripheral or signal types in processor JSON with explicit `peripherals` metadata. `SignalResolver` remains a compatibility adapter for legacy definitions.

## Project structure

```text
com.example.configtool
├── domain             immutable definitions, selected state, validation contracts/rules
├── application        use-case facade and repository/export ports
├── infrastructure     Jackson processor repository and JSON exporter
├── presentation       Swing GUI and Picocli CLI adapters
└── bootstrap          explicit dependency composition
```

See [docs/DESIGN.md](docs/DESIGN.md) for the concise architecture document.
