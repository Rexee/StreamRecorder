package com.StreamRecorder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Commander {

    volatile List<Stream> mStreams;

    int     mRescanTime      = 60;
    String  mLivestreamerEXE = "";
    String  mOutputFolder    = "";
    boolean mWriteLogs       = false;

    boolean initialized;

    public Commander() {
        this.initialized = false;
        this.mStreams = new ArrayList<Stream>();
    }

    public boolean loadSettings() {

        Path pathFileSettings = Paths.get(Constants.SETTINGS_FILE);

        if (!Files.exists(pathFileSettings)) {
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(pathFileSettings, Constants.DEFAULT_CHARSET);
            String curStr;
            for (int i = 0; i < lines.size(); i++) {
                curStr = lines.get(i);

                if (curStr.isEmpty()) {
                    continue;
                }

                String[] settingsArr = curStr.split(Constants.SETTINGS_DELIMITER);

                String settingsName = settingsArr[0];
                String settingsValue = settingsArr[1];

                if (settingsName.equals(Constants.SETTINGS_RESCAN_TIME)) {
                    mRescanTime = Integer.parseInt(settingsValue);
                } else if (settingsName.equals(Constants.SETTINGS_LS_PATH)) {
                    mLivestreamerEXE = settingsValue;
                } else if (settingsName.equals(Constants.SETTINGS_OUTPUT_FOLDER)) {
                    mOutputFolder = settingsValue;
                } else if (settingsName.equals(Constants.SETTINGS_WRITE_LOGS)) {
                    mWriteLogs = Boolean.parseBoolean(settingsValue);
                }

            }

            loadStreams();

        } catch (IOException e) {
            System.out.println("Settings file reading error!");
            e.printStackTrace();
            return false;

        }

        return true;
    }

    private void loadStreams() {

        Path pathFileStreams = Paths.get(Constants.STREAMS_FILE);
        if (!Files.exists(pathFileStreams)) {
            return;
        }

        for (Stream stream : mStreams) {
            stream.needDelete = true;
        }

        try {

            List<String> lines = Files.readAllLines(pathFileStreams, Constants.DEFAULT_CHARSET);

            for (int i = 0; i < lines.size(); i++) {
                String[] streamStrs = lines.get(i).split(Constants.STREAMS_DELIMITER);
                if (streamStrs[0].isEmpty()) {
                    continue;
                }
                String streamName = streamStrs[0];
                String streamIndex = streamStrs[1];
                String streamNeedToRecord = streamStrs[2];
                String streamPath = streamStrs[3];

                Stream stream = getStream(streamName, streamPath, Integer.parseInt(streamIndex));
                stream.setGlobalParams(mLivestreamerEXE, mOutputFolder, mWriteLogs, mRescanTime);

                stream.needRecord = !streamNeedToRecord.equals(Constants.DO_NOT_RECORD_CHAR);

                mStreams.add(stream);

            }

        } catch (IOException e) {
            System.out.println("Streams file reading error!");
            e.printStackTrace();
        }

    }

    private Stream getStream(String streamName, String streamPath, int index) {

        for (Stream stream : mStreams) {
            if (stream.name.equals(streamName)) {
                stream.index = index;
                stream.needDelete = false;
                return stream;
            }
        }

        return new Stream(streamName, index, streamPath);
    }

    public void start() {

        if (!loadSettings()) {
            return;
        }

        Iterator<Stream> iterator = mStreams.iterator();
        while (iterator.hasNext()) {
            Stream stream = iterator.next();

            if (stream.needDelete) {
                stream.stopRecording();
                iterator.remove();
            } else if (!stream.needRecord) {
                stream.stopRecording();
            } else if (stream.isRunning) {
                //do nothing
            } else {
                stream.startRecording();
            }
        }

        this.initialized = true;
    }

    public String getHTML_Main() {

        String errorHTML = "<HTML>No index html template</HTML>";

        Path pathFile = Paths.get(Constants.HTML_PAGE_MAIN);
        if (!Files.exists(pathFile)) {
            return errorHTML;
        }

        Path pathFileRow = Paths.get(Constants.HTML_PAGE_ROW);
        if (!Files.exists(pathFileRow)) {
            return errorHTML;
        }

        String htmlStringIndex;
        String htmlStringRow;

        try {
            byte[] encodedRow = Files.readAllBytes(pathFileRow);
            htmlStringRow = new String(encodedRow, Constants.DEFAULT_CHARSET);

            String rowCalculated;
            StringBuilder rows = new StringBuilder();

            Random randomGenerator = new Random();
            for (Stream stream : mStreams) {

                String btnLabel = "Resume";
                if (stream.isRunning) {
                    btnLabel = "Stop";
                }

                rowCalculated = htmlStringRow.replace("{StreamName}", stream.name);
                rowCalculated = rowCalculated.replace("{StreamLink}", stream.path);
                rowCalculated = rowCalculated.replace("{StatusClass}", stream.getStatusClass());
                rowCalculated = rowCalculated.replace("{Status}", stream.getStatusHTML());
                rowCalculated = rowCalculated.replace("{StreamIndex}", stream.index + "&" + randomGenerator.nextInt(10000));
                rowCalculated = rowCalculated.replace("{StopResumeLabel}", btnLabel);

                rows.append(rowCalculated);
                rows.append("\n");

            }

            byte[] encodedIndex = Files.readAllBytes(pathFile);
            htmlStringIndex = new String(encodedIndex, Constants.DEFAULT_CHARSET);


            htmlStringIndex = htmlStringIndex.replace("{version}", Constants.VERSION);
            htmlStringIndex = htmlStringIndex.replace("{rescanTime}", Integer.toString(mRescanTime));
            htmlStringIndex = htmlStringIndex.replace("{trInline}", rows.toString());


        } catch (IOException e) {
            e.printStackTrace();
            return errorHTML;
        }

        return htmlStringIndex;

    }

    public void stopResume(String streamIndex) {

        int index = Integer.parseInt(streamIndex);
        for (Stream stream : mStreams) {
            if (stream.index == index) {
                if (stream.isRunning) {
                    stream.needRecord = false;
                    stream.stopRecording();
                } else {
                    stream.needRecord = true;
                    stream.startRecording();
                }
                break;
            }
        }

        saveStreams();

    }

    private void saveStreams() {

        StringBuilder sb = new StringBuilder();
        for (Stream stream : mStreams) {
            sb.append(stream.name);
            sb.append(Constants.STREAMS_DELIMITER);
            sb.append(stream.index);
            sb.append(Constants.STREAMS_DELIMITER);
            sb.append(stream.needRecord ? "1" : "0");
            sb.append(Constants.STREAMS_DELIMITER);
            sb.append(stream.path);
            sb.append("\n");
        }

        try {
            OpenOption[] options = new OpenOption[]{StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE};
            Files.write(Paths.get(Constants.STREAMS_FILE), sb.toString().getBytes(), options);
        } catch (IOException e) {
            System.out.println("Streams file writing error!");
            e.printStackTrace();
        }

    }

    public void remove(String streamIndex) {

        int index = Integer.parseInt(streamIndex);

        Iterator<Stream> iterator = mStreams.iterator();
        while (iterator.hasNext()) {
            Stream stream = iterator.next();
            if (stream.index == index) {
                if (stream.isRunning) {
                    stream.needDelete = true;
                    stream.stopRecording();
                }
                iterator.remove();
                break;

            }
        }

        saveStreams();

    }

    public String getHTML_Add() {
        String errorHTML = "<HTML>No add html template</HTML>";

        Path pathFile = Paths.get(Constants.HTML_PAGE_ADD);
        if (!Files.exists(pathFile)) {
            return errorHTML;
        }

        String htmlString;

        try {
            byte[] encoded = Files.readAllBytes(pathFile);
            htmlString = new String(encoded, Constants.DEFAULT_CHARSET);

        } catch (IOException e) {
            e.printStackTrace();
            return errorHTML;
        }

        return htmlString;

    }

    public void addStream(String newStreamName, String newStreamPath, boolean newStreamRecord) {

        for (Stream stream : mStreams) {
            if (stream.name.equals(newStreamName)) {
                return;
            }
        }

        int index = getMaxStreamIndex();

        Stream newStream = new Stream(newStreamName, index, newStreamPath);
        newStream.setGlobalParams(mLivestreamerEXE, mOutputFolder, mWriteLogs, mRescanTime);
        newStream.needRecord = newStreamRecord;
        newStream.startRecording();

        mStreams.add(newStream);

        saveStreams();
    }

    private int getMaxStreamIndex() {
        int res = 0;
        for (Stream stream : mStreams) {
            if (stream.index > res) {
                res = stream.index;
            }
        }

        return ++res;
    }

    public String getHTML_Settings() {

        String errorHTML = "<HTML>No settins html template</HTML>";

        Path pathFile = Paths.get(Constants.HTML_PAGE_SETTINGS);
        if (!Files.exists(pathFile)) {
            return errorHTML;
        }

        String htmlString;

        try {
            byte[] encoded = Files.readAllBytes(pathFile);

            htmlString = new String(encoded, Constants.DEFAULT_CHARSET);

            htmlString = htmlString.replace("{livestreamerEXE}", mLivestreamerEXE);
            htmlString = htmlString.replace("{outputFolder}", mOutputFolder);
            htmlString = htmlString.replace("{rescanTime}", Integer.toString(mRescanTime));
            htmlString = htmlString.replace("{writeLogs}", mWriteLogs ? "checked" : "");

        } catch (IOException e) {
            e.printStackTrace();
            return errorHTML;
        }

        return htmlString;
    }

    public void applySettings(String settingsLS, String settingsOutput, String settingsRescan, String settingsLogs) {

        mLivestreamerEXE = settingsLS;
        mOutputFolder = settingsOutput;
        mWriteLogs = Boolean.parseBoolean(settingsLogs);
        mRescanTime = Integer.parseInt(settingsRescan);

        for (Stream stream : mStreams) {
            stream.setGlobalParams(mLivestreamerEXE, mOutputFolder, mWriteLogs, mRescanTime);
        }

        initialized = true;
        saveSettings();

    }

    private void saveSettings() {
        StringBuilder sb = new StringBuilder();
        addSettingsString(sb, Constants.SETTINGS_LS_PATH, mLivestreamerEXE);
        addSettingsString(sb, Constants.SETTINGS_OUTPUT_FOLDER, mOutputFolder);
        addSettingsString(sb, Constants.SETTINGS_RESCAN_TIME, Integer.toString(mRescanTime));
        addSettingsString(sb, Constants.SETTINGS_WRITE_LOGS, Boolean.toString(mWriteLogs));

        try {
            OpenOption[] options = new OpenOption[]{StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE};
            Files.write(Paths.get(Constants.SETTINGS_FILE), sb.toString().getBytes(), options);

        } catch (IOException e) {
            System.out.println("Settings file writing error!");
            e.printStackTrace();
        }
    }

    private void addSettingsString(StringBuilder sb, String settingsName, String settingsValue) {
        sb.append(settingsName);
        sb.append(Constants.SETTINGS_DELIMITER);
        sb.append(settingsValue);
        sb.append("\n");
    }
}
