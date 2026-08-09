package com.nc.formengine.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    static final String PROBLEM_SCHEMA = "ProblemDetail";
    static final String PROBLEM_JSON = "application/problem+json";

    private static final String PROBLEM_REF = "#/components/schemas/" + PROBLEM_SCHEMA;

    @Bean
    public OpenAPI formEngineOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Form Engine API")
                .version("v1")
                .description("""
                        REST API for the form engine. Form structure is defined at runtime, not at \
                        compile time: a form definition owns its fields, options, dependencies and \
                        per-device layouts, and submissions are stored against those definitions.

                        Errors are returned as RFC 7807 problem details (application/problem+json): \
                        400 for a malformed or invalid request, 404 for an unknown resource, \
                        409 for a duplicate form code, and 422 when a request is well formed but \
                        breaks a domain rule.""")
                .license(new License().name("MIT")));
    }

    /**
     * Documents the error responses every operation shares, instead of repeating the same four
     * {@code @ApiResponse} blocks on 51 handler methods. Codes specific to one endpoint (409, 422)
     * stay annotated on that endpoint.
     */
    @Bean
    public OpenApiCustomizer problemDetailResponses() {
        return openApi -> {
            openApi.schema(PROBLEM_SCHEMA, problemDetailSchema());

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        addProblemResponse(operation, "400", "The request was malformed or failed validation.");
                        if (hasPathParameter(operation)) {
                            addProblemResponse(operation, "404", "No such resource.");
                        }
                        addProblemResponse(operation, "500", "Unexpected server error.");
                    }));
        };
    }

    private static boolean hasPathParameter(Operation operation) {
        return operation.getParameters() != null
                && operation.getParameters().stream().map(Parameter::getIn).anyMatch("path"::equals);
    }

    /** Never overwrites a response the endpoint documented itself. */
    private static void addProblemResponse(Operation operation, String status, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        if (responses.containsKey(status)) {
            return;
        }
        responses.addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_JSON,
                        new MediaType().schema(new Schema<>().$ref(PROBLEM_REF)))));
    }

    /**
     * Written out by hand rather than reflected off {@code ProblemDetail}, because the interesting
     * parts — {@code errors}, {@code timestamp} — are dynamic properties the class does not declare.
     * Keep in step with {@code GlobalExceptionHandler}.
     */
    private static Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("RFC 7807 problem detail.")
                .addProperty("type", new StringSchema()
                        .format("uri")
                        .example("https://form-engine/errors/not-found"))
                .addProperty("title", new StringSchema().example("Resource Not Found"))
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("detail", new StringSchema().example("Form not found with id: 42"))
                .addProperty("instance", new StringSchema()
                        .format("uri")
                        .example("/form-engine/api/v1/form-definitions/42"))
                .addProperty("timestamp", new StringSchema()
                        .format("date-time")
                        .description("When the error was produced."))
                .addProperty("errors", new ArraySchema()
                        .description("Present on 400 validation failures (one entry per violated "
                                + "constraint) and on 422 domain failures (one entry per broken rule).")
                        .items(new Schema<>().oneOf(List.of(
                                new ObjectSchema()
                                        .description("A violated bean-validation constraint (400).")
                                        .addProperty("field", new StringSchema())
                                        .addProperty("message", new StringSchema())
                                        .addProperty("rejectedValue", new StringSchema()),
                                new StringSchema()
                                        .description("A broken domain rule (422).")))));
    }
}
