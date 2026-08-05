<div align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="112" alt="Иконка MeteoMate">
  <h1>MeteoMate</h1>
  <p>Погодный помощник для Android с подробным прогнозом, анализом ветра и полезными уведомлениями.</p>
  <p>
    <a href="https://www.rustore.ru/catalog/app/com.example.meteomate"><strong>Скачать в RuStore</strong></a>
  </p>
</div>

## О приложении

MeteoMate помогает быстро понять не только температуру, но и то, как погода повлияет на день. Приложение показывает почасовой и семидневный прогноз, состояние воздуха, солнечные и геомагнитные показатели, а отдельный раздел подробно разбирает ветер.

## Возможности

- текущая погода, ощущаемая температура и прогноз на 48 часов;
- семидневный прогноз и рекорды выбранного города;
- скорость, направление и порывы ветра, роза ветров и сравнение погодных моделей;
- модели ECMWF, GFS, ICON, NAM, HRRR, WRF, AROME и другие;
- качество воздуха, УФ-индекс и таймер защиты от солнца;
- восход, закат, золотой час и лунный календарь;
- текущий индекс Kp и прогноз геомагнитной активности;
- погодные предупреждения, тихие часы и уведомления о сильном ветре;
- виджет с температурой, осадками и ветром;
- избранные города, геолокация и локальный кэш прогноза.

## Скриншоты

<p align="center">
  <img src="rustore_screenshots_phone_1080x1920/MeteoMate_RuStore_01.jpg" width="30%" alt="Главный экран MeteoMate">
  <img src="rustore_screenshots_phone_1080x1920/MeteoMate_RuStore_05.jpg" width="30%" alt="Данные о ветре и солнечной дуге">
  <img src="rustore_screenshots_phone_1080x1920/MeteoMate_RuStore_08.jpg" width="30%" alt="Роза ветров и оповещения">
</p>

## Установка

Готовую версию приложения можно [скачать в RuStore](https://www.rustore.ru/catalog/app/com.example.meteomate).

Для сборки из исходного кода понадобятся Android Studio, JDK 17 и Android SDK 35.

1. Клонируйте репозиторий.
2. Скопируйте `secrets.properties.example` в `secrets.properties`.
3. Укажите свой API-ключ OpenWeather в `WEATHER_API_KEY`.
4. Откройте проект в Android Studio или выполните:

```powershell
.\gradlew.bat assembleDebug
```

Debug APK появится в `app/build/outputs/apk/debug/`.

## Технологии

- Kotlin и Jetpack Compose;
- Material 3;
- Hilt;
- Retrofit и Gson;
- Room и DataStore;
- WorkManager;
- OpenWeather, Open-Meteo и NOAA SWPC.

## Безопасность

Не добавляйте в Git `secrets.properties`, ключи подписи, пароли или готовые APK. Эти файлы уже исключены через `.gitignore`.

## Версия

Текущая версия проекта — **1.6.1**. Минимальная версия Android — **7.0 (API 24)**.
