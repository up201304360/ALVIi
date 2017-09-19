package pt.lsts.alvii;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import pt.lsts.imc.lsf.LsfIndex;

/**
 * Created by pedro on 9/15/17.
 * LSTS - FEUP
 */

class MraLiteStorage {

    private static final String TAG = "MEU MraLiteStorage";
    private LsfIndex m_index;
    private Context m_context;
    private int numberThread = 4;
    String messageList[] = new String[512];
    private String messageListPart[][] = new String[numberThread][512];
    int cntMsgByTheard[] = new int[numberThread];
    private int numberOfMessage = 0;
    private int split;
    private int cntThread[] = new int[numberThread];
    private boolean isThreadFinish[] = new boolean[numberThread];
    private int cntListLog = 0;
    private File m_file_path;

    public MraLiteStorage(Context context) {
        m_context = context;
        for(int i = 0; i < numberThread; i++){
            cntThread[i] = 0;
            cntMsgByTheard[i] = 0;
            isThreadFinish[i] = false;
        }
    }

    public void initMraLiteStorage(){
        for(int i = 0; i < numberThread; i++){
            cntThread[i] = 0;
            cntMsgByTheard[i] = 0;
            isThreadFinish[i] = false;
        }
    }

    public long getProcessStageValue(){
        long result = 0;
        for(int i = 0; i < numberThread; i++)
            result = result + cntThread[i];

        return result + 1;
    }

    public int getNumberMessages(){
        return numberOfMessage;
    }

    public int getNumberOfListMsg() { return cntListLog; }

    public boolean isAllThreadFinish(){
        boolean noRun = true;
        for(int t = 0; t < numberThread; t ++){
            if(!isThreadFinish[t])
                noRun = false;
        }

        if(noRun)
            return true;
        else
            return false;
    }

    public String[] getListOfMessages(){
        return messageList;
    }

