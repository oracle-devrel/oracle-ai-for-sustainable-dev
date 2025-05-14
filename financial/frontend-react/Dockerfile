# Step 1: Build React App
FROM node:20 as build

WORKDIR /app
COPY package*.json ./
RUN npm install
RUN #npm install && npm run build && rm -rf node_modules

# Pass the environment variable during the build
ARG REACT_APP_BACKEND_URL
ENV REACT_APP_BACKEND_URL=$REACT_APP_BACKEND_URL

COPY . .
RUN npm run build

# Step 2: Serve using nginx
FROM nginx:alpine

COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
