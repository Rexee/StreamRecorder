package com.StreamRecorder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Stream implements Runnable {

    public String name;
    public String path;
    public int    index;

    public boolean isRecording;
    public boolean isRunning;
    public boolean needRecord;
    public boolean needDelete;
    public Thread  thread;
    public Process process;
    public int     mRefreshTime;

    String  mLivestreamerEXE;
    String  mOutputFolder;
    boolean mWriteLogs;

    public Stream(String name, int index, String path) {
        this.name = name;
        this.path = path;
        this.index = index;

        this.needDelete = false;
        this.isRecording = false;
        this.isRunning = false;
        this.thread = null;
    }

    public void setGlobalParams(String livestreamerEXE, String outputFolder, boolean writeLogs, int rescanTime) {
        this.mLivestreamerEXE = livestreamerEXE;
        this.mOutputFolder = outputFolder;
        this.mWriteLogs = writeLogs;
        this.mRefreshTime = rescanTime;
    }

    public void startRecording() {

        thread = new Thread(this, name);
        thread.start();

    }

    @Override
    public void run() {

        while (!needDelete && needRecord) {

            if (Thread.currentThread().isInterrupted()) return;

            isRunning = true;

            try {
                runEXE();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            sleep(mRefreshTime);

        }
    }

    private void log(String message) {

        if (!mWriteLogs) return;

        FileWriter fw = null;
        try {
            DateFormat df = new SimpleDateFormat("dd HH:mm:ss");
            fw = new FileWriter(mOutputFolder + "\\" + name + ".log", true);
            fw.append("" + df.format(Calendar.getInstance().getTime()) + " " + message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fw != null) try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runEXE() throws IOException, InterruptedException {

        log("\nrunEXE+");

        String filename = generateFilename();
        log(filename);

        ProcessBuilder pb = new ProcessBuilder(mLivestreamerEXE, Constants.PARAM_WRITE_FILE, filename, Constants.PARAM_FORCE, path, Constants.STREAM_QUALITY);
        pb.redirectErrorStream(true);

        File logFile = null;
        if (mWriteLogs) {
            logFile = new File(mOutputFolder + "\\" + name + ".log");
            pb.redirectOutput(Redirect.appendTo(logFile));
        }

        process = null;
        try {
            process = pb.start();

            if (mWriteLogs) {
                assert pb.redirectInput() == Redirect.PIPE;
                assert pb.redirectOutput().file() == logFile;
                assert process.getInputStream().read() == -1;
            }

            sleep(Constants.IS_PROCESS_ALIVE_CHECKING_TIME_SEC);

            if (process.isAlive()) {
                isRecording = true;
            }

            process.waitFor();

            int shellExitValue = process.exitValue();
            log("Exit value: " + shellExitValue);


        } catch (IOException e) {
            e.printStackTrace();
            log("IOException" + e.toString());
        }

        isRecording = false;
        log("\nrunEXE-");
    }

    private void sleep(int sleepTime) {
        try {
            Thread.sleep(1000L * sleepTime);
        } catch (InterruptedException ignored) {
        }
    }

    public void stopRecording() {
        if (process != null) process.destroy();

        if (thread == null || !thread.isAlive()) {
            return;
        }

        isRunning = false;
        isRecording = false;

        thread.interrupt();
    }

    private String generateFilename() {
        DateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);

        StringBuilder sb = new StringBuilder();

        sb.append(mOutputFolder);
        sb.append("\\");
        sb.append(name);
        sb.append("_");
        sb.append(df.format(Calendar.getInstance().getTime()));
        sb.append(Constants.VIDEO_FILE_EXTENSION);
        return sb.toString();
    }

    public String getStatusHTML() {
        if (isRecording) return "Recording";
        else if (isRunning) return "Searching";
        else if (needDelete) return "Deleting";

        return "";
    }

    public String getStatusClass() {
        if (isRecording) return "recording";
        return "normal";
    }
}
