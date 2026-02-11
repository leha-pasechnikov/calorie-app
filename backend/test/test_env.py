def test_import():
    """Проверка наличия переменных в env"""
    import src.env as env

    assert hasattr(env, 'API_KEY')
    assert hasattr(env, 'is_production')

    assert env.API_KEY is not None
    assert env.is_production is not None