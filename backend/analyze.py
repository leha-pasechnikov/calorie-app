from google.genai import Client, types
from PIL import Image
import json
import re
import requests
from env import API_KEY
import logging

logger = logging.getLogger("api.analyze")

async def analyze_food_json(name):
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

    from statistics import median

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


async def analyze_food_image(image: Image.Image, api_key=API_KEY) -> dict:
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
        return {"status": "error", "message": "Ошибка сервиса распознавания фото"}

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
            json_match = re.search(r'\{.*}', text, re.DOTALL)
            if json_match:
                try:
                    data = json.loads(json_match.group())
                    return data
                except json.JSONDecodeError:
                    pass

            return {"status": "error", "message": "Ошибка формирования ответа"}

    except Exception as err:
        logger.error(f"Ошибка при анализе: {err}")
        return {"status": "error", "message": "Ошибка при анализе изображения"}


async def run():
    # path = "image/3.webp" # Опасно
    # path = "image/4.webp" # Не еда
    path = "image/2.jpg"  # Еда
    # path = 'image/1.jpg'
    photo = Image.open(path)
    result = await analyze_food_image(photo)
    return result


if __name__ == "__main__":
    import asyncio

    # res = asyncio.run(run())
    res = asyncio.run(analyze_food_json('pepsi'))
    print(res)

"""
analyze_food_image варианты ответов
- Еда
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
}
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
}
- Не еда: 
{
    "status":"not_found",
    "message":"На изображении не найдено продуктов питания"
}
- Опасное: 
{
    "status":"danger",
    "message":"Это ядовитый гриб - мухомор красный.",
    "food_items": {
        "мухомор":{
            "proteins":2.0,
            "fats":0.0,
            "carbohydrates":3.0,
            "weight":100,
            "benefit_score":0.0
        }
    }
}

- Ошибка
{
    'status': 'error', 
    'message': 'Ошибка при анализе изображения'
}
"""
