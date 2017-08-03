/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.


Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.app.UserDefinedTargets;

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ImageTargetBuilder;
import com.vuforia.ObjectTracker;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.SampleApplicationControl;
import com.vuforia.samples.SampleApplication.SampleApplicationException;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.samples.SampleApplication.utils.SampleApplicationGLView;
import com.vuforia.samples.SampleApplication.utils.Texture;
import com.vuforia.samples.VuforiaSamples.R;
import com.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenu;
import com.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenuGroup;
import com.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenuInterface;


// The main activity for the UserDefinedTargets sample.
public class UserDefinedTargets extends Activity implements
    SampleApplicationControl, SampleAppMenuInterface
{
    private static final String LOGTAG = "UserDefinedTargets";

    //cria uma session usando a classe ja implementada
    private SampleApplicationSession vuforiaAppSession;
    
    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    
    // Our renderer:
    private UserDefinedTargetRenderer mRenderer;
    
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    
    // View overlays to be displayed in the Augmented View
    private RelativeLayout mUILayout;
    private View mBottomBar;
    private View mCameraButton;

    private float sceneSizeW = 320.0f;

    // Alert dialog for displaying SDK errors
    private AlertDialog mDialog;
    
    int targetBuilderCounter = 1;
    
    DataSet dataSetUserDef = null;
    
    private GestureDetector mGestureDetector;

    //menu que tem em todos exemplos do vuforia
    private SampleAppMenu mSampleAppMenu;
    //acredito que sejam os itens desse menu
    private ArrayList<View> mSettingsAdditionalViews;

    //valor inicial do extended tracking, tava false originalmente, passei pra true
    private boolean mExtendedTracking = true;
    
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
        this);
    
    RefFreeFrame refFreeFrame;
    
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;
    
    boolean mIsDroidDevice = false;
    
    
    // Called when the activity first starts or needs to be recreated after
    // resuming the application or a configuration change.
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        // inicia a sessao do app passando um applicationControl como parametro que, no caso,
        // eh esta classe uma que esta classe implementa a interface SampleApplicationControl
        vuforiaAppSession = new SampleApplicationSession(this);

        // Initializes Vuforia and sets up preferences.
        // como parametros precisa da activity e orientacao da tela
        // initAR() chama a tarefa InitVuforiaTask(), que realmente inicializa o vuforia,
        // que por sua vez chama doInitTrackers() e depois chama a tarefa LoadTrackerTask() que
        // por sua vez chama o metodo doLoadTrackersData(). Eventualmente em algum momento nesses
        // metodos, dependendo se as inicializacoes deram certo ou nao, o metodo onInitARDone()
        // eh chamado e este por sua vez chama o metodo initApplicationAR() que de fato inicializa
        // a aplicacao e chama o metodo addOverlayView() que setta o layout da view e depois
        // adiciona um GLView na view e por fim adiciona menu lateral na view e os itens do mesmo
        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Load any sample specific textures:
        mTextures = new Vector<Texture>();
        loadTextures();

        // cria um novo detector de gestos passando o contexto e a callback para lidar com os eventos de gestos
        // pelo que entendi do gestureListener, ele apenas lida com o singleTap na tela
        // para ativar o autofocus da camera
        mGestureDetector = new GestureDetector(this, new GestureListener());
        
        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
            "droid");
    }
    
    // Process Single Tap event to trigger autofocus
    // quando o usuario toca da tela o autofocus eh atividado
    private class GestureListener extends
        GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();
        
        
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }
        
        
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            // Generates a Handler to trigger autofocus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
                    
                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);
            
            return true;
        }
    }
    
    
    // We want to load specific textures from the APK, which we will later use
    // for rendering.
    // carrega as testura do teapot
    private void loadTextures()
    {
        // a leitura das texturas necessarias, que neste caso eh apenas a do teapot
        mTextures.add(Texture.loadTextureFromApk("TextureTeapotBlue.png",
            getAssets()));
    }
    
    
    // Called when the activity will start interacting with the user.
    // chamado quando a atividade volta do background
    @Override
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();
        
        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        
        try
        {
            // dentro desse metodo eh chamado o onResume() do Vuforia que usado na inicializacao do
            // app que, entre outras coisas, ativa o rendering. Dentro desse metodo tbm eh chamado
            // o metodo startAR() que ativa a camera e que chama o metodo doStartTrackers()
            // como esse metodo vai ser chamado quando o foco volta pro app, faz sentido inicializar as coisas de novo
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
        
    }
    
    
    // Called when the system is about to start resuming a previous activity.
    // pausa as coisas quando a atividade vai pra background
    @Override
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
        
        try
        {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
    }
    
    
    // The final call you receive before your activity is destroyed.
    // comentario acima diz tudo
    @Override
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        
        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Unload texture:
        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }
    
    
    // Callback for configuration changes the activity handles itself
    // calback chamada quando o tamanho da tela muda
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();
        
        // Removes the current layout and inflates a proper layout
        // for the new screen orientation
        
        if (mUILayout != null)
        {
            mUILayout.removeAllViews();
            ((ViewGroup) mUILayout.getParent()).removeView(mUILayout);
            
        }
        
        addOverlayView(false);
    }
    
    
    // Shows error message in a system dialog box
    // metodo responsavel por mostrar o alerta na tela quando o frame escolhido
    // como target tem baixa qualidade
    private void showErrorDialog()
    {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
        
        mDialog = new AlertDialog.Builder(UserDefinedTargets.this).create();
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        };
        
        mDialog.setButton(DialogInterface.BUTTON_POSITIVE,
            getString(R.string.button_OK), clickListener);
        
        mDialog.setTitle(getString(R.string.target_quality_error_title));
        
        String message = getString(R.string.target_quality_error_desc);
        
        // Show dialog box with error message:
        mDialog.setMessage(message);
        mDialog.show();
    }
    
    
    // Shows error message in a system dialog box on the UI thread
    // cria uma thread pra rodar o alerta de frame de baixa qualidade (metodo showErrorDialog)
    void showErrorDialogInUIThread()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                showErrorDialog();
            }
        });
    }
    
    
    // Initializes AR application components.
    // inicializa as classes necessarias para usar o OpenGL ES
    private void initApplicationAR()
    {
        // Do application initialization
        // classes que lidaram com a renderizacao (essa, RefFreeFrame, e a
        // RefFreeFrameGL que eh instanciada no construtor da RefFreeFrame)
        refFreeFrame = new RefFreeFrame(this, vuforiaAppSession);
        // carrega duas texturas que eu particularmente ainda nao vi serem usadas durante a execucao da aplicacao
        refFreeFrame.init();
        
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        // para usar OpenGLES eh necessario tem uma classe que extenda GLSurfaceView
        // que nesse caso eh a classe SampleApplicationGLView()
        // e outra que extenda GLSurfaceView.Renderer
        // que nesse caso eh a classe UserDefinedTargetRenderer()
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        // instancia que cuida da renderizacao
        mRenderer = new UserDefinedTargetRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
        // basicamente adiciona o layout na tela
        addOverlayView(true);
        
    }
    
    
    // Adds the Overlay view to the GLView
    // adiciona o layout que contem o botao da camera na tela
    private void addOverlayView(boolean initLayout)
    {
        // Inflates the Overlay Layout to be displayed above the Camera View
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(
            R.layout.camera_overlay_udt, null, false);
        
        mUILayout.setVisibility(View.VISIBLE);
        
        // If this is the first time that the application runs then the
        // uiLayout background is set to BLACK color, will be set to
        // transparent once the SDK is initialized and camera ready to draw
        if (initLayout)
        {
            mUILayout.setBackgroundColor(Color.BLACK);
        }
        
        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
        
        // Gets a reference to the bottom navigation bar
        mBottomBar = mUILayout.findViewById(R.id.bottom_bar);
        
        // Gets a reference to the Camera button
        mCameraButton = mUILayout.findViewById(R.id.camera_button);
        
        // Gets a reference to the loading dialog container
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_layout);
        // inicia o escaneamento da cena
        startUserDefinedTargets();
        // deixa o botao da camera visivel
        initializeBuildTargetModeViews();
        // traz o layout pra frente, que se resume a uma camada transparente e o botao da camera
        mUILayout.bringToFront();
    }
    
    
    // Button Camera clicked
    // metodo chamado quando o botao da camera eh clicado,
    // esse metodo foi configurado pra ser o onclick da camera atraves do layout
    public void onCameraClick(View v)
    {
        if (isUserDefinedTargetsRunning())
        {
            // Shows the loading dialog
            // mostra aquele anelzinho de loading
            loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            
            // Builds the new target
            startBuild();
        }
    }
    
    public void onPlusClick(View v){
        mRenderer.increaseSampling();
    }

    public void onMinusClick(View v){
        mRenderer.decreaseSampling();
    }

    public void onPlusClick2(View v){
        mRenderer.increaseSize();
    }

    public void onMinusClick2(View v){
        mRenderer.decreaseSize();
    }


    // Creates a texture given the filename
    Texture createTexture(String nName)
    {
        return Texture.loadTextureFromApk(nName, getAssets());
    }


    // Callback function called when the target creation finished
    // remove o anel de loading da tela e muda o status do refFreeFrame para idle
    void targetCreated()
    {
        // Hides the loading dialog
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        
        if (refFreeFrame != null)
        {
            refFreeFrame.reset();
        }
        
    }
    
