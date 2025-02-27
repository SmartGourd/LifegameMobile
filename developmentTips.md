# Kotlin mobile app development tips
- **Používat singletony je to nejsnažší!**

### Singletony
- Kotlin má hezkou podporu pro Singletony pomocí Object
- pak není potřeba vytvářet instanci a lze k nim přistupovat jakoby staticky
- dobře fungují že pak není potřeba řešit žádné dependency injection ani dělat nějaký factory
- kdybychom jo chtěli tak by do nich šlo ukládat instance něčeho pro composables a tim je zachovávat i po změně konfigurace

## View a Model
### Model
- je potřeba oddělit Model od View
- Model načítá data a musí být oddolný proti změně konfigurace - například otočení obrazovky - nesmí ho vymazat
- nejlepší je použít na to singletony když to jde, ty drží svůj stav
- pokuď chceme instance máme jako vlastní třídu a buď použijeme Hilt či Koin abychom ho mohli používat jako dependency injection v Composable nebo si pro něj vytvoříme Factory

### View
- neměl by obsahovat žádnou bussiness logiku
- Composables jsou vlastně komponenty
- věci napsané nahoře v Composables se volají pokaždé když je composable mountnutá a nebo při přenačtení konfigurace
- je taky best practice dávat do view jen funkcionalitu co dané view potřebuje a ne celý model
- v Composables voláme věci pomocí launchEffect které zaručí že se provedou pouze 1


### Použití lint
- je dobré použít lint
- měl by odhalit i hard-encoded text ve view a další problémy i když se mi ho tak úplně nepovedlo nastavit
