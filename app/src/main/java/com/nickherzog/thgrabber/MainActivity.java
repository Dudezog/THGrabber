package com.nickherzog.thgrabber;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.R.attr.tag;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView article;
    private String userClipboard;
    private ShareActionProvider mShareActionProvider;
    private Intent mShareIntent;
    private String trimmedArticle = "";
    private int i;
    private Spanned spannedValue;

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


        //Share action intent
        //Grab the article, see if there's an article or if it's our default instructions
        if (trimmedArticle.isEmpty()) {
            trimmedArticle = "No Article Grabbed!\n" + "Sent via TH Grabber";
        }

        mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.setType("text/plain");
        mShareIntent.putExtra(Intent.EXTRA_TEXT, trimmedArticle);

        final ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        //Initially check for a url on user's clipboard
        //Try/catch in case clipboard is empty.  That would produce a null pointer error
        try {
            userClipboard = clipboard.getText().toString();
            if (userClipboard.startsWith("http://www.telegraphherald.com")) {
                grabArticle(userClipboard);
                Toast.makeText(MainActivity.this, "TH Article Grabbed!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            //Nothing on clipboard, Post the app instructions on the textview
            article.setText("Instructions:\n" +
                    "Copy an article link from http://www.telegraphherald.com/ to your clipboard.\n" +
                    "Open this app and the article will be displayed!\n" +
                    "Enjoy!");
        }
        //Listen for additional clipboard changes & update the app
        clipboard.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            public void onPrimaryClipChanged() {
                userClipboard = clipboard.getText().toString();
                if (userClipboard.startsWith("http://www.telegraphherald.com")) {
                    grabArticle(userClipboard);
                    Toast.makeText(MainActivity.this, "TH Article Grabbed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate menu resource file
        getMenuInflater().inflate(R.menu.main, menu);

        //Locate Menu Item w Share Action Provider
        MenuItem item = menu.findItem(R.id.menu_share);

        //fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(mShareIntent);
        }

        return true;
    }


    public void grabArticle(String url) {
        //Start a volley request
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            //All articles begin with this tag
                            String begin = "<div itemprop=\"articleBody\" class=\"asset-content  subscriber-premium\">";
                            //And end with this tag
                            String end = "<div id=\"tncms-region-article_instory_bottom\" class=\"tncms-region hidden-print\">";
                            //Get all the text in-between those 2 tags
                            trimmedArticle = response.substring(response.indexOf(begin), response.indexOf(end));
                            //Set the text to the TextView as HTML code
                            article.setText(Html.fromHtml(trimmedArticle));
                            //Call to update the share intent so user can share article
                            //Add a tag for our app at the bottom of the article
                            mShareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(trimmedArticle).toString() + "\n Sent via TH Grabber");
                            Log.d(TAG, trimmedArticle);

                            //Grab images from article
                            //A lot of tags in the source code start with data-src.
                            //Add in '="h' to grab the image url tags
                            String imgStart = "data-src=\"h";
                            //This tag is thrown on the end of images url, so it's our endpoint
                            String imgEnd = "?resize";
                            final ArrayList<String> allOccurrences = new ArrayList<>();
                            //Do a search of the whole response to look for text in-between our start/end
                            //That's our image url
                            String regexString = Pattern.quote(imgStart) + "(.*?)" + Pattern.quote(imgEnd);
                            Pattern pattern = Pattern.compile(regexString);
                            Matcher matcher = pattern.matcher(response);
                            //Find each occurrence and save it to an array
                            while (matcher.find()) {
                                String imgURL = matcher.group(1);
                                //Add back in the 'h' in 'http'.  The h got cut off in the regex search
                                allOccurrences.add("h" + imgURL);
                            }
                            //If our occurrences array is not empty, we have images to display
                            if(!allOccurrences.isEmpty()) {
                                article.append(Html.fromHtml("<p><h3>Images from Article</h3><br>"));
                                //Print out all the occurrences.  Set them as html links
                                //User can click link & image will open in browser
                                for (i = 0; i < allOccurrences.size(); i++) {
                                    //Offset the 0 index
                                    int photoNum = i+1;
                                    Log.d("Images", allOccurrences.get(i));
                                    article.append(Html.fromHtml("<a href=\"" + allOccurrences.get(i) + "\">Image" + photoNum + "</a><br>"));
                                }
                                //Make links clickable
                                article.setMovementMethod(LinkMovementMethod.getInstance());
                            }

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Toast.makeText(MainActivity.this, "Could not load article.  Please check your link.", Toast.LENGTH_SHORT).show();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Sorry, could not load article at this time.", Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(stringRequest);
    }

    //Launch TH in user's browser
    public void goToTH(View view) {
        Uri uri = Uri.parse("http://www.telegraphherald.com/");
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }
}

