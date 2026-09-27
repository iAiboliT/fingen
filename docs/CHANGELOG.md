# Changelog

## 0.3.0 — 2026-09-27
- QR-чек: сканирование фискального QR и распределение позиций по категориям.
- Отдельный слой ReceiptProvider для ФНС/сторонних провайдеров.
- Автоимпорт банковских уведомлений через NotificationListenerService.
- Экран «Операции на проверке» с подтверждением перед записью в Financial Engine.
- Распознавание входящих денег: зарплата, возврат долга, оплата услуг, продажа, возврат, перевод, подарок, другое.
- При совпадении суммы с активным долгом «Мне должны» предлагается возврат долга; подтверждение уменьшает остаток долга атомарно с записью операции.
- Для расходов из уведомлений пользователь выбирает категорию.
- Gmail: официальный OAuth + Gmail API, поиск электронных чеков и импорт их как кандидатов.
- Уникальный sourceKey предотвращает повторный импорт одного банковского уведомления или Gmail-письма.
- Room migration 4→5 для таблицы import_candidates.
- Тесты парсинга банковских уведомлений и Gmail-чеков.

## 0.1.0 — 2026-09-27
- FingenNext production foundation.
- Kotlin/Compose/Material 3/Hilt/Room 3 stack.
- Ledger-based account balances.
- Atomic transaction + debt persistence.
- Debt UX separated by meaning.
- Versioned Russian credit-card templates.
- Deterministic money calculations using Long minor units.
- Migration path for database versions 1→2→3.
- Dark/light dynamic theme.
- GitHub Actions Android CI.
- Financial unit tests.

> Development branch: fingen-next.
