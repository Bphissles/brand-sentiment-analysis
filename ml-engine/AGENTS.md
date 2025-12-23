# Repository Guidelines

## Project Structure & Modules
- `app/`: Core ML and API code (`api.py`, `sentiment.py`, `clustering.py`, `preprocessing.py`, `models.py`).
- `tests/`: Pytest test suite (add `test_*.py` files here).
- `requirements.txt`: Python dependencies for the ML engine and API.
- `.env.example` / `.env`: Environment variables (API keys, ports, Supabase, Gemini). Keep `.env` out of version control.
- `venv/`: Local virtual environment; do not edit or commit code here.

## Setup, Build, and Run
- Create a virtualenv and install deps:
  - `python -m venv venv && source venv/bin/activate`
  - `pip install -r requirements.txt`
- Run the API locally (development):
  - `python app/api.py`
- Production-style run (example):
  - `gunicorn app.api:app`

## Coding Style & Naming
- Python 3, 4-space indentation, UTF-8, one import group per block.
- Use `snake_case` for functions/variables, `PascalCase` for classes, and `UPPER_SNAKE_CASE` for constants.
- Prefer type hints and docstrings (see `app/sentiment.py` for patterns).
- Use `logging.getLogger(__name__)` instead of `print` for runtime diagnostics.

## Testing Guidelines
- Framework: `pytest` configured via `pytest.ini` with markers `unit`, `integration`, and `slow`.
- Run all tests: `pytest`
- Run with coverage: `pytest --cov=app`
- Place tests under `tests/` and name files `test_<module>.py`, functions `test_<behavior>()`.

## Commit & Pull Request Guidelines
- Write imperative, descriptive commit messages (e.g., `Add Gemini fallback for sentiment`, `Improve clustering for short posts`).
- Keep commits focused and reasonably small; group related changes.
- For PRs, include:
  - Summary of changes and rationale.
  - How to reproduce and test (commands, environment notes).
  - Linked issue or ticket ID, and screenshots/log excerpts if behavior changes.

