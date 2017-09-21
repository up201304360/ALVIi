package pt.lsts.alvii;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;

/**
 * Created by pedro on 9/20/17.
 * LSTS - FEUP
 */

public class DisplayGraph extends AppCompatActivity {
    final Context context = this;
    private String TAG = "MEU DisplayGraph";
    private String path = Environment.getExternalStorageDirectory().getPath() + File.separator + "alvii";
    private File storageDir = new File(path);
    private LsfIndex m_index;
    private File source;
    private String imcMessageName;
    private String entityLabel[] = new String[1024];
    GraphView graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
                    storageDir.toString(), ""));
        }
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.mra_plot_graph);
        getSupportActionBar().hide();
        Bundle b = getIntent().getExtras();
        final String path = b.getString("BUNDLE_INDEX_PATH");
        imcMessageName  = b.getString("BUNDLE_IMCMESSAGE");
        source = new File(path);
        try {
            m_index = new LsfIndex(source, IMCDefinition.getInstance());
        } catch (Exception e) {
            Log.i(TAG, "" + e);
        }

        graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(false);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        getEntityInfo("EntityInfo");

        if(imcMessageName.equals("Rpm"))
            plotRpm();
        else if(imcMessageName.equals("SetThrusterActuation"))
            plotSetThrusterActuation();
        else if(imcMessageName.equals("EulerAngles"))
            plotEulerAngles();
        else {
            Toast.makeText(context, "Message not supported!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void plotEulerAngles() {
        graph.setTitle(imcMessageName);
        double value[][] = new double[4][320000];
        int cnt[] = new int[4];
        for(int i = 0; i < 4; i++)
            cnt[i] = 0;

        for (IMCMessage m : m_index.getIterator(imcMessageName)) {
            value[0][cnt[0]++] = m.getDouble("phi");
            value[1][cnt[1]++] = m.getDouble("theta");
            value[2][cnt[2]++] = m.getDouble("psi");
            value[3][cnt[3]++] = m.getDouble("psi_magnetic");
        }

        DataPoint[][] dataPoints0 = new DataPoint[4][cnt[0]];

        for(int i = 0; i < 4; i++){
            for (int t = 0; t < cnt[0]; t++)
                dataPoints0[i][t] = new DataPoint(t, value[0][t]);
        }

        LineGraphSeries<DataPoint> series[] = new LineGraphSeries[4];
        for(int i = 0; i < 4; i++)
            series[i] = new LineGraphSeries<DataPoint>(dataPoints0[i]);

        //graph.getViewport().setMinY(-0.15);
        //graph.getViewport().setMaxY(0.15);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(cnt[0]);
        series[0].setTitle("phi");
        series[1].setTitle("theta");
        series[0].setColor(Color.BLUE);
        series[1].setColor(Color.GREEN);
        graph.addSeries(series[0]);
        graph.addSeries(series[1]);

    }

    private void getEntityInfo(String msg) {
        for (IMCMessage m : m_index.getIterator(msg)) {
            entityLabel[m.getInteger("id")] = m.getString("label");
            //Log.i(TAG, "" + m.getInteger("id") + " - "+m.getString("label"));
        }
    }

    private void plotRpm() {

    }

    private void plotSetThrusterActuation() {
        double value[][] = new double[2][320000];
        boolean haveTwoMotors = false;
        int cnt0 = 0;
        int cnt1 = 0;
        graph.setTitle(imcMessageName);
        for (IMCMessage m : m_index.getIterator(imcMessageName)) {
            if(m.getInteger("id") != 0){
                haveTwoMotors = true;
                break;
            }
        }
        for (IMCMessage m : m_index.getIterator(imcMessageName)) {
            if(m.getInteger("id") == 0)
                value[0][cnt0++] = m.getDouble("value");
            else if(m.getInteger("id") == 1)
                value[1][cnt1++] = m.getDouble("value");
        }

        DataPoint[] dataPoints0 = new DataPoint[cnt0];
        for (int i = 0; i < cnt0; i++)
            dataPoints0[i] = new DataPoint(i, value[0][i]);

        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dataPoints0);
        //graph.getViewport().setMinY(-1.2);
        //graph.getViewport().setMaxY(1.2);
        graph.getViewport().setMinX(0);
        if(cnt0 >= cnt1)
            graph.getViewport().setMaxX(cnt0);
        else
            graph.getViewport().setMaxX(cnt1);

        series.setTitle("Id 0");
        series.setColor(Color.GREEN);
        graph.addSeries(series);

        if(haveTwoMotors) {
            DataPoint[] dataPoints1 = new DataPoint[cnt1];
            for (int i = 0; i < cnt1; i++)
                dataPoints1[i] = new DataPoint(i, value[1][i]);

            LineGraphSeries<DataPoint> series2 = new LineGraphSeries<DataPoint>(dataPoints1);
            series2.setTitle("Id 1");
            series2.setColor(Color.BLUE);
            graph.addSeries(series2);
        }

    }
}
