package com.StreamRecorder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Constants {
    //livestreamer [OPTIONS] [URL] [STREAM]

    public static final String SETTINGS_LS_PATH       = "LS_PATH";
    public static final String SETTINGS_OUTPUT_FOLDER = "OUTPUT_FOLDER";
    public static final String SETTINGS_RESCAN_TIME   = "RESCAN_TIME";
    public static final String SETTINGS_WRITE_LOGS    = "WRITE_LOGS";

    public static final String SETTINGS_FILE = "settings.txt";
    public static final String STREAMS_FILE  = "streams.txt";

    public static final Charset DEFAULT_CHARSET = StandardCharsets.US_ASCII;

    public static final String STREAM_QUALITY = "best";

    public static final String PARAM_WRITE_FILE = "-o";
    public static final String PARAM_FORCE      = "-f";

    public static final String DATE_FORMAT          = "MM_dd__HH_mm_ss";
    public static final String VIDEO_FILE_EXTENSION = ".ts";

    public static final String STREAMS_DELIMITER  = ";";
    public static final String SETTINGS_DELIMITER = "=";
    public static final String DO_NOT_RECORD_CHAR = "0";

    public static final int IS_PROCESS_ALIVE_CHECKING_TIME_SEC = 10;


    public static final int HTTP_PORT = 80;

    public static final String HTTP_PAGE_MAIN     = "/streamrecorder";
    public static final String HTTP_PAGE_ADD      = "/add";
    public static final String HTTP_PAGE_SETTINGS = "/settings";
    public static final String HTTP_CSS           = "/css";

    public static final String HTTP_PAGE_STOPRESUME = "/stopresume";
    public static final String HTTP_PAGE_REMOVE     = "/remove";

    public static final String HTML_PATH          = "./html/";
    public static final String HTML_PAGE_MAIN     = HTML_PATH + "index.html";
    public static final String HTML_PAGE_ROW      = HTML_PATH + "indexRow.html";
    public static final String HTML_PAGE_ADD      = HTML_PATH + "add.html";
    public static final String HTML_PAGE_SETTINGS = HTML_PATH + "settings.html";


}
