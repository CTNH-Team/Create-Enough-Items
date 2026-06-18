# Create-Enough-Items KNOWLEDGE BASE

## OVERVIEW
Create-Enough-Items (`cei`) is the CTNH EMI experience module: sidebar collapsible groups, recipe-page filters, associated search, drag-to-search, GTCEu voltage filtering, and static EMI rule resources.

## WHERE TO LOOK
- Mod entry: `src/main/java/com/moguang/cei/CreateEnoughItems.java`. Forge mod initialization and `CEIRegistrate`.
- Proxies: `src/main/java/com/moguang/cei/client/ClientProxy.java`, `common/CommonProxy.java`. Common proxy registers CEI registrate, datagen lang processor, and no-op GTCEu registry listeners.
- Registrate: `src/main/java/com/moguang/cei/registry/CEIRegistrate.java`. Thin `CNRegistrate` wrapper using mod id `cei`.
- Datagen hook: `src/main/java/com/moguang/cei/data/CEIDatagen.java`. Currently only adds the lang processor; no generated resource tree was present in this snapshot.
- EMI mixins: `src/main/java/com/moguang/cei/mixin/emi/`. Recipe screen buttons, sidebar behavior, search refresh hooks, tag expansion, and recipe manager replacement.
- Accessors: `src/main/java/com/moguang/cei/mixin/accessor/`, `mixin/emi/accessor/`. Keep accessor names aligned with mixin targets.
- EMI features: `src/main/java/com/moguang/cei/utils/emi/`. Collapsible groups, duplicate/featured recipe filtering, associated search, fast recipe indexing, drag search fill, and voltage filtering.
- Static rule JSON: `src/main/resources/assets/cei/emi/emi_collapsible_groups.json`, `emi_featured_recipes.json`.
- Lang/resources: `src/main/resources/assets/cei/lang/`, `META-INF/mods.toml`, `cei.mixins.json`.

## REGISTRATION ENTRYPOINTS
- `CreateEnoughItems.REGISTRATE` is created from `CEIRegistrate.create()` and registered in `CommonProxy.init()`.
- `CommonProxy.init()` also calls `CEIDatagen.init()` and registers no-op listeners for `MachineDefinition`, `GTRecipeType`, and `RecipeConditionType`; fill these only when CEI really registers GTCEu content.
- No `*GTAddon.java` exists in this module; GTCEu integration is currently through EMI/GTCEu mixins and recipe inspection helpers.
- `CEICollapsibleGroups` reads sidebar grouping rules and persists local expanded/collapsed state under `config/cei/collapsible_emi_groups.json`.
- `CEIFeaturedRecipes`, `CEIDuplicateRecipes`, `CEIAssociatedSearch`, and `CEIVoltageRecipeFilter` back the recipe-page buttons described in the README.

## CONVENTIONS
- Namespace is `com.moguang.cei`; class prefixes use `CEI`.
- This module depends on `:modules:CTNH-Lib` via `dependencies.gradle`; it does not depend on CTNH-Core.
- User toggle state is runtime config under `config/cei/`; built-in defaults are static JSON under `src/main/resources/assets/cei/emi/`.
- Mixin targets include EMI and GTCEu EMI classes; inspect target method/field names before changing injection points.
- Rule JSON accepts item IDs, tags, regex forms, negation, grouped OR/AND syntax, and recipe/category/input/output/catalyst selectors for featured filters.

## COMMANDS
```bash
./gradlew :modules:Create-Enough-Items:build
./gradlew :modules:Create-Enough-Items:runData
./gradlew :modules:Create-Enough-Items:spotlessCheck
```

## ANTI-PATTERNS
- Do not move EMI UI behavior into CTNH-Core; CEI owns EMI sidebar/search/recipe-page customization.
- Do not edit runtime `config/cei/*.json` to change defaults; edit the static rule files in `src/main/resources/assets/cei/emi/`.
- Do not treat voltage filtering as generic EMI filtering; it only handles GTCEu `GTEmiRecipe` paths.
- Do not change mixin accessor signatures without checking `cei.mixins.json` and the upstream EMI/GTCEu target members.
