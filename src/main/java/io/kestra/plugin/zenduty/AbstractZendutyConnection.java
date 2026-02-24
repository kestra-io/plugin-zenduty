package io.kestra.plugin.zenduty;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.http.client.configurations.TimeoutConfiguration;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractZendutyConnection extends Task implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Tune Zenduty HTTP client",
        description = "Optional HTTP client settings such as timeouts, charset, headers, and response size limits. Defaults: read timeout 10s, read idle timeout 5m, connection pool idle timeout 0s (no reuse), max content length 10 MB, charset UTF-8"
    )
    @PluginProperty(dynamic = true)
    protected RequestOptions options;

    protected HttpConfiguration httpClientConfigurationWithOptions() throws IllegalVariableEvaluationException {
        HttpConfiguration.HttpConfigurationBuilder configuration = HttpConfiguration.builder();

        if (this.options != null) {

            configuration
                .timeout(TimeoutConfiguration.builder()
                    .connectTimeout(this.options.getConnectTimeout())
                    .readIdleTimeout(this.options.getReadIdleTimeout())
                .build())
                .defaultCharset(this.options.getDefaultCharset());
        }

        return configuration.build();
    }

    protected HttpRequest.HttpRequestBuilder createRequestBuilder(
        RunContext runContext) throws IllegalVariableEvaluationException {

        HttpRequest.HttpRequestBuilder builder = HttpRequest.builder();

        if (this.options != null && this.options.getHeaders() != null) {
            Map<String, String> headers = runContext.render(this.options.getHeaders())
                .asMap(String.class, String.class);

            if (headers != null) {
                headers.forEach(builder::addHeader);
            }
        }
        return builder;
    }

    @Getter
    @Builder
    public static class RequestOptions {
        @Schema(title = "Connection timeout before failing")
        private final Property<Duration> connectTimeout;

        @Schema(title = "Read timeout before failing", description = "Defaults to 10s; aborts if the server does not send data within this duration")
        @Builder.Default
        private final Property<Duration> readTimeout = Property.ofValue(Duration.ofSeconds(10));

        @Schema(title = "Idle read timeout", description = "Defaults to 5m; closes the connection if no data is read during this interval")
        @Builder.Default
        private final Property<Duration> readIdleTimeout = Property.ofValue(Duration.of(5, ChronoUnit.MINUTES));

        @Schema(title = "Connection pool idle timeout", description = "Defaults to 0s; disables keeping idle connections in the pool")
        @Builder.Default
        private final Property<Duration> connectionPoolIdleTimeout = Property.ofValue(Duration.ofSeconds(0));

        @Schema(title = "Maximum response size", description = "Defaults to 10 MB; responses larger than this fail")
        @Builder.Default
        private final Property<Integer> maxContentLength = Property.ofValue(1024 * 1024 * 10);

        @Schema(title = "Request charset", description = "Defaults to UTF-8 for request encoding")
        @Builder.Default
        private final Property<Charset> defaultCharset = Property.ofValue(StandardCharsets.UTF_8);

        @Schema(
            title = "HTTP headers",
            description = "Additional headers to include on the Zenduty request; supports expression rendering"
        )
        public Property<Map<String,String>> headers;
    }
}
