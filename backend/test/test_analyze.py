from unittest.mock import Mock, MagicMock, patch
from pathlib import Path
from typing import List
from PIL import Image
import pytest
import json

from src.api.analyze import analyze_food_image, analyze_food_json
from src.env import skip_api_request


class TestAnalyzeFoodImage:
    """Тестирование функции analyze_food_image"""

    @pytest.fixture(scope='session')
    def get_test_data(self) -> List[str]:
        """Подставляет тестовые данные для ответа от analyze_food_image()"""
        return [
            """{
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
            }""",
            """{
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
            }""",
            """{
                "status": "not_found",
                "message": "На изображении не найдено продуктов питания"
            }""",
            """{
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
            }""",
            """{
                "status": "error",
                "message": "Ошибка ответа в поле status"
            }""",
            """```json{
                "status": "success",
                "message": "Обработка ошибки markdown"
            }```
            """,
            "Некоторый текст содержащий valid json: {'key': 'value'}.",
            "Некоторый текст содержащий invalid json: { key: value }.",
            """{
                "status": "error",
                "message": "Ошибка незакрытой скобки"
            
            """,
            "не JSON"
        ]

    @pytest.mark.asyncio
    @pytest.mark.parametrize("size_image", [(2700, 600), (2700, 2800), (200, 5662)])
    async def test_image_max_size(self, size_image):
        """Проверка на слишком большое изображение"""
        mock_image = Mock(spec=Image.Image)
        mock_image.size = size_image
        result = {
            "status": "error",
            "message": "Изображение слишком длинное или слишком широкое"
        }
        assert result == await analyze_food_image(mock_image, api_key="test", test_answer="тестовый ответ")

    @pytest.mark.asyncio
    @pytest.mark.parametrize("size_image", [(50, 600), (20, 10), (200, 11)])
    async def test_image_min_size(self, size_image):
        """Проверка на слишком маленькое изображение"""
        mock_image = Mock(spec=Image.Image)
        mock_image.size = size_image
        result = {
            "status": "error",
            "message": "Изображение слишком короткое или слишком узкое"
        }
        assert result == await analyze_food_image(mock_image, api_key="test", test_answer="тестовый ответ")

    @pytest.mark.asyncio
    async def test_api_key(self):
        """Проверка на неверный api ключ"""
        mock_image = Mock(spec=Image.Image)
        mock_image.size = (500, 500)
        result = {
            "status": "error",
            "message": "Ошибка сервиса распознавания фото"
        }
        assert result == await analyze_food_image(mock_image, api_key="", test_answer="тестовый ответ")

    @pytest.mark.asyncio
    async def test_text_fake(self, get_test_data):
        """Проверка тестовых ответов"""
        answers = get_test_data
        current_dir = Path(__file__).parent
        photo_path = current_dir / "../src/image/2.jpg"
        photo = Image.open(photo_path.resolve())
        for counter, answer in enumerate(answers):
            mocked_response = MagicMock()
            mocked_response.status_code = 200
            mocked_response.json.return_value = "неверный ответ от api"
            with patch("requests.post", return_value=mocked_response):
                result = await analyze_food_image(photo, test_answer=str(answer))
                if counter in [0, 1, 2, 3, 4]:  # валидные значения
                    answer_obj = json.loads(answer)
                    assert answer_obj == result
                elif counter == 5:
                    assert result == {
                        "status": "success",
                        "message": "Обработка ошибки markdown"
                    }
                elif counter in [6, 7, 8, 9]:  # не валидные значения
                    assert result == {
                        "status": "error",
                        "message": "Ошибка формирования ответа"
                    }
                else:
                    raise AssertionError(f"Не все данные из get_test_data протестированы: {counter}")

    @pytest.mark.asyncio
    async def test_critical_error(self):
        """Проверка ошибки при анализе изображения"""
        error_image = Mock(spec=Image.Image)
        error_image.size = (500, 500)
        result = await analyze_food_image(error_image, test_answer={"неверный тип ответа"}) # type: ignore
        assert result == {
            "status": "error",
            "message": "Ошибка при анализе изображения"
        }

    @pytest.mark.asyncio
    @pytest.mark.skipif(skip_api_request, reason="требует стабильного vpn")
    @pytest.mark.parametrize("path", ["../src/image/2.jpg", "../src/image/1.jpg"])
    async def test_text_real_success(self, path):
        """Проверка реального корректного изображения"""
        current_dir = Path(__file__).parent
        photo_path = current_dir / path
        photo = Image.open(photo_path.resolve())
        result = await analyze_food_image(photo)
        assert result.get("status") == "success" or result == {
            "status": "error",
            "message": "Ограничение лимита"
        }

    @pytest.mark.asyncio
    @pytest.mark.skipif(skip_api_request, reason="требует стабильного vpn")
    async def test_text_real_danger(self):
        """Проверка реального опасного изображения"""
        current_dir = Path(__file__).parent
        photo_path = current_dir / "../src/image/3.webp"
        photo = Image.open(photo_path.resolve())
        result = await analyze_food_image(photo)
        print(result)
        assert result.get("status") == "danger" or result == {
            "status": "error",
            "message": "Ограничение лимита"
        }

    @pytest.mark.asyncio
    @pytest.mark.skipif(skip_api_request, reason="требует стабильного vpn")
    async def test_text_real_not_found(self):
        """Проверка реального изображения без еды"""
        current_dir = Path(__file__).parent
        photo_path = current_dir / "../src/image/4.webp"
        photo = Image.open(photo_path.resolve())
        result = await analyze_food_image(photo)
        assert result.get("status") == "not_found" or result == {
            "status": "error",
            "message": "Ограничение лимита"
        }


class TestAnalyzeFoodJSON:
    @pytest.mark.asyncio
    @pytest.mark.parametrize("name, result", [
        ("яблоко", {'calories': 47.5, 'proteins': 0.4, 'fats': 0.4, 'carbohydrates': 9.8, 'weight': 100.0}),
        ("вода", {'calories': 5.0, 'proteins': 0.0, 'fats': 0.0, 'carbohydrates': 1.25, 'weight': 100.0}),
        ("свиной шашлык", {'calories': 315.0, 'carbohydrates': 1.5, 'fats': 25.0, 'proteins': 20.0, 'weight': 100.0})])
    async def test_valid_name(self, name, result):
        res = await analyze_food_json(name=name)
        assert result == res

    @pytest.mark.asyncio
    @pytest.mark.parametrize("name, result", [("ябл1ко", False), ("perfect food", False), ("бррр", False)])
    async def test_invalid_name(self, name, result):
        res = await analyze_food_json(name=name)
        assert result == res

    @pytest.mark.asyncio
    async def test_bad_status(self):
        mocked_response = MagicMock()
        mocked_response.status_code = 404
        with patch("requests.post", return_value=mocked_response):
            res = await analyze_food_json(name="яблоко")
            assert res == False

    @pytest.mark.asyncio
    async def test_bad_data(self):
        mocked_response = MagicMock()
        mocked_response.status_code = 200
        mocked_response.json.return_value = "неверный ответ от api"
        with patch("requests.post", return_value=mocked_response):
            res = await analyze_food_json(name="яблоко")
            assert res == False
