package com.cbtunnel.plus.thread;

import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.service.HarlieService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Locale;

import io.michaelrocks.paranoid.Obfuscate;


//MODIFIED BY CHADDEVZ
@Obfuscate
public class HTTPInjectorThread extends Thread
{
    private final String TAG = "HTTPInjectorThread";
    Socket incoming;
    Socket outgoing;
    String buffReq = "1024";
    String buffRes = "4096";
    private boolean clientToServer;

    public HTTPInjectorThread(Socket socket, Socket socket2, boolean z, String buffReq, String buffRes)
    {
        incoming = socket;
        outgoing = socket2;
        this.clientToServer = z;
        this.buffReq = buffReq;
        this.buffRes = buffRes;
        setDaemon(true);
    }


    public final void run() {
        try{
            byte[] buffer;
            if (clientToServer) {
                buffer = new byte[Integer.parseInt(buffReq)];
            } else {
                buffer = new byte[Integer.parseInt(buffRes)];
            }
            InputStream FromClient = this.incoming.getInputStream();
            OutputStream ToClient = this.outgoing.getOutputStream();
            while (true) {
                int numberRead = FromClient.read(buffer);
                if (numberRead == -1) {
                    break;
                }
                String result = new String(buffer, 0, numberRead);
                if (this.clientToServer) {
                    ToClient.write(buffer, 0, numberRead);
                    ToClient.flush();
                } else {
                    String[] split = result.split("\r\n");
                    if (split[0].toLowerCase(Locale.getDefault()).startsWith("http")) {
                        result = split[0].substring(9, 12);
                        addLog(split[0]);
                        if (result.contains ( "200" )) {
                            ToClient.write(buffer, 0, numberRead);
                            ToClient.flush();
                        } else {
                            if (split[0].split(" ")[0].equals("HTTP/1.1")) {
                                addLog("replace 200 OK");
                                ToClient.write( (split[0].split ( " " )[0] + " 200 OK\r\n\r\n").getBytes());
                            } else {
                                try {
                                    addLog("<b>Status: 200 (Connection established) Successful</b> - The action requested by the client was successful.");
                                    ToClient.write( (split[0].split ( " " )[0] + " 200 Connection established\r\n\r\n").getBytes());
                                } catch (Exception e) {
                                    try {
                                        if (this.incoming != null) {
                                            this.incoming.close();
                                        }
                                        if (this.outgoing != null) {
                                            this.outgoing.close();
                                            return;
                                        }
                                        return;
                                    } catch (IOException e2) {
                                        return;
                                    }
                                } catch (Throwable th) {
                                    try {
                                        if (this.incoming != null) {
                                            this.incoming.close();
                                        }
                                        if (this.outgoing != null) {
                                            this.outgoing.close();
                                        }
                                    } catch (IOException e3) {
                                        addLog ( e3.getMessage () );
                                    }
                                }
                            }
                            ToClient.flush();
                        }
                    } else {
                        ToClient.write(buffer, 0, numberRead);
                        ToClient.flush();
                    }
                }
            }
            FromClient.close();
            ToClient.close();
        }catch(Exception e){
            try {
                if (this.incoming != null) {
                    this.incoming.close();
                }
                if (this.outgoing != null) {
                    this.outgoing.close();
                }
            } catch (IOException e4) {
                addLog ( e4.getMessage () );
            }
        }
    }


    public static void connect(Socket first, Socket second, String buffReq, String buffRes)
    {
        new HTTPInjectorThread(first, second, true, buffReq, buffRes).start();
        new HTTPInjectorThread(second, first, false, buffReq, buffRes).start();
    }

    void addLog(String str) {
        hLogStatus.logInfo(str);
    }
}
