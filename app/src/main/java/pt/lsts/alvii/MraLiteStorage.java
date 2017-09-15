package pt.lsts.alvii;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;

/**
 * Created by pedro on 9/15/17.
 * LSTS - FEUP
 */

class MraLiteStorage {

    private static final String TAG = "MEU MraLiteStorage";
    private LsfIndex m_index;
    private Context m_context;
    String messageList[] = new String[256];
    private String messageListPart[][] = new String[4][256];
    int cntMsgByTheard[] = new int[4];
    private int numberOfMessage = 0;
    private int split;
    private int cntThread[] = new int[4];
    private boolean isThreadFinish[] = new boolean[4];

    public MraLiteStorage(Context context) {
        m_context = context;
        for(int i = 0; i < 4; i++){
            cntThread[i] = 0;
            cntMsgByTheard[i] = 0;
            isThreadFinish[i] = false;
        }
    }

    public void addMsg(IMCMessage message) {
    }

    public long getProcessStageValue(){
        return (cntThread[0] + cntThread[1] + cntThread[2] + cntThread[3]) + 1;
    }

    public int getNumberMessages(){
        return numberOfMessage;
    }

    public boolean isAllThreadFinish(){
        if(isThreadFinish[0] && isThreadFinish[1] && isThreadFinish[2] && isThreadFinish[3])
            return true;
        else
            return false;
    }

    public String[] getListOfMessages(){
        return messageList;
    }

    public void indexListOfMessage(LsfIndex index) {
        m_index = index;
        numberOfMessage = m_index.getNumberOfMessages();
        split = (numberOfMessage / 4);
        // Since reading log takes more time, let's run it on a separate threads.
        new Thread(new Runnable() {
            @Override
            public void run() {
                getPartOfListMessage(1, split, 1);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                getPartOfListMessage(split + 1, split * 2, 2);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                getPartOfListMessage(split * 2 + 1, split * 3, 3);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                getPartOfListMessage(split * 3 + 1, numberOfMessage - 1, 4);
            }
        }).start();

        //isThreadFinish[2] = true;
        //isThreadFinish[3] = true;
    }

    private void getPartOfListMessage(int startCnt, int ends, int idThread){
        int state_msg;
        cntThread[idThread - 1] = 0;
        isThreadFinish[idThread - 1] = false;
        boolean haveName;
        String msgName;
        for(state_msg = startCnt; state_msg <= ends; state_msg++){
            haveName = false;
            msgName = m_index.getMessage(state_msg).getAbbrev();
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
        Log.i(TAG, "Finish "+ idThread + " - "+ cntThread[idThread - 1] + "  cnt: " + cntMsgByTheard[idThread - 1]);
        isThreadFinish[idThread - 1] = true;
        if(isThreadFinish[0] && isThreadFinish[1] && isThreadFinish[2] && isThreadFinish[3])
            mergeListMessagePart();
    }

    private void mergeListMessagePart() {
        //TODO
        /*for(int i = 0; i < cntMsgByTheard[0]; i++)
            Log.i(TAG, messageListPart[0][i]);*/

        //messageList = Arrays.copyOf(messageListPart[0], cntMsgByTheard[0]);

        for(int i = 0; i < cntMsgByTheard[0]; i++)
            messageList[i] = messageListPart[0][i];

        int size = cntMsgByTheard[0];
        boolean haveImcMessage;

        try {

            for (int i = 0; i < cntMsgByTheard[1]; i++) {
                Log.i(TAG, "AQUI 1: "+i+" - "+cntMsgByTheard[1] + " S: "+size);
                haveImcMessage = false;
                for (int t = 0; t < size; t++) {
                    if (messageList[t].equals(messageListPart[1][i]))
                        haveImcMessage = true;
                }
                if (!haveImcMessage) {
                    size++;
                    messageList[size] = messageListPart[1][i];
                }
            }

            for (int i = 0; i < cntMsgByTheard[2]; i++) {
                Log.i(TAG, "AQUI 2: "+i+" - "+cntMsgByTheard[2] + " S: "+size);
                haveImcMessage = false;
                for (int t = 0; t < size; t++) {
                    if (messageList[t].equals(messageListPart[2][i]))
                        haveImcMessage = true;
                }
                if (!haveImcMessage) {
                    size++;
                    messageList[size] = messageListPart[2][i];
                }
            }

            for (int i = 0; i < cntMsgByTheard[3]; i++) {
                Log.i(TAG, "AQUI 3: "+i+" - "+cntMsgByTheard[3] + " S: "+size);
                haveImcMessage = false;
                for (int t = 0; t < size; t++) {
                    if (messageList[t].equals(messageListPart[3][i]))
                        haveImcMessage = true;
                }
                if (!haveImcMessage) {
                    size++;
                    messageList[size] = messageListPart[3][i];

                }
            }
        }catch (Exception io){
            Log.i(TAG, "ERROR: ",io);
        }

        Log.i(TAG, "SIZE FINAL: "+size);
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

