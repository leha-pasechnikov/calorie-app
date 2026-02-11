from fastapi import FastAPI, UploadFile, File, HTTPException, Request, Query
from logging.handlers import RotatingFileHandler
from cachetools import TTLCache
from PIL import Image
import asyncio
import logging
import time
import sys
import io

from src.api.analyze import analyze_food_image, analyze_food_json
from src.env import is_production
from src.api.schemas import *

# данные последнего использования
_client_locks = TTLCache(maxsize=1000, ttl=60)
_client_locks_lock = asyncio.Lock()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        RotatingFileHandler("../api.log", maxBytes=5 * 1024 * 1024, backupCount=3)
    ]
)
logger = logging.getLogger("api")

app = FastAPI(
    title="Food Analysis API",
    description="API для анализа изображений еды и определения пищевой ценности",
    version="1.0.0"
)


@app.post("/analyze/", response_model=FoodResponse)
async def analyze_food(
        request: Request,
        file: UploadFile = File(..., description="Изображение еды для анализа")
):
    """Анализирует изображение еды и возвращает результат."""
    max_file_size: int = 10 * 1024 * 1024  # 10Мб
    client_ip = request.client.host if request.client else "unknown"
    async with _client_locks_lock:
        if client_ip not in _client_locks:
            _client_locks[client_ip] = asyncio.Lock()
        lock = _client_locks[client_ip]

    async with lock:
        # Чтение файла
        contents = await file.read()

        # Проверка типа файла
        allowed_types = {"image/jpeg", "image/png"}
        if file.content_type not in allowed_types:
            raise HTTPException(
                status_code=400,
                detail="Поддерживаются только JPEG и PNG"
            )

        if len(contents) > max_file_size:
            raise HTTPException(
                status_code=400,
                detail=f"Размер файла превышает {max_file_size // (1024 * 1024)}MB"
            )

        # Открытие изображения
        try:
            image = Image.open(io.BytesIO(contents))
            if image.mode in ('RGBA', 'LA', 'P'):
                image = image.convert('RGB')
        except Exception:
            raise HTTPException(
                status_code=400,
                detail="Невозможно обработать изображение"
            )

        # Анализ изображения
        try:
            result = await asyncio.wait_for(analyze_food_image(image), timeout=45.0)
        except asyncio.TimeoutError:
            logger.error("Food analysis timed out after 45 seconds")
            raise HTTPException(
                status_code=408,
                detail="Анализ изображения превысил лимит времени"
            )
        finally:
            image.close()
        status = result.get('status')
        logger.debug(f"Analysis result: {result}")

        if status == 'success':
            return FoodSuccessResponse(**result)
        elif status == 'not_found':
            return NotFoundResponse(**result)
        elif status == 'danger':
            return DangerResponse(**result)
        elif status == 'error':
            raise HTTPException(
                status_code=400,
                detail=result.get('message')
            )
        else:
            # Некорректный статус от Gemini API
            raise HTTPException(
                status_code=500,
                detail=f"Некорректный статус от сервиса анализа"
            )


@app.get("/search/", response_model=FoodAPIResponseSearch)
async def get_nutrition_info(
        food_name: str = Query(..., min_length=1, max_length=100, description="Название продукта")
):
    """
    Возвращает пищевую ценность продукта на 100 г.
    """
    # обработка поиска
    try:
        result = await asyncio.wait_for(analyze_food_json(food_name), timeout=15.0)
    except asyncio.TimeoutError:
        logger.error("Food searching timed out after 15 seconds")
        return ErrorFoundResponseSearch()

    # возврат результата
    if result:
        return FoodResponseSearch(
            food_name=food_name,
            nutrition=NutritionSearch(**result)
        )
    else:
        return NotFoundResponseSearch()


@app.get("/")
async def root():
    """Информация о API"""
    return {
        "message": "Food Analysis API",
        "description": "Анализ изображений еды и определение пищевой ценности"
    }


@app.get("/health")
async def health_check():
    """Проверка работоспособности API"""
    return {"status": "healthy", "service": "food-detect"}


@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Логирование HTTP-запросов и ответов"""
    # Получаем IP-адрес клиента
    client_host = request.client.host if request.client else "unknown"
    start_time = time.time()
    logger.info(f"Incoming request | Method: {request.method} | URL: {request.url.path} | Client: {client_host}")

    try:
        response = await call_next(request)
        process_time = (time.time() - start_time) * 1000
        logger.info(
            f"Outgoing response | Status: {response.status_code} | "
            f"Method: {request.method} | URL: {request.url.path} | "
            f"Duration: {process_time:.2f}ms | Client: {client_host}"
        )
        return response
    except Exception as e:
        process_time = (time.time() - start_time) * 1000
        logger.error(
            f"Request failed | Method: {request.method} | URL: {request.url.path} | "
            f"Client: {client_host} | Duration: {process_time:.2f}ms | Error: {e}"
        )
        raise


if __name__ == "__main__":
    import uvicorn

    if is_production:
        uvicorn.run("api:app", host="0.0.0.0", port=8000, log_level="warning", reload=False)
    else:
        uvicorn.run("api:app", host="127.0.0.1", port=8000, reload=True)
