# FingenNext — архитектура

## Слои

`UI (Compose) → ViewModel → UseCase/Domain → Repository → Data Source (Room)`

Финансовые правила находятся в `domain` и не зависят от Android API.

## Основные модули текущего foundation

- `domain/` — Money, операции, долги, баланс, кредитные карты.
- `data/` — Room entities/DAO/repository/migrations.
- `ui/` — Compose navigation, screens и ViewModels.
- `di/` — Hilt wiring.

## Финансовая модель

Баланс счёта вычисляется из opening balance и ledger entries. Ручного поля «текущий баланс» нет.

Кредитная карта рассматривается как обязательство; кредитный лимит не является доходом.
