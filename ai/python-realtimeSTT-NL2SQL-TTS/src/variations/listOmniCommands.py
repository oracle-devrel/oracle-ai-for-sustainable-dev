import omni.kit.commands

# List all available commands in the current session
available_commands = omni.kit.commands.get_command_names()

# Print the commands
for command in available_commands:
    print(command)
