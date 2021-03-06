package de.xikolo.controllers.secondscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yatatsu.autobundle.AutoBundleField;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import de.xikolo.R;
import de.xikolo.controllers.base.BaseFragment;
import de.xikolo.controllers.dialogs.DownloadSlidesDialog;
import de.xikolo.controllers.dialogs.ProgressDialogIndeterminate;
import de.xikolo.controllers.dialogs.ProgressDialogIndeterminateAutoBundle;
import de.xikolo.events.DownloadCompletedEvent;
import de.xikolo.managers.DownloadManager;
import de.xikolo.managers.SecondScreenManager;
import de.xikolo.models.DownloadAsset;
import de.xikolo.models.Item;
import de.xikolo.models.Video;
import de.xikolo.models.dao.ItemDao;
import de.xikolo.models.dao.VideoDao;

public class SlideViewerFragment extends BaseFragment implements OnLoadCompleteListener, OnPageChangeListener {

    public static final String TAG = SlideViewerFragment.class.getSimpleName();

    private DownloadManager downloadManager;

    private ProgressDialogIndeterminate progressDialog;

    @BindView(R.id.pdf_view) PDFView pdfView;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.text_current_page) TextView textCurrentPage;

    @AutoBundleField String itemId;

    @AutoBundleField(required = false) int currentPage = -1;

    private DownloadAsset.Course.Item.Slides slides;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        downloadManager = new DownloadManager(getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slide_viewer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Item item = ItemDao.Unmanaged.find(itemId);
        Video video = VideoDao.Unmanaged.find(item.contentId);
        slides = new DownloadAsset.Course.Item.Slides(item, video);

        fab.setOnClickListener(v -> {
            fab.hide();
            textCurrentPage.setVisibility(View.GONE);
            if (currentPage >= 0) {
                pdfView.jumpTo(currentPage);
            }
        });

        if (downloadManager.downloadExists(slides)) {
            initSlidesViewer();
        } else {
            DownloadSlidesDialog dialog = new DownloadSlidesDialog();
            dialog.setListener(new DownloadSlidesDialog.Listener() {
                @Override
                public void onDialogPositiveClick() {
                    downloadManager.startAssetDownload(slides);
                    progressDialog = ProgressDialogIndeterminateAutoBundle.builder().build();
                    progressDialog.show(getFragmentManager(), ProgressDialogIndeterminate.TAG);
                }

                @Override
                public void onDialogNegativeClick() {
                    SlideViewerFragment.this.getActivity().finish();
                }
            });
            dialog.show(getFragmentManager(), DownloadSlidesDialog.TAG);
        }
    }

    private void initSlidesViewer() {
        if (downloadManager != null && downloadManager.downloadExists(slides)) {
            File file = downloadManager.getDownloadFile(slides);
            pdfView.fromFile(file)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .onPageChange(this)
                .scrollHandle(new DefaultScrollHandle(getActivity()))
                .load();
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        if (currentPage >= 0 && page != currentPage) {
            fab.show();
            textCurrentPage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void loadComplete(int nbPages) {
        if (currentPage >= 0) {
            pdfView.jumpTo(currentPage);
            if (fab != null) {
                fab.hide();
                textCurrentPage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSecondScreenUpdateVideoEvent(SecondScreenManager.SecondScreenUpdateVideoEvent event) {
        if (event.itemId.equals(itemId)) {
            if (event.webSocketMessage.payload.containsKey("slide_number")) {
                try {
                    int page = Integer.parseInt(event.webSocketMessage.payload.get("slide_number"));
                    if (pdfView != null) {
                        currentPage = page;
                        if (currentPage != pdfView.getCurrentPage() && fab != null && !fab.isShown()) {
                            textCurrentPage.setText(String.format(getString(R.string.second_screen_pdf_pager), currentPage + 1));
                            pdfView.jumpTo(currentPage);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't parse Integer from " + event.webSocketMessage.payload.get("slide_number") + ": " + e.getMessage());
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadCompletedEvent(DownloadCompletedEvent event) {
        if (event.url.equals(slides.getUrl())) {
            if (progressDialog != null && progressDialog.getDialog().isShowing()) {
                progressDialog.getDialog().cancel();
            }
            initSlidesViewer();
        }
    }

}
