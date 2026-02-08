from pydantic import BaseModel, Field
from typing import Dict, Union, Literal
from enum import Enum


class StatusEnum(str, Enum):
    success = "success"
    not_found = "not_found"
    danger = "danger"
    error = "error"


class FoodItem(BaseModel):
    proteins: float = Field(ge=0)
    fats: float = Field(ge=0)
    carbohydrates: float = Field(ge=0)
    water: float = Field(ge=0)
    weight: int = Field(gt=0)
    benefit_score: float = Field(ge=0, le=5)


class FoodSuccessResponse(BaseModel):
    status: StatusEnum = StatusEnum.success
    food_items: Dict[str, FoodItem]


class NotFoundResponse(BaseModel):
    status: StatusEnum = StatusEnum.not_found
    message: str


class ErrorResponse(BaseModel):
    status: StatusEnum = StatusEnum.error
    message: str


class DangerResponse(BaseModel):
    status: StatusEnum = StatusEnum.danger
    message: str
    food_items: Dict[str, FoodItem]


# Union тип для всех возможных ответов /analyze/
FoodResponse = Union[FoodSuccessResponse, NotFoundResponse, ErrorResponse, DangerResponse]


class NutritionSearch(BaseModel):
    calories: float = Field(ge=0.0, description="Калории (ккал) на 100 г")
    proteins: float = Field(ge=0.0, description="Белки (г) на 100 г")
    fats: float = Field(ge=0.0, description="Жиры (г) на 100 г")
    carbohydrates: float = Field(ge=0.0, description="Углеводы (г) на 100 г")
    weight: float = Field(default=100.0, description="Вес в граммах (всегда 100 г)")


class FoodResponseSearch(BaseModel):
    status: Literal["success"] = "success"
    food_name: str
    nutrition: NutritionSearch


class NotFoundResponseSearch(BaseModel):
    status: Literal["not_found"] = "not_found"
    message: str = "Продукт не найден"


class ErrorFoundResponseSearch(BaseModel):
    status: Literal["error"] = "error"
    message: str = "Ошибка при поиске"


# Union тип для всех возможных ответов /search/
FoodAPIResponseSearch = Union[FoodResponseSearch, NotFoundResponseSearch, ErrorFoundResponseSearch]
