package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"strconv"
	"strings"

	"google.golang.org/adk/agent"
	"google.golang.org/adk/cmd/launcher"
	"google.golang.org/adk/cmd/launcher/web"
	adka2a "google.golang.org/adk/cmd/launcher/web/a2a"
	"google.golang.org/adk/session"
)

func main() {
	ctx := context.Background()
	coordinator := newInventoryActionCoordinator()

	rootAgent, err := coordinator.buildRootAgent(ctx)
	if err != nil {
		log.Fatalf("Failed to build inventory action agent: %v", err)
	}

	port := envInt("ACTION_AGENT_PORT", envInt("PORT", 8080))
	rpcURL := buildRPCURL(port)

	webLauncher := web.NewLauncher(adka2a.NewLauncher())
	_, err = webLauncher.Parse([]string{
		"--port", strconv.Itoa(port),
		"a2a", "--a2a_agent_url", rpcURL,
	})
	if err != nil {
		log.Fatalf("launcher.Parse() error = %v", err)
	}

	config := &launcher.Config{
		AgentLoader:    agent.NewSingleLoader(rootAgent),
		SessionService: session.InMemoryService(),
	}

	log.Printf("Starting Oracle Inventory Action Agent on %s", rpcURL)
	if err := webLauncher.Run(ctx, config); err != nil {
		log.Fatalf("webLauncher.Run() error = %v", err)
	}
}

func envInt(key string, fallback int) int {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return fallback
	}
	return parsed
}

func buildRPCURL(port int) string {
	if explicit := firstNonBlank(os.Getenv("ACTION_AGENT_URL"), os.Getenv("A2A_URL")); explicit != "" {
		return strings.TrimRight(explicit, "/")
	}
	publicProtocol := firstNonBlank(os.Getenv("PUBLIC_PROTOCOL"), "http")
	publicHost := firstNonBlank(os.Getenv("PUBLIC_HOST"), "localhost")
	return fmt.Sprintf("%s://%s:%d", publicProtocol, publicHost, port)
}
