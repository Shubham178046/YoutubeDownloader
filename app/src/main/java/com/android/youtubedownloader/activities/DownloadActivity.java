package com.android.youtubedownloader.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.youtubeextractor.VideoMeta;
import com.example.youtubeextractor.YouTubeExtractor;
import com.example.youtubeextractor.YtFile;
import com.android.youtubedownloader.R;
import com.android.youtubedownloader.Utility.Constant;
import com.android.youtubedownloader.adapter.DownloadAdapter;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


@SuppressLint("StaticFieldLeak")
public class DownloadActivity extends AppCompatActivity {
    public String ytLink = "https://www.youtube.com/watch?v=";
    private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 1000;
    BroadcastReceiver downloadReceiver;
    String idOfVideo;
    RecyclerView recyclerView;
    DownloadAdapter downloadAdapter;
    YouTubePlayerView youTubePlayerView;

    private static final String TAG = "DownloadActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        // Receive the id of teh video
        Intent intent = getIntent();
        idOfVideo = intent.getStringExtra(Constant.ID);
        Log.d(TAG, idOfVideo + "");

        playYoutubeVideo();

        setUpRecyclerView();

        showDownloadList();
    }

    private void setUpRecyclerView() {
        recyclerView = findViewById(R.id.downloadList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false).getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void playYoutubeVideo() {
        youTubePlayerView = findViewById(R.id.youtube_player_view);
        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(idOfVideo, 0);
            }
        });
    }


    private void showDownloadList() {
        new YouTubeExtractor(this) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {

                if (ytFiles != null) {

                    Set<String> uniqueFiles = new HashSet<String>();
                    ArrayList<YtFile> videoStreams = new ArrayList<YtFile>();

                    // Iterate over ytFiles
                    for (int i = 0, itag; i < ytFiles.size(); i++) {
                        itag = ytFiles.keyAt(i);
                        String format = ytFiles.get(itag).getFormat().getHeight() + ytFiles.get(itag).getFormat().getExt();
                        boolean isAdded = uniqueFiles.add(format);
                        if (isAdded) {
                            videoStreams.add(ytFiles.get(itag));
                        }
                    }

                    downloadAdapter = new DownloadAdapter(getBaseContext(), videoStreams, new DownloadAdapter.CallBack() {
                        @Override
                        public void onClickItem(String url) {
                            boolean granted = checkPermissionForWriteExternalStorage();
                            if (!granted) {
                                requestPermissionForWriteExternalStorage();
                            } else {
                                DownloadVideo(url, vMeta.getTitle(), ".mp4");
                               // DownloadManagingF(url, vMeta.getTitle(), ".mp4");
                                // download(url);
                            }
                        }
                    });
                    recyclerView.setAdapter(downloadAdapter);
                }
            }
        }.extract(ytLink + idOfVideo, true, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        youTubePlayerView.release();
    }

    public void DownloadVideo(String downloadURL, String videoTitle, String extentiondwn) {
        if (downloadURL != null) {
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/YouTubeDownloader");
            if (!storageDir.exists()) {
                storageDir.mkdir();
            }
            File file;
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
            file = new File(
                    storageDir.getPath() + "/" + videoTitle + extentiondwn
            );
            request.setTitle(
                    videoTitle + extentiondwn);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setVisibleInDownloadsUi(true);
            request.setDestinationUri(Uri.parse("file:" + file.getAbsolutePath()));
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            request.setMimeType("video/mp4");
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
            downloadManager.enqueue(request);
            downloadReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(referenceId);
                    Cursor cur = downloadManager.query(query);
                    if (cur.moveToFirst()) {
                        int columnIndex = cur.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == cur.getInt(columnIndex)) {
                            Toast.makeText(DownloadActivity.this, "Downloading Complate...", Toast.LENGTH_LONG).show();
                        } else if (DownloadManager.STATUS_FAILED == cur.getInt(columnIndex)) {
                            int columnReason = cur.getColumnIndex(DownloadManager.COLUMN_REASON);
                            int reason = cur.getInt(columnReason);
                            switch (reason) {
                                case DownloadManager.ERROR_FILE_ERROR: {
                                    Toast.makeText(DownloadActivity.this, "Download Failed.File is corrupt.", Toast.LENGTH_LONG).show();
                                }
                                case DownloadManager.ERROR_HTTP_DATA_ERROR: {
                                    Toast.makeText(DownloadActivity.this, "Download Failed.Http Error Found.", Toast.LENGTH_LONG).show();
                                }
                                case DownloadManager.ERROR_INSUFFICIENT_SPACE: {
                                    Toast.makeText(DownloadActivity.this, "Download Failed due to insufficient space in internal storage", Toast.LENGTH_LONG).show();
                                }
                                case DownloadManager.ERROR_UNHANDLED_HTTP_CODE: {
                                    Toast.makeText(DownloadActivity.this, "Download Failed. Http Code Error Found.", Toast.LENGTH_LONG).show();
                                }
                                case DownloadManager.ERROR_UNKNOWN: {
                                    Toast.makeText(DownloadActivity.this, "Download Failed.", Toast.LENGTH_LONG).show();
                                }
                                case DownloadManager.ERROR_CANNOT_RESUME: {
                                    Toast.makeText(DownloadActivity.this, "ERROR_CANNOT_RESUME", Toast.LENGTH_LONG).show();
                                }
                                case DownloadManager.ERROR_TOO_MANY_REDIRECTS: {
                                    Toast.makeText(DownloadActivity.this, "ERROR_TOO_MANY_REDIRECTS", Toast.LENGTH_LONG).show();
                                }
                                case DownloadManager.ERROR_DEVICE_NOT_FOUND: {
                                    Toast.makeText(DownloadActivity.this, "ERROR_DEVICE_NOT_FOUND", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            this.registerReceiver(downloadReceiver, filter);
        }
    }

    public void DownloadManagingF(String downloadURL, String videoTitle, String extentiondwn) {
        if (downloadURL != null) {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
            request.setTitle(videoTitle);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir("/Download/YouTubeDownloader/", videoTitle + extentiondwn);
            if (downloadManager != null) {
                Toast.makeText(getApplicationContext(), "Downloading...", Toast.LENGTH_SHORT).show();
                downloadManager.enqueue(request);
            }
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                public void onReceive(Context ctxt, Intent intent) {
                    Toast.makeText(getApplicationContext(), "Download Completed", Toast.LENGTH_SHORT).show();

                    Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/Download/YouTubeDownloader/");
                    Intent intentop = new Intent(Intent.ACTION_VIEW);
                    intentop.setDataAndType(selectedUri, "resource/folder");

                    if (intentop.resolveActivityInfo(getPackageManager(), 0) != null) {
                        startActivity(intentop);
                    } else {
                        Toast.makeText(getApplicationContext(), "Saved on: Download/YouTubeDownloader", Toast.LENGTH_LONG).show();
                    }
                    unregisterReceiver(this);
                    finish();
                }
            };
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        }
    }

    private String createRandomImageName() {
        return "video" + Math.random() + ".mp4";
    }

    public boolean checkPermissionForWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public void requestPermissionForWriteExternalStorage() {
        try {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
