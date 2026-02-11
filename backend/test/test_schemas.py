from pydantic import ValidationError
from typing import Any
import pytest

from src.api.schemas import *


class TestStatusEnum:
    """Тестирование StatusEnum"""

    def test_fields(self):
        """Проверка полей в StatusEnum"""
        assert StatusEnum.success == "success"
        assert StatusEnum.not_found == "not_found"
        assert StatusEnum.danger == "danger"
        assert StatusEnum.error == "error"

    @pytest.mark.parametrize("value", ["success", "not_found", "danger", "error"])
    def test_values(self, value):
        """Проверка значений в StatusEnum"""
        assert value in StatusEnum.__members__.values()


class TestFoodItem:
    """Тестирование FoodItem"""
    DATA = {
        "proteins": 12.0,
        "fats": 5.0,
        "carbohydrates": 20.0,
        "water": 10.0,
        "weight": 100,
        "benefit_score": 3.0
    }

    def get_base_data(self, **kwargs: Any) -> dict[str, Any]:
        """Тестовые данные для FoodItem"""
        data = self.DATA.copy()
        data.update(kwargs)
        return data

    def test_fields(self):
        """Проверка полей в FoodItem"""
        schema_fields = FoodItem.model_json_schema()["properties"]
        expected_fields = self.DATA.keys()
        for field in expected_fields:
            assert field in schema_fields, f"Поле {field} отсутствует в схеме"
        assert len(expected_fields) == len(schema_fields), f"Ожидалось {len(expected_fields)} полей"

    @pytest.mark.parametrize("proteins", [1.0, 45.0, 17.0, 0.0])
    def test_valid_proteins(self, proteins):
        """Проверка валидных значений для белков"""
        assert FoodItem(**self.get_base_data(proteins=proteins)).proteins == proteins

    @pytest.mark.parametrize("proteins", [-1.0, -10 * 20, None, [], "abc", {}])
    def test_invalid_proteins(self, proteins):
        """Проверка не валидных значений для белков"""
        with pytest.raises(ValidationError):
            FoodItem(**self.get_base_data(proteins=proteins))

    @pytest.mark.parametrize("fats", [1.0, 45.0, 17.0, 0.0])
    def test_valid_fats(self, fats):
        """Проверка валидных значений для жиров"""
        assert FoodItem(**self.get_base_data(fats=fats)).fats == fats

    @pytest.mark.parametrize("fats", [-1.0, -10 * 20, None, [], "abc", {}])
    def test_invalid_fats(self, fats):
        """Проверка не валидных значений для жиров"""
        with pytest.raises(ValidationError):
            FoodItem(**self.get_base_data(fats=fats))

    @pytest.mark.parametrize("carbohydrates", [1.0, 45.0, 17.0, 0.0])
    def test_valid_carbohydrates(self, carbohydrates):
        """Проверка валидных значений для углеводов"""
        assert FoodItem(**self.get_base_data(carbohydrates=carbohydrates)).carbohydrates == carbohydrates

    @pytest.mark.parametrize("carbohydrates", [-1.0, -10 * 20, None, [], "abc", {}])
    def test_invalid_carbohydrates(self, carbohydrates):
        """Проверка не валидных значений для углеводов"""
        with pytest.raises(ValidationError):
            FoodItem(**self.get_base_data(carbohydrates=carbohydrates))

    @pytest.mark.parametrize("water", [1.0, 45.0, 17.0, 0.0])
    def test_valid_water(self, water):
        """Проверка валидных значений для воды"""
        assert FoodItem(**self.get_base_data(water=water)).water == water

    @pytest.mark.parametrize("water", [-1.0, -10 * 20, None, [], "abc", {}])
    def test_invalid_water(self, water):
        """Проверка не валидных значений для воды"""
        with pytest.raises(ValidationError):
            FoodItem(**self.get_base_data(water=water))

    @pytest.mark.parametrize("weight", [1, 45, 17, 1000])
    def test_valid_weight(self, weight):
        """Проверка валидных значений для веса"""
        assert FoodItem(**self.get_base_data(weight=weight)).weight == weight

    @pytest.mark.parametrize("weight", [-1, 0, -10 * 20, None, [], "abc", {}, 5.1])
    def test_invalid_weight(self, weight):
        """Проверка не валидных значений для веса"""
        with pytest.raises(ValidationError):
            FoodItem(**self.get_base_data(weight=weight))

    @pytest.mark.parametrize("score", [0.0, 2.5, 5.0])
    def test_valid_score(self, score):
        """Проверка валидных значений для оценки"""
        assert FoodItem(**self.get_base_data(benefit_score=score)).benefit_score == score

    @pytest.mark.parametrize("score", [-1.0, 5.1, -10 * 20, None, [], "abc", {}])
    def test_invalid_score(self, score):
        """Проверка не валидных значений для оценки"""
        with pytest.raises(ValidationError):
            FoodItem(**self.get_base_data(benefit_score=score))


