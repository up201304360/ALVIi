package pt.lsts.alvii;

import android.content.Context;
import android.util.Log;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;

/**
 * Created by pedro on 9/20/17.
 * LSTS - FEUP
 */

class MraLiteDisplayPlot {
    private Context m_context;
    private static final String TAG = "MEU MraLiteStorage";
    private LsfIndex index;
    private String imcMessagePlot;
    private IMCDefinition defs;

    public MraLiteDisplayPlot(Context context) {
        m_context = context;
    }

    public void messageToDisplay(LsfIndex m_index, String imcMessage) {
        index = m_index;
        imcMessagePlot = imcMessage;
        /*IMCDefinition defs = m_index.getDefinitions();
        boolean hasValue = null != defs.getType(imcMessage).getFieldType("value");
        Log.i(TAG,imcMessage+" - "+hasValue);
        for (IMCMessage m : m_index.getIterator(imcMessage)) {
            //double val = m.getDouble("value");
            Log.i(TAG,"value: "+m.getDouble("value"));
        }*/
        if(imcMessage.equals("Rpm"))
            plotRpm();

        if(imcMessage.equals("SetThrusterActuation"))
            plotSetThrusterActuation();
    }

    private void plotSetThrusterActuation() {
        defs = index.getDefinitions();
        boolean hasValue = null != defs.getType(imcMessagePlot).getFieldType("value");
        boolean hasId = null != defs.getType(imcMessagePlot).getFieldType("id");
        Log.i(TAG,imcMessagePlot+" value: "+hasValue+" ID: "+hasId);
    }

    private void plotRpm() {
        defs = index.getDefinitions();
        boolean hasValue = null != defs.getType(imcMessagePlot).getFieldType("value");
        boolean hasId = null != defs.getType(imcMessagePlot).getFieldType("id");
        Log.i(TAG,imcMessagePlot+" value: "+hasValue+" ID: "+hasId);
    }
}
