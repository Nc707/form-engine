package com.nc.formengine.flow.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * What an application can change about the views it gets from this module.
 */
@ConfigurationProperties("formengine.flow")
public class FormEngineFlowProperties {

    private final Routes routes = new Routes();

    /**
     * Who submissions are attributed to when the application has not said. Replacing
     * {@link com.nc.formengine.flow.spi.SubmissionAuthorProvider} is how an application with real
     * accounts answers that properly.
     */
    private String defaultAuthor = "anonymous";

    public Routes getRoutes() {
        return routes;
    }

    public String getDefaultAuthor() {
        return defaultAuthor;
    }

    public void setDefaultAuthor(String defaultAuthor) {
        this.defaultAuthor = defaultAuthor;
    }

    /**
     * Where the views are mounted.
     *
     * <p>Every path is {@code prefix} followed by the segment for that view, so the defaults put
     * the whole module under {@code form-engine/}. An application that wants them elsewhere moves
     * them wholesale by setting the prefix, or one at a time by setting a segment; setting the
     * prefix to the empty string mounts them at the root.
     *
     * <p>The route parameters themselves are not configurable. They are part of what a view is —
     * the renderer cannot work without knowing which form to render — not part of where it lives.
     */
    public static class Routes {

        private boolean enabled = true;

        private String prefix = "form-engine";

        /** The list of form definitions, the builder's home. */
        private String definitions = "definitions";

        /** The editor for one definition. Takes an optional form id. */
        private String builder = "builder";

        /** The list of published forms to fill in. */
        private String forms = "forms";

        /** The renderer. Takes a form id and an optional draft id. */
        private String fill = "fill";

        /** The submissions, and under it {@code /form/{id}} and {@code /submission/{id}}. */
        private String responses = "responses";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getDefinitions() {
            return definitions;
        }

        public void setDefinitions(String definitions) {
            this.definitions = definitions;
        }

        public String getBuilder() {
            return builder;
        }

        public void setBuilder(String builder) {
            this.builder = builder;
        }

        public String getForms() {
            return forms;
        }

        public void setForms(String forms) {
            this.forms = forms;
        }

        public String getFill() {
            return fill;
        }

        public void setFill(String fill) {
            this.fill = fill;
        }

        public String getResponses() {
            return responses;
        }

        public void setResponses(String responses) {
            this.responses = responses;
        }
    }
}
