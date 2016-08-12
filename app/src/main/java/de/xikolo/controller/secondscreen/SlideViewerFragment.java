package de.xikolo.controller.secondscreen;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.ScrollBar;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

import java.io.File;

import de.greenrobot.event.EventBus;
import de.xikolo.GlobalApplication;
import de.xikolo.R;
import de.xikolo.controller.dialogs.DownloadSlidesDialog;
import de.xikolo.controller.dialogs.ProgressDialog;
import de.xikolo.data.entities.Course;
import de.xikolo.data.entities.Item;
import de.xikolo.data.entities.Module;
import de.xikolo.data.entities.VideoItemDetail;
import de.xikolo.managers.SecondScreenManager;
import de.xikolo.model.DownloadModel;
import de.xikolo.model.events.DownloadCompletedEvent;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SlideViewerFragment extends Fragment implements OnLoadCompleteListener, OnPageChangeListener {

    public static final String TAG = SlideViewerFragment.class.getSimpleName();

    private PDFView pdfView;

    private DownloadModel downloadModel;

    private ProgressDialog progressDialog;

    private FloatingActionButton fab;
    private TextView textCurrentPage;

    private Course course;
    private Module module;
    private Item<VideoItemDetail> item;

    public static final String ARG_COURSE = "arg_course";
    public static final String ARG_MODULE = "arg_module";
    public static final String ARG_ITEM = "arg_item";

    private final static String KEY_CURRENT_PAGE = "current_page";

    private int currentPage;

    public SlideViewerFragment() {
        // Required empty public constructor
    }

    public static SlideViewerFragment newInstance(Course course, Module module, Item item) {
        SlideViewerFragment fragment = new SlideViewerFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_COURSE, course);
        args.putParcelable(ARG_MODULE, module);
        args.putParcelable(ARG_ITEM, item);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            course = getArguments().getParcelable(ARG_COURSE);
            module = getArguments().getParcelable(ARG_MODULE);
            item = getArguments().getParcelable(ARG_ITEM);
        }

        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE);
        } else {
            currentPage = 0;
        }

        downloadModel = new DownloadModel(GlobalApplication.getInstance().getJobManager(), getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slide_viewer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        textCurrentPage = (TextView) view.findViewById(R.id.text_current_page);

        pdfView = (PDFView) view.findViewById(R.id.pdf_view);

        ScrollBar scrollBar = (ScrollBar) view.findViewById(R.id.scroll_bar);
        pdfView.setScrollBar(scrollBar);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fab.hide();
                textCurrentPage.setVisibility(View.GONE);
                if (currentPage > 0) {
                    pdfView.jumpTo(currentPage);
                }
            }
        });

        if (downloadModel.downloadExists(DownloadModel.DownloadFileType.SLIDES, course, module, item)) {
            initSlidesViewer();
        } else {
            DownloadSlidesDialog dialog = DownloadSlidesDialog.getInstance();
            dialog.setListener(new DownloadSlidesDialog.DownloadSlidesDialogListener() {
                @Override
                public void onDialogPositiveClick() {
                    downloadModel.startDownload(item.detail.slides_url, DownloadModel.DownloadFileType.SLIDES, course, module, item);
                    progressDialog = ProgressDialog.getInstance();
                    progressDialog.show(getFragmentManager(), ProgressDialog.TAG);
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
        if (downloadModel != null && downloadModel.downloadExists(DownloadModel.DownloadFileType.SLIDES, course, module, item)) {
            File file = downloadModel.getDownloadFile(DownloadModel.DownloadFileType.SLIDES, course, module, item);
            pdfView.fromFile(file)
                    .swipeVertical(true)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .onPageChange(this)
                    .load();
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        if (currentPage > 0 && page != currentPage) {
            fab.show();
            textCurrentPage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void loadComplete(int nbPages) {
        if (currentPage > 0) {
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_CURRENT_PAGE, currentPage);
        super.onSaveInstanceState(outState);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(SecondScreenManager.SecondScreenUpdateVideoEvent event) {
        if (event.getItem().equals(item)) {
            if (event.getWebSocketMessage().payload().containsKey("slide_number")) {
                try {
                    int page = Integer.parseInt(event.getWebSocketMessage().payload().get("slide_number"));
                    if (pdfView != null) {
                        currentPage = page + 1;
                        if (currentPage != pdfView.getCurrentPage() && fab != null && !fab.isShown()) {
                            textCurrentPage.setText(String.format(getString(R.string.second_screen_pdf_pager), currentPage));
                            pdfView.jumpTo(currentPage);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't parse Integer from " + event.getWebSocketMessage().payload().get("slide_number") + ": " + e.getMessage());
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DownloadCompletedEvent event) {
        if (event.getDownload().localUri.contains(item.id)
                && DownloadModel.DownloadFileType.getDownloadFileTypeFromUri(event.getDownload().localUri) == DownloadModel.DownloadFileType.SLIDES) {
            if (progressDialog != null && progressDialog.getDialog().isShowing()) {
                progressDialog.getDialog().cancel();
            }
            initSlidesViewer();
        }
    }

}
