# repository

Разработка локального
поискового движка по сайту.

Принципы работы поискового движка:
1. В конфигурационном файле перед запуском приложения задаются адреса сайтов, по которым движок должен осуществлять поиск.
2. Поисковый движок должен самостоятельно обходить все страницы заданных сайтов и индексировать их (создавать так называемый индекс) так, чтобы затем находить наиболее релевантные страницы по любому поисковому запросу.
3. Пользователь присылает запрос через API движка. Запрос — это набор слов, по которым нужно найти страницы сайта.
4. Запрос определённым образом трансформируется в список слов, переведённых в базовую форму. Например, для существительных — именительный падеж, единственное число.
5. В индексе ищутся страницы, на которых встречаются все эти слова.
6. Результаты поиска ранжируются, сортируются и отдаются пользователю.

В проект входит веб-страница, которая позволяет управлять процессами, реализованными в движке.

Страница содержит три вкладки:

1. DASHBOARD 
- На ней выводится статистика по сайтам: списрк сайтовб а по каждому сайту - информация о количестве проиндексированных страниц и лемм.
2. MANAGEMENT 
- Работает запуск и остановка индексации
3. SEARCH 
- Работает поиск как по всем сайтам, так и по каждому отдельно. В поисковой выдаче выводится список найденных страницб соответствующих запросую.
