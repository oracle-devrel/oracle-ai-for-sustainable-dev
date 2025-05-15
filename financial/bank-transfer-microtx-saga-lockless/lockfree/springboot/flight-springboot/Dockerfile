# Copyright (c) 2023, Oracle and/or its affiliates. **

# The Universal Permissive License (UPL), Version 1.0 **

# Subject to the condition set forth below, permission is hereby granted to any person obtaining a copy of this software, associated documentation and/or data
# (collectively the "Software"), free of charge and under any and all copyright rights in the Software, and any and all patent rights owned or freely licensable by each
# licensor hereunder covering either the unmodified Software as contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both
# ** (a) the Software, and (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the Software (each a "Larger Work" to which
# the Software is contributed by such licensors), **
# without restriction, including without limitation the rights to copy, create derivative works of, display, perform, and distribute the Software and make, use, sell,
# offer for sale, import, export, have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights on either these or other terms. **

# This license is subject to the following condition: The above copyright notice and either this complete permission notice or at a minimum a reference to the UPL must be
# included in all copies or substantial portions of the Software. **

# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
# PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
# CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

# 1st stage, build the app
FROM maven:3.8.3-openjdk-17 as build

WORKDIR /flight-sb-app

# Create a first layer to cache the "Maven World" in the local repository.
# Incremental docker builds will always resume after that, unless you update
# the pom

COPY pom.xml .

# Do the Maven build!
# Incremental docker builds will resume here when you change sources
ADD src src
RUN mvn package -DskipTests
RUN echo "done!"

# 2nd stage, build the runtime image
FROM openjdk:17.0.1-jdk-slim
WORKDIR /flight-sb-app

# Copy the binary built in the 1st stage
COPY --from=build  /flight-sb-app/target/lockfree-flight-sb.jar ./

CMD ["java", "-jar", "lockfree-flight-sb.jar"]

EXPOSE 8083