//     mostra a barra inferior que contem o logo do vuforia e o botao da camera
    // Initialize views
    private void initializeBuildTargetModeViews()
    {
        // Shows the bottom bar
        mBottomBar.setVisibility(View.VISIBLE);
        mCameraButton.setVisibility(View.VISIBLE);
    }

    // processa os gestos feitos na tela
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Process the Gestures
        // se o menu ja esta criado processa o evento de toque dele que simplesmente
        // mostra ou esconde o menu lateral alem de processar os eventos de toque da propria view
        // processEvent retorna true se os eventos da view foram "lidados"
        if (mSampleAppMenu != null && mSampleAppMenu.processEvent(event))
            return true;
        //caso contrario os eventos da view sao lidados pelo gestureDetector desta classe
        return mGestureDetector.onTouchEvent(event);
    }
    
    // basicamente parece que este metodo inicia o processo de escaneamento da cena
    boolean startUserDefinedTargets()
    {
        Log.d(LOGTAG, "startUserDefinedTargets");
        
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager
            .getTracker(ObjectTracker.getClassType()));
        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                .getImageTargetBuilder();
            
            if (targetBuilder != null)
            {
                // if needed, stop the target builder
                if (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE)
                    targetBuilder.stopScan();
                
                objectTracker.stop();
                
                targetBuilder.startScan();
                
            }
        } else
            return false;
        
        return true;
    }
    
    // verifica se o tracker foi inicializado e se esta no modo de escaneamento
    boolean isUserDefinedTargetsRunning()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        
        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                .getImageTargetBuilder();
            if (targetBuilder != null)
            {
                Log.e(LOGTAG, "Quality> " + targetBuilder.getFrameQuality());
                return (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE) ? true
                    : false;
            }
        }
        
        return false;
    }
    
    // cria um trackable, chamado ao clicar no botao da camera
    void startBuild()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        
        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                .getImageTargetBuilder();
            if (targetBuilder != null)
            {
                if (targetBuilder.getFrameQuality() == ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_LOW)
                {
                    // se a qualidade do frame for baixa mostra o alerta informando isso
                     showErrorDialogInUIThread();
                }
                
                String name;
                do
                {
                    name = "UserTarget-" + targetBuilderCounter;
                    Log.d(LOGTAG, "TRYING " + name);
                    targetBuilderCounter++;
                    // enquanto nao conseguir um frame valido para criar um trackable,
                    // o segundo parametro altera o tamanho do objeto virtual de forma inversamente proporcional
                } while (!targetBuilder.build(name, sceneSizeW));

                // muda o status do refFreeFrame para criando
                refFreeFrame.setCreating();
            }
        }
    }

    // metodo chamado quando as dimensoes da tela sao alteradas
    void updateRendering()
    {
        // estrutura que contem informacoes da tela, como tamanho, densidade e escala
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // reinicializa as coisas do opengles necessarias na rederizacao e atualiza o tamanho do video
        refFreeFrame.initGL(metrics.widthPixels, metrics.heightPixels);
    }
    
    // tem alguns metodos que mexem com trackers aqui embaixo mas nenhum deles guarda
    // a referencia pros trackers criados, entao fiquei meio confuso quanto ao objetivo
    // deles, mas parece ser o seguinte: este metodo inicializa os trackers,
    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;
        
        // Initialize the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker tracker = trackerManager.initTracker(ObjectTracker
            .getClassType());
        if (tracker == null)
        {
            Log.d(LOGTAG, "Failed to initialize ObjectTracker.");
            result = false;
        } else
        {
            Log.d(LOGTAG, "Successfully initialized ObjectTracker.");
        }
        
        return result;
    }
    
    // este metodo cria uma image tracker apenas para criar o dataset de UDT
    @Override
    public boolean doLoadTrackersData()
    {
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            Log.d(
                LOGTAG,
                "Failed to load tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }
        
        // Create the data set:
        dataSetUserDef = objectTracker.createDataSet();
        if (dataSetUserDef == null)
        {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }
        
        if (!objectTracker.activateDataSet(dataSetUserDef))
        {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }
        
        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }
    
    // e por fim esse metodo "starta" um tracker
    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();
        
        return result;
    }
    
    // para os trackers
    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();
        
        return result;
    }
    
    // "descarrega" os dados dos trackers
    @Override
    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;
        
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            result = false;
            Log.d(
                LOGTAG,
                "Failed to destroy the tracking data set because the ObjectTracker has not been initialized.");
        }
        
        if (dataSetUserDef != null)
        {
            if (objectTracker.getActiveDataSet(0) != null
                && !objectTracker.deactivateDataSet(dataSetUserDef))
            {
                Log.d(
                    LOGTAG,
                    "Failed to destroy the tracking data set because the data set could not be deactivated.");
                result = false;
            }
            
            if (!objectTracker.destroyDataSet(dataSetUserDef))
            {
                Log.d(LOGTAG, "Failed to destroy the tracking data set.");
                result = false;
            }
            
            Log.d(LOGTAG, "Successfully destroyed the data set.");
            dataSetUserDef = null;
        }
        
        return result;
    }
    
    // "desinicializa" os trackers parando o escaneamento
    @Override
    public boolean doDeinitTrackers()
    {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;
        
        if (refFreeFrame != null)
            refFreeFrame.deInit();
        
        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());
        
        return result;
    }

    // chamado quando o processo de inicializacao do vuforia termina,
    // ativa a renderizacao, traz o layout pra frente, cria e configura o menu lateral
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        // se nao teve nenhuma excecao durante a  inicializacao
        // continua com a inicializacao das outras coisas
        // senao chama um alerta de erro que mostra a excecao ocorrida e encerra a atividade
        if (exception == null)
        {
            // faz as inicializacoes necessarias para o OpenGL ES e configura o layout da atividade
            initApplicationAR();

            // configura o modo de video, marca-o como ativo e ajusta a imagem da camera
            // de acordo com a razao de aspecto e orientacao da tela
            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();
            
            // Hides the Loading Dialog
            loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            
            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);
            
            try
            {
                // cuida da inicializacao da camera e do tracker
                // nao ficou bem claro o que a inicializacao do tracker faz
                // ativa a camera e chama o metodo doStartTrackers()
                vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (SampleApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }
            
            boolean result = CameraDevice.getInstance().setFocusMode(
                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);
            
            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");

            // coloca a bottombar do layout (que eh a parte que possui o botao da camera)
            // num vetor de views chamado mSettingsAdditionalViews
            setSampleAppMenuAdditionalViews();
            // configura o menu lateral que lidara com as configuracoes do exemplo escolhido,
            // como parametros recebe uma classe que implementa a SampleAppMenuInterface,
            // que neste caso eh esta, a atividade que tbm eh esta classe,
            // o titulo do menu, uma GLSurfaceView, um layout, e uma lista de views adicionais
            mSampleAppMenu = new SampleAppMenu(this, this,
                "User Defined Targets", mGlView, mUILayout,
                mSettingsAdditionalViews);
            // adiciona os itens ao menu lateral
            setSampleAppMenuSettings();
            
        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }
    
    
    // Shows initialization error messages as System dialogs
    // adicionalmente encerra a atividade quando ocorre um erro (chamada ao metodo finish() no botao OK)
    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }
                
                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                    UserDefinedTargets.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton(getString(R.string.button_OK),
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }

    // callback chamada a cada ciclo, aparentemente apenas verifica se
    // existe um novo trackable na cena(?) e o insere no dataset de trackables
    @Override
    public void onVuforiaUpdate(State state)
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
            .getTracker(ObjectTracker.getClassType());
        // se tiver um novo trackable
        if (refFreeFrame.hasNewTrackableSource())
        {
            Log.d(LOGTAG,
                "Attempting to transfer the trackable source to the dataset");
            
            // Deactivate current dataset
            objectTracker.deactivateDataSet(objectTracker.getActiveDataSet(0));
            
            // Clear the oldest target if the dataset is full or the dataset
            // already contains five user-defined targets.
            // aparentemente o maximo de UDTs eh fixado aqui como 5
            if (dataSetUserDef.hasReachedTrackableLimit()
                || dataSetUserDef.getNumTrackables() >= 5)
                dataSetUserDef.destroy(dataSetUserDef.getTrackable(0));
            
            if (mExtendedTracking && dataSetUserDef.getNumTrackables() > 0)
            {
                // We need to stop the extended tracking for the previous target
                // so we can enable it for the new one
                int previousCreatedTrackableIndex = 
                    dataSetUserDef.getNumTrackables() - 1;
                
                objectTracker.resetExtendedTracking();
                dataSetUserDef.getTrackable(previousCreatedTrackableIndex)
                    .stopExtendedTracking();
            }
            
            // Add new trackable source
            Trackable trackable = dataSetUserDef.createTrackable(refFreeFrame.getNewTrackableSource());
            
            // Reactivate current dataset
            objectTracker.activateDataSet(dataSetUserDef);
            
            if (mExtendedTracking)
            {
                trackable.startExtendedTracking();
            }
            
        }
    }

    final public static int CMD_BACK = -1;
    final public static int CMD_EXTENDED_TRACKING = 1;
    final public static int CMD_SELECTION_ITEM = 2;
    final public static int CMD_TEXT_ITEM = 3;
    final public static int CMD_RADIO_ITEM = 4;
    final public static int CMD_PLUS = 5;
    final public static int CMD_MINUS = 6;
    
    // This method sets the additional views to be moved along with the GLView
    // se eu adicionar mais elementos ao layout que aparece por cima da imagem do video da camera,
    // provavelmente preciso adicionar seus repectivos views como views adicionais tbm
    private void setSampleAppMenuAdditionalViews()
    {
        mSettingsAdditionalViews = new ArrayList<View>();
        mSettingsAdditionalViews.add(mBottomBar);
    }
    
    
    // This method sets the menu's settings
    // quandos os itens do menu sao criados tbm sao criadas callbacks de clickListener
    // pra elas que chamam o metodo menuProcess e depois escondem o menu
    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;
        
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);
        
        group = mSampleAppMenu.addGroup("", false);
        group.addSelectionItem(getString(R.string.menu_extended_tracking),
            CMD_EXTENDED_TRACKING, true);

        // os itens abaixo foram adicionados por mim apenas para fins de teste
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem("MenuTextItem", CMD_TEXT_ITEM);

        group = mSampleAppMenu.addGroup("", false);
        group.addSelectionItem("MenuSelectionItem",
                CMD_SELECTION_ITEM, true);

        group = mSampleAppMenu.addGroup("Radio Buttons", true);
        group.addRadioItem("MenurRadioItem1",
                CMD_RADIO_ITEM, true);
        group.addRadioItem("MenurRadioItem2",
                CMD_RADIO_ITEM, true);
        group.addRadioItem("MenurRadioItem3",
                CMD_RADIO_ITEM, true);

        group = mSampleAppMenu.addGroup("sceneSizeWidth", true);
        group.addTextItem("Plus (*2)", CMD_PLUS);
        group.addTextItem("Minus (/2)", CMD_MINUS);
        
        mSampleAppMenu.attachMenu();
    }
    
    // lida com os cliques do menu lateral
    @Override
    public boolean menuProcess(int command)
    {
        boolean result = true;
        
        switch (command)
        {
            case CMD_BACK:
                finish();
                break;
            
            case CMD_EXTENDED_TRACKING:
                if (dataSetUserDef.getNumTrackables() > 0)
                {
                    int lastTrackableCreatedIndex = 
                        dataSetUserDef.getNumTrackables() - 1;
                    
                    Trackable trackable = dataSetUserDef
                        .getTrackable(lastTrackableCreatedIndex);
                    
                    if (!mExtendedTracking)
                    {
                        if (!trackable.startExtendedTracking())
                        {
                            Log.e(LOGTAG,
                                "Failed to start extended tracking target");
                            result = false;
                        } else
                        {
                            Log.d(LOGTAG,
                                "Successfully started extended tracking target");
                        }
                    } else
                    {
                        if (!trackable.stopExtendedTracking())
                        {
                            Log.e(LOGTAG,
                                "Failed to stop extended tracking target");
                            result = false;
                        } else
                        {
                            Log.d(LOGTAG,
                                "Successfully stopped extended tracking target");
                        }
                    }
                }
                
                if (result)
                    mExtendedTracking = !mExtendedTracking;
                
                break;
            // os comandos abaixo foram adicionados por mim apenas para fins de teste
            /*
            case CMD_TEXT_ITEM:
                MshowDialog("MenuTextItem", "Testando item de texto do menu");
                break;

            case CMD_SELECTION_ITEM:
                MshowDialog("MenuSelectionItem", "Testando item de seleção do menu");
                break;

            case CMD_RADIO_ITEM:
                MshowDialog("MenuRadioItem", "Testando item de 'radio' do menu");
                break;
            */
            case CMD_PLUS:
                sceneSizeW = sceneSizeW < 1280.0f ? sceneSizeW*2 : sceneSizeW;
                break;

            case CMD_MINUS:
                sceneSizeW = sceneSizeW > 80.0f ? sceneSizeW/2 : sceneSizeW;
                break;
        }
        
        return result;
    }

    // metodo adicionado por mim apenas para fins de testte
    // esse metodo eh usado para mostrar alertas e eh chamado quando
    // os itens de teste do menu lateral sao clicados
    private void MshowDialog(String title, String message)
    {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();

        mDialog = new AlertDialog.Builder(UserDefinedTargets.this).create();
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        };

        mDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.button_OK), clickListener);

        mDialog.setTitle(title);

        //String message = getString(R.string.target_quality_error_desc);

        // Show dialog box with error message:
        mDialog.setMessage(message);
        mDialog.show();
    }
}
