from unittest.mock import patch, AsyncMock
from fastapi.testclient import TestClient
from io import BytesIO
from PIL import Image
import asyncio
import pytest

from src.api.api import app

client = TestClient(app)


class TestStaticEndpoints:
    """Тестирование статичных эндпоинтов"""

    def test_root_endpoint(self):
        """Проверяет корневой эндпоинт /"""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        assert data["message"] == "Food Analysis API"
        assert data["description"] == "Анализ изображений еды и определение пищевой ценности"

    def test_health_check_endpoint(self):
        """Проверяет эндпоинт /health"""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data == {"status": "healthy", "service": "food-detect"}


class TestAnalyzeFoodEndpoint:
    """Тестирование эндпоинта /analyze/"""

    @pytest.fixture(scope="session")
    def get_test_data(self) -> list[dict]:
        """Подставляет тестовые данные для ответа от analyze_food()"""
        return [
            {
                "status": "success",
                "food_items": {
                    "куриная грудка гриль": {
                        "proteins": 45.0,
                        "fats": 5.0,
                        "carbohydrates": 0.0,
                        "weight": 180,
                        "water": 126.0,
                        "benefit_score": 4.5
                    },
                    "отварной рис": {
                        "proteins": 4.0,
                        "fats": 1.0,
                        "carbohydrates": 38.0,
                        "weight": 150,
                        "water": 105.0,
                        "benefit_score": 3.8
                    },
                    "салат из свежих овощей": {
                        "proteins": 2.0,
                        "fats": 0.0,
                        "carbohydrates": 8.0,
                        "weight": 120,
                        "water": 108.0,
                        "benefit_score": 4.8
                    }
                }
            },
            {
                "status": "success",
                "food_items": {
                    "Мясное ассорти с картофелем фри, соусом и огурцами": {
                        "proteins": 129.5,
                        "fats": 145.0,
                        "carbohydrates": 61.0,
                        "water": 480.0,
                        "weight": 850,
                        "benefit_score": 2.5
                    }
                }
            },
            {
                "status": "not_found",
                "message": "На изображении не найдено продуктов питания"
            },
            {
                "status": "danger",
                "message": "Это ядовитый гриб - мухомор красный.",
                "food_items": {
                    "мухомор": {
                        "proteins": 2.0,
                        "fats": 0.0,
                        "carbohydrates": 3.0,
                        "water": 60.0,
                        "weight": 100,
                        "benefit_score": 0.0
                    }
                }
            },
            {
                "status": "error",
                "message": "Ошибка ответа в поле status"
            }
        ]

    @staticmethod
    def create_test_image(format_: str = "JPEG") -> BytesIO:
        """Создаёт простое тестовое изображение."""
        img = Image.new("RGB", (100, 100), color=(73, 109, 137))
        buf = BytesIO()
        img.save(buf, format=format_)
        buf.seek(0)
        return buf

    @pytest.mark.asyncio
    async def test_valid_image_analysis_with_get_test_data(self, get_test_data):
        """Тестирует обработку изображения для всех кейсов из get_test_data."""
        for mock_result in get_test_data:
            with patch("src.api.api.analyze_food_image", new_callable=AsyncMock) as mock_analyze:
                mock_analyze.return_value = mock_result

                img_buf = self.create_test_image()
                response = client.post(
                    "/analyze/",
                    files={"file": ("test.jpg", img_buf, "image/jpeg")}
                )

                status = mock_result["status"]

                if status == "error":
                    assert response.status_code == 400
                    assert mock_result["message"] in response.json()["detail"]
                else:
                    assert response.status_code == 200
                    resp_json = response.json()
                    assert resp_json["status"] == status

                    if status in ("success", "danger"):
                        assert "food_items" in resp_json
                    elif status == "not_found":
                        assert "message" in resp_json

    @pytest.mark.asyncio
    async def test_invalid_content_type(self):
        """Неподдерживаемый тип файла → 400 ошибка."""
        response = client.post("/analyze/", files={"file": ("test.txt", b"fake text", "text/plain")})
        assert response.status_code == 400
        assert "Поддерживаются только JPEG и PNG" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_file_too_large(self):
        """Файл больше 10 МБ → 400 ошибка."""
        large_file = b"x" * (10 * 1024 * 1024 + 1)  # чуть больше 10 МБ
        response = client.post("/analyze/", files={"file": ("big.jpg", large_file, "image/jpeg")})
        assert response.status_code == 400
        assert "Размер файла превышает 10MB" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_corrupted_image(self):
        """Некорректное изображение → 400 ошибка."""
        response = client.post("/analyze/", files={"file": ("bad.jpg", b"not an image", "image/jpeg")})
        assert response.status_code == 400
        assert "Невозможно обработать изображение" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_timeout_during_analysis(self):
        """Таймаут анализа → 408 ошибка."""
        with patch("src.api.api.analyze_food_image", new_callable=AsyncMock) as mock_analyze:
            mock_analyze.side_effect = asyncio.TimeoutError()

            img_buf = self.create_test_image()
            response = client.post(
                "/analyze/",
                files={"file": ("test.jpg", img_buf, "image/jpeg")}
            )
            assert response.status_code == 408
            assert "Анализ изображения превысил лимит времени" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_unknown_status_from_analyzer(self):
        """Неизвестный статус → 500 ошибка."""
        mock_result = {"status": "unknown_status"}

        with patch("src.api.api.analyze_food_image", new_callable=AsyncMock) as mock_analyze:
            mock_analyze.return_value = mock_result

            img_buf = self.create_test_image()
            response = client.post(
                "/analyze/",
                files={"file": ("test.jpg", img_buf, "image/jpeg")}
            )
            assert response.status_code == 500
            assert "Некорректный статус от сервиса анализа" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_rgba_image_conversion(self):
        """Проверяет, что RGBA-изображение конвертируется в RGB без ошибок."""
        # Создаём RGBA-изображение
        img = Image.new("RGBA", (100, 100), color=(73, 109, 137, 255))
        buf = BytesIO()
        img.save(buf, format="PNG")
        buf.seek(0)

        mock_result = {
            "status": "success",
            "food_items": {"тест": {"proteins": 1, "fats": 1, "carbohydrates": 1, "weight": 100, "water": 50,
                                    "benefit_score": 4.0}}
        }

        with patch("src.api.api.analyze_food_image", new_callable=AsyncMock) as mock_analyze:
            mock_analyze.return_value = mock_result

            response = client.post(
                "/analyze/",
                files={"file": ("test_rgba.png", buf, "image/png")}
            )

            assert response.status_code == 200
            assert response.json()["status"] == "success"
            # Убеждаемся, что функция анализа была вызвана (значит, ошибки конвертации не было)
            mock_analyze.assert_awaited_once()

    @pytest.mark.asyncio
    async def test_palette_image_conversion(self):
        """Проверяет обработку палитрового изображения (режим 'P')."""
        # Создаём изображение в режиме 'P' (палитра)
        img = Image.new("RGB", (100, 100), color=(100, 150, 200))
        img = img.convert("P")  # конвертируем в палитровый режим
        buf = BytesIO()
        img.save(buf, format="PNG")
        buf.seek(0)

        mock_result = {
            "status": "success",
            "food_items": {"тест": {"proteins": 1, "fats": 1, "carbohydrates": 1, "weight": 100, "water": 50,
                                    "benefit_score": 4.0}}
        }

        with patch("src.api.api.analyze_food_image", new_callable=AsyncMock) as mock_analyze:
            mock_analyze.return_value = mock_result

            response = client.post(
                "/analyze/",
                files={"file": ("test_palette.png", buf, "image/png")}
            )

            assert response.status_code == 200
            assert response.json()["status"] == "success"
            mock_analyze.assert_awaited_once()


