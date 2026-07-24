package com.cbtunnel.plus.thread;

import android.os.CountDownTimer;

public class RetryingThread {

    protected CountDownTimer mCountDownTimer;

    public static boolean thVerifyIsRunning = false;

    protected int retryingCount = 0;
    public RetryingThreadListener mListener;
    public interface RetryingThreadListener {
        void onRetrying(int i);
    }

    public RetryingThread(RetryingThreadListener mListener){
        this.mListener = mListener;
    }

    private void stopVerifyingAccount(){
        if (mCountDownTimer!=null){
            mCountDownTimer.cancel();
            mCountDownTimer = null;
            thVerifyIsRunning = false;
        }
    }
    public void startVerifyingAccount(){
        stopVerifyingAccount();
        retryingCount++;
        mCountDownTimer = new CountDownTimer(180000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                thVerifyIsRunning = true;
            }
            @Override
            public void onFinish() {
                thVerifyIsRunning = false;
                if (retryingCount==1){
                    mListener.onRetrying(1);
                } else if (retryingCount==2){
                    mListener.onRetrying(2);
                    retryingCount = 0;
                }
                startVerifyingAccount();
            }
        }.start();
    }


     /*private Thread thVerify;
    private int retryingCount = 0;
    private boolean thVerifyIsRunning = false;
    private void stopVerifyingAccount(){
        try {
            if (thVerify!=null && thVerify.isAlive()){
                thVerifyIsRunning = false;
                thVerify.interrupt();
                thVerify = null;
            }
        } catch(Exception ignored) {}
    }
    private void startVerifyingAccount(){
        if (thVerifyIsRunning){
            addlogInfo("startVerifyingAccount is running!");
            return;
        }
        retryingCount = 0;
        try {
            int timePing = 60;
            if (!hLogStatus.isTunnelActive()) {
                throw new Exception();
            }
            thVerify = new Thread() {
                @Override
                public void run() {
                    while (hLogStatus.isTunnelActive()) {
                        thVerifyIsRunning = true;
                        retryingCount++;
                        try {
                            verify();
                        } catch(InterruptedException e) {
                            break;
                        }
                    }
                    stopVerifyingAccount();
                }
                private synchronized void verify() throws InterruptedException {
                    if (retryingCount==1){
                        showExpireDate();
                        addlogInfo("showExpireDate....");
                    }else if (retryingCount==2){
                        autoUpdate();
                        retryingCount = 0;
                        addlogInfo("autoUpdate....");
                    }
                    if (!hLogStatus.isTunnelActive()){
                        stopVerifyingAccount();
                    }
                    if (timePing == 0)
                        return;
                    if (timePing > 0)
                        sleep(timePing*1000);
                    else {
                        stopVerifyingAccount();
                        throw new InterruptedException();
                    }
                }
            };
            thVerify.start();
        } catch(Exception ignored) {}
    }*/


}


