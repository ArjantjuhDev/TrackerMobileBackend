from pathlib import Path
import sys
sys.path.append(str(Path(__file__).parent.parent))
from tracker_server import app
# Vercel will use 'app' as the entry point for the Python API
