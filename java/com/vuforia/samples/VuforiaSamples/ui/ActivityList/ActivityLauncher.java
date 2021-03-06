/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2015 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/


package com.vuforia.samples.VuforiaSamples.ui.ActivityList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.vuforia.samples.VuforiaSamples.R;


// This activity starts activities which demonstrate the Vuforia features
public class ActivityLauncher extends ListActivity
{
    //lista de strings que serviram como nomes para os itens do menu,
    // basicamente o nome de cada outra atividade que sera chamada para demonstrar os exemplos com as capacidades da vuforia
    private String mActivities[] = { "Image Targets", "VuMark", "Cylinder Targets",
            "Multi Targets", "User Defined Targets", "Object Reco", "Cloud Reco",
            "Text Reco", "Virtual Buttons"};

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        //sempre tem em todas atividades
        super.onCreate(savedInstanceState);
        //esse adapter serve para preecher layout do tipo listview
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
            R.layout.activities_list_text_view, mActivities);
        //deixa a tela sem titulo e em fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //adiciona o layout na view
        //aparentemente, eh preciso criar um inflater e um layout apenas na primeira atividade,
        // e entao adicionar conteudos a view. Posteriormente, nas outras atividades,
        // apenas eh preciso settar o conteudo na view ao inves de criar o layout de novo (acho que eh isso, nao tenho certeza)
        setContentView(R.layout.activities_list);
        // usa esse adaptador ja com as strings referentes aos itens para preencher a lista que esta no layout
        setListAdapter(adapter);
    }
    
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        //callback para lidar com o click em cada item da lista
        //cria novo itent que chamara  a atividade que mostra a tela "sobre" de cada exemplo
        Intent intent = new Intent(this, AboutScreen.class);
        //pega a informacao do titulo do exemplo para passar para a proxima atividade
        intent.putExtra("ABOUT_TEXT_TITLE", mActivities[position]);

        //de acordo com o item clicado, eh passado o recurso html que contem a descricao do exemplo,
        // alem de qual atividade sera lancada a partir da proxima tela
        switch (position)
        {
            case 0:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.ImageTargets.ImageTargets");
                intent.putExtra("ABOUT_TEXT", "ImageTargets/IT_about.html");
                break;
            case 1:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                        "app.VuMark.VuMark");
                intent.putExtra("ABOUT_TEXT", "VuMark/VM_about.html");
                break;
            case 2:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.CylinderTargets.CylinderTargets");
                intent.putExtra("ABOUT_TEXT", "CylinderTargets/CY_about.html");
                break;
            case 3:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.MultiTargets.MultiTargets");
                intent.putExtra("ABOUT_TEXT", "MultiTargets/MT_about.html");
                break;
            case 4:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.UserDefinedTargets.UserDefinedTargets");
                intent.putExtra("ABOUT_TEXT",
                    "UserDefinedTargets/UD_about.html");
                break;
            case 5:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.ObjectRecognition.ObjectTargets");
                intent.putExtra("ABOUT_TEXT", "ObjectRecognition/OR_about.html");
                break;
            case 6:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.CloudRecognition.CloudReco");
                intent.putExtra("ABOUT_TEXT", "CloudReco/CR_about.html");
                break;
            case 7:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.TextRecognition.TextReco");
                intent.putExtra("ABOUT_TEXT", "TextReco/TR_about.html");
                break;
            case 8:
                intent.putExtra("ACTIVITY_TO_LAUNCH",
                    "app.VirtualButtons.VirtualButtons");
                intent.putExtra("ABOUT_TEXT", "VirtualButtons/VB_about.html");
                break;
        }
        
        startActivity(intent);
        
    }
}
