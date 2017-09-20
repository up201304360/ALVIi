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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
                    storageDir.toString(), ""));
        }
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.mra_plot_graph);
        Bundle b = getIntent().getExtras();
        final String path = b.getString("BUNDLE_INDEX_PATH");
        imcMessageName  = b.getString("BUNDLE_IMCMESSAGE");
        source = new File(path);
        try {
            m_index = new LsfIndex(source, IMCDefinition.getInstance());
        } catch (Exception e) {
            Log.i(TAG, "" + e);
        }

        if(imcMessageName.equals("Rpm"))
            plotRpm();
        else if(imcMessageName.equals("SetThrusterActuation"))
            plotSetThrusterActuation();
        else {
            Toast.makeText(context, "Message not supported!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void plotRpm() {
        //boolean hasValue = null != defs.getType(imcMessagePlot).getFieldType("value");
        //boolean hasId = null != defs.getType(imcMessagePlot).getFieldType("id");
        //Log.i(TAG,imcMessagePlot+" value: "+hasValue+" ID: "+hasId);
    }

    private void plotSetThrusterActuation() {
        //boolean hasValue = null != defs.getType(imcMessagePlot).getFieldType("value");
        //boolean hasId = null != defs.getType(imcMessagePlot).getFieldType("id");
        //Log.i(TAG,imcMessagePlot+" value: "+hasValue+" ID: "+hasId);
        double value[][] = new double[2][32000];
        int cnt0 = 0;
        int cnt1 = 0;
        for (IMCMessage m : m_index.getIterator(imcMessageName)) {
            if(m.getInteger("id") == 0)
            {
                value[0][cnt0++] = m.getDouble("value");
                //Log.i(TAG, "id 0 - "+value[0][cnt0 - 1]);
            }
            else if(m.getInteger("id") == 1)
            {
                value[1][cnt1++] = m.getDouble("value");
                //Log.i(TAG, "id 1 - "+value[1][cnt1 - 1]);
            }
        }


        DataPoint[] dataPoints0 = new DataPoint[cnt0];
        for (int i = 0; i < cnt0; i++) {
            // add new DataPoint object to the array for each of your list entries
            dataPoints0[i] = new DataPoint(i, value[0][i]); // not sure but I think the second argument should be of type double
        }

        DataPoint[] dataPoints1 = new DataPoint[cnt1];
        for (int i = 0; i < cnt1; i++) {
            // add new DataPoint object to the array for each of your list entries
            dataPoints1[i] = new DataPoint(i, value[1][i]); // not sure but I think the second argument should be of type double
        }

        GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dataPoints0);
        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<DataPoint>(dataPoints1);
        // enable scaling and scrolling
        // set manual X bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1.2);
        graph.getViewport().setMaxY(1.2);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        if(cnt0 >= cnt1)
            graph.getViewport().setMaxX(cnt0);
        else
            graph.getViewport().setMaxX(cnt1);

        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);

        series.setTitle("Id 0");
        series.setColor(Color.GREEN);
        series2.setTitle("Id 1");
        series2.setColor(Color.BLUE);

        graph.addSeries(series);
        graph.addSeries(series2);

        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
    }
}
