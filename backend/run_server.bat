@echo off
echo Starting GraphRAG Server...
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo Python is not installed or not in PATH
    pause
    exit /b 1
)

REM Check if virtual environment exists
if not exist "venv" (
    echo Creating virtual environment...
    python -m venv venv
)

REM Activate virtual environment
echo Activating virtual environment...
call venv\Scripts\activate.bat

REM Install dependencies
echo Installing dependencies...
pip install -r requirements.txt

REM Check if .env file exists
if not exist ".env" (
    echo .env file not found!
    echo Please create a .env file with your OpenAI API key:
    echo OPENAI_API_KEY=your-api-key-here
    pause
    exit /b 1
)

REM Load environment variables
for /f "usebackq tokens=1,2 delims==" %%a in (".env") do set %%a=%%b

REM Check if API key is set
if "%OPENAI_API_KEY%"=="" (
    echo Please set your OpenAI API key in .env file
    pause
    exit /b 1
)

REM Start server
echo Starting GraphRAG server with FastAPI...
echo Server will be available at http://localhost:5000
echo API documentation available at http://localhost:5000/docs
echo Press Ctrl+C to stop the server
echo.
python graphrag_server.py

pause
