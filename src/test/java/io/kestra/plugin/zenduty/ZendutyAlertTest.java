package io.kestra.plugin.zenduty;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.google.common.io.Files;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.flows.Flow;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.validations.ModelValidator;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.YamlParser;
import io.kestra.core.utils.IdUtils;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@KestraTest
public class ZendutyAlertTest {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private ModelValidator modelValidator;

    @Test
    void run() throws Exception {
        var runContext = runContextFactory.of(
            Map.of(
                "message", "Zenduty test alert notification",
                "entityId", IdUtils.create()
            )
        );

        var task = ZendutyAlert.builder()
            .url(startServer().getURI() + "/webhook-unit-test")
            .payload(
                Property.ofValue(
                    Files.asCharSource(
                        new File(Objects.requireNonNull(ZendutyAlertTest.class.getClassLoader().getResource("zenduty.peb")).toURI()),
                        StandardCharsets.UTF_8
                    ).read()
                )
            )
            .build();

        task.run(runContext);

        assertThat(FakeWebhookController.data, containsString("ge *with some bold text* an"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void flowSettingDeprecatedOptionsStillValidatesAndRuns() throws Exception {
        var message = "deprecated-options-" + IdUtils.create();
        var flow = YamlParser.parse(
            """
                id: zenduty_deprecated_options
                namespace: io.kestra.tests
                tasks:
                  - id: alert
                    type: io.kestra.plugin.zenduty.ZendutyAlert
                    url: "%s/webhook-unit-test"
                    payload: '{"message": "%s", "alert_type": "error"}'
                    options:
                      readTimeout: PT30S
                      connectionPoolIdleTimeout: PT10S
                      maxContentLength: 1024
                """.formatted(startServer().getURI(), message),
            Flow.class
        );

        assertThat(modelValidator.isValid(flow).isEmpty(), is(true));

        var runContext = runContextFactory.of(Map.of());
        var task = (ZendutyAlert) flow.getTasks().getFirst();
        var options = task.getOptions();
        assertThat(runContext.render(options.getReadTimeout()).as(Duration.class).orElseThrow(), is(Duration.ofSeconds(30)));
        assertThat(runContext.render(options.getConnectionPoolIdleTimeout()).as(Duration.class).orElseThrow(), is(Duration.ofSeconds(10)));
        assertThat(runContext.render(options.getMaxContentLength()).as(Integer.class).orElseThrow(), is(1024));

        task.run(runContext);

        assertThat(FakeWebhookController.data, containsString(message));
    }

    private EmbeddedServer startServer() {
        var embeddedServer = applicationContext.getBean(EmbeddedServer.class);
        embeddedServer.start();
        return embeddedServer;
    }
}
