# =============================================================================
# dev-tools Dockerfile
# =============================================================================
# This container gives you a MANUAL development environment.
# It has everything installed:
#   • JDK 8  (includes wsimport at $JAVA_HOME/bin/wsimport)
#   • Apache Ant (to run build.xml manually)
#   • curl (to test the SOAP endpoints)
#
# PURPOSE: You connect to this container with:
#   docker exec -it dev-tools bash
#
# Then you can:
#   1. Run wsimport manually against the live server WSDL
#   2. Compile the generated stubs + your client code with javac
#   3. Run the client with java
#   4. Test with curl
#
# This is the "I want to understand every step" workflow —
# the opposite of the auto-generated Maven approach.
# =============================================================================
FROM maven:3.8.6-openjdk-8

# Install Ant and curl
RUN apt-get update && apt-get install -y \
    ant \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create workspace directories that Ant's build.xml writes into
RUN mkdir -p /workspace/generated-src/getbook \
             /workspace/generated-src/createbook \
             /workspace/build \
             /workspace/dist

# Copy the Ant build script
COPY ant/build.xml /workspace/build.xml

# Copy WSDL and XSD files (the contract files wsimport will read)
# XSD files are co-located with the WSDLs so relative schemaLocation
# imports ("book.xsd", "common.xsd") resolve correctly.
COPY wsdl/ /wsdl/

# Copy the hand-written client source code into the workspace.
# Ant's "compile" target will compile this together with the generated stubs.
COPY client-src/ /workspace/client-src/

WORKDIR /workspace

# Keep the container alive so you can exec into it
# (without this it would exit immediately)
CMD ["tail", "-f", "/dev/null"]
