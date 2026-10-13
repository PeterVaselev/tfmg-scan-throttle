# TFMG Scan Throttle

Серверный мод-спутник для NeoForge 1.21.1, который делает редкой самую дорогую часть
`Create: The Factory Must Grow` (TFMG) — ежесекундный (каждый тик) обход всех тикающих
чанков каждого измерения.

## Зачем

`com.drmangotea.tfmg.base.events.TFMGCommonEvents.levelTick(LevelTickEvent.Post)` на каждом
тике каждого измерения:

1. берёт все видимые чанки через `ChunkMapAccessor.getVisibleChunkMap()`,
2. для каждого чанка делает `getTickingChunk()`,
3. проверяет конфиг `worldGen.infiniteDeposits` и наличие data-аттачмента `FLUID_RESERVOIR`,
4. если резервуар уже пуст — вызывает `removeEmptyDeposits` и снимает data-аттачмент.

Эта работа — уборка мусора: пустая залежь удаляется в любом случае, разница только в том,
сколько он живёт. На сервере с ~2000 тикающих чанков и **нулём** машин TFMG этот обход
занимал **12.48 % всего тик-времени** (spark-профиль). При этом полезного результата в
99.9 % тиков нет: пустые залежи появляются крайне редко.

## Как работает

Один миксин, ничего больше:

`net.tfmgscanthrottle.mixin.TFMGCommonEventsMixin`

```java
@Mixin(value = TFMGCommonEvents.class, remap = false)
...
@Inject(method = "levelTick(Lnet/neoforged/neoforge/event/tick/LevelTickEvent$Post;)V",
        at = @At("HEAD"), cancellable = true, require = 1)
private static void tfmgscanthrottle$throttleReservoirScan(LevelTickEvent.Post event, CallbackInfo ci)
```

* HEAD-инжект: если событие пришло для `ServerLevel` и тик этой размерности не «попал» в
  интервал — `ci.cancel()`, тело `levelTick` не выполняется вообще. Тело метода TFMG состоит
  только из этого обхода (проверено `javap -c`), никакого другого поведения не теряется.
* Счётчик тиков ведётся **пер-измерение** (`Map<ResourceKey<Level>, long[]>`), поэтому у
  каждого мира своя фаза. Первый тик после запуска скан проходит сразу.
* `remap = false` — цель лежит в классах мода, они не ремапятся; refmap не нужен.
* Миксин добавляется в свой mixin-конфиг `tfmgscanthrottle.mixins.json`, объявленный в
  `neoforge.mods.toml` секцией `[[mixins]]`. Конфиг TFMG (`tfmg.mixins.json`) не трогается,
  конфликтовать им нечем — у каждого мода свой mixin-конфиг.

## Зависимости и сторона

* `neoforge`, `minecraft` — required;
* `tfmg` — **required**, `ordering = AFTER` (без TFMG мод не грузится вообще: он бессмыслен);
* `displayTest = "IGNORE_ALL_VERSION"` — серверный мод: клиенты без него могут подключаться;
* миксин-конфиг помечен `"required": false` — если в будущей версии TFMG метод переименуют,
  мод **не уронит сервер**: в логе будет ERROR про неприменённый инжект, а поведение TFMG
  останется исходным (скан каждый тик).

## Конфиг

Файл: `config/tfmgscanthrottle-common.toml` (создаётся при первом запуске).

```toml
# How often (in ticks) TFMG scans ticking chunks for empty fluid reservoirs.
# 20 ticks = 1 second, so 600 = once every 30 seconds.
# Values <= 1 disable the throttle and restore TFMG's original per-tick scan.
throttleTicks = 600
```

Тип `COMMON` выбран специально: NeoForge не позволяет задать произвольное имя файла
(имя = `<modid>-<type>.toml`), а тип `SERVER` положил бы конфиг в
`<world>/serverconfig/…`, то есть пер-мир. `COMMON` даёт обычный `config/…-common.toml`
рядом с остальными модами и читается на старте.

## Сборка

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk   # системная Java 27 не подходит
cd /home/peter/p/tfmg-scan-throttle
./gradlew build
```

Первый запуск долгий (NeoForm декомпилирует Minecraft), дальше секунды.
Результат: `build/libs/tfmgscanthrottle-1.0.0.jar`.

Для компиляции миксина нужен jar TFMG — он лежит в `libs/tfmg-1.3.2a-community.jar`
и подключён `compileOnly` (в готовый jar он не встраивается; в рантайме класс берётся
из установленного мода).

## Установка

Скопировать `build/libs/tfmgscanthrottle-1.0.0.jar` в `mods/` сервера рядом с TFMG.
Клиенту мод не нужен.

## Как проверить эффект

* в логе старта: `TFMG scan throttle enabled; interval comes from …` и, когда конфиг загружен,
  `TFMG scan throttle: chunk scan interval = 600 ticks`;
* spark-профиль: доля `TFMGCommonEvents.levelTick` падает примерно в 600 раз (вместо ~12 %
  остаются доли процента, раз в 30 секунд);
* `throttleTicks = 1` возвращает исходное поведение — удобно для A/B.

## Ограничения

* Пустая залежь удаляется с задержкой до `throttleTicks` тиков (30 с по умолчанию). На
  геймплей не влияет: залежь уже пуста, обход только убирает data-аттачмент.
* Фаза счётчика не сбрасывается при перезагрузке мира — после рестарта скан пройдёт
  в первые 30 секунд.
* Если TFMG сменит имя/дескриптор метода `levelTick`, инжект не применится (см. выше про
  `required = false`). Строку дескриптора надо будет обновить.

## Что проверено

* `./gradlew build` — успешно; в jar лежат `META-INF/neoforge.mods.toml` (с `[[mixins]]` и
  required-зависимостью на `tfmg`), `tfmgscanthrottle.mixins.json` и 4 `.class`;
* дескриптор в `@Inject` сверен с реальным методом TFMG рефлексией — скрипт
  `bash tools/verify-injection-target.sh` (нужен один раз собранный проект) сравнивает строку
  из миксина с дескриптором, полученным от `com.drmangotea.tfmg.base.events.TFMGCommonEvents`
  (метод `static`);
* `javap -v` по собранному классу миксина: `@Inject` (method/at/cancellable/require),
  `@Mixin(TFMGCommonEvents, remap = false)`, обработчик `private static`.

Не проверялось: запуск модового сервера вживую (в задаче запрещено трогать живой сервер,
а локального dev-сервера с TFMG нет).
