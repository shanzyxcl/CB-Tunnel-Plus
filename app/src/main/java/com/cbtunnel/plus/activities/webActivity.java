package com.cbtunnel.plus.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.utils.util;

public class webActivity extends OpenVPNClientBase {
    private RotateAnimation ra = null;
    private SwipeRefreshLayout refreshWebView;
    private TextView web_title;
    private EditText search;
    private WebView webView;
    private static boolean isError = false;
    private int onDoubleClick = -1;
    private Context mContext;
    private ImageView mPoint,webImage,web_iv_close,web_iv_menu,btn_clear,btn_search;
    private ProgressBar progressBar1,progressBar2;
    public ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getConfig().getColorAccent());
        setContentView(R.layout.activity_web);
        mContext = webActivity.this;
        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        String configPanelUrl = bundle.getString("mConfigPanelRenew");
        if (!configPanelUrl.isEmpty()){
            getEditor().putString("savePrefUrl_key", configPanelUrl).apply();
        }
        initView();
        initWeb();
        scheduler.scheduleAtFixedRate(() -> runOnUiThread(() -> onDoubleClick = 0), 0, 1, TimeUnit.SECONDS);
    }

    private void hideProgrss(){
        hideKeyboard(search);
        btn_clear.setVisibility(search.length()>0? View.VISIBLE:View.GONE);
        btn_search.setVisibility(search.length()>0? View.VISIBLE:View.GONE);
        progressBar1.setVisibility(View.GONE);
        progressBar2.setVisibility(View.GONE);
        mPoint.setVisibility(View.GONE);
        mPoint.clearAnimation();
        if(ra!=null)ra.cancel();
        isError = (search.getText().toString().contains("asset/error_internet")||search.getText().toString().contains("asset/no_internet"));
    }

    private void showProgrss(){
        ra = new RotateAnimation(0,360, Animation.RELATIVE_TO_PARENT,0.37f,Animation.RELATIVE_TO_PARENT,0.37f);
        ra.setDuration(2000);
        ra.setRepeatCount(Animation.INFINITE);
        ra.setRepeatMode(Animation.RESTART);
        btn_clear.setVisibility(View.GONE);
        btn_search.setVisibility(View.GONE);
        progressBar1.setVisibility(View.VISIBLE);
        progressBar2.setVisibility(View.VISIBLE);
        mPoint.setVisibility(View.VISIBLE);
        mPoint.startAnimation(ra);
        isError = (search.getText().toString().contains("asset/error_internet")||search.getText().toString().contains("asset/no_internet"));
    }

    private int getProgressColor(int progress){
        if (progress == 10){
            return Color.parseColor("#ffd50000");
        }else if (progress == 20){
            return Color.parseColor("#ffb71c1c");
        } else if (progress == 30){
            return Color.parseColor("#ff2962ff");
        } else if (progress == 40){
            return Color.parseColor("#ff3e2723");
        } else if (progress == 50){
            return Color.parseColor("#ff304ffe");
        } else if (progress == 60){
            return Color.parseColor("#ffdd2c00");
        } else if (progress == 70){
            return Color.parseColor("#ffffd600");
        } else if (progress == 80){
            return Color.parseColor("#ff6200ea");
        } else if (progress == 90){
            return Color.parseColor("#ff0091ea");
        } else if (progress == 100){
            return Color.parseColor("#ff00c853");
        }
        return getConfig().gettextColor();
    }

    private boolean savePrefUrl(String url){
        if (url.contains("asset/error_internet")){
            return false;
        }else if (url.contains("asset/no_internet")){
            return false;
        }else if (url.isEmpty()){
            return false;
        }
        return true;
    }

    private void initView() {
        web_title = findViewById(R.id.web_title);
        search = findViewById(R.id.web_search);
        progressBar1 = findViewById(R.id.webviewProgressBar1);
        progressBar2 = findViewById(R.id.webviewProgressBar2);
        web_iv_close = findViewById(R.id.web_iv_close);
        web_iv_menu = findViewById(R.id.web_iv_menu);
        webImage = findViewById(R.id.web_iv);
        webView = findViewById(R.id.mWebview);
        refreshWebView = findViewById(R.id.swipeRefresh);
        mPoint = findViewById(R.id.progPoint);
        btn_clear = findViewById(R.id.btn_clear);
        btn_search = findViewById(R.id.btn_search);
        mPoint.setColorFilter(getConfig().getColorAccent(), PorterDuff.Mode.SRC_IN);
        web_title.setTextColor(getConfig().getAppThemeUtil()?Color.BLACK:Color.WHITE);
        web_title.setBackgroundColor(getConfig().getColorAccent());
        findViewById(R.id.toolbar_main_appbar).setBackgroundColor(getConfig().getColorAccent());
        findViewById(R.id.web_edit_ly).setBackgroundTintList(ColorStateList.valueOf(getConfig().getMainLayoutBG()));
        search.setOnEditorActionListener((v, actionId, event) -> {
            String input = search.getText().toString().replace(" ","").trim();
            if (savePrefUrl(input))getEditor().putString("savePrefUrl_key", input).apply();
            if(util.isNetworkAvailable(webActivity.this)){
                if (!isHttpUrl(input)) {
                    try {
                        input = URLEncoder.encode(input, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (input.equals("")){
                        input = "https://www.google.com";
                    } else if (input.contains(".")) {
                        input = "https://" + input;
                    }else{
                        input = "https://www.google.com/search?q=" + input;
                    }
                }
                webView.loadUrl(input);
            } else {
                progressBar1.setVisibility(View.GONE);
                progressBar2.setVisibility(View.GONE);
                search.setEnabled(true);
                webView.loadUrl("file:///android_asset/no_internet.html");
            }
            hideKeyboard(v);
            return false;
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btn_clear.setVisibility(search.length()>0? View.VISIBLE:View.GONE);
                btn_search.setVisibility(search.length()>0? View.VISIBLE:View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {
                String input = search.getText().toString().trim();
                if (savePrefUrl(input))getEditor().putString("savePrefUrl_key", input).apply();
            }
        });
        progressBar2.setProgress(0);
        progressBar2.setMax(100);
        refreshWebView.setOnRefreshListener(() -> {
            refreshWebView.setRefreshing(true);
            reloadWebView();
            refreshWebView.setRefreshing(false);
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading file...");
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
        });
        web_iv_close.setColorFilter(getConfig().getAppThemeUtil()?Color.BLACK:Color.WHITE, PorterDuff.Mode.SRC_IN);
        btn_clear.setColorFilter(getConfig().gettextColor(), PorterDuff.Mode.SRC_IN);
        btn_search.setColorFilter(getConfig().gettextColor(), PorterDuff.Mode.SRC_IN);
        web_iv_close.setOnClickListener(v -> webActivity.this.finish());
        web_iv_menu.setColorFilter(getConfig().getAppThemeUtil()?Color.BLACK:Color.WHITE, PorterDuff.Mode.SRC_IN);
        web_iv_menu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(webActivity.this, v);
            popup.getMenu().add(1, 1, 1, "Go Forward");
            popup.getMenu().add(2, 2, 2, "Go Back");
            popup.getMenu().add(3, 3, 3, "Reload");
            popup.getMenu().add(4, 4, 4, "Stop");
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        if (webView.canGoForward())webView.goForward();
                        break;
                    case 2:
                        if (webView.canGoBack())webView.goBack();
                        break;
                    case 3:
                        reloadWebView();
                        break;
                    case 4:
                        webView.stopLoading();
                        search.setEnabled(true);
                        progressBar1.setVisibility(View.GONE);
                        progressBar2.setVisibility(View.GONE);
                        btn_clear.setVisibility(search.length()>0? View.VISIBLE:View.GONE);
                        btn_search.setVisibility(search.length()>0? View.VISIBLE:View.GONE);
                        isError = (search.getText().toString().contains("asset/error_internet")||search.getText().toString().contains("asset/no_internet"));
                        break;
                }
                return true;
            });
            popup.show();
        });
        btn_clear.setOnClickListener(v -> {
            getEditor().putString("savePrefUrl_key", "").apply();
            search.getText().clear();
        });
        btn_search.setOnClickListener(v -> {
            String input = search.getText().toString().replace(" ","").trim();
            if (savePrefUrl(input))getEditor().putString("savePrefUrl_key", input).apply();
            if(util.isNetworkAvailable(webActivity.this)){
                if (!isHttpUrl(input)) {
                    try {
                        input = URLEncoder.encode(input, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (input.equals("")){
                        input = "https://www.google.com";
                    } else if (input.contains(".")) {
                        input = "https://" + input;
                    }else{
                        input = "https://www.google.com/search?q=" + input;
                    }
                }
                webView.loadUrl(input);
            } else {
                progressBar1.setVisibility(View.GONE);
                progressBar2.setVisibility(View.GONE);
                search.setEnabled(true);
                webView.loadUrl("file:///android_asset/no_internet.html");
            }
            hideKeyboard(v);
        });
    }

    private void reloadWebView(){
        if(isError)
            initWeb();
        else
            webView.reload();
        isError = false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWeb() {
        webView.setWebViewClient(new MkWebViewClient());
        webView.setWebChromeClient(new MkWebChromeClient());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString() + " HRLWEB2024/" + getVerName(mContext));
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setDefaultTextEncodingName("utf-8");
        settings.setDomStorageEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        String input = getPref().getString("savePrefUrl_key","").replace(" ","").trim();
        if(util.isNetworkAvailable(webActivity.this)){
            if (!isHttpUrl(input)) {
                try {
                    input = URLEncoder.encode(input, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (input.equals("")){
                    input = "https://www.google.com";
                } else if (input.contains(".")) {
                    input = "https://" + input;
                }else{
                    input = "https://www.google.com/search?q=" + input;
                }
            }
            webView.loadUrl(input);
        } else {
            progressBar1.setVisibility(View.GONE);
            progressBar2.setVisibility(View.GONE);
            search.setEnabled(true);
            webView.loadUrl("file:///android_asset/no_internet.html");
        }
        search.setText(input);
        hideKeyboard(search);
    }

    private class MkWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            isError = true;
            if(util.isNetworkAvailable(webActivity.this)){
                webView.loadUrl("file:///android_asset/error_internet.html");
            } else {
                webView.loadUrl("file:///android_asset/no_internet.html");
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            search.setText(url);
            setWebImage(view.getFavicon());
           if (url.contains("intent")) {
               try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        mContext.startActivity(intent);
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            else if (url.contains("mailto:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    mContext.startActivity(intent);
                    return true;
                }catch (Exception ignored){
                }
            }
            else if (url.contains("tel:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try{
                    mContext.startActivity(intent);
                    return true;
                }catch (Exception ignored){
                }
            }else if (url.contains("https://play.google.com/store/") || url.startsWith("market://")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    mContext.startActivity(intent);
                    System.out.println("Play Store!!");
                    return true;
                }catch(Exception ignored){
                }
            }else if (url.contains("https://maps.google.") || url.startsWith("intent://maps.google.")) {
                if (url.contains("intent://")) {
                    url = url.replace("intent://", "https://");
                    url = url.substring(0, url.indexOf("#Intent;"));
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    mContext.startActivity(intent);
                    return true;
                }catch(Exception ignored){
                }
            }else if (url.contains("youtube.com/")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    mContext.startActivity(intent);
                    return true;
                }catch(Exception ignored){
                }
            }else if (!url.startsWith("http://") && !url.startsWith("https://")){
                Intent intent = null;
                try {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                } catch (URISyntaxException ignored) {
                }
                if (intent != null) {
                    mContext.startActivity(intent);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            search.setText(url);
            setWebImage(view.getFavicon());
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            setWebImage(view.getFavicon());
            search.setText(url);
            web_title.setText(view.getTitle());
        }
    }

    private class MkWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int progress) {
            progressBar1.setIndeterminateTintList(ColorStateList.valueOf(getProgressColor(progress)));
            progressBar1.setProgressTintList(ColorStateList.valueOf(getProgressColor(progress)));
            progressBar2.setIndeterminateTintList(ColorStateList.valueOf(getProgressColor(progress)));
            progressBar2.setProgressTintList(ColorStateList.valueOf(getProgressColor(progress)));
            progressBar2.setSecondaryProgressTintList(ColorStateList.valueOf(getProgressColor(progress)));
            progressBar2.setProgress(progress);
            progressBar2.setSecondaryProgress(progress+5);
            web_title.setVisibility(Objects.equals(web_title.getText().toString(), "")?View.GONE:View.VISIBLE);
            if (progress==100||progress==0){
                search.setEnabled(true);
                hideProgrss();
            }else{
                search.setEnabled(false);
                showProgrss();
            }
            super.onProgressChanged(view, progress);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            setWebImage(icon);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            setWebImage(view.getFavicon());
            web_title.setText(title);
        }
    }

    @Override
    public void onBackPressed() {
        onDoubleClick++;
        if (onDoubleClick==2){
            onDoubleClick = 0;
            super.onBackPressed();
            webActivity.this.finish();
        } else if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    private boolean isHttpUrl(String urls) {
        boolean isUrl;
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))" + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";
        Pattern pat = Pattern.compile(regex.trim());
        Matcher mat = pat.matcher(urls.trim());
        isUrl = mat.matches();
        return isUrl;
    }

    private static String getVerName(Context context) {
        String verName = "unKnow";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }


    private void setWebImage(Bitmap icon){
        if (icon!=null){
            webImage.setImageBitmap(icon);
        }else{
            webImage.setImageResource(R.drawable.web_server);
        }
    }

    void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
