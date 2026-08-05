FROM python:3.11-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Tizim paketlarini o'rnatish
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    v4l-utils \
    && rm -rf /var/lib/apt/lists/*

# XAVFSIZLIK FIXI: Setuptools/wheel tarkibidagi zaifliklarni bartaraf etish uchun yangilash
RUN pip install --no-cache-dir --upgrade pip setuptools wheel

# Loyiha kutubxonalarini o'rnatish
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Loyiha fayllarini nusxalash
COPY . .

# Non-root foydalanuvchi sozlamasi (Best Practice)
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

CMD ["python", "main_windows.py"]