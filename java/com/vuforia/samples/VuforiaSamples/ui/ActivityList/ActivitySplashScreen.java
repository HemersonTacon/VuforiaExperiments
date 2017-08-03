/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.ui.ActivityList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.vuforia.samples.VuforiaSamples.R;


public class ActivitySplashScreen extends Activity
{
    
    private static long SPLASH_MILLIS = 450;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        //Necessario na inicializacao da atividade. Parece que sempre tem isso.
        super.onCreate(savedInstanceState);

        //Bem intuitivo, não mostra o titulo da janela. Precisa ser feito antes de settar o layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Coloca o app em fullscreen. Precisa ser feito antes de settar o layout
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Instancia o layout
        LayoutInflater inflater = LayoutInflater.from(this);
        RelativeLayout layout = (RelativeLayout) inflater.inflate(
            R.layout.splash_screen, null, false);

        //adiciona o layout na view
        addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));

        //necessario para criar o runnable
        final Handler handler = new Handler();

        //Mostra a splash screen por 450 ms definido pelo SPLASH_MILLIS
        /** Na verdade, a proxima atividade eh que vai ser iniciada apos o delay,
         mas como uma atividade sobrepoe a outra eh como se fosse isso mesmo dito no comentario acima */
        handler.postDelayed(new Runnable()
        {
            // @Override garante que o metodo abaixo sobrecarregue um existente.
            // Se não houver um metodo com o mesmo nome em uma das supercalsses, o compilador gera um erro
            @Override
            public void run()
            {
                //instancia um intent para chamar a proxima atividade
                Intent intent = new Intent(ActivitySplashScreen.this,
                    ActivityLauncher.class);
                //chama a proxima atividade
                startActivity(intent);
                
            }
            
        }, SPLASH_MILLIS);
    }
    
}
