#!/bin/bash
# Activate pyenv environment for Oracle AI project
export PATH="$HOME/.pyenv/bin:$PATH"
eval "$(pyenv init -)"
pyenv shell 3.12.12
echo "Python: $(python --version)"
echo "pyenv active: $(pyenv version-name)"
echo ""
echo "Environment ready. Run the menu with:"
echo "  ./run.sh"