class TestFoodSuccessResponse:
    """Тестирование FoodSuccessResponse"""

    def test_valid_success_response(self):
        """Проверка корректного создания FoodSuccessResponse с объектами FoodItem"""
        chicken = FoodItem(
            proteins=45.0, fats=5.0, carbohydrates=0.0, weight=180, water=126.0, benefit_score=4.5
        )
        rice = FoodItem(
            proteins=4.0, fats=1.0, carbohydrates=38.0, weight=150, water=105.0, benefit_score=3.8
        )
        food_items = {"куриная грудка гриль": chicken, "отварной рис": rice}
        response = FoodSuccessResponse(food_items=food_items)
        assert response.status == StatusEnum.success
        assert len(response.food_items) == 2
        assert response.food_items["куриная грудка гриль"].proteins == 45.0

    def test_empty_food_items(self):
        """Проверка ответа с пустым словарём food_items"""
        response = FoodSuccessResponse(food_items={})
        assert response.status == StatusEnum.success
        assert response.food_items == {}

    def test_invalid_food_items_type(self):
        """Проверка ошибки при некорректном типе food_items"""
        with pytest.raises(ValidationError):
            FoodSuccessResponse(food_items="not a dict")  # type: ignore

    def test_invalid_food_item_value(self):
        """Проверка ошибки при попытке передать невалидный объект (не FoodItem)"""
        with pytest.raises(ValidationError):
            FoodSuccessResponse(food_items={"bad": "not a FoodItem"})  # type: ignore


class TestNotFoundResponse:
    """Тестирование NotFoundResponse"""

    def test_valid_not_found_response(self):
        """Проверка корректного создания NotFoundResponse"""
        message = "На изображении не найдено продуктов питания"
        response = NotFoundResponse(message=message)
        assert response.status == StatusEnum.not_found
        assert response.message == message

    def test_missing_message(self):
        """Проверка обязательности поля message"""
        with pytest.raises(ValidationError):
            NotFoundResponse()  # type: ignore


class TestErrorResponse:
    """Тестирование ErrorResponse"""

    def test_valid_error_response(self):
        """Проверка корректного создания ErrorResponse"""
        message = "Ошибка при анализе изображения"
        response = ErrorResponse(message=message)
        assert response.status == StatusEnum.error
        assert response.message == message

    def test_missing_message(self):
        """Проверка обязательности поля message"""
        with pytest.raises(ValidationError):
            ErrorResponse()  # type: ignore


class TestDangerResponse:
    """Тестирование DangerResponse"""

    def test_valid_danger_response(self):
        """Проверка корректного создания DangerResponse с объектом FoodItem"""
        mushroom = FoodItem(
            proteins=2.0, fats=0.0, carbohydrates=3.0, weight=100, water=60.0, benefit_score=0.0
        )
        food_items = {"мухомор": mushroom}
        message = "Это ядовитый гриб - мухомор красный."
        response = DangerResponse(message=message, food_items=food_items)
        assert response.status == StatusEnum.danger
        assert response.message == message
        assert "мухомор" in response.food_items
        assert response.food_items["мухомор"].benefit_score == 0.0

    def test_missing_message(self):
        """Проверка обязательности поля message"""
        mushroom = FoodItem(proteins=2.0, fats=0.0, carbohydrates=3.0, weight=100, water=60.0, benefit_score=0.0)
        with pytest.raises(ValidationError):
            DangerResponse(food_items={"мухомор": mushroom})  # type: ignore

    def test_missing_food_items(self):
        """Проверка обязательности поля food_items"""
        with pytest.raises(ValidationError):
            DangerResponse(message="Опасный продукт")  # type: ignore

    def test_invalid_food_items_content(self):
        """Проверка ошибки при передаче невалидного FoodItem"""
        with pytest.raises(ValidationError):
            DangerResponse(
                message="Опасный продукт",
                food_items={
                    "bad": FoodItem(proteins=-5.0, fats=0, carbohydrates=0, weight=100, water=0, benefit_score=0)}
            )


