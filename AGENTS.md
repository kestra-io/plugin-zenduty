# Kestra Zenduty Plugin

## What

- Provides plugin components under `io.kestra.plugin.zenduty`.
- Includes classes such as `ZendutyExecution`, `AlertType`, `ZendutyTemplate`, `ZendutyAlert`.

## Why

- This plugin integrates Kestra with Zenduty.
- It provides tasks that create incidents in Zenduty.

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
