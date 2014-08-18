package de.xikolo.controller.module;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import de.xikolo.R;
import de.xikolo.entities.Course;
import de.xikolo.entities.Item;
import de.xikolo.entities.ItemText;
import de.xikolo.entities.Module;
import de.xikolo.model.ItemModel;
import de.xikolo.model.OnModelResponseListener;
import de.xikolo.util.NetworkUtil;

public class TextFragment extends PagerFragment<ItemText> {

    public static final String TAG = TextFragment.class.getSimpleName();

    private WebView mWebView;
    private ProgressBar mProgressBar;

    private ItemModel mItemModel;

    public TextFragment() {

    }

    public static PagerFragment newInstance(Course course, Module module, Item item) {
        return PagerFragment.newInstance(new TextFragment(), course, module, item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mItemModel = new ItemModel(getActivity(), jobManager);
        mItemModel.setRetrieveItemDetailListener(new OnModelResponseListener<Item>() {
            @Override
            public void onResponse(final Item response) {
                if (response != null) {
                    mItem = response;
                    displayBody();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_text, container, false);
        mWebView = (WebView) layout.findViewById(R.id.webView);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progress);

        final Activity activity = getActivity();

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100) {
                    mProgressBar.setVisibility(ProgressBar.VISIBLE);
                }
                if (progress == 100) {
                    mProgressBar.setVisibility(ProgressBar.GONE);
                }
            }

        });
        mWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, "An error occurred" + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                if (url.contains(Config.URI_HOST_HPI)) {
//                    view.loadUrl(url);
//                } else {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
//                }
                return true;
            }
        });

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (NetworkUtil.isOnline(getActivity())) {
            mItemModel.retrieveItemDetail(mCourse.id, mModule.id, mItem.id, Item.TYPE_TEXT, true);
        } else {
            NetworkUtil.showNoConnectionToast(getActivity());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
//            case R.id.action_refresh:
//                onRefresh();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void displayBody() {
        mWebView.loadData(mItem.object.body, "text/html", "charset=UTF-8");
    }

    @Override
    public void pageChanged() {

    }

    @Override
    public void pageScrolling(int state) {

    }

}