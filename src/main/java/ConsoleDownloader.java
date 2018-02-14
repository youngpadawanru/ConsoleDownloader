

import com.google.common.util.concurrent.RateLimiter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class ConsoleDownloader {

    private static long resultSize = 0;

    static synchronized void sumSize(long size) {
        resultSize += size;
    }

    public static void main(String[] args) {

        String path = null;
        String urlListPath = null;
        ArrayList<String> urlList;
        double speed = 1048576;
        int numOfTrds = 1; //По умолчанию утилита качает одним потоком
        boolean flag = false; //Флаг вкл./выкл. ограничение скорости скачивания

        for (int i = 0; i < args.length; i += 2) {
            switch (args[i]) {
                case "-n":
                    numOfTrds = Integer.parseInt(args[i + 1]);
                    break;
                case "-l":
                    if (args[i + 1].endsWith("k")) {
                        String str = args[i + 1].replaceAll("k", "");
                        speed = (Double.parseDouble(str) * 1024);
                        flag = true;
                    } else if (args[i + 1].endsWith("m")) {
                        String str = args[i + 1].replaceAll("m", " ");
                        speed = (Double.parseDouble(str) * 1048576);
                        flag = true;
                    } else {
                        speed = Double.parseDouble(args[i + 1]);
                        flag = true;
                    }
                    break;
                case "-o":
                    path = args[i + 1];
                    break;
                case "-f":
                    urlListPath = args[i + 1];
                    break;
            }
        }
        if (urlListPath == null) {
            System.out.println("Не указан путь к файлу со списком ссылок.");
            System.exit(0);
        }
        if (path == null) {
            System.out.println("Не указана директория для сохренения файлов.");
            System.exit(0);
        }

        urlList = getUrlFromText(urlListPath);//Получение списка ссылок и имен из файла

        ExecutorService threadPool = Executors.newFixedThreadPool(numOfTrds);
        //Инициализация пула потоков с передачей в него ограничения по количеству потоков

        long startTime = System.currentTimeMillis();
        System.out.println("Загрузка файлов начата.");

        for (int i = 0; i < urlList.size(); i += 2) {
            threadPool.submit(new Download(urlList.get(i), urlList.get(i + 1), path, speed / numOfTrds, flag));
            //Запуск пула потоков, инициализация потоков и передача их в пул
        }
        threadPool.shutdown();//Прекращение приема новых задач пулом
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);//Ожидание окончания работы пула
        } catch (Exception e) {
            e.printStackTrace();
        }

        int timeSpent = (int) (System.currentTimeMillis() - startTime) / 1000;
        if (timeSpent >= 60) {
            int modulo = timeSpent - ((timeSpent / 60) * 60);
            timeSpent /= 60;
            System.out.println("Загрузка завершена. Загружено "
                    + resultSize / 1048576 + " Мб за " + timeSpent
                    + " минут" + checkTime(timeSpent) + " "
                    + modulo + " секунд" + checkTime(modulo) + ".");
        } else {
            System.out.println("Загрузка завершена. Загружено "
                    + resultSize / 1048576 + " Мб за " + timeSpent
                    + " секунд" + checkTime(timeSpent) + ".");
        }
    }

    private static String checkTime(int time) {
        String result = "";
        if (10 < time & time < 21) {
            return result;
        }
        if (time > 10) {
            time = time % 10;
        }

        switch (time) {
            case 1:
                result = "у";
                break;
            case 2:
            case 3:
            case 4:
                result = "ы";
                break;
        }
        return result;
    }

    private static byte[] getBytes(String urlSpec, double speed, boolean flag) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        RateLimiter rateLimiter = RateLimiter.create(speed);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                if (flag) {
                    rateLimiter.acquire(bytesRead);
                }
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private static void getFile(byte[] array, String path) {
        try {
            FileOutputStream stream = new FileOutputStream(path);
            try {
                stream.write(array);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<String> getUrlFromText(String path) {
        ArrayList<String> list = new ArrayList<>();
        String[] buffer;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            try {
                String line = br.readLine();
                while (line != null) {
                    buffer = line.split(" ");
                    list.add(buffer[0]);
                    list.add(buffer[1]);
                    line = br.readLine();
                }
            } catch (IndexOutOfBoundsException obe) {
                System.out.println("Проверьте правильность заполнения входного файла.");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    static class Download implements Runnable {

        byte[] result = new byte[]{};
        String url;
        String fileName;
        String path;
        double speed;
        boolean flag;

        Download(String url, String fileName, String path, double speed, boolean flag) {
            this.url = url;
            this.fileName = fileName;
            this.path = path;
            this.speed = speed;
            this.flag = flag;
        }

        @Override
        public void run() {
            try {
                result = getBytes(url, speed, flag);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            sumSize(result.length);
            getFile(result, path + fileName);
        }
    }
}