# Use Node.js as the base image
FROM node:20

# Set the working directory
WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the backend source code
COPY . .

# Expose the backend port
EXPOSE 5000

# Set environment variables for the backend
ENV PORT=5000
ENV MONGODB_URL=$MONGODB_URL

# Start the backend server
CMD ["node", "app.js"]