class TestSearchEndpoint:
    """Тестирование эндпоинта /search/"""

    def test_search_existing_food(self):
        """Проверяет поиск существующего продукта (например, 'яблоко')."""
        response = client.get("/search/", params={"food_name": "шашлык"})
        assert response.status_code == 200

        data = response.json()
        assert data["food_name"] == "шашлык"
        assert "nutrition" in data
        nutrition = data["nutrition"]

        # Проверяем, что есть основные поля и они числовые
        assert isinstance(nutrition["calories"], float)
        assert isinstance(nutrition["proteins"], float)
        assert isinstance(nutrition["fats"], float)
        assert isinstance(nutrition["carbohydrates"], float)
        assert nutrition["calories"] > 0
        assert nutrition["proteins"] > 0
        assert nutrition["fats"] > 0
        assert nutrition["carbohydrates"] > 0

    def test_search_nonexistent_food(self):
        """Проверяет поиск несуществующего продукта."""
        response = client.get("/search/", params={"food_name": "бррр123xyz"})
        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert data["status"] == "not_found"

    def test_search_empty_query(self):
        """Пустой запрос → 422 ошибка валидации (из-за min_length=1)."""
        response = client.get("/search/", params={"food_name": ""})
        assert response.status_code == 422

    def test_search_too_long_query(self):
        """Слишком длинный запрос → 422 ошибка."""
        long_name = "a" * 101
        response = client.get("/search/", params={"food_name": long_name})
        assert response.status_code == 422


class TestLoggingMiddleware:
    """Тестирование логирования"""

    @pytest.mark.asyncio
    async def test_successful_request_logging(self):
        """Проверяет логирование успешного запроса."""
        with patch("src.api.api.logger") as mock_logger:
            response = client.get("/health")
            assert response.status_code == 200

            mock_logger.info.assert_any_call(
                "Incoming request | Method: GET | URL: /health | Client: testclient"
            )

            # Проверка исходящего лога
            outgoing_calls = [
                call for call in mock_logger.info.call_args_list
                if call[0][0].startswith("Outgoing response")
            ]
            assert len(outgoing_calls) == 1
            log_msg = outgoing_calls[0][0][0]
            assert "Status: 200" in log_msg
            assert "Method: GET" in log_msg
            assert "URL: /health" in log_msg
            assert "Duration:" in log_msg
            assert "Client: testclient" in log_msg
