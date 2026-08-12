# Развёртывание (deployment)

Статус: черновик. Заполняется по мере реализации. Цель — единообразно развернуть Android-сборку и Supabase-проект.

## Компоненты

1. **Supabase** — Postgres (схема + RLS), Storage bucket, Auth, edge-функция `generate-report`.
2. **Android-приложение** — сборка APK/AAB, публикация (play / sideload).

## Supabase

### Локальная разработка

```bash
# инициализация проекта (создаёт supabase/config.toml)
supabase init

# запуск локального стека
supabase start

# миграции создаются через supabase migration new
supabase db push
```

### Edge-функция

```bash
supabase functions deploy generate-report
supabase secrets set AI_API_KEY=<key> AI_PROVIDER=<provider> AI_MODEL=<model>
```

Локально:

```bash
supabase functions serve generate-report --env-file ./supabase/.env.local
```

### Storage bucket

- Bucket `photo`, приватный.
- Политика: владелец может читать/писать только свои объекты (по `auth.uid()`).

## Android

### Требования

- JDK 17+
- Android SDK (API 33+)
- Android Studio

### Сборка

```bash
cd android
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (нужна подпись)
./gradlew bundleRelease          # AAB для Play
```

Подпись: release-ключ хранится локально, **в репозиторий не попадает** (см. .gitignore: `*.keystore`, `*.jks`).

### Публикация

- Вариант 1: Play Console (рассмотреть после подтверждения MVP).
- Вариант 2: прямая установка APK (для тестовой группы) — через ссылку на файл.

## Переменные окружения

| Переменная | Где | Назначение |
|---|---|---|
| `SUPABASE_URL` | .env (локально), gradle properties | URL проекта |
| `SUPABASE_ANON_KEY` | .env | публичный ключ |
| `AI_PROVIDER` | секреты edge-функции | провайдер |
| `AI_API_KEY` | секреты edge-функции | ключ AI |
| `AI_MODEL` | секреты edge-функции | модель |

## CI/CD

- MVP: ручной деплой (supabase CLI + gradle) — без CI до появления тестовой группы.
- Позже: GitHub Actions (сборка + тесты; деплой edge-функций; публикация APK).

## Дорожная карта заполнения этого документа

- [ ] Миграции БД (схема + RLS)
- [ ] Настройка Storage bucket
- [ ] Деплой edge-функции
- [ ] Сборка подписи и публикация Android
