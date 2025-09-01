# Use Ubuntu base and install Flutter manually for better control
FROM ubuntu:22.04

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV FLUTTER_HOME=/opt/flutter
ENV PATH="$FLUTTER_HOME/bin:$PATH"

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    wget \
    unzip \
    xz-utils \
    zip \
    libglu1-mesa \
    python3 \
    psmisc \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Download and install Flutter
RUN git clone https://github.com/flutter/flutter.git $FLUTTER_HOME && \
    cd $FLUTTER_HOME && \
    git checkout stable

# Pre-download Flutter dependencies
RUN flutter precache --web

# Enable flutter web
RUN flutter config --enable-web

# Create app directory
WORKDIR /app

# Copy the app source code
COPY . /app/

# Copy custom chrome configuration if needed
RUN if [ -f "chrome.dart" ]; then \
        cp chrome.dart $FLUTTER_HOME/packages/flutter_tools/lib/src/web/chrome.dart; \
    fi

# Build the Flutter web app
RUN flutter build web

# Record the exposed port
EXPOSE 5000

# Make server startup script executable and start the web server
RUN chmod +x /app/server/server.sh

ENTRYPOINT ["/app/server/server.sh"]
