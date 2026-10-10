# NXP Mini Configuration Tool

Desktop configuration utility implemented in Java 21 with a Swing GUI and a Picocli CLI. The application loads processor capabilities from JSON, keeps mutable user configuration separate from static hardware definitions, validates embedded constraints through composable rules, and exports a versioned JSON configuration.

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

The toolbar controls processor selection and power mode. The left navigation opens:

- **Overview** for configuration metrics and workflow guidance
- **Pin Configuration** for searchable, constrained function assignment
- **Peripherals & Clocks** for enable and clock state plus resource visibility
- **Validation** for actionable errors and warnings; double-clicking navigates to the resource
- **Configuration Summary** for a complete pre-export review

Changing processors does not silently discard edits. If the current state is dirty, the GUI asks for confirmation and the CLI requires `select <id> --discard`.

## Validation behavior

Mandatory constraints:

- one active function per physical pin;
- every enabled peripheral requires its clock;
- CAN, SPI, and Ethernet are rejected in `LOW_POWER`; UART and ADC remain allowed.

Additional independent rules:

- assigned functions must be supported by the selected pin;
- an exclusive signal cannot be active on more than one pin;
- an assignment cannot target a disabled peripheral.

An enabled peripheral without assigned resources is reported as a warning. Errors block export; warnings do not.

The naming adapter deliberately maps `LPUART` signals to the `UART` power category and preserves the source identifier `I2C_SCL` while associating it with `I2C0`. No LOW_POWER restrictions are invented for categories not covered by the requirements.

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
      "availableFunctions": ["GPIO", "LPUART9_TX"]
    }
  ]
}
```

Place one or more definitions in an external directory and provide it at startup:

```powershell
java -Dconfigtool.processorDir=C:\hardware\processors -jar target/mini-configuration-tool-0.0.1-SNAPSHOT-all.jar gui
```

Alternatively set `CONFIGTOOL_PROCESSOR_DIR`. Files are validated during startup; duplicate processor IDs, pin names, physical numbers, malformed function lists, and unsupported schema versions fail with a diagnostic instead of producing unpredictable state.

## Extension points

- Add validation by implementing `ValidationRule` and registering it in `DefaultValidationRules` or in another composition root. `ValidationEngine` does not change.
- Add an export format by implementing `ConfigurationExporter` and injecting it into `ConfigurationService`. Configuration and validation logic do not change.
- Add another interface by calling the same `ConfigurationService`; Swing and Picocli are existing examples.
- New signal naming conventions are isolated in `SignalResolver`. Extending that policy does not change raw processor JSON.

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
