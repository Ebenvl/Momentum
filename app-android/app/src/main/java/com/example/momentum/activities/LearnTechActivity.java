package com.example.momentum.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

// Shows the LearnTech learning platform inside the app, so learners can do
// their courses without leaving it.
//
// The learner logs into LearnTech here the first time. The login is kept
// (like a browser remembering you), so after that they go straight in.
// Our app never sees or stores their LearnTech password.
public class LearnTechActivity extends BaseActivity {

    // The page to open, passed in by whoever opens this screen.
    public static final String EXTRA_URL = "url";

    // Pages on this domain stay in the app, everything else opens in the browser.
    private static final String LEARNTECH_DOMAIN = "learntech.co.za";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        // Courses are interactive e-learning packages, so they need JavaScript
        // and their own storage. Videos may start without an extra tap.
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Keeps the LearnTech login between visits. Course players often run in
        // a frame from another address, so those cookies are allowed too.
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        // LearnTech's own pages and course players open here. A link to any other
        // site opens in the phone's browser instead, so this screen only ever shows LearnTech.
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host != null && host.endsWith(LEARNTECH_DOMAIN)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(LearnTechActivity.this, "There's no app on this phone to open that link",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        // Lets videos and course players run properly.
        webView.setWebChromeClient(new WebChromeClient());

        String url = getIntent().getStringExtra(EXTRA_URL);
        webView.loadUrl(url != null ? url : "https://eduvos.learntech.co.za/");

        // Back goes back a page on LearnTech first, and only leaves the screen
        // when there are no pages left to go back to.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    // Saves the login to the phone straight away, so it's kept even if the app is closed.
    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
