# ConsoleDownloader

Консольная утилита для скачивания файлов по http протоколу.

При запуске принимает входные параметры:

-n количество одновременно качающих потоков (1,2,3,4....)

-l общее ограничение на скорость скачивания, для всех потоков, размерность - байт/секунда, можно использовать суффиксы k,m (k=1024, m=1024*1024)

-f путь к файлу со списком ссылок

-o имя папки, куда складывать скачанные файлы
