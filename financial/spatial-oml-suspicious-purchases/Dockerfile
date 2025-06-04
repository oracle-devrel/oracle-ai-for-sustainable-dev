FROM jupyter/base-notebook

COPY notebooks/ /home/jovyan/work/
COPY sql/ /home/jovyan/sql/

# Optional: install extra dependencies
COPY requirements.txt /tmp/
RUN pip install --no-cache-dir -r /tmp/requirements.txt
