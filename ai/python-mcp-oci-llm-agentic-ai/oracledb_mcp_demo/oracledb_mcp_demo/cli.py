"""CLI application for Oracle Mcp."""

import asyncio
import logging
import sys

from typing import Optional

import click
from rich.console import Console
from rich.panel import Panel
from rich.text import Text

from core.orchestrator import Orchestrator

console = Console()
logger = logging.getLogger(__name__)


class OracleMcpCLI:
    """Oracle Mcp CLI application."""
    
    def __init__(self, config_path: Optional[str] = None):
        """Initialize CLI application.
        
        Args:
            config_path: Path to configuration file
        """
        self.orchestrator = Orchestrator(config_path)
        self.console = console
    
    def start(self) -> None:
        """Start the CLI application."""
        try:
            # Start database connection
            logger.info("Starting CLI with database connection")
            asyncio.run(self.orchestrator.start_mcp_server()) ## Tools available
            
            self._show_welcome()
            self._interactive_loop()
        except KeyboardInterrupt:
            self._handle_exit()
        except Exception as e:
            self.console.print(f"[red]Error: {e}[/red]")
            sys.exit(1)
        finally:
            # Stop database session
            try:
                asyncio.run(self.orchestrator.stop_mcp_server())
            except Exception as e:
                logger.warning(f"Error stopping database session: {e}")
    
    def _show_welcome(self) -> None:
        """Show welcome message."""
        welcome_text = Text()
        welcome_text.append("🧠 Oracle Mcp CLI", style="bold blue")
        welcome_text.append("\nNatural Language Database Assistant")
        welcome_text.append("\n\nJust tell me what you want to do!")
        welcome_text.append("\nType 'help' for examples")
        
        panel = Panel(welcome_text, border_style="blue")
        self.console.print(panel)
    
    def _interactive_loop(self) -> None:
        """Main interactive loop."""
        while True:
            try:
                # Show prompt
                current_db = self.orchestrator.get_current_database()
                if current_db:
                    self.console.print(f"[green]{current_db}[/green] > ", end="")
                else:
                    self.console.print("[yellow]no-db[/yellow] > ", end="")
                
                # Get user input
                user_input = input().strip()
                
                if not user_input:
                    continue
                
                # Process command
                self._process_command(user_input)
                
            except EOFError:
                break
            except KeyboardInterrupt:
                self._handle_exit()
                break
    
    def _process_command(self, command: str) -> None:
        """Process user command using NLP.
        
        Args:
            command: User command
        """
        if not command.strip():
            return
        
        # Handle special commands
        if command.lower() in ['exit', 'quit', 'q']:
            self._handle_exit()
            return
        
        if command.lower() == 'help':
            self._show_help()
            return
        
        if command.lower() == 'status':
            self._show_status()
            return
        
        # Process natural language command
        self._process_nlp_command(command)
    
    def _show_help(self) -> None:
        """Show help information."""
        help_text = Text()
        help_text.append("📚 Oracle Mcp Help\n\n", style="bold blue")
        help_text.append("Available Commands:\n", style="bold")
        help_text.append("  help     - Show this help message\n")
        help_text.append("  status   - Show current system status\n")
        help_text.append("  exit/quit - Exit the application\n\n")
        help_text.append("Natural Language Examples:\n", style="bold")
        help_text.append("  'Show me the top 10 SQL statements by CPU usage'\n")
        help_text.append("  'Connect to mydb and show current user'\n")
        help_text.append("  'List all tables in the current schema'\n")
        help_text.append("  'Help me troubleshoot a slow query'\n")
        
        panel = Panel(help_text, border_style="green")
        self.console.print(panel)
    
    def _show_status(self) -> None:
        """Show current system status."""
        status_text = Text()
        status_text.append("📊 System Status\n\n", style="bold blue")
        
        # Get orchestrator status
        try:
            current_db = self.orchestrator.get_current_database()
            status_text.append(f"Database: ", style="bold")
            if current_db:
                status_text.append(f"{current_db}", style="green")
            else:
                status_text.append("Not connected", style="red")
            status_text.append("\n")
            
            # Add more status information as needed
            status_text.append("MCP Server: ", style="bold")
            status_text.append("Running", style="green")
            status_text.append("\n")
            
        except Exception as e:
            status_text.append(f"Error getting status: {e}", style="red")
        
        panel = Panel(status_text, border_style="yellow")
        self.console.print(panel)
    
    def _process_nlp_command(self, prompt: str) -> None:
        """Process natural language command using orchestrator.
        
        Args:
            prompt: Natural language prompt
        """
        try:
            # Show processing indicator
            with self.console.status("[bold green]Processing..."):
                response = asyncio.run(self.orchestrator.process_prompt(prompt))
            
            # Display results
            self._display_response(response)
            
        except Exception as e:
            self.console.print(f"[red]Error processing prompt: {e}[/red]")
    
    def _display_response(self, response: dict) -> None:
        """Display LLM response.
        
        Args:
            response: Response dictionary
        """
        # Show feedback loop metadata if available
        if response.get("feedback_loop_metadata"):
            self._display_feedback_metadata(response["feedback_loop_metadata"])

        
        # Show SQL if generated
        if response.get("sql"):
            sql_panel = Panel(
                response["sql"],
                title="🧠 Generated SQL",
                border_style="blue"
            )
            self.console.print(sql_panel)
        
        # Show results if available
        if response.get("results"):
            results_panel = Panel(
                response["results"],
                title="✅ Results",
                border_style="green"
            )
            self.console.print(results_panel)
        
        # Show explanation if available
        if response.get("explanation"):
            explanation_panel = Panel(
                response["explanation"],
                title="📝 Explanation",
                border_style="yellow"
            )
            self.console.print(explanation_panel)
    
    def _display_feedback_metadata(self, metadata: dict) -> None:
        """Display feedback loop metadata."""
        feedback_text = Text()
        feedback_text.append("🔄 Iterative Processing Results\n\n", style="bold blue")
        feedback_text.append(f"📊 Total Iterations: {metadata['total_iterations']}\n")
        feedback_text.append(f"🏆 Best Score: {metadata['best_score']:.2f}\n")
        feedback_text.append(f"📈 Final Score: {metadata['final_score']:.2f}\n\n")
        
        if metadata.get('iteration_history'):
            feedback_text.append("📋 Iteration History:\n", style="bold")
            for iteration in metadata['iteration_history']:
                score_color = "green" if iteration['score'] >= 0.8 else "yellow" if iteration['score'] >= 0.5 else "red"
                feedback_text.append(f"  Iteration {iteration['iteration']}: ", style="white")
                feedback_text.append(f"{iteration['score']:.2f}", style=score_color)
                if iteration.get('feedback'):
                    feedback_text.append(f" - {iteration['feedback']}\n", style="dim")
                else:
                    feedback_text.append("\n")
        
        panel = Panel(feedback_text, title="🔄 Feedback Loop Results", border_style="magenta")
        self.console.print(panel)
    

    def _handle_exit(self) -> None:
        """Handle application exit."""
        self.console.print("\n[yellow]Goodbye! 👋[/yellow]")


@click.command()
@click.option(
    "--config",
    "-c",
    "config_path",
    help="Path to configuration file"
)
def main(config_path: Optional[str] = None) -> None:
    """Oracle Mcp CLI - DBA Assistant with LLM-powered SQL generation."""
    # Configure logging
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    
    # Start CLI
    cli = OracleMcpCLI(config_path)
    cli.start()


if __name__ == "__main__":
    main()
