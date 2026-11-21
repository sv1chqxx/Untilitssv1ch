# Untilits (Fabric 1.21.11)

Клиентский мод. **P** — меню Liquid Glass (HUD, эффекты, цвета).

- Секретное меню: еле заметный круг в правом нижнем углу меню → ввод кода (Enter).
- Код задаётся в чате: `.password set 1234` (1–4 цифры). Команда в чат/на сервер не уходит.
- В секретном меню: PlayerESP, NameTags, System HUD (панель перетаскивается мышью).
- Конфиг: `config/untilits.json`.

## Сборка
Нужны JDK 21 и интернет.

    gradle build          # Gradle 9.2+; jar: build/libs/untilits-1.0.0.jar

Без установки: залить папку в GitHub-репозиторий — workflow `.github/workflows/build.yml` соберёт jar (Actions → artifact).
