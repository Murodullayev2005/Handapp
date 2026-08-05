FROM python:3.11-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# OpenCV, GUI va Kamera interfeyslari uchun zarur bo'lgan tizim paketlari
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    v4l-utils \
    && rm -rf /var/lib/apt/lists/*

# Talablarni o'rnatish
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Loyihaning barcha fayllarini nusxalash
COPY . /app

# Asosiy ishga tushirish fayli
CMD ["python", "main_windows.py"]