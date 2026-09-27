# FingenNext

Современный Android-преемник Fingen.

## Текущий статус

Версия 0.3.2. Production hardening / активная разработка. Рабочая основа включает Kotlin, Compose/Material 3, Hilt, Room 3, локальный ledger, счета, операции, долги и версионируемые шаблоны кредитных карт.

Это еще не финальный Google Play release: впереди импорт старого Fingen, полный CRUD, SMS/OCR, финансовый календарь, backup/restore, paging, security hardening и release signing.

## Стек

Kotlin 2.3.21, AGP 9.4.0, Gradle 9.6, Compose BOM 2026.09.00, Material 3, Room 3.0.3, Hilt 2.60.1, Navigation 2.10.2, Coroutines/Flow, DataStore.

## Архитектура

UI → ViewModel → Repository → Room/Data Source.

Финансовая логика находится в `domain` и не зависит от Android UI.

Баланс счета не хранится как редактируемое число: база хранит начальный остаток и ledger entries, а текущий баланс вычисляется SQL-запросом.

## Финансовые правила

- деньги — Long в minor units;
- перевод между своими счетами не доход и не расход;
- кредит не является доходом;
- погашение кредита уменьшает деньги и обязательство;
- «мне должны» — актив, но не доступные деньги;
- «я должен» — обязательство;
- кредитные обязательства не должны учитываться дважды.

## Кредитные карты

Условия банков представлены пользовательскими версионируемыми шаблонами. Они не являются юридической гарантией тарифа. Изменение шаблона не должно менять исторические расчеты.

## Сборка

JDK 17, Android SDK 37, Android Studio с поддержкой AGP 9.4.

```bash
gradle test
gradle assembleDebug
```

CI настроен в `.github/workflows/android.yml`.


> Development branch: `fingen-next`.