class TestFoodResponseUnion:
    """Тестирование совместимости моделей с FoodResponse (Union)"""

    def test_success_instance(self):
        item = FoodItem(proteins=4.0, fats=1.0, carbohydrates=38.0, weight=150, water=105.0, benefit_score=3.8)
        obj = FoodSuccessResponse(food_items={"рис": item})
        assert isinstance(obj, FoodSuccessResponse)

    def test_not_found_instance(self):
        obj = NotFoundResponse(message="Не найдено")
        assert isinstance(obj, NotFoundResponse)

    def test_error_instance(self):
        obj = ErrorResponse(message="Ошибка")
        assert isinstance(obj, ErrorResponse)

    def test_danger_instance(self):
        item = FoodItem(proteins=2.0, fats=0.0, carbohydrates=3.0, weight=100, water=60.0, benefit_score=0.0)
        obj = DangerResponse(message="Опасность!", food_items={"гриб": item})
        assert isinstance(obj, DangerResponse)


class TestNutritionSearch:
    """Тестирование NutritionSearch"""
    DATA = {
        "calories": 250.0,
        "proteins": 12.0,
        "fats": 5.0,
        "carbohydrates": 20.0,
        "weight": 100.0
    }

    def get_base_data(self, **kwargs: Any) -> dict[str, Any]:
        """Тестовые данные для NutritionSearch"""
        data = self.DATA.copy()
        data.update(kwargs)
        return data

    def test_fields(self):
        """Проверка полей в NutritionSearch"""
        schema_fields = NutritionSearch.model_json_schema()["properties"]
        expected_fields = self.DATA.keys()
        for field in expected_fields:
            assert field in schema_fields, f"Поле {field} отсутствует в схеме"
        assert len(expected_fields) == len(schema_fields), f"Ожидалось {len(expected_fields)} полей"

    @pytest.mark.parametrize("calories", [0.0, 1.0, 45.0, 17.0, 1000.0])
    def test_valid_calories(self, calories):
        """Проверка валидных значений для калорий"""
        assert NutritionSearch(**self.get_base_data(calories=calories)).calories == calories

    @pytest.mark.parametrize("calories", [-1.0, -10 * 20, None, [], "abc", {}])
    def test_invalid_calories(self, calories):
        """Проверка не валидных значений для калорий"""
        with pytest.raises(ValidationError):
            NutritionSearch(**self.get_base_data(calories=calories))

    @pytest.mark.parametrize("proteins", [0.0, 1.0, 45.0, 17.0])
    def test_valid_proteins(self, proteins):
        """Проверка валидных значений для белков"""
        assert NutritionSearch(**self.get_base_data(proteins=proteins)).proteins == proteins

    @pytest.mark.parametrize("proteins", [-1.0, -10 * 20, None, [], "abc", {}])
    def test_invalid_proteins(self, proteins):
        """Проверка не валидных значений для белков"""
        with pytest.raises(ValidationError):
            NutritionSearch(**self.get_base_data(proteins=proteins))

    @pytest.mark.parametrize("fats", [0.0, 1.0, 45.0, 17.0])
    def test_valid_fats(self, fats):
        """Проверка валидных значений для жиров"""
        assert NutritionSearch(**self.get_base_data(fats=fats)).fats == fats

    @pytest.mark.parametrize("fats", [-1.0, -10 * 20, None, [], "abc", {}])
    def test_invalid_fats(self, fats):
        """Проверка не валидных значений для жиров"""
        with pytest.raises(ValidationError):
            NutritionSearch(**self.get_base_data(fats=fats))

    @pytest.mark.parametrize("carbohydrates", [0.0, 1.0, 45.0, 17.0])
    def test_valid_carbohydrates(self, carbohydrates):
        """Проверка валидных значений для углеводов"""
        assert NutritionSearch(**self.get_base_data(carbohydrates=carbohydrates)).carbohydrates == carbohydrates

    @pytest.mark.parametrize("carbohydrates", [-1.0, -10 * 20, None, [], "abc", {}])
    def test_invalid_carbohydrates(self, carbohydrates):
        """Проверка не валидных значений для углеводов"""
        with pytest.raises(ValidationError):
            NutritionSearch(**self.get_base_data(carbohydrates=carbohydrates))

    @pytest.mark.parametrize("weight", [100.0])
    def test_valid_weight(self, weight):
        """Проверка валидных значений для веса (по умолчанию 100.0)"""
        assert NutritionSearch(**self.get_base_data(weight=weight)).weight == weight

    @pytest.mark.parametrize("weight", [-1.1, -10 * 20, None, [], "abc", {}, 0.0, 50.3, 200.2])
    def test_invalid_or_nonstandard_weight(self, weight):
        """Проверка не валидных значений для веса"""
        if isinstance(weight, (int, float)) and weight > 0:
            obj = NutritionSearch(**self.get_base_data(weight=weight))
            assert obj.weight == float(weight)
        else:
            with pytest.raises(ValidationError):
                NutritionSearch(**self.get_base_data(weight=weight))


