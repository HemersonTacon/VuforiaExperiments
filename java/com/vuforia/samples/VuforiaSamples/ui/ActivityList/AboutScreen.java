/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.ui.ActivityList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.vuforia.samples.VuforiaSamples.R;


public class AboutScreen extends Activity implements OnClickListener
{
    private static final String LOGTAG = "AboutScreen";
    
    private WebView mAboutWebText;
    private Button mStartButton;
    private TextView mAboutTextTitle;
    private String mClassToLaunch;
    private String mClassToLaunchPackage;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //sem titulo, full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //setta layout
        setContentView(R.layout.about_screen);

        //bundle para pegar a infos vindas da atividade que iniciou esta atividade
        Bundle extras = getIntent().getExtras();
        String webText = extras.getString("ABOUT_TEXT");
        // recupera o nome do pacote que no caso eh com.vuforia.samples.VuforiaSamples
        mClassToLaunchPackage = getPackageName();
        // e concatena com o nome da atividade que sera iniciada, e.g. app.UserDefinedTargets.UserDefinedTargets
        mClassToLaunch = mClassToLaunchPackage + "."
            + extras.getString("ACTIVITY_TO_LAUNCH");

        // gera uma referencia para a view que contem um webView
        mAboutWebText = (WebView) findViewById(R.id.about_html_text);
        
        AboutWebViewClient aboutWebClient = new AboutWebViewClient();
        //usado para lidar com os clicks da webView
        mAboutWebText.setWebViewClient(aboutWebClient);
        
        String aboutText = " ";
        try
        {
            //le o html para colocar na webView
            InputStream is = getAssets().open(webText);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));
            String line;
            
            while ((line = reader.readLine()) != null)
            {
                aboutText += line;
            }
        } catch (IOException e)
        {
            Log.e(LOGTAG, "About html loading failed");
        }
        //carrega o html na webView
        mAboutWebText.loadData(aboutText, "text/html", "UTF-8");
        //referencia ao botao do layout e configura seu clickListener
        mStartButton = (Button) findViewById(R.id.button_start);
        mStartButton.setOnClickListener(this);
        //referencia o titulo do layout e setta ele de acordo com a info vinda da atividade anterior
        mAboutTextTitle = (TextView) findViewById(R.id.about_text_title);
        mAboutTextTitle.setText(extras.getString("ABOUT_TEXT_TITLE"));
        
    }
    
    
    // Starts the chosen activity
    private void startARActivity()
    {
        Intent i = new Intent();
        //settando a atividade que sera iniciada e em seguida chamando-a
        i.setClassName(mClassToLaunchPackage, mClassToLaunch);
        startActivity(i);
    }
    
    
    @Override
    public void onClick(View v)
    {
        //callback chamada quando algum botao dessa atividade eh clicado
        switch (v.getId())
        {
            //a verificacao de qual botao foi clicado eh feita atraves do id da view (neste caso a view sera o button)
            case R.id.button_start:
                startARActivity();
                break;
        }
    }

    //classe para lidar com os redirecionamentos de url dentro da webView
    private class AboutWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }
}
