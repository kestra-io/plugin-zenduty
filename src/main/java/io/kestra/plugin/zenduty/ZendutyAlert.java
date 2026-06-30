package io.kestra.plugin.zenduty;

import java.net.URI;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Send Zenduty alert payload",
    description = "Posts a custom JSON payload to the Zenduty Events API. Use in `errors` to signal flow failures. Payload must follow the Events API (required: `message`, `alert_type`)."
)
@Plugin(
    examples = {
        @Example(
            title = "Send a Zenduty alert on a failed flow execution. Make sure that the payload follows the [Zenduty Events API specification](https://apidocs.zenduty.com/#tag/Events/paths/~1api~1events~1%7Bintegration_key%7D~1/post), including the `message` and `alert_type` payload properties, which are required.",
            full = true,
            code = """
                id: unreliable_flow
                namespace: company.team

                tasks:
                  - id: fail
                    type: io.kestra.plugin.scripts.shell.Commands
                    commands:
                      - exit 1

                errors:
                  - id: alert_on_failure
                    type: io.kestra.plugin.zenduty.ZendutyAlert
                    url: "https://www.zenduty.com/api/events/{{ secret('ZENDUTY_INTEGRATION_KEY') }}/"
                    payload: |
                      {
                        "alert_type": "info",
                        "message": "This is info alert",
                        "summary": "This is the incident summary",
                        "suppressed": false,
                        "entity_id": 12345,
                        "payload": {
                            "status": "ACME Payments are failing",
                            "severity": "1",
                            "project": "kubeprod"
                          },
                        "urls": [
                          {
                            "link_url": "https://www.example.com/alerts/12345/",
                            "link_text": "Alert URL"
                          }
                        ]
                      }
                """
        )
    },
    aliases = "io.kestra.plugin.notifications.zenduty.ZendutyAlert"
)
public class ZendutyAlert extends AbstractZendutyConnection {

    @Schema(
        title = "Zenduty Events API URL",
        description = "Full Events API endpoint including integration key, e.g. `https://www.zenduty.com/api/events/<integration_key>/`"
    )
    @PluginProperty(dynamic = true, group = "main", secret = true)
    @ToString.Exclude
    @NotBlank
    protected String url;

    @Schema(
        title = "Alert payload JSON",
        description = "Raw JSON body sent to Zenduty; supports expressions before send"
    )
    @PluginProperty(group = "main")
    protected Property<String> payload;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        String url = runContext.render(this.url);

        try (HttpClient client = new HttpClient(runContext, super.httpClientConfigurationWithOptions())) {
            String payload = runContext.render(this.payload).as(String.class).orElse(null);

            runContext.logger().debug("Send Zenduty webhook: {}", payload);
            HttpRequest.HttpRequestBuilder requestBuilder = createRequestBuilder(runContext)
                .addHeader("Content-Type", "application/json")
                .uri(URI.create(url))
                .method("POST")
                .body(
                    HttpRequest.StringRequestBody.builder()
                        .content(payload)
                        .build()
                );

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = client.request(request, String.class);

            runContext.logger().debug("Response: {}", response.getBody());

            if (response.getStatus().getCode() == 200) {
                runContext.logger().info("Request succeeded");
            }
        }

        return null;
    }
}
