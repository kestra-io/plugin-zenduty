package io.kestra.plugin.zenduty;

import java.util.Map;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.plugins.notifications.ExecutionInterface;
import io.kestra.core.plugins.notifications.ExecutionService;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Notify Zenduty on flow execution",
    description = "Sends a templated Zenduty message with execution link, namespace, flow, start time, duration, status, and failing task (if any). Use only in Flow trigger notifications; for `errors` handlers prefer `ZendutyAlert`."
)
@Plugin(
    examples = {
        @Example(
            title = "Send a Zenduty notification on a failed flow execution.",
            full = true,
            code = """
                id: zenduty_failure_alert
                namespace: company.team

                tasks:
                  - id: send_alert
                    type: io.kestra.plugin.zenduty.ZendutyExecution
                    url: "https://www.zenduty.com/api/events/{{ secret('ZENDUTY_INTEGRATION_KEY') }}/"
                    executionId: "{{ trigger.executionId }}"
                    message: Kestra workflow execution {{ trigger.executionId }} of a flow {{ trigger.flowId }} in the namespace {{ trigger.namespace }} changed status to {{ trigger.state }}

                triggers:
                  - id: failed_prod_workflows
                    type: io.kestra.plugin.core.trigger.Flow
                    conditions:
                      - type: io.kestra.plugin.core.condition.ExecutionStatus
                        in:
                          - FAILED
                          - WARNING
                      - type: io.kestra.plugin.core.condition.ExecutionNamespace
                        namespace: prod
                        prefix: true
                """
        )
    },
    aliases = "io.kestra.plugin.notifications.zenduty.ZendutyExecution"
)
public class ZendutyExecution extends ZendutyTemplate implements ExecutionInterface {
    @Builder.Default
    private final Property<String> executionId = Property.ofExpression("{{ execution.id }}");
    private Property<Map<String, Object>> customFields;
    private Property<String> customMessage;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        this.templateUri = Property.ofValue("zenduty-template.peb");
        this.templateRenderMap = Property.ofValue(ExecutionService.executionMap(runContext, this));

        return super.run(runContext);
    }
}
