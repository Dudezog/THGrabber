package com.nickherzog.thgrabber;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView article;
    private String userClipboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Initiate our widget
        article = (TextView) findViewById(R.id.displayArticle);
         //Make text view scrollable so user can see whole article
        article.setMovementMethod(new ScrollingMovementMethod());
        article.setText("Instructions:\n" +
                "Copy an article link from http://www.telegraphherald.com/ to your clipboard.\n" +
                "Open this app and the article will be displayed!\n" +
                "Enjoy!");


        final ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        //Initially check for a url on user's clipboard
        //Try/catch in case clipboard is empty.  That would produce a null pointer error
        try {
            userClipboard = clipboard.getText().toString();
            if (userClipboard.startsWith("http://www.telegraphherald.com")) {
                grabArticle(userClipboard);
                Toast.makeText(MainActivity.this, "TH Article Grabbed!", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            //Nothing on clipboard, Post the app instructions on the textview
            article.setText("Instructions:\n" +
                    "Copy an article link from http://www.telegraphherald.com/ to your clipboard.\n" +
                    "Open this app and the article will be displayed!\n" +
                    "Enjoy!");
        }
        //Listen for additional clipboard changes & update the app
        clipboard.addPrimaryClipChangedListener( new ClipboardManager.OnPrimaryClipChangedListener() {
            public void onPrimaryClipChanged() {
                userClipboard = clipboard.getText().toString();
                if(userClipboard.startsWith("http://www.telegraphherald.com"))
                {
                    grabArticle(userClipboard);
                    Toast.makeText(MainActivity.this, "TH Article Grabbed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void grabArticle(String url){
        //Start a volley request
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            try {
                                String wholeArticle = response.toString();

                                String begin = "<div itemprop=\"articleBody\" class=\"asset-content  subscriber-premium\">";
                                String end = "<div id=\"tncms-region-article_instory_bottom\" class=\"tncms-region hidden-print\">";

                                String trimmedArticle = wholeArticle.substring(wholeArticle.indexOf(begin), wholeArticle.indexOf(end));
                                article.setText(Html.fromHtml(trimmedArticle));
                                Log.d(TAG, trimmedArticle);
                            }catch (Exception ex){
                                ex.printStackTrace();
                                Toast.makeText(MainActivity.this,"Could not load article.  Please check your link.", Toast.LENGTH_SHORT).show();
                            }

                        }
                }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(MainActivity.this,"Sorry, could not load article at this time.", Toast.LENGTH_SHORT).show();
                }
                });
            queue.add(stringRequest);
    }

//Launch TH in user's browser
    public void goToTH(View view){
    Uri uri = Uri.parse("http://www.telegraphherald.com/");
    Intent i = new Intent(Intent.ACTION_VIEW, uri);
    startActivity(i);
    }


}

