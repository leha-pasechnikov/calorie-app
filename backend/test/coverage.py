from pathlib import Path
import subprocess
import webbrowser
import os

os.chdir("..")

subprocess.run([
    "pytest", "test/",
    "--cov=src",
    "--cov-report=html",
    "--cov-report=term-missing",
    "--color=yes",
    # "--run-skipped",
    "-v"
])

report_path = Path("htmlcov/index.html").resolve()
webbrowser.open(report_path.as_uri())