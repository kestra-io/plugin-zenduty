package io.kestra.plugin.zenduty;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.JacksonMapper;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Renders zenduty-template.peb the same way ZendutyTemplate#run does
 * (render map through Property, render template, parse as JSON) and checks
 * how customMessage and customFields land in the payload.
 */
@KestraTest
class ZendutyTemplateCustomFieldsTest {
    private static final List<String> BUILT_IN = List.of(
        "Namespace", "Flow ID", "Execution ID", "Execution Status", "Final task ID", "Duration"
    );

    @Inject
    private RunContextFactory runContextFactory;

    private Map<String, Object> baseVars() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(
            "execution", Map.of(
                "id", "exec-1",
                "namespace", "company.team",
                "flowId", "my-flow",
                "state", Map.of("current", "FAILED")
            )
        );
        vars.put("lastTask", Map.of("taskId", "failed"));
        vars.put("duration", "00:00:01.000");
        vars.put("link", "https://kestra.example/ui/executions/exec-1");
        vars.put("firstFailed", true);
        return vars;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> renderPayload(Map<String, Object> vars) throws Exception {
        RunContext runContext = runContextFactory.of();
        String template = IOUtils.toString(
            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("zenduty-template.peb")),
            StandardCharsets.UTF_8
        );
        Map<String, Object> renderMap = runContext.render(Property.ofValue(vars)).asMap(String.class, Object.class);
        String rendered = runContext.render(template, renderMap);
        Map<String, Object> body = (Map<String, Object>) JacksonMapper.ofJson().readValue(rendered, Object.class);
        return (Map<String, Object>) body.get("payload");
    }

    @Test
    void neither() throws Exception {
        Map<String, Object> payload = renderPayload(baseVars());
        assertThat(payload.keySet(), containsInAnyOrder(BUILT_IN.toArray()));
    }

    @Test
    void customMessageOnly() throws Exception {
        Map<String, Object> vars = baseVars();
        vars.put("customMessage", "Check the nightly ETL");
        Map<String, Object> payload = renderPayload(vars);
        assertThat(payload.get("Message"), is("Check the nightly ETL"));
        assertThat(payload.size(), is(BUILT_IN.size() + 1));
    }

    @Test
    void customFieldsOnly() throws Exception {
        Map<String, Object> vars = baseVars();
        vars.put("customFields", Map.of("Team", "data-platform", "Env", "prod"));
        Map<String, Object> payload = renderPayload(vars);
        assertThat(payload.get("Team"), is("data-platform"));
        assertThat(payload.get("Env"), is("prod"));
        assertThat(payload, not(hasKey("Message")));
        assertThat(payload.size(), is(BUILT_IN.size() + 2));
    }

    @Test
    void emptyCustomFields() throws Exception {
        Map<String, Object> vars = baseVars();
        vars.put("customFields", Map.of());
        Map<String, Object> payload = renderPayload(vars);
        assertThat(payload.keySet(), containsInAnyOrder(BUILT_IN.toArray()));
    }

    @Test
    void specialCharactersStayValidJson() throws Exception {
        String tricky = "say \"hi\"\nline2\ttab \\ back\\slash ünïcødé 🚀 </script>";
        Map<String, Object> vars = baseVars();
        vars.put("customMessage", tricky);
        vars.put("customFields", Map.of("Key \"quoted\"\n\\", tricky));
        Map<String, Object> payload = renderPayload(vars);
        assertThat(payload.get("Message"), is(tricky));
        assertThat(payload.get("Key \"quoted\"\n\\"), is(tricky));
    }

    @Test
    void nonStringValuesKeepTheirJsonType() throws Exception {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("Retries", 3);
        fields.put("Ratio", 0.5);
        fields.put("Critical", true);
        fields.put("Owners", List.of("a", "b"));
        fields.put("Meta", Map.of("region", "eu", "tier", 1));
        Map<String, Object> vars = baseVars();
        vars.put("customFields", fields);
        Map<String, Object> payload = renderPayload(vars);
        assertThat(payload.get("Retries"), is(3));
        assertThat(payload.get("Ratio"), is(0.5));
        assertThat(payload.get("Critical"), is(true));
        assertThat(payload.get("Owners"), is(List.of("a", "b")));
        assertThat(payload.get("Meta"), is(Map.of("region", "eu", "tier", 1)));
    }

    @Test
    void builtInKeysCannotBeOverridden() throws Exception {
        Map<String, Object> vars = baseVars();
        vars.put("customMessage", "the message");
        vars.put(
            "customFields", Map.of(
                "Namespace", "spoofed",
                "Execution Status", "SUCCESS",
                "Message", "other",
                "Team", "data-platform"
            )
        );
        Map<String, Object> payload = renderPayload(vars);
        assertThat(payload.get("Namespace"), is("company.team"));
        assertThat(payload.get("Execution Status"), is("FAILED"));
        assertThat(payload.get("Message"), is("the message"));
        assertThat(payload.get("Team"), is("data-platform"));
    }

    @Test
    void messageFieldAllowedWhenNoCustomMessage() throws Exception {
        Map<String, Object> vars = baseVars();
        vars.put("customFields", Map.of("Message", "from fields"));
        Map<String, Object> payload = renderPayload(vars);
        assertThat(payload.get("Message"), is("from fields"));
    }
}
