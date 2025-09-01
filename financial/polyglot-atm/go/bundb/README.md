# GoLang ORM using BunDB and Oracle Database
This code creates a Docker container with Oracle Database installed, waits for the database to start, and then connects to it using Go's `github.com/uptrace/bun` package. It creates a simple "products" 
table, inserts few products, reads all products, updates a product, and deletes a product.

Note that you'll need to have `podman` installed on your system. Install it using [installation instructions](https://podman.io/docs/installation).

Also, this is just an example code and it's not meant to be used in production without proper error handling and security considerations.