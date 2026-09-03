import pytest

from config import Settings


BASE_ENV = {
    "API_KEY": "upstream-secret",
    "CLIENT_TOKEN": "device-secret",
}


def test_requires_api_key():
    with pytest.raises(ValueError, match="API_KEY is required"):
        Settings.from_mapping({"CLIENT_TOKEN": "device-secret"})


def test_requires_client_token():
    with pytest.raises(ValueError, match="CLIENT_TOKEN is required"):
        Settings.from_mapping({"API_KEY": "upstream-secret"})


@pytest.mark.parametrize(
    "name,value",
    [
        ("PORT", "0"),
        ("UPSTREAM_TIMEOUT_SECONDS", "0"),
        ("MAX_REQUEST_BYTES", "-1"),
        ("MAX_RESPONSE_BYTES", "abc"),
        ("RATE_LIMIT_PER_MINUTE", "0"),
    ],
)
def test_rejects_invalid_positive_integer(name, value):
    env = dict(BASE_ENV)
    env[name] = value
    with pytest.raises(ValueError, match=name):
        Settings.from_mapping(env)


def test_loads_defaults():
    settings = Settings.from_mapping(BASE_ENV)
    assert settings.port == 8080
    assert settings.upstream_timeout_seconds == 60
    assert settings.max_request_bytes == 8192
    assert settings.max_response_bytes == 32768
    assert settings.rate_limit_per_minute == 10
