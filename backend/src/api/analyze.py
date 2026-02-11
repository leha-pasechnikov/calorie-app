from google.genai import Client, types
from typing import Union, Literal
from statistics import median
from PIL import Image
import requests
import logging
import json

from src.env import API_KEY, is_production

logger = logging.getLogger("api.analyze")


async def analyze_food_json(name: str) -> Union[Literal[False], dict]:
    """Проверяет данные с помощью https://health-diet.ru"""
    params = {
        'query': f'{name}',
        'nutrientDataSourceFilter[]': 'other'
    }
    url = 'https://health-diet.ru/api3/Food/FoodSearch'
    response = requests.post(
        url,
        data=params,
        timeout=10
    )

    if response.status_code == 200:
        data = response.json()
        try:
            foods = data.get('result', {}).get('foods', [])
            calories_list = []
            proteins_list = []
            fats_list = []
            carbs_list = []

            for food in foods:
                food_name = food.get("name", "")
                if set(name.lower().split()) == set(food_name.lower().split()):
                    food_cal = food.get("info", "").split(', ')
                    nutrients = {"calories": 0.0, "proteins": 0.0, "fats": 0.0, "carbohydrates": 0.0}
                    for f_c in food_cal:
                        parts = f_c.split()
                        if not parts:
                            continue
                        if f_c.endswith('ккал'):
                            nutrients["calories"] = float(parts[0])
                        elif parts[0] == 'Б':
                            nutrients["proteins"] = float(parts[1])
                        elif parts[0] == 'Ж':
                            nutrients["fats"] = float(parts[1])
                        elif parts[0] == 'У':
                            nutrients["carbohydrates"] = float(parts[1])

                    calories_list.append(nutrients["calories"])
                    proteins_list.append(nutrients["proteins"])
                    fats_list.append(nutrients["fats"])
                    carbs_list.append(nutrients["carbohydrates"])

            if not calories_list:
                return False

            return {
                "calories": round(median(calories_list), 2),
                "proteins": round(median(proteins_list), 2),
                "fats": round(median(fats_list), 2),
                "carbohydrates": round(median(carbs_list), 2),
                "weight": 100.0
            }

        except Exception as err:
            logger.error(f"Ошибка поиска: {err}")
            return False
    else:
        return False


async def analyze_food_image(image: Image.Image, api_key: str = API_KEY, test_answer: str = "") -> dict:
    """Анализирует изображение еды через Gemini API"""

    width, height = image.size

    if width > 2500 or height > 2500:
        return {
            "status": "error",
            "message": "Изображение слишком длинное или слишком широкое"
        }
    elif width < 100 or height < 100:
        return {
            "status": "error",
            "message": "Изображение слишком короткое или слишком узкое"
        }

    try:
        client = Client(api_key=api_key)
    except Exception as err:
        logger.error(f"Ошибка инициализации клиента: {err}")
        return {
            "status": "error",
            "message": "Ошибка сервиса распознавания фото"
        }

    prompt = """Ты — эксперт по диетологии. Проанализируй фото и верни ТОЛЬКО JSON. Сравнивай с данными из USDA, Роспотребнадзор, Open Food Facts.

            СТРУКТУРА JSON:
            1. Еда найдена: {"status": "success", "food_items": {"название": {"proteins": float, "fats": float, "carbohydrates": float, "water": float, "weight": int, "benefit_score": float}}}
            2. Не еда: {"status": "not_found", "message": "Причина"}
            3. Опасно (яд/испорчено): {"status": "danger", "message": "Почему опасно", "food_items": {...}}

            ПРАВИЛА:
            - Ответ ТОЛЬКО в формате JSON без markdown и комментариев.
            - Язык: РУССКИЙ.
            - БЖУ: только ДРОБНЫЕ числа (граммы например: 12.5 или 5.0).
            - Вес: только ЦЕЛЫЕ числа (граммы) на основе визуальной оценки порции.
            - Benefit_score: от 1.0 до 5.0 Если status=danger, score всегда 0.0.
            - Составные блюда (суп, салат) — одним объектом.
            - Все данные считать для уже готового блюда.
            - Вес оценивай визуально по размеру порции на фото."""

    config = types.GenerateContentConfig(
        temperature=0.1,
        top_p=0.95,
        top_k=1,
        response_mime_type="application/json",
        # tools=[
        #     types.Tool(google_search=types.GoogleSearch())
        # ]

    )

    try:
        if test_answer and not is_production:
            text = test_answer
        else:
            response = client.models.generate_content(
                model="gemini-3-flash-preview",
                contents=[prompt, image],
                config=config
            )
            text = response.text
        if text.startswith('```json'):
            text = text[7:]
        if text.endswith('```'):
            text = text[:-3]

        try:
            data = json.loads(text)
            return data

        except json.JSONDecodeError:
            logger.error(f"Ошибка обработки JSON")
            return {
                "status": "error",
                "message": "Ошибка формирования ответа"
            }

    except Exception as err:
        logger.error(f"Ошибка при анализе: {err}")
        return {
            "status": "error",
            "message": "Ошибка при анализе изображения"
        }
