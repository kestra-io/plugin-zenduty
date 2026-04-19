# Kestra Zenduty Plugin

## What

- Provides plugin components under `io.kestra.plugin.zenduty`.
- Includes classes such as `ZendutyExecution`, `AlertType`, `ZendutyTemplate`, `ZendutyAlert`.

## Why

- What user problem does this solve? Teams need to create incidents in Zenduty from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Zenduty steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Zenduty.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `zenduty`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.zenduty.ZendutyAlert`
- `io.kestra.plugin.zenduty.ZendutyExecution`

### Project Structure

```
plugin-zenduty/
├── src/main/java/io/kestra/plugin/zenduty/
├── src/test/java/io/kestra/plugin/zenduty/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
