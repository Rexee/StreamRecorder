package com.StreamRecorder;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.zip.GZIPOutputStream;

public class WebServer {

    Commander  commander;
    HttpServer server;

    public void start() {

        commander = new Commander();
        commander.start();

        try {
//            InetAddress.getLoopbackAddress()
            String serverAddress = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Server started at address: http://" + serverAddress + "/" + Constants.HTTP_PAGE_MAIN);

            server = HttpServer.create(new InetSocketAddress(serverAddress, Constants.HTTP_PORT), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        server.createContext(Constants.HTTP_PAGE_MAIN, new Handler_Main());
        server.createContext(Constants.HTTP_PAGE_STOPRESUME, new Handler_StopResume());
        server.createContext(Constants.HTTP_PAGE_REMOVE, new Handler_Remove());
        server.createContext(Constants.HTTP_PAGE_ADD, new Handler_Add());
        server.createContext(Constants.HTTP_PAGE_SETTINGS, new Handler_Settings());
        server.createContext(Constants.HTTP_CSS, new Handler_CSS());
        server.setExecutor(null);
        server.start();
    }

    private void redirectToPage(HttpExchange httpExchange, String PageName) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        headers.set("Content-Type", "text/html");

//        InetAddress.getLocalHost().getHostName();
//        InetAddress.getLocalHost().getHostAddress()

        headers.set("Location", "http:/" + server.getAddress() + PageName);
        headers.set("Cache-Control", "private, no-cache, no-store, must-revalidate");
        headers.set("Connection", "close");
        httpExchange.sendResponseHeaders(301, -1);
        httpExchange.close();
    }

    private void showPage(String pageHTML, HttpExchange httpExchange) throws IOException {

        Headers headers = httpExchange.getResponseHeaders();
        headers.set("Content-Type", "text/html");
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        httpExchange.sendResponseHeaders(200, pageHTML.length());

        OutputStream os = httpExchange.getResponseBody();
        os.write(pageHTML.getBytes());
        os.close();
    }

    private String getPathUni(String value) throws UnsupportedEncodingException {
        String res = URLDecoder.decode(value, "UTF-8");
        if (res.substring(res.length() - 1).equals("/")) {
            res = res.substring(0, res.length() - 1);
        }
        return res;
    }

    private class Handler_Main implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            if (!commander.initialized) {
                redirectToPage(httpExchange, Constants.HTTP_PAGE_SETTINGS);
            } else {
                showPage(commander.getHTML_Main(), httpExchange);
            }
        }
    }

    private class Handler_StopResume implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String[] variables = httpExchange.getRequestURI().getRawQuery().split("=");
            if (variables.length == 2) {
                if (variables[0].equals("s")) {
                    String[] values = variables[1].split("&");
                    commander.stopResume(values[0]);
                }
            }
            redirectToPage(httpExchange, Constants.HTTP_PAGE_MAIN);

        }
    }

    private class Handler_Remove implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String[] variables = httpExchange.getRequestURI().getRawQuery().split("=");
            if (variables.length == 2) {
                if (variables[0].equals("s")) {
                    String[] values = variables[1].split("&");
                    commander.remove(values[0]);
                }
            }
            redirectToPage(httpExchange, Constants.HTTP_PAGE_MAIN);
        }
    }

    private class Handler_Add implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestMethod = httpExchange.getRequestMethod();
            if (!"post".equalsIgnoreCase(requestMethod)) {
                showPage(commander.getHTML_Add(), httpExchange);
            } else {
                InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                String[] params = query.split("&");

                String newStreamName = "";
                String newStreamPath = "";
                boolean newStreamRecord = false;

                for (String param : params) {
                    String[] values = param.split("=");
                    if (values[0].equals("name")) {
                        newStreamName = URLDecoder.decode(values[1], "UTF-8");
                    } else if (values[0].equals("path")) {
                        newStreamPath = getPathUni(values[1]);
                    } else if (values[0].equals("record")) {
                        newStreamRecord = values[1].equals("true");
                    }
                }

                if (!newStreamPath.isEmpty()) commander.addStream(newStreamName, newStreamPath, newStreamRecord);

                redirectToPage(httpExchange, Constants.HTTP_PAGE_MAIN);
            }
        }
    }

    private class Handler_Settings implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestMethod = httpExchange.getRequestMethod();
            if (!"post".equalsIgnoreCase(requestMethod)) {
                showPage(commander.getHTML_Settings(), httpExchange);
            } else {
                InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                String[] params = query.split("&");

                String settingsLS = "";
                String settingsOutput = "";
                String settingsRescan = "0";
                String settingsLogs = "false";

                for (String param : params) {
                    String[] values = param.split("=");
                    if (values.length < 2) continue;

                    if (values[0].equals("lspath")) {
                        settingsLS = URLDecoder.decode(values[1], "UTF-8");
                    } else if (values[0].equals("output")) {
                        settingsOutput = getPathUni(values[1]);
                    } else if (values[0].equals("rescan")) {
                        settingsRescan = values[1];
                    } else if (values[0].equals("logs")) {
                        settingsLogs = values[1];
                    }
                }

                if (settingsLS.isEmpty() || settingsOutput.isEmpty()) {
                    showPage(commander.getHTML_Settings(), httpExchange);
                    return;
                }
                commander.applySettings(settingsLS, settingsOutput, settingsRescan, settingsLogs);

                redirectToPage(httpExchange, Constants.HTTP_PAGE_MAIN);
            }

        }
    }

    private class Handler_CSS implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestMethod = httpExchange.getRequestMethod();

            if (requestMethod.contentEquals("GET")) {
                String path = "." + httpExchange.getRequestURI().getPath();
                File cssFile = new File(path);
                if (!cssFile.exists()) return;

                Headers headers = httpExchange.getResponseHeaders();
                headers.set("Content-Type", "text/css");
                headers.set("Content-Encoding", "gzip");
                headers.set("Connection", "keep-alive");
                httpExchange.sendResponseHeaders(200, 0);

                long toRead = cssFile.length();

                GZIPOutputStream gos = new GZIPOutputStream(httpExchange.getResponseBody());
                BufferedInputStream bufread = new BufferedInputStream(new FileInputStream(cssFile));

                byte buffer[] = new byte[2048];
                int bytesRead = 0;
                while (toRead > 0 && (bytesRead = bufread.read(buffer)) != -1) {
                    gos.write(buffer, 0, bytesRead);
                    gos.flush();
                    toRead = toRead - bytesRead;
                }
                gos.finish();
                gos.close();
                bufread.close();
                httpExchange.close();
            }
        }
    }
}


