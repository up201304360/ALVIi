package pt.lsts.alvii;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.lsf.LsfIndex;

public class MRALite extends AppCompatActivity {

    private static final String TAG = "MEU MRALite";
    private File storageDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "alvii");
    final Context context = this;
    int state_msg = 0;

    /** LSF (LSTS Serialized Format) file extension */
    public static final String FILE_TYPE_LSF = "lsf";
    /** GZipped compressed LSF (LSTS Serialized Format) file extension */
    public static final String FILE_TYPE_LSF_COMPRESSED = "lsf.gz";
    /** BZipped2 compressed LSF (LSTS Serialized Format) file extension */
    public static final String FILE_TYPE_LSF_COMPRESSED_BZIP2 = "lsf.bz2";
    private InputStream activeInputStream = null;

    private ProgressDialog pDialog;
    private Handler updateBarHandler;
    private Handler customHandler;
    boolean checkIndex = false;
    MraLiteStorage m_mra_storage = new MraLiteStorage(context);
    private boolean alreadyConverted;

    //Run task periodically
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            if(checkIndex) {
                if (m_mra_storage.isAllThreadFinish()) {
                    Log.i(TAG, "Reading Log - Msg " + m_mra_storage.getProcessStageValue() + " of " + m_mra_storage.getNumberMessages());
                    Log.i(TAG, "number of messages: " + m_mra_storage.getNumberOfListMsg());
                    updateBarHandler.removeCallbacksAndMessages(null);
                    pDialog.cancel();
                    checkIndex = false;
                    diplayList();
                }
                else {
                    updateBarHandler.post(new Runnable() {
                        public void run() {
                            if (state_msg >= 0)
                                pDialog.setMessage("Reading Log - Msg " + m_mra_storage.getProcessStageValue() + " of " + m_mra_storage.getNumberMessages());
                        }
                    });
                }
            }
            customHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mralite);
        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
                    storageDir.toString(), ""));
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Bundle b = getIntent().getExtras();
        final String path = b.getString("BUNDLE_PATH");
        final String file_name = b.getString("BUNDLE_FILENAME");
        customHandler = new android.os.Handler();

        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Reading Log...");
        pDialog.setCancelable(false);
        pDialog.show();
        updateBarHandler =new Handler();
        final File logLsf = new File(path + File.separator + file_name);

        File file = new File(logLsf.getParent(), "indexMessageList.stackIndex");
        if(file.exists()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setTitle("Log Index");
            alertDialogBuilder
                    .setMessage("Log already Index!\nIndex again?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                FileUtils.forceDelete(new File(logLsf.getParent(), "indexMessageList.stackIndex"));
                            } catch (Exception e) {
                                Log.i(TAG, "Error while trying to delete mra/ folder", e);
                            }
                            startIndex(logLsf);
                            customHandler.postDelayed(updateTimerThread,2000);
                            checkIndex = true;
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            updateBarHandler.removeCallbacksAndMessages(null);
                            pDialog.cancel();
                            m_mra_storage.getListMessgeByOldIndex(logLsf);
                            Log.i(TAG, "Reading Log - Msg " + m_mra_storage.getProcessStageValue() + " of " + m_mra_storage.getNumberMessages());
                            Log.i(TAG, "number of messages: " + m_mra_storage.getNumberOfListMsg());
                            checkIndex = false;
                            diplayList();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else{
            customHandler.postDelayed(updateTimerThread,2000);
            checkIndex = true;
            startIndex(logLsf);
        }
    }

    private void startIndex(final File logLsf){
        // Since reading log takes more time, let's run it on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                openLog(logLsf);
            }
        }).start();
        //openLog(logLsf);
    }

    /**
     * Does the necessary pre-processing of a log file based on it's extension
     * Currently supports gzip, bzip2 and no-compression formats.
     * @param fx
     * @return True on success, False on failure
     */
    public boolean openLog(File fx) {
        File fileToOpen = null;
        updateBarHandler.post(new Runnable() {
            public void run() {
                pDialog.setMessage("Open Log (lsf) ...");
            }
        });

        if (fx.getName().toLowerCase().endsWith(FILE_TYPE_LSF_COMPRESSED)) {
            fileToOpen = extractGzip(fx);
        }
        else if (fx.getName().toLowerCase().endsWith(FILE_TYPE_LSF_COMPRESSED_BZIP2)) {
            fileToOpen = extractBzip2(fx);
        }
        else if (fx.getName().toLowerCase().endsWith(FILE_TYPE_LSF)) {
            fileToOpen = fx;
        }

        if (fileToOpen == null) {
            Log.i(TAG,"Invalid LSF file, LSF file does not exist!");
            return false;
        }

        return openLSF(fileToOpen);
    }

    // --- Extractors ---
    /**
     * Extract GNU zip files
     * @param f input file
     * @return decompressed file
     */
    private File extractGzip(File f) {
        GzipCompressorInputStream gzDataLog = null;
        try {
            //Log.i(TAG, "Decompressing LSF Data...");
            updateBarHandler.post(new Runnable() {
                public void run() {
                    pDialog.setMessage("Decompressing LSF Data...");
                }
            });
            gzDataLog = new GzipCompressorInputStream(new FileInputStream(f), true);
            activeInputStream = gzDataLog;
            File outputFile = new File(f.getParent(), "Data.lsf");
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            FilterCopyDataMonitor fis = createCopyMonitor(gzDataLog);
            StreamUtil.copyStreamToFile(fis, outputFile);

            File res = new File(f.getParent(), "Data.lsf");

            return res;
        }
        catch (Exception ioe) {
            Log.i(TAG, "Exception has been thrown Decompressing LSF Data..." + "   " + ioe);
            return null;
        }
        finally {
            if (gzDataLog != null) {
                try {
                    gzDataLog.close();
                }
                catch (IOException e) {
                    Log.i(TAG,""+e);
                }
            }
        }
    }

    /**
     * @param stream
     * @return
     */
    private FilterCopyDataMonitor createCopyMonitor(InputStream stream) {
        FilterCopyDataMonitor fis = new FilterCopyDataMonitor(stream) {
            long targetStep = 1 * 1024 * 1024;
            long target = targetStep;
            protected String decompressed = "Decompressed" + " ";

            @Override
            public void updateValueInMessagePanel() {
                if (downloadedSize > target) {
                    target += targetStep;
                }
            }
        };
        return fis;
    }

    /**
     * Extract BZip files with BZip2 compressor.
     * @param f
     * @return decompressed file
     */
    private File extractBzip2(File f) {
        //Log.i(TAG, "Decompressing BZip2 LSF Data...");
        updateBarHandler.post(new Runnable() {
            public void run() {
                pDialog.setMessage("Decompressing BZip2 LSF Data...");
            }
        });
        BZip2CompressorInputStream bz2DataLog = null;
        try {
            FileInputStream fxInStream = new FileInputStream(f);
            bz2DataLog = new BZip2CompressorInputStream(fxInStream, true);
            activeInputStream = bz2DataLog;
            File outFile = new File(f.getParent(), "Data.lsf");
            if (!outFile.exists()) {
                outFile.createNewFile();
            }

            FilterCopyDataMonitor fis = createCopyMonitor(bz2DataLog);
            StreamUtil.copyStreamToFile(fis, outFile);

            return outFile;
        }
        catch (Exception e) {
            Log.i(TAG, "Exception has been thrown: " + e + "  " + "Decompressing LSF Data..." + "   " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        finally {
            if (bz2DataLog != null) {
                try {
                    bz2DataLog.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Open LSF Log file
     * @param f
     * @return
     */
    private boolean openLSF(File f) {
        if (!f.exists()) {
            Log.i(TAG, "Invalid LSF file - LSF file does not exist!");
            return false;
        }

        //Log.i(TAG, "Loading LSF Data");
        updateBarHandler.post(new Runnable() {
            public void run() {
                pDialog.setMessage("Loading LSF Data...");
            }
        });

        final File lsfDir = f.getParentFile();
        alreadyConverted = false;
        if (lsfDir.isDirectory()) {
            if (new File(lsfDir, "mra/lsf.index").canRead())
                alreadyConverted = true;

        }
        else if (new File(lsfDir, "mra/lsf.index").canRead())
            alreadyConverted = true;

        if (alreadyConverted) {
            try {
                FileUtils.deleteDirectory(new File(lsfDir, "mra"));
            } catch (Exception e) {
                Log.i(TAG, "Error while trying to delete mra/ folder", e);
            }
        }

        try {
            //Log.i(TAG, "Indexing log");
            openLogSource(f);
            return true;
        }
        catch (Exception e) {
            Log.i(TAG, "Invalid LSF index "+ e.getMessage());
            return false;
        }
    }

    private void openLogSource(final File source) {
        m_mra_storage.initMraLiteStorage();
        updateBarHandler.post(new Runnable() {
            public void run() {
                pDialog.setMessage("Indexing Log...");
            }
        });
        //Log.i(TAG, ""+source.toString());
        LsfIndex index = null;
        try {
            index = new LsfIndex(source, IMCDefinition.getInstance());
        } catch (Exception e) {
            Log.i(TAG, "" + e);
            updateBarHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pDialog.cancel();
                }
            }, 0);
        }
        m_mra_storage.indexListOfMessage(index, source);
        checkIndex = true;
    }

    private void diplayList() {
        String[] listMessage = Arrays.copyOf(m_mra_storage.getListOfMessages(), m_mra_storage.getNumberOfListMsg());
        ListView listView = (ListView) findViewById(R.id.msg_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, listMessage);
        // Assign adapter to ListView
        listView.setAdapter(adapter);
    }
}