class TestFoodResponseSearch:
    """Тестирование FoodResponseSearch"""

    def test_valid_success_response(self):
        """Проверка корректного создания FoodResponseSearch с объектом NutritionSearch"""
        nutrition = NutritionSearch(
            calories=165.0,
            proteins=31.0,
            fats=3.6,
            carbohydrates=0.0,
            weight=100.0
        )
        response = FoodResponseSearch(food_name="Куриная грудка", nutrition=nutrition)
        assert response.status == "success"
        assert response.food_name == "Куриная грудка"
        assert response.nutrition.proteins == 31.0
        assert response.nutrition.weight == 100.0

    def test_missing_food_name(self):
        """Проверка обязательности поля food_name"""
        nutrition = NutritionSearch(calories=100.0, proteins=10.0, fats=5.0, carbohydrates=20.0)
        with pytest.raises(ValidationError):
            FoodResponseSearch(nutrition=nutrition)  # type: ignore

    def test_missing_nutrition(self):
        """Проверка обязательности поля nutrition"""
        with pytest.raises(ValidationError):
            FoodResponseSearch(food_name="Яблоко")  # type: ignore

    def test_invalid_nutrition_type(self):
        """Проверка ошибки при передаче невалидного объекта вместо NutritionSearch"""
        with pytest.raises(ValidationError):
            FoodResponseSearch(food_name="Банан", nutrition="not a NutritionSearch")  # type: ignore

    def test_status_is_fixed(self):
        """Проверка, что status всегда 'success' и не может быть изменён"""
        nutrition = NutritionSearch(calories=52.0, proteins=0.3, fats=0.2, carbohydrates=14.0)
        response = FoodResponseSearch(food_name="Яблоко", nutrition=nutrition)
        assert response.status == "success"

        response2 = FoodResponseSearch(food_name="Апельсин", nutrition=nutrition, status="success")  # допустимо
        assert response2.status == "success"

        with pytest.raises(ValidationError):
            FoodResponseSearch(food_name="Груша", nutrition=nutrition, status="error")  # type: ignore


class TestNotFoundResponseSearch:
    """Тестирование NotFoundResponseSearch"""

    def test_valid_not_found_response(self):
        """Проверка корректного создания NotFoundResponseSearch"""
        response = NotFoundResponseSearch()
        assert response.status == "not_found"
        assert response.message == "Продукт не найден"

    def test_custom_message(self):
        """Проверка возможности указать кастомное сообщение"""
        response = NotFoundResponseSearch(message="Такой продукт отсутствует в базе")
        assert response.status == "not_found"
        assert response.message == "Такой продукт отсутствует в базе"

    def test_status_is_fixed(self):
        """Проверка, что status всегда 'not_found'"""
        response = NotFoundResponseSearch()
        assert response.status == "not_found"

        with pytest.raises(ValidationError):
            NotFoundResponseSearch(status="error")  # type: ignore


class TestErrorFoundResponseSearch:
    """Тестирование ErrorFoundResponseSearch"""

    def test_valid_error_response(self):
        """Проверка корректного создания ErrorFoundResponseSearch"""
        response = ErrorFoundResponseSearch()
        assert response.status == "error"
        assert response.message == "Ошибка при поиске"

    def test_custom_message(self):
        """Проверка возможности указать кастомное сообщение об ошибке"""
        response = ErrorFoundResponseSearch(message="Сервис временно недоступен")
        assert response.status == "error"
        assert response.message == "Сервис временно недоступен"

    def test_status_is_fixed(self):
        """Проверка, что status всегда 'error'"""
        response = ErrorFoundResponseSearch()
        assert response.status == "error"

        with pytest.raises(ValidationError):
            ErrorFoundResponseSearch(status="success")  # type: ignore


class TestFoodAPIResponseSearchUnion:
    """Тестирование совместимости моделей с FoodAPIResponseSearch (Union)"""

    def test_success_instance(self):
        nutrition = NutritionSearch(calories=89.0, proteins=1.1, fats=0.3, carbohydrates=23.0)
        obj = FoodResponseSearch(food_name="Банан", nutrition=nutrition)
        assert isinstance(obj, FoodResponseSearch)

    def test_not_found_instance(self):
        obj = NotFoundResponseSearch()
        assert isinstance(obj, NotFoundResponseSearch)

    def test_error_instance(self):
        obj = ErrorFoundResponseSearch()
        assert isinstance(obj, ErrorFoundResponseSearch)