    public void indexListOfMessage(LsfIndex index, File path) {
        m_index = index;
        numberOfMessage = m_index.getNumberOfMessages();
        split = (numberOfMessage / numberThread);
        // Since reading log takes more time, let's run it on a separate threads.
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                getPartOfListMessage(1, split, 1, m_index);
            }
        });
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                getPartOfListMessage(split + 1, split * 2, 2,m_index);
            }
        });
        Thread thread3 = new Thread(new Runnable() {
            @Override
            public void run() {
                getPartOfListMessage(split * 2 + 1, split * 3, 3, m_index);
            }
        });
        Thread thread4 = new Thread(new Runnable() {
            @Override
            public void run() {
                getPartOfListMessage(split * 3 + 1, numberOfMessage - 1, 4, m_index);
            }
        });

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        m_file_path = path;
    }

    private void getPartOfListMessage(int startCnt, int ends, int idThread, LsfIndex t_index){
        int state_msg;
        cntThread[idThread - 1] = 0;
        isThreadFinish[idThread - 1] = false;
        boolean haveName;
        String msgName;
        for(state_msg = startCnt; state_msg <= ends; state_msg++){
            haveName = false;
            msgName = t_index.getMessage(state_msg).getAbbrev();
            if(cntMsgByTheard[idThread - 1] == 0){
                messageListPart[idThread - 1][cntMsgByTheard[idThread - 1]] = msgName;
                cntMsgByTheard[idThread - 1]++;
            }
            else{
                for(int i = 0; i < cntMsgByTheard[idThread - 1]; i++)
                {
                    if(messageListPart[idThread - 1][i].equals(msgName))
                        haveName = true;
                }
                if(!haveName){
                    messageListPart[idThread - 1][cntMsgByTheard[idThread - 1]] = msgName;
                    cntMsgByTheard[idThread - 1]++;
                }
            }
            cntThread[idThread - 1]++;
        }
        //Log.i(TAG, "Finish "+ idThread + " - "+ cntThread[idThread - 1] + "  cnt: " + cntMsgByTheard[idThread - 1]);
        isThreadFinish[idThread - 1] = true;

        boolean noRun = true;
        for(int t = 0; t < numberThread; t ++){
            if(!isThreadFinish[t])
                noRun = false;
        }
        if(noRun)
            mergeListMessagePart();
    }

    private void mergeListMessagePart() {
        for(int i = 0; i < cntMsgByTheard[0]; i++)
            messageList[i] = messageListPart[0][i];

        int size = cntMsgByTheard[0];
        boolean haveImcMessage;

        for(int r = 1; r < numberThread; r++){
            try {
                for (int i = 0; i < cntMsgByTheard[r]; i++) {
                    //Log.i(TAG, "AQUI "+r+":"+i+" - "+cntMsgByTheard[r] + " S: "+size);
                    haveImcMessage = false;
                    for (int t = 0; t < size; t++) {
                        if (messageList[t].equals(messageListPart[r][i]))
                            haveImcMessage = true;
                    }
                    if (!haveImcMessage) {
                        messageList[size++] = messageListPart[r][i];
                    }
                }
            }
            catch (Exception io){
                Log.i(TAG, "ERROR: "+io);
            }
        }
        size--;
        cntListLog = size;
        File root = new File(m_file_path.getParent());
        if (!root.exists()) {
            root.mkdirs();
        }
        File gpxfile = new File(root, "indexMessageList.stackIndex");
        FileWriter writer = null;
        try {
            writer = new FileWriter(gpxfile);
            for (int i = 0; i < cntListLog; i++)
                writer.append(messageList[i]+"\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getListMessgeByOldIndex(File source){
        isThreadFinish[0] = false;
        for(int i = 1; i < numberThread; i++){
            isThreadFinish[i] = true;
        }
        Log.i(TAG, "EXIST: "+source.getParent().toString());
        int cnt = 0;
        File gpxfile = new File(source.getParent(), "indexMessageList.stackIndex");
        FileInputStream is = null;
        try {
            is = new FileInputStream(gpxfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        try {
            line = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(line != null){
            messageList[cnt] = line;
            cnt++;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cntListLog = cnt;
    }

            /*double endTime = index.getEndTime();
        double startTime = index.getStartTime();
        double pivot = (endTime + startTime) / 2.0;
        Log.i(TAG,""+endTime+" --> "+ pivot+ " --> "+ startTime);

        for (int i = index.getFirstMessageOfType(EntityState.ID_STATIC); i != -1; i = index.getNextMessageOfType(EntityState.ID_STATIC, i)) {
            Log.i(TAG,index.getSystemName(index.sourceOf(i))+", "+index.entityOf(i)+", "+index.hashOf(i));
        }*/

    /*int cnt_number_of = 0;

        while(state_msg != -1)
        {
            state_msg = index.getNextMessageOfType(EntityState.ID_STATIC, state_msg);
            Log.i(TAG, "number: " + state_msg);
            try {
                Log.i(TAG,""+index.getMessage(state_msg, EntityState.class));
                cnt_number_of++;
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateBarHandler.post(new Runnable() {
                public void run() {
                    if(state_msg >= 0)
                        pDialog.setMessage("Reading Log - Msg " + state_msg + " of " + cnt);
                }
            });
        }*/

        /*for(state_msg = 0; state_msg < cnt; state_msg++){
            //Log.i(TAG, index.getMessage(state_msg).getAbbrev());
            //m_mra_storage.addMsg(index.getMessage(state_msg));
            updateBarHandler.post(new Runnable() {
                public void run() {
                    if(state_msg >= 0)
                        pDialog.setMessage("Reading Log - Msg " + state_msg + " of " + cnt);
                }
            });
        }*/

    //Log.i(TAG, "total: "+cnt_number_of);
    // Dismiss the progressbar after 500 millisecondds

    //Log.i(TAG,""+index.getNextMessageOfType(EntityState.ID_STATIC, 820));
    //Log.i(TAG,""+index.getMessageBeforeOrAt("EntityState", index.getNumberOfMessages(), pivot));
}

