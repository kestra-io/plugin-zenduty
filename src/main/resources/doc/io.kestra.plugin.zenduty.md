# How to use the Zenduty plugin

Trigger Zenduty alerts and send execution summaries from Kestra flows.

## Authentication

Set `url` to your Zenduty service's incoming webhook URL. Store it in a [secret](https://kestra.io/docs/concepts/secret).

## Tasks

`ZendutyAlert` triggers an alert as a step within a flow — set `payload` to a JSON body in the Zenduty alert format.

`ZendutyExecution` sends a structured execution summary including status, duration, and an execution link, and is designed for use with a [Flow trigger](https://kestra.io/docs/workflow-components/triggers) in a dedicated monitoring namespace that watches other namespaces for failures.
