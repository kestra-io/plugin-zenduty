package io.kestra.plugin.zenduty;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;

import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;

import io.swagger.v3.oas.annotations.media.Schema;
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
public abstract class ZendutyTemplate extends ZendutyAlert {

    @Schema(
        title = "Template path",
        description = "Internal template resource path rendered before sending to Zenduty",
        hidden = true
    )
    protected Property<String> templateUri;

    @Schema(
        title = "Template variables map",
        description = "Values injected into the template; supports expressions"
    )
    protected Property<Map<String, Object>> templateRenderMap;

    @Schema(
        title = "Event title",
        description = "Rendered into `message` in the Zenduty payload"
    )
    protected Property<String> message;

    @Schema(
        title = "Event summary",
        description = "Rendered into `summary` in the Zenduty payload"
    )
    protected Property<String> summary;

    @Schema(
        title = "Alert severity",
        description = "Zenduty alert type value rendered to lowercase string",
        implementation = AlertType.class
    )
    protected Property<AlertType> alertType;

    @Schema(
        title = "Alert entity id",
        description = "Optional unique id for the alert; Zenduty generates one if omitted"
    )
    protected Property<String> entityId;

    @Schema(
        title = "Related URLs",
        description = "List of URLs added under `urls` in the payload"
    )
    protected Property<List<String>> urls;

    @SuppressWarnings("unchecked")
    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        Map<String, Object> map = new HashMap<>();

        final var renderedTemplateUri = runContext.render(this.templateUri).as(String.class);
        if (renderedTemplateUri.isPresent()) {
            String template = IOUtils.toString(
                Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(renderedTemplateUri.get())),
                StandardCharsets.UTF_8
            );

            String render = runContext.render(
                template, templateRenderMap != null ? runContext.render(templateRenderMap).asMap(String.class, Object.class) : Map.of()
            );
            map = (Map<String, Object>) JacksonMapper.ofJson().readValue(render, Object.class);
        }

        if (runContext.render(this.message).as(String.class).isPresent()) {
            map.put("message", runContext.render(this.message).as(String.class).get());
        }

        if (runContext.render(this.summary).as(String.class).isPresent()) {
            map.put("summary", runContext.render(this.summary).as(String.class).get());
        }

        if (runContext.render(this.alertType).as(AlertType.class).isPresent()) {
            map.put("alert_type", runContext.render(this.alertType).as(AlertType.class).get().name().toLowerCase());
        }

        if (runContext.render(this.entityId).as(String.class).isPresent()) {
            map.put("entity_id", runContext.render(this.entityId).as(String.class).get());
        }

        if (!runContext.render(this.urls).asList(String.class).isEmpty()) {
            map.put("urls", runContext.render(this.urls).asList(String.class));
        }

        this.payload = Property.ofValue(JacksonMapper.ofJson().writeValueAsString(map));

        return super.run(runContext);
    }

}
