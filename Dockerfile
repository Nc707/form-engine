# syntax=docker/dockerfile:1

# Builds the demo application from source, so that running it needs nothing but Docker. Everything
# heavy — the JDK, Maven, the Node toolchain, the whole ~/.m2 — lives in the build stage and is
# thrown away; the runtime stage keeps one jar.

FROM maven:3.9-eclipse-temurin-21 AS build

# Node comes from the official image rather than from apt or from the vaadin-maven-plugin.
#
# apt is out because Debian and Ubuntu ship Node 18-20, and flow-server 25's
# FrontendTools.SUPPORTED_NODE_VERSION is major 24: NodeResolver would log "older than the required
# minimum version" and quietly fall back to downloading its own toolchain into ~/.vaadin. That
# fallback is the thing being avoided — it makes every cold build wait on nodejs.org, so a build
# that should only need Docker Hub starts failing whenever that host is unreachable.
#
# Copying the binary is pinned and offline in a way `curl | bash` from NodeSource is not.
COPY --from=node:24-bookworm-slim /usr/local/bin/node /usr/local/bin/node
COPY --from=node:24-bookworm-slim /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && node --version && npm --version

WORKDIR /build
COPY . .

# -Pproduction builds the Vaadin frontend bundle into the jar. Without it the jar boots in
# development mode and tries to run Vite against sources that are not in the runtime image, so the
# UI never renders.
#
# -am is not optional: it is what makes Maven build the twelve sibling modules from this reactor.
# Without it they are resolved from ~/.m2, which in a fresh container holds nothing at all.
#
# The cache mount keeps ~/.m2 across builds. It is a mount rather than a COPYed layer on purpose:
# baking the repository into the image would put a few hundred megabytes of jars in a stage that
# only exists to produce one.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Pproduction -pl form-engine-demo -am clean package -DskipTests


FROM eclipse-temurin:21-jre AS runtime

# A process that only serves HTTP has no reason to be able to write to its own image. Running as
# root by default means any remote-code-execution bug in the app is immediately a root shell.
RUN useradd --system --create-home --uid 10001 formengine
USER formengine
WORKDIR /home/formengine

# The wildcard is on the version, so bumping form-engine.version does not silently break the build
# here; there is exactly one jar under that path.
COPY --from=build --chown=formengine:formengine /build/form-engine-demo/target/form-engine-demo-*.jar app.jar

# Matches server.port=${PORT:8081} in the demo's application.properties.
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
