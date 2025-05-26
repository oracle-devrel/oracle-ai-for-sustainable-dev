package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/containers/common/libnetwork/types"
	"github.com/containers/podman/v5/pkg/bindings"
	"github.com/containers/podman/v5/pkg/bindings/containers"
	"github.com/containers/podman/v5/pkg/bindings/images"
	"github.com/containers/podman/v5/pkg/specgen"

	specs "github.com/opencontainers/runtime-spec/specs-go"
	go_ora "github.com/sijms/go-ora/v2"
	"github.com/uptrace/bun"
	"github.com/uptrace/bun/dialect/oracledialect"
)

type Product struct {
	bun.BaseModel `bun:"table:products,alias:u"`

	ID    int64 `bun:",pk,autoincrement"`
	Name  string
	Price float64
}

func main() {

	conn, err := bindings.NewConnection(context.Background(), "unix://"+os.Getenv("XDG_RUNTIME_DIR")+"/podman/podman.sock")
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	rawImage := "container-registry.oracle.com/database/free:latest"
	imageExists, err := images.Exists(conn, rawImage, nil)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	if !imageExists {
		fmt.Println("Pulling Oracle DB image...")
		_, err = images.Pull(conn, rawImage, nil)
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
	} else {
		fmt.Println("Using existing Oracle DB image...")
	}

	containerExists, err := containers.Exists(conn, "oracle-container", nil)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	containerHealthy := false
	if containerExists {
		inspectResult, err := containers.Inspect(conn, "oracle-container", nil)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error inspecting container: %v\n", err)
			os.Exit(1)
		}

		if inspectResult.State.Health.Status == "healthy" {
			containerHealthy = true
		}
	}

	if !containerExists || !containerHealthy {
		tmpDir, err := os.MkdirTemp("", "oradata")
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
		os.Chmod(tmpDir, os.ModePerm)

		trueVal := true

		containers.Remove(conn, "oracle-container", &containers.RemoveOptions{
			Force: &trueVal,
		})

		s := specgen.NewSpecGenerator(rawImage, false)
		s.Name = "oracle-container"
		s.Mounts = []specs.Mount{
			{
				Type:        "bind",
				Source:      tmpDir,
				Destination: "/opt/oracle/oradata",
			},
		}
		s.PortMappings = []types.PortMapping{
			{
				ContainerPort: 1521,
				HostPort:      1521,
				Protocol:      "tcp",
				HostIP:        "0.0.0.0",
			},
		}

		s.Hostname = "oracle-container"
		s.Env = map[string]string{
			"ORACLE_PWD": "oracle123",
		}
		createResponse, err := containers.CreateWithSpec(conn, s, nil)
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
		fmt.Println("Container created.")
		if err := containers.Start(conn, createResponse.ID, nil); err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
		fmt.Println("Container started.")

		status := ""
		// Wait for the database to start
		for status != "healthy" {

			inspectResult, err := containers.Inspect(conn, createResponse.ID, nil)
			if err != nil {
				fmt.Fprintf(os.Stderr, "Error inspecting container: %v\n", err)
				os.Exit(1)
			}

			status = inspectResult.State.Health.Status

			if status == "healthy" {
				break
			} else {
				log.Println("Waiting for database to start...")
				time.Sleep(10 * time.Second)
			}

		}

	} else {
		log.Println("Using existing database...")
	}

	log.Println("Connecting to database...")
	sqldb, err := sql.Open("oracle", go_ora.BuildUrl("localhost", 1521, "FREEPDB1", "SYSTEM", "oracle123", nil))
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	log.Println("Connected to database...")

	db := bun.NewDB(sqldb, oracledialect.New())

	log.Println("Creating table...")
	// Drop and create tables.
	err = db.ResetModel(context.Background(), (*Product)(nil))
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	log.Println("Created table...")

	// Insert a new product
	log.Println("Inserting data to the table...")

	// Insert multiple products (bulk-insert).
	p1 := Product{Name: "apple", Price: 5.99}
	p2 := Product{Name: "orange", Price: 4.99}
	products := []Product{p1, p2}
	_, err = db.NewInsert().Model(&products).Exec(context.Background())
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	log.Println("Inserted data to the table...")

	log.Println("Reading data from the table...")
	// Read all products
	var allProducts []Product
	err = db.NewSelect().Model(&allProducts).Scan(context.Background(), &allProducts)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	for _, product := range allProducts {
		fmt.Printf("Product %d: %s - $%.2f\n", product.ID, product.Name, product.Price)
	}
	log.Println("Read data from the table...")

	log.Println("Updating data in the table...")
	// Update a product
	allProducts[0].Name = "banana"
	_, err = db.NewUpdate().Model(&allProducts[0]).Column("name").WherePK().Exec(context.Background())
	if err != nil {
		log.Fatal(err)
	}
	log.Println("Updated data in the table...")

	log.Println("Deleting data from the table...")
	// Delete a product
	_, err = db.NewDelete().Model(&allProducts[1]).WherePK().Exec(context.Background())
	if err != nil {
		log.Fatal(err)
	}
	log.Println("Deleted data from the table...")
}